package com.Group13_65CNTT.AutoGarage.notification.service;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.util.SafeFailureLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxWorker {
    private final OutboxService outboxService;
    private final NotificationDeliveryService deliveryService;
    private final NotificationProperties properties;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${garage.notification.poll-delay}")
    public void dispatch() {
        if (!properties.isWorkerEnabled()) return;
        try {
            dispatchBatch();
        } catch (RuntimeException failure) {
            SafeFailureLog.error(log, "Notification outbox job failed", failure);
        }
    }

    private void dispatchBatch() {
        String leaseId = UUID.randomUUID().toString();
        // Claim commits before provider I/O; a fresh lease id fences stale workers after recovery.
        for (var notice : outboxService.claim(leaseId)) {
            if (notice.getLockedUntil() == null
                    || !notice.getLockedUntil().isAfter(clock.instant())) continue;
            String providerReference;
            try {
                providerReference = deliveryService.deliver(notice);
            } catch (RuntimeException failed) {
                SafeFailureLog.error(
                        log, "Notification delivery failed: id=" + notice.getId(), failed);
                outboxService.fail(notice.getId(), leaseId);
                if (Thread.currentThread().isInterrupted()) return;
                continue;
            }
            outboxService.complete(notice.getId(), leaseId, providerReference);
        }
    }
}

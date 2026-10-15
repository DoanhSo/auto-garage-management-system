package com.Group13_65CNTT.AutoGarage.notification.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;
import com.Group13_65CNTT.AutoGarage.notification.repository.NotificationRepository;
import com.Group13_65CNTT.AutoGarage.notification.service.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {
    private final NotificationRepository repository;
    private final NotificationProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public List<Notification> claim(String workerId) {
        List<Notification> notices = repository.lockDue(clock.instant(), properties.getBatchSize());
        for (Notification notice : notices) {
            if (notice.getAttemptCount() >= properties.getMaxAttempts()) {
                notice.setStatus("FAILED");
                release(notice);
                continue;
            }
            notice.setStatus("PROCESSING");
            notice.setLockedBy(workerId);
            notice.setLockedUntil(clock.instant().plus(properties.getLease()));
            notice.setAttemptCount(notice.getAttemptCount() + 1);
        }
        repository.flush();
        return notices.stream()
                .filter(
                        n ->
                                "PROCESSING".equals(n.getStatus())
                                        && Objects.equals(n.getLockedBy(), workerId))
                .toList();
    }

    @Override
    @Transactional
    public void complete(Long id, String workerId, String providerReference) {
        repository
                .findLockedById(id)
                .filter(n -> ownsLease(n, workerId))
                .ifPresent(
                        notice -> {
                            notice.setStatus("SENT");
                            notice.setSentAt(clock.instant());
                            notice.setProviderReference(providerReference);
                            notice.setLastError(null);
                            release(notice);
                            repository.saveAndFlush(notice);
                        });
    }

    @Override
    @Transactional
    public void fail(Long id, String workerId) {
        repository
                .findLockedById(id)
                .filter(n -> ownsLease(n, workerId))
                .ifPresent(
                        notice -> {
                            notice.setStatus(
                                    notice.getAttemptCount() >= properties.getMaxAttempts()
                                            ? "FAILED"
                                            : "PENDING");
                            notice.setNextAttemptAt(
                                    clock.instant().plus(properties.getRetryDelay()));
                            notice.setLastError("Delivery failed; consult provider diagnostics");
                            release(notice);
                            repository.saveAndFlush(notice);
                        });
    }

    private boolean ownsLease(Notification notice, String workerId) {
        return "PROCESSING".equals(notice.getStatus())
                && Objects.equals(notice.getLockedBy(), workerId)
                && notice.getLockedUntil() != null
                && notice.getLockedUntil().isAfter(clock.instant());
    }

    private void release(Notification notice) {
        notice.setLockedBy(null);
        notice.setLockedUntil(null);
    }
}

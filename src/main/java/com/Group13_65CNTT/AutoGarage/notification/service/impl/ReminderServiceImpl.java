package com.Group13_65CNTT.AutoGarage.notification.service.impl;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.NotificationConstants;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.repository.ReminderRepository;
import com.Group13_65CNTT.AutoGarage.notification.repository.projection.ReminderCandidate;
import com.Group13_65CNTT.AutoGarage.notification.service.NotificationService;
import com.Group13_65CNTT.AutoGarage.notification.service.ReminderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderServiceImpl implements ReminderService {
    private final ReminderRepository repository;
    private final NotificationService notificationService;
    private final NotificationProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public int enqueueDueReminders() {
        var now = clock.instant();
        var until = now.plus(Duration.ofDays(properties.getReminderDays()));
        var candidates =
                Stream.concat(
                                repository
                                        .warrantyReminders(now, until, properties.getBatchSize())
                                        .stream(),
                                repository
                                        .maintenanceReminders(until, properties.getBatchSize())
                                        .stream())
                        .toList();
        for (ReminderCandidate candidate : candidates) {
            notificationService.enqueue(
                    NotificationCreateRequest.builder()
                            .userId(candidate.getUserId())
                            .customerId(candidate.getCustomerId())
                            .eventKey(candidate.getEventKey())
                            .channel(NotificationConstants.CHANNEL_WEB)
                            .destination(
                                    NotificationConstants.USER_RECIPIENT_PREFIX
                                            + candidate.getUserId())
                            .payloadJson(candidate.getPayloadJson())
                            .build());
        }
        return candidates.size();
    }
}

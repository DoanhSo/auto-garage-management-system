package com.Group13_65CNTT.AutoGarage.notification.service;

import com.Group13_65CNTT.AutoGarage.common.config.NotificationProperties;
import com.Group13_65CNTT.AutoGarage.common.util.SafeFailureLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderScheduler {
    private final ReminderService reminderService;
    private final NotificationProperties properties;

    @Scheduled(cron = "${garage.notification.reminder-cron}", zone = "${garage.notification.zone}")
    public void enqueueDueReminders() {
        if (!properties.isRemindersEnabled()) return;
        try {
            for (int batch = 0; batch < properties.getReminderMaxBatches(); batch++) {
                if (reminderService.enqueueDueReminders() < properties.getBatchSize()) break;
            }
        } catch (RuntimeException failure) {
            SafeFailureLog.error(log, "Reminder job failed", failure);
        }
    }
}

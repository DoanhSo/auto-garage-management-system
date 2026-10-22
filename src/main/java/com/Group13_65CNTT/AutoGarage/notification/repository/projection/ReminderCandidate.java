package com.Group13_65CNTT.AutoGarage.notification.repository.projection;

public interface ReminderCandidate {
    Long getUserId();

    Long getCustomerId();

    String getEventKey();

    String getPayloadJson();
}

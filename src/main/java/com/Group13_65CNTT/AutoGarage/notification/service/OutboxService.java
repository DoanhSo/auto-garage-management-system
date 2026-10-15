package com.Group13_65CNTT.AutoGarage.notification.service;

import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;

import java.util.List;

public interface OutboxService {
    List<Notification> claim(String workerId);

    void complete(Long id, String workerId, String providerReference);

    void fail(Long id, String workerId);
}

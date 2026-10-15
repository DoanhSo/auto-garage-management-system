package com.Group13_65CNTT.AutoGarage.notification.service;

import com.Group13_65CNTT.AutoGarage.notification.entity.Notification;

public interface NotificationDeliveryService {
    String deliver(Notification notification);
}

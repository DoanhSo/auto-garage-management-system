package com.Group13_65CNTT.AutoGarage.notification.service;

import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    NotificationResponse enqueue(NotificationCreateRequest request);

    Page<NotificationResponse> inbox(boolean unread, Pageable pageable);

    NotificationResponse getNotification(Long id);

    NotificationResponse markRead(Long id);
}

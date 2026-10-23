package com.Group13_65CNTT.AutoGarage.notification.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String channel;
    private String subject;
    private String payloadJson;
    private String status;
    private Instant readAt;
    private Instant createdAt;
}

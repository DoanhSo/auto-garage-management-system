package com.Group13_65CNTT.AutoGarage.notification.dto.request;

import com.Group13_65CNTT.AutoGarage.common.constant.NotificationConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {
    @Positive(message = "User ID must be positive")
    private Long userId;

    @Positive(message = "Customer ID must be positive")
    private Long customerId;

    @NotBlank(message = "Event key is required")
    @Size(
            max = NotificationConstants.EVENT_KEY_MAX_LENGTH,
            message = "Event key must not exceed 150 characters")
    private String eventKey;

    @NotBlank(message = "Channel is required")
    @Pattern(
            regexp = NotificationConstants.CHANNEL_PATTERN,
            message = "Channel must be WEB, EMAIL or SMS")
    private String channel;

    @NotBlank(message = "Destination is required")
    @Size(
            max = NotificationConstants.DESTINATION_MAX_LENGTH,
            message = "Destination must not exceed 254 characters")
    private String destination;

    @Size(
            max = NotificationConstants.SUBJECT_MAX_LENGTH,
            message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Payload is required")
    @Size(
            max = NotificationConstants.PAYLOAD_MAX_LENGTH,
            message = "Payload must not exceed 16384 characters")
    private String payloadJson;
}

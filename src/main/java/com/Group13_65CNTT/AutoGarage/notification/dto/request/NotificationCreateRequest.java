package com.Group13_65CNTT.AutoGarage.notification.dto.request;

import jakarta.validation.constraints.*;

import lombok.*;

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
    @Size(max = 150, message = "Event key must not exceed 150 characters")
    private String eventKey;

    @NotBlank(message = "Channel is required")
    @Pattern(regexp = "WEB|EMAIL|SMS", message = "Channel must be WEB, EMAIL or SMS")
    private String channel;

    @NotBlank(message = "Destination is required")
    @Size(max = 254, message = "Destination must not exceed 254 characters")
    private String destination;

    @Size(max = 255, message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Payload is required")
    @Size(max = 16384, message = "Payload must not exceed 16384 characters")
    private String payloadJson;
}

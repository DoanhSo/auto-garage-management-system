package com.Group13_65CNTT.AutoGarage.notification.dto.request;

import com.Group13_65CNTT.AutoGarage.common.constant.NotificationConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBroadcastRequest {
    private Role targetRole;

    private List<Role> targetRoles;

    private String channel;

    @Size(
            max = NotificationConstants.EVENT_KEY_MAX_LENGTH,
            message = "Event key must not exceed 150 characters")
    private String eventKey;

    @NotBlank(message = "Subject is required")
    @Size(
            max = NotificationConstants.SUBJECT_MAX_LENGTH,
            message = "Subject must not exceed 255 characters")
    private String subject;

    @NotBlank(message = "Payload JSON is required")
    @Size(
            max = NotificationConstants.PAYLOAD_MAX_LENGTH,
            message = "Payload JSON must not exceed 65536 characters")
    private String payloadJson;
}

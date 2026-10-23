package com.Group13_65CNTT.AutoGarage.notification.dto.response;

import com.Group13_65CNTT.AutoGarage.common.enums.Role;

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
public class NotificationBroadcastResponse {
    private String eventKey;
    private Role targetRole;
    private List<Role> targetRoles;
    private String channel;
    private String subject;
    private int recipientCount;
}

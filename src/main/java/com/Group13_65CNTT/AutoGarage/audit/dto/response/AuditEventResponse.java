package com.Group13_65CNTT.AutoGarage.audit.dto.response;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse {
    private Long id;
    private Long actorUserId;
    private String action;
    private String entityType;
    private Long entityId;
    private String afterJson;
    private Instant createdAt;
}

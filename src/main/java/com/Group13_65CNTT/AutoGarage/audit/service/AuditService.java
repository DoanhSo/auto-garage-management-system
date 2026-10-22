package com.Group13_65CNTT.AutoGarage.audit.service;

import java.util.Map;

public interface AuditService {
    void record(Long actorId, String action, Long entityId, Map<String, Object> details);

    void recordEvent(
            Long actorId,
            String action,
            String entityType,
            Long entityId,
            Map<String, Object> details);
}

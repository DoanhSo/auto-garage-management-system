package com.Group13_65CNTT.AutoGarage.audit.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.entity.AuditEvent;
import com.Group13_65CNTT.AutoGarage.audit.repository.AuditEventRepository;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final AuditEventRepository auditRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Long actorId, String action, Long entityId, Map<String, Object> details) {
        recordEvent(actorId, action, "USER", entityId, details);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEvent(
            Long actorId,
            String action,
            String entityType,
            Long entityId,
            Map<String, Object> details) {
        AuditEvent event =
                AuditEvent.builder()
                        .actorUserId(actorId)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .afterJson(objectMapper.writeValueAsString(details))
                        .build();
        auditRepository.saveAndFlush(event);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedLogin() {
        // A failed attempt is an independent event; successful mutations always share the caller
        // transaction.
        AuditEvent event =
                AuditEvent.builder()
                        .action("AUTH_LOGIN_FAILED")
                        .entityType("USER")
                        .afterJson("{}")
                        .build();
        auditRepository.saveAndFlush(event);
    }
}

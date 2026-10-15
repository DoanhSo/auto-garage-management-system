package com.Group13_65CNTT.AutoGarage.audit.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.entity.AuditEvent;
import com.Group13_65CNTT.AutoGarage.audit.repository.AuditEventRepository;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.DatabaseFailures;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.service.NotificationService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationAuditServiceImpl implements NotificationAudit {
    private final NotificationService notificationService;
    private final AuditEventRepository auditRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Notice notice) {
        if (notice == null || notice.channel() == null) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        notificationService.enqueue(
                NotificationCreateRequest.builder()
                        .userId(notice.userId())
                        .customerId(notice.customerId())
                        .eventKey(notice.eventKey())
                        .channel(notice.channel().name())
                        .destination(notice.destination())
                        .payloadJson(notice.payloadJson())
                        .build());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public String findCompletedRequest(String requestKey, String requestHash) {
        validateKey(requestKey, requestHash);
        long actorId = currentUser.userId();
        // Serializes retries by actor before the caller starts a mutation; lock is held to the
        // business commit.
        userRepository
                .findLockedById(actorId)
                .filter(u -> u.isActive())
                .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
        return auditRepository
                .findByRequestKey(requestKey)
                .map(
                        event -> {
                            if (!Objects.equals(event.getActorUserId(), actorId)
                                    || !Objects.equals(event.getRequestHash(), requestHash))
                                conflict();
                            return event.getResultJson();
                        })
                .orElse(null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordCompletedRequest(
            long actorUserId,
            String action,
            String entityType,
            long entityId,
            String requestKey,
            String requestHash,
            String resultJson) {
        validateKey(requestKey, requestHash);
        if (actorUserId != currentUser.userId())
            throw new BusinessException(ResponseCode.FORBIDDEN);
        if (action == null
                || action.isBlank()
                || action.length() > 100
                || entityType == null
                || entityType.isBlank()
                || entityType.length() > 80
                || entityId <= 0
                || resultJson == null
                || resultJson.length() > 65536) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        String canonical;
        try {
            var tree = objectMapper.readTree(resultJson);
            if (tree == null || tree.isMissingNode())
                throw new BusinessException(ResponseCode.BAD_REQUEST);
            canonical = objectMapper.writeValueAsString(tree);
        } catch (JacksonException invalid) {
            throw new BusinessException(ResponseCode.BAD_REQUEST, invalid);
        }
        String existing = findCompletedRequest(requestKey, requestHash);
        if (existing != null) return;
        AuditEvent event =
                AuditEvent.builder()
                        .actorUserId(actorUserId)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .requestKey(requestKey)
                        .requestHash(requestHash)
                        .resultJson(canonical)
                        .build();
        try {
            auditRepository.saveAndFlush(event);
        } catch (DataIntegrityViolationException failure) {
            if (DatabaseFailures.isDuplicateKey(failure)) {
                throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE, failure);
            }
            throw failure;
        }
    }

    private void validateKey(String key, String hash) {
        if (key == null
                || key.isBlank()
                || key.length() > 100
                || hash == null
                || !hash.matches("[a-f0-9]{64}")) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
    }

    private void conflict() {
        throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
    }
}

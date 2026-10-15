package com.Group13_65CNTT.AutoGarage.audit.aspect;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditService;
import com.Group13_65CNTT.AutoGarage.auth.dto.response.LoginResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;

import lombok.RequiredArgsConstructor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
@Order(10)
@RequiredArgsConstructor
public class AuditLogAspect {
    private final AuditService auditService;
    private final CurrentUserProvider currentUser;

    @Around("@annotation(audited)")
    public Object record(ProceedingJoinPoint invocation, Audited audited) throws Throwable {
        Object result;
        try {
            result = invocation.proceed();
        } catch (BusinessException failure) {
            if ("AUTH_LOGIN".equals(audited.action())
                    && failure.getResponseCode() == ResponseCode.BAD_CREDENTIALS) {
                auditService.recordFailedLogin();
            }
            throw failure;
        }
        if (audited.skipNullResult() && result == null) return null;
        Long actorId = currentUser.userIdOrNull();
        Long entityId = actorId;
        Map<String, Object> details = Map.of();
        if (result instanceof UserResponse user) {
            entityId = user.getId();
            details = Map.of("role", user.getRole().name(), "active", user.isActive());
        } else if (result instanceof LoginResponse login) {
            actorId = login.getUser().getId();
            entityId = actorId;
        } else if (result
                instanceof
                com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse
                        notification) {
            entityId = notification.getId();
        } else if (audited.idArgument() >= 0) {
            entityId = ((Number) invocation.getArgs()[audited.idArgument()]).longValue();
        }
        if (actorId == null && "ADMIN_BOOTSTRAPPED".equals(audited.action())) actorId = entityId;
        // Only explicit safe metadata is logged; never reflect or serialize request
        // arguments/results.
        if ("USER".equals(audited.entity()))
            auditService.record(actorId, audited.action(), entityId, details);
        else
            auditService.recordEvent(
                    actorId, audited.action(), audited.entity(), entityId, details);
        return result;
    }
}

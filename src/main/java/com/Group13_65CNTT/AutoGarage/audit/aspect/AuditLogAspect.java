package com.Group13_65CNTT.AutoGarage.audit.aspect;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditService;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.dto.response.UserResponse;
import com.Group13_65CNTT.AutoGarage.notification.dto.response.NotificationResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.OrderCommissionResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollItemResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodDetailResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.EmployeeResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ServiceBayResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;

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
        Object result = invocation.proceed();
        if (audited.skipNullResult() && result == null) return null;
        Long actorId = currentUser.userIdOrNull();
        // Account-level actions (e.g. logout everywhere) target the actor's own account; any other
        // entity must name its own id, otherwise it is left empty rather than pointing at the actor.
        Long entityId = AuditConstants.ENTITY_USER.equals(audited.entity()) ? actorId : null;
        Map<String, Object> details = Map.of();
        Long resultId = resultEntityId(result);
        if (resultId != null) {
            entityId = resultId;
        } else if (result instanceof UserResponse user) {
            entityId = user.getId();
            details =
                    Map.of(
                            AuditConstants.DETAIL_ROLE,
                            user.getRole().name(),
                            AuditConstants.DETAIL_ACTIVE,
                            user.isActive());
        } else if (result instanceof NotificationResponse notification) {
            entityId = notification.getId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.customer.dto.CustomerResponse response) {
            entityId = response.getId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse response) {
            entityId = response.getId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse response) {
            entityId = response.getId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.reception.dto.BookingResponse response) {
            entityId = response.getId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse response) {
            entityId = response.getRepairOrderId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse response) {
            entityId = response.getId();
        } else if (result instanceof com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse response) {
            entityId = response.getId();
        } else if (audited.idArgument() >= 0) {
            entityId = ((Number) invocation.getArgs()[audited.idArgument()]).longValue();
        }
        if (actorId == null && AuditConstants.ACTION_ADMIN_BOOTSTRAPPED.equals(audited.action()))
            actorId = entityId;
        // Only explicit safe metadata is logged; never reflect or serialize request
        // arguments/results.
        if (AuditConstants.ENTITY_USER.equals(audited.entity()))
            auditService.record(actorId, audited.action(), entityId, details);
        else
            auditService.recordEvent(
                    actorId, audited.action(), audited.entity(), entityId, details);
        return result;
    }

    /** Id of the record a service call created or changed, read from its typed response. */
    private static Long resultEntityId(Object result) {
        return switch (result) {
            case PayrollPeriodResponse period -> period.id();
            case PayrollPeriodDetailResponse detail ->
                    detail.period() == null ? null : detail.period().id();
            case PayrollItemResponse item -> item.id();
            case OrderCommissionResponse commission -> commission.id();
            case EmployeeResponse employee -> employee.getId();
            case ShiftResponse shift -> shift.getId();
            case ServiceBayResponse bay -> bay.getId();
            case BayReservationResponse reservation -> reservation.getId();
            case null, default -> null;
        };
    }
}

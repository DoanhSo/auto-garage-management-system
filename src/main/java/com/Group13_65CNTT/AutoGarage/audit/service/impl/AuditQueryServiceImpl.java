package com.Group13_65CNTT.AutoGarage.audit.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.mapper.AuditEventMapper;
import com.Group13_65CNTT.AutoGarage.audit.repository.AuditEventRepository;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditQueryService;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriod;
import com.Group13_65CNTT.AutoGarage.payroll.repository.PayrollPeriodRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.EmployeeRepository;
import com.Group13_65CNTT.AutoGarage.scheduling.repository.ServiceBayRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AuditQueryServiceImpl implements AuditQueryService {
    private final PaginationPolicy pagination;
    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final ServiceBayRepository serviceBayRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> searchEvents(Long actorId, String action, Pageable pageable) {
        pagination.validate(pageable);
        if ((actorId != null && actorId <= 0)
                || (action != null && action.length() > AuditConstants.ACTION_MAX_LENGTH)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        Page<AuditEventResponse> page =
                repository
                        .searchEvents(
                                actorId, action == null || action.isBlank() ? null : action, pageable)
                        .map(mapper::toResponse);
        describeUsers(page.getContent());
        describeEntities(page.getContent());
        return page;
    }

    private void describeUsers(List<AuditEventResponse> events) {
        Set<Long> userIds = new HashSet<>();
        for (AuditEventResponse event : events) {
            if (event.getActorUserId() != null) {
                userIds.add(event.getActorUserId());
            }
            if (AuditConstants.ENTITY_USER.equals(event.getEntityType())
                    && event.getEntityId() != null) {
                userIds.add(event.getEntityId());
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        Map<Long, User> users = new HashMap<>();
        userRepository.findAllById(userIds).forEach(user -> users.put(user.getId(), user));
        for (AuditEventResponse event : events) {
            User actor = users.get(event.getActorUserId());
            if (actor != null) {
                event.setActorFullName(actor.getFullName());
                event.setActorUsername(actor.getUsername());
                event.setActorRole(actor.getRole().name());
            }
            if (AuditConstants.ENTITY_USER.equals(event.getEntityType())) {
                User target = users.get(event.getEntityId());
                if (target != null) {
                    event.setEntityLabel(target.getFullName() + " (@" + target.getUsername() + ")");
                }
            }
        }
    }

    /** Gives the most common non-account targets a readable label; the client adds the type name. */
    private void describeEntities(List<AuditEventResponse> events) {
        Map<Long, Employee> employees =
                load(events, AuditConstants.ENTITY_EMPLOYEE, employeeRepository::findAllById, Employee::getId);
        Map<Long, PayrollPeriod> periods =
                load(
                        events,
                        AuditConstants.ENTITY_PAYROLL_PERIOD,
                        payrollPeriodRepository::findAllById,
                        PayrollPeriod::getId);
        Map<Long, ServiceBay> bays =
                load(
                        events,
                        AuditConstants.ENTITY_SERVICE_BAY,
                        serviceBayRepository::findAllById,
                        ServiceBay::getId);
        Map<Long, User> employeeUsers = new HashMap<>();
        Set<Long> employeeUserIds = new HashSet<>();
        employees.values().forEach(employee -> employeeUserIds.add(employee.getUserId()));
        if (!employeeUserIds.isEmpty()) {
            userRepository
                    .findAllById(employeeUserIds)
                    .forEach(user -> employeeUsers.put(user.getId(), user));
        }
        for (AuditEventResponse event : events) {
            Long id = event.getEntityId();
            if (id == null) continue;
            switch (event.getEntityType()) {
                case AuditConstants.ENTITY_EMPLOYEE -> {
                    Employee employee = employees.get(id);
                    if (employee != null) {
                        User user = employeeUsers.get(employee.getUserId());
                        event.setEntityLabel(
                                user == null
                                        ? employee.getEmployeeCode()
                                        : employee.getEmployeeCode()
                                                + AuditConstants.LABEL_CODE_SEPARATOR
                                                + user.getFullName());
                    }
                }
                case AuditConstants.ENTITY_PAYROLL_PERIOD -> {
                    PayrollPeriod period = periods.get(id);
                    if (period != null) event.setEntityLabel(monthLabel(period.getPeriodMonth()));
                }
                case AuditConstants.ENTITY_SERVICE_BAY -> {
                    ServiceBay bay = bays.get(id);
                    if (bay != null) {
                        event.setEntityLabel(
                                bay.getBayCode()
                                        + AuditConstants.LABEL_CODE_SEPARATOR
                                        + bay.getBayName());
                    }
                }
                default -> {
                    // Other entity types keep the "#id" fallback rendered by the client.
                }
            }
        }
    }

    private static <T> Map<Long, T> load(
            List<AuditEventResponse> events,
            String entityType,
            Function<Set<Long>, Iterable<T>> finder,
            Function<T, Long> idOf) {
        Set<Long> ids = new HashSet<>();
        for (AuditEventResponse event : events) {
            if (entityType.equals(event.getEntityType()) && event.getEntityId() != null) {
                ids.add(event.getEntityId());
            }
        }
        Map<Long, T> found = new HashMap<>();
        if (!ids.isEmpty()) finder.apply(ids).forEach(item -> found.put(idOf.apply(item), item));
        return found;
    }

    /** "2026-11" → "11/2026"; any other shape is shown as stored. */
    private static String monthLabel(String periodMonth) {
        if (periodMonth == null || periodMonth.length() != 7 || periodMonth.charAt(4) != '-') {
            return periodMonth;
        }
        return periodMonth.substring(5) + "/" + periodMonth.substring(0, 4);
    }
}

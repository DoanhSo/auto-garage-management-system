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

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AuditQueryServiceImpl implements AuditQueryService {
    private final PaginationPolicy pagination;
    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;
    private final UserRepository userRepository;

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
}

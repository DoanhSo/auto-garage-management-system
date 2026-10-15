package com.Group13_65CNTT.AutoGarage.audit.service.impl;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.mapper.AuditEventMapper;
import com.Group13_65CNTT.AutoGarage.audit.repository.AuditEventRepository;
import com.Group13_65CNTT.AutoGarage.audit.service.AuditQueryService;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
public class AuditQueryServiceImpl implements AuditQueryService {
    private final PaginationPolicy pagination;
    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditEventResponse> searchEvents(Long actorId, String action, Pageable pageable) {
        pagination.validate(pageable);
        if ((actorId != null && actorId <= 0) || (action != null && action.length() > 100)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        return repository
                .searchEvents(actorId, action == null || action.isBlank() ? null : action, pageable)
                .map(mapper::toResponse);
    }
}

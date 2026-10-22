package com.Group13_65CNTT.AutoGarage.audit.service;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditQueryService {
    Page<AuditEventResponse> searchEvents(Long actorId, String action, Pageable pageable);
}

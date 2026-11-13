package com.Group13_65CNTT.AutoGarage.audit.service;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;

import org.springframework.data.domain.Page;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AuditQueryService {
    /**
     * @param query free text matched against action code, entity type, entity id and actor name/username
     * @param actions extra action codes that also match the free text (e.g. resolved from Vietnamese labels)
     */
    Page<AuditEventResponse> searchEvents(
            Long actorId, String action, String query, List<String> actions, Pageable pageable);
}

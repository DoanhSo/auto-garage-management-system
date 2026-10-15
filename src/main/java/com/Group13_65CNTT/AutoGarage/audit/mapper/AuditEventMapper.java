package com.Group13_65CNTT.AutoGarage.audit.mapper;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.entity.AuditEvent;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditEventMapper {
    AuditEventResponse toResponse(AuditEvent event);
}

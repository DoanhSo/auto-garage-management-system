package com.Group13_65CNTT.AutoGarage.audit.mapper;

import com.Group13_65CNTT.AutoGarage.audit.dto.response.AuditEventResponse;
import com.Group13_65CNTT.AutoGarage.audit.entity.AuditEvent;

import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AuditEventMapper {
    @Mapping(target = "actorFullName", ignore = true)
    @Mapping(target = "actorUsername", ignore = true)
    @Mapping(target = "actorRole", ignore = true)
    @Mapping(target = "entityLabel", ignore = true)
    AuditEventResponse toResponse(AuditEvent event);
}

package com.Group13_65CNTT.AutoGarage.repair.mapper;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairTaskCreateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairTaskUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairTaskResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RepairTaskMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "repairOrderId", ignore = true)
    @Mapping(target = "estimateItemId", ignore = true)
    @Mapping(target = "taskType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "completionNote", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    RepairTask toEntity(RepairTaskCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "repairOrderId", ignore = true)
    @Mapping(target = "estimateItemId", ignore = true)
    @Mapping(target = "serviceId", ignore = true)
    @Mapping(target = "taskType", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(RepairTaskUpdateRequest request, @MappingTarget RepairTask entity);

    RepairTaskResponse toResponse(RepairTask entity);
}

package com.Group13_65CNTT.AutoGarage.repair.mapper;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.TaskAssignmentRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.TaskAssignmentResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskAssignment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TaskAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "taskId", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "assignedBy", ignore = true)
    @Mapping(target = "assignedAt", ignore = true)
    @Mapping(target = "releasedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    TaskAssignment toEntity(TaskAssignmentRequest request);

    @Mapping(target = "employeeName", ignore = true)
    TaskAssignmentResponse toResponse(TaskAssignment entity);
}

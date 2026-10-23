package com.Group13_65CNTT.AutoGarage.scheduling.mapper;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.EmployeeUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.EmployeeResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.Employee;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmployeeMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "employeeCode", source = "employeeCode")
    @Mapping(target = "skillsJson", source = "skillsJson")
    @Mapping(target = "canQc", source = "canQc")
    @Mapping(target = "hourlyCost", source = "hourlyCost")
    Employee toEntity(EmployeeCreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "employeeCode", source = "employeeCode")
    @Mapping(target = "skillsJson", source = "skillsJson")
    @Mapping(target = "canQc", source = "canQc")
    @Mapping(target = "hourlyCost", source = "hourlyCost")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "version", source = "version")
    EmployeeResponse toResponse(Employee entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "skillsJson", source = "skillsJson")
    @Mapping(target = "canQc", source = "canQc")
    @Mapping(target = "hourlyCost", source = "hourlyCost")
    @Mapping(target = "isActive", source = "isActive")
    void updateEntityFromRequest(EmployeeUpdateRequest request, @MappingTarget Employee entity);
}

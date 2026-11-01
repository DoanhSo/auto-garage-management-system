package com.Group13_65CNTT.AutoGarage.scheduling.mapper;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ShiftCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.EmployeeShift;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EmployeeShiftMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "employeeId", source = "employeeId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "shiftType", source = "shiftType")
    @Mapping(target = "serviceBayId", source = "serviceBayId")
    @Mapping(target = "notes", source = "notes")
    EmployeeShift toEntity(ShiftCreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "employeeId", source = "employeeId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "shiftType", source = "shiftType")
    @Mapping(target = "serviceBayId", source = "serviceBayId")
    @Mapping(target = "actualStartsAt", source = "actualStartsAt")
    @Mapping(target = "actualEndsAt", source = "actualEndsAt")
    @Mapping(target = "attendanceStatus", source = "attendanceStatus")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "registeredAt", source = "registeredAt")
    @Mapping(target = "approvedBy", source = "approvedBy")
    @Mapping(target = "rejectionReason", source = "rejectionReason")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "version", source = "version")
    ShiftResponse toResponse(EmployeeShift entity);
}

package com.Group13_65CNTT.AutoGarage.scheduling.mapper;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayCreateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.request.ServiceBayUpdateRequest;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ServiceBayResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ServiceBay;
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
public interface ServiceBayMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "bayCode", source = "bayCode")
    @Mapping(target = "bayName", source = "bayName")
    @Mapping(target = "bayType", source = "bayType")
    @Mapping(target = "description", source = "description")
    ServiceBay toEntity(ServiceBayCreateRequest request);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "bayCode", source = "bayCode")
    @Mapping(target = "bayName", source = "bayName")
    @Mapping(target = "bayType", source = "bayType")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "version", source = "version")
    ServiceBayResponse toResponse(ServiceBay entity);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "bayName", source = "bayName")
    @Mapping(target = "bayType", source = "bayType")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isActive", source = "isActive")
    void updateEntityFromRequest(ServiceBayUpdateRequest request, @MappingTarget ServiceBay entity);
}

package com.Group13_65CNTT.AutoGarage.repair.mapper;

import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderCreateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.RepairOrderUpdateRequest;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.RepairOrderResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface RepairOrderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "odometerOut", ignore = true)
    @Mapping(target = "diagnosis", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "latestQcCheckId", ignore = true)
    @Mapping(target = "handedOverBy", ignore = true)
    @Mapping(target = "handedOverAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    RepairOrder toEntity(RepairOrderCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orderCode", ignore = true)
    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "vehicleId", ignore = true)
    @Mapping(target = "orderType", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "odometerIn", ignore = true)
    @Mapping(target = "odometerOut", ignore = true)
    @Mapping(target = "receivedBy", ignore = true)
    @Mapping(target = "receivedAt", ignore = true)
    @Mapping(target = "latestQcCheckId", ignore = true)
    @Mapping(target = "handedOverBy", ignore = true)
    @Mapping(target = "handedOverAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromRequest(RepairOrderUpdateRequest request, @MappingTarget RepairOrder entity);

    RepairOrderResponse toResponse(RepairOrder entity);
}

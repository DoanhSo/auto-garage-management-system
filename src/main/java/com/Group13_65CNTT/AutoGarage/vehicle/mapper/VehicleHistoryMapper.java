package com.Group13_65CNTT.AutoGarage.vehicle.mapper;

import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleHistoryEntryDto;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.ERROR)
public interface VehicleHistoryMapper {
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "repairOrderId", source = "id")
    @Mapping(target = "orderCode", source = "orderCode")
    @Mapping(target = "receivedAt", source = "receivedAt")
    @Mapping(target = "completedAt", source = "closedAt")
    @Mapping(target = "odometerIn", source = "odometerIn")
    @Mapping(target = "diagnosis", source = "diagnosis")
    @Mapping(target = "status", source = "status")
    VehicleHistoryEntryDto toResponse(RepairOrder order);
}

package com.Group13_65CNTT.AutoGarage.scheduling.mapper;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayReservationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.BayReservation;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BayReservationMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "bayId", source = "bayId")
    @Mapping(target = "bookingId", source = "bookingId")
    @Mapping(target = "repairOrderId", source = "repairOrderId")
    @Mapping(target = "reservationType", source = "reservationType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "expiresAt", source = "expiresAt")
    @Mapping(target = "releasedAt", source = "releasedAt")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "version", source = "version")
    BayReservationResponse toResponse(BayReservation entity);
}

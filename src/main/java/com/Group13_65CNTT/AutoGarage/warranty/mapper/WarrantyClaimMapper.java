package com.Group13_65CNTT.AutoGarage.warranty.mapper;

import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyClaimResponse;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyClaim;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarrantyClaimMapper {

    WarrantyClaimResponse toResponse(WarrantyClaim entity);
}

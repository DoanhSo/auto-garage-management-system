package com.Group13_65CNTT.AutoGarage.quality.mapper;

import com.Group13_65CNTT.AutoGarage.quality.dto.response.QualityCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface QualityCheckMapper {

    QualityCheckResponse toResponse(VehicleCheck entity);
}

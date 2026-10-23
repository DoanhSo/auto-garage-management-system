package com.Group13_65CNTT.AutoGarage.estimate.mapper;

import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateItemCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateItemResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EstimateItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estimateId", ignore = true)
    @Mapping(target = "lineNo", ignore = true)
    @Mapping(target = "unitCost", ignore = true)
    @Mapping(target = "lineTotal", ignore = true)
    @Mapping(target = "approvalStatus", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "decidedAt", ignore = true)
    @Mapping(target = "decisionNote", ignore = true)
    @Mapping(target = "reservedQuantity", ignore = true)
    @Mapping(target = "warrantyDays", ignore = true)
    @Mapping(target = "warrantyKm", ignore = true)
    @Mapping(target = "warrantyTerms", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    EstimateItem toEntity(EstimateItemCreateRequest request);

    EstimateItemResponse toResponse(EstimateItem entity);
}

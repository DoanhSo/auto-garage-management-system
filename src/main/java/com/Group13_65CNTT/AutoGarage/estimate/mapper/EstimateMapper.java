package com.Group13_65CNTT.AutoGarage.estimate.mapper;

import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateItemResponse;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EstimateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estimateCode", ignore = true)
    @Mapping(target = "versionNo", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "discountAmount", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Estimate toEntity(EstimateCreateRequest request);

    @Mapping(target = "items", ignore = true)
    EstimateResponse toResponse(Estimate entity);

    /** An estimate still awaiting the customer past its validity date can no longer be decided. */
    @org.mapstruct.AfterMapping
    default void markTimeExpired(Estimate entity, @org.mapstruct.MappingTarget EstimateResponse response) {
        boolean awaiting = entity.getStatus() == com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.SENT;
        if (awaiting && entity.getValidUntil() != null && !java.time.Instant.now().isBefore(entity.getValidUntil())) {
            response.setStatus(com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus.EXPIRED);
        }
    }

    default EstimateResponse toResponseWithItems(Estimate entity, List<EstimateItemResponse> items) {
        EstimateResponse res = toResponse(entity);
        if (res != null) {
            res.setItems(items);
        }
        return res;
    }
}

package com.Group13_65CNTT.AutoGarage.warranty.mapper;

import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyResponse;
import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.Instant;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WarrantyMapper {

    WarrantyResponse toResponse(Warranty entity);

    /** The stored status only flips on a claim attempt, so derive time-based expiry when reading. */
    @AfterMapping
    default void markTimeExpired(Warranty entity, @MappingTarget WarrantyResponse response) {
        if (response.getStatus() == WarrantyStatus.ACTIVE
                && entity.getExpiresAt() != null
                && !Instant.now().isBefore(entity.getExpiresAt())) {
            response.setStatus(WarrantyStatus.EXPIRED);
        }
    }
}

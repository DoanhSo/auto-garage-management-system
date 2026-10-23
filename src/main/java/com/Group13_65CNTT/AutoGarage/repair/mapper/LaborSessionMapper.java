package com.Group13_65CNTT.AutoGarage.repair.mapper;

import com.Group13_65CNTT.AutoGarage.repair.dto.response.LaborSessionResponse;
import com.Group13_65CNTT.AutoGarage.repair.entity.LaborSession;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.Duration;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LaborSessionMapper {

    @Mapping(target = "durationSeconds", expression = "java(calcDuration(entity))")
    LaborSessionResponse toResponse(LaborSession entity);

    default Long calcDuration(LaborSession entity) {
        if (entity == null || entity.getStartsAt() == null || entity.getEndsAt() == null) {
            return 0L;
        }
        return Duration.between(entity.getStartsAt(), entity.getEndsAt()).getSeconds();
    }
}

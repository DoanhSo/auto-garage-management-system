package com.Group13_65CNTT.AutoGarage.catalog.mapper;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.dto.UpdateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import org.springframework.stereotype.Component;

@Component
public class ServiceMapper {

    public ServiceEntity toEntity(CreateServiceRequest request) {
        if (request == null) {
            return null;
        }
        ServiceEntity entity = new ServiceEntity();
        entity.setServiceCode(request.getServiceCode());
        entity.setServiceName(request.getServiceName().trim());
        entity.setServiceType(request.getServiceType());
        entity.setDescription(request.getDescription());
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            entity.setUnit(request.getUnit().trim());
        }
        entity.setUnitPrice(request.getUnitPrice());
        entity.setStandardMinutes(request.getStandardMinutes());
        entity.setRequiredBayType(request.getRequiredBayType());
        entity.setRequiredSkillsJson(request.getRequiredSkillsJson());
        entity.setIntervalKm(request.getIntervalKm());
        entity.setIntervalMonths(request.getIntervalMonths());
        entity.setWarrantyDays(request.getWarrantyDays());
        entity.setWarrantyKm(request.getWarrantyKm());
        entity.setWarrantyTerms(request.getWarrantyTerms());
        return entity;
    }

    public void updateEntity(ServiceEntity entity, UpdateServiceRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setServiceName(request.getServiceName().trim());
        if (request.getServiceType() != null) {
            entity.setServiceType(request.getServiceType());
        }
        entity.setDescription(request.getDescription());
        if (request.getUnit() != null && !request.getUnit().isBlank()) {
            entity.setUnit(request.getUnit().trim());
        }
        if (request.getUnitPrice() != null) {
            entity.setUnitPrice(request.getUnitPrice());
        }
        if (request.getStandardMinutes() != null) {
            entity.setStandardMinutes(request.getStandardMinutes());
        }
        entity.setRequiredBayType(request.getRequiredBayType());
        entity.setRequiredSkillsJson(request.getRequiredSkillsJson());
        entity.setIntervalKm(request.getIntervalKm());
        entity.setIntervalMonths(request.getIntervalMonths());
        if (request.getWarrantyDays() != null) {
            entity.setWarrantyDays(request.getWarrantyDays());
        }
        if (request.getWarrantyKm() != null) {
            entity.setWarrantyKm(request.getWarrantyKm());
        }
        entity.setWarrantyTerms(request.getWarrantyTerms());
        if (request.getIsActive() != null) {
            entity.setIsActive(request.getIsActive());
        }
    }

    public ServiceResponse toResponse(ServiceEntity entity) {
        if (entity == null) {
            return null;
        }
        ServiceResponse response = new ServiceResponse();
        response.setId(entity.getId());
        response.setServiceCode(entity.getServiceCode());
        response.setServiceName(entity.getServiceName());
        response.setServiceType(entity.getServiceType());
        response.setDescription(entity.getDescription());
        response.setUnit(entity.getUnit());
        response.setUnitPrice(entity.getUnitPrice());
        response.setStandardMinutes(entity.getStandardMinutes());
        response.setRequiredBayType(entity.getRequiredBayType());
        response.setRequiredSkillsJson(entity.getRequiredSkillsJson());
        response.setIntervalKm(entity.getIntervalKm());
        response.setIntervalMonths(entity.getIntervalMonths());
        response.setWarrantyDays(entity.getWarrantyDays());
        response.setWarrantyKm(entity.getWarrantyKm());
        response.setWarrantyTerms(entity.getWarrantyTerms());
        response.setIsActive(entity.getIsActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setVersion(entity.getVersion());
        return response;
    }
}

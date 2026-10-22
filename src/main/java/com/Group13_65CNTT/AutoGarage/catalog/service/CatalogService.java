package com.Group13_65CNTT.AutoGarage.catalog.service;

import com.Group13_65CNTT.AutoGarage.catalog.dto.CreateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.dto.ServiceResponse;
import com.Group13_65CNTT.AutoGarage.catalog.dto.UpdateServiceRequest;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceType;

import java.util.List;

public interface CatalogService {

    ServiceResponse createService(CreateServiceRequest request);

    ServiceResponse getServiceById(Long id);

    ServiceResponse getServiceByCode(String code);

    List<ServiceResponse> searchServices(String search, ServiceType type, Boolean isActive);

    ServiceResponse updateService(Long id, UpdateServiceRequest request);

    void deactivateService(Long id);
}

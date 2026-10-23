package com.Group13_65CNTT.AutoGarage.inventory.service;

import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.SupplierResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {
    SupplierResponse createSupplier(SupplierCreateRequest request);

    SupplierResponse getSupplierById(Long id);

    SupplierResponse getSupplierByCode(String code);

    Page<SupplierResponse> searchSuppliers(String query, Boolean active, Pageable pageable);

    SupplierResponse updateSupplier(Long id, SupplierUpdateRequest request);

    void deactivateSupplier(Long id);
}

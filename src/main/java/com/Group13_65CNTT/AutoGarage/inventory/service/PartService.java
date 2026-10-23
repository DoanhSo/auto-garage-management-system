package com.Group13_65CNTT.AutoGarage.inventory.service;

import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.PartResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PartService {
    PartResponse createPart(PartCreateRequest request);

    PartResponse getPartById(Long id);

    PartResponse getPartBySku(String sku);

    Page<PartResponse> searchParts(String query, Long supplierId, Boolean active, Pageable pageable);

    PartResponse updatePart(Long id, PartUpdateRequest request);

    void deactivatePart(Long id);

    List<PartResponse> getLowStockParts();
}

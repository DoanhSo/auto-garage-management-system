package com.Group13_65CNTT.AutoGarage.warranty.service;

import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimDecideRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimOpenRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyClaimResponse;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface WarrantyService {

    WarrantyResponse getWarrantyById(Long id);

    WarrantyResponse getWarrantyByCode(String warrantyCode);

    Page<WarrantyResponse> getWarrantiesByVehicleId(Long vehicleId, int page, int size);

    Page<WarrantyResponse> getWarrantiesByCustomerId(Long customerId, int page, int size);

    WarrantyClaimResponse openClaim(Long actorUserId, WarrantyClaimOpenRequest request);

    WarrantyClaimResponse decideClaim(Long actorUserId, Long claimId, WarrantyClaimDecideRequest request);

    WarrantyClaimResponse getClaimById(Long claimId);

    Page<WarrantyClaimResponse> getClaimsByWarrantyId(Long warrantyId, int page, int size);
}

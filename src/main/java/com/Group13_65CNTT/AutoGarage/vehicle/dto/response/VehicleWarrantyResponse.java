package com.Group13_65CNTT.AutoGarage.vehicle.dto.response;

import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import java.time.Instant;

public record VehicleWarrantyResponse(Long repairOrderId, Long id, String warrantyCode,
        Instant startsAt, Instant expiresAt, Integer endOdometer, WarrantyStatus status) {}

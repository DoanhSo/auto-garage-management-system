package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;

import java.util.List;

public interface VehicleCheckService {

    VehicleCheckResponse recordVehicleCheck(CreateVehicleCheckRequest request);

    VehicleCheckResponse getVehicleCheckById(Long id);

    List<VehicleCheckResponse> getChecksByRepairOrder(Long repairOrderId);
}

package com.Group13_65CNTT.AutoGarage.vehicle.service;

import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.TransferOwnershipRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;

import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(CreateVehicleRequest request);

    VehicleResponse getVehicleById(Long id);

    VehicleResponse getVehicleByPlate(String plate);

    List<VehicleResponse> searchVehicles(Long customerId, String search, Boolean isActive);

    VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request);

    VehicleResponse transferOwnership(Long vehicleId, TransferOwnershipRequest request);

    void deactivateVehicle(Long id);
}

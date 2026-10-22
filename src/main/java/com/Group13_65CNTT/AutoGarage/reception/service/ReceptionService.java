package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleResponse;

public interface ReceptionService {

    CheckInVehicleResponse checkInVehicle(CheckInVehicleRequest request);
}

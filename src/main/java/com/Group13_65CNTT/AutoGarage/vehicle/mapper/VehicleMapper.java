package com.Group13_65CNTT.AutoGarage.vehicle.mapper;

import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public Vehicle toEntity(CreateVehicleRequest request) {
        if (request == null) {
            return null;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setCustomerId(request.getCustomerId());
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setVin(request.getVin());
        vehicle.setEngineNumber(request.getEngineNumber());
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setManufactureYear(request.getManufactureYear());
        vehicle.setColor(request.getColor());
        vehicle.setFuelType(request.getFuelType());
        vehicle.setOdometer(request.getOdometer());
        vehicle.setInspectionDueDate(request.getInspectionDueDate());
        return vehicle;
    }

    public void updateEntity(Vehicle vehicle, UpdateVehicleRequest request) {
        if (vehicle == null || request == null) {
            return;
        }
        vehicle.setLicensePlate(request.getLicensePlate());
        vehicle.setVin(request.getVin());
        vehicle.setEngineNumber(request.getEngineNumber());
        vehicle.setBrand(request.getBrand().trim());
        vehicle.setModel(request.getModel().trim());
        vehicle.setManufactureYear(request.getManufactureYear());
        vehicle.setColor(request.getColor());
        vehicle.setFuelType(request.getFuelType());
        if (request.getOdometer() != null) {
            vehicle.setOdometer(request.getOdometer());
        }
        vehicle.setInspectionDueDate(request.getInspectionDueDate());
        if (request.getIsActive() != null) {
            vehicle.setIsActive(request.getIsActive());
        }
    }

    public VehicleResponse toResponse(Vehicle vehicle, String customerName, String customerPhone) {
        if (vehicle == null) {
            return null;
        }
        VehicleResponse response = new VehicleResponse();
        response.setId(vehicle.getId());
        response.setCustomerId(vehicle.getCustomerId());
        response.setCustomerName(customerName);
        response.setCustomerPhone(customerPhone);
        response.setLicensePlate(vehicle.getLicensePlate());
        response.setVin(vehicle.getVin());
        response.setEngineNumber(vehicle.getEngineNumber());
        response.setBrand(vehicle.getBrand());
        response.setModel(vehicle.getModel());
        response.setManufactureYear(vehicle.getManufactureYear());
        response.setColor(vehicle.getColor());
        response.setFuelType(vehicle.getFuelType());
        response.setOdometer(vehicle.getOdometer());
        response.setInspectionDueDate(vehicle.getInspectionDueDate());
        response.setIsActive(vehicle.getIsActive());
        response.setCreatedAt(vehicle.getCreatedAt());
        response.setUpdatedAt(vehicle.getUpdatedAt());
        response.setVersion(vehicle.getVersion());
        return response;
    }
}

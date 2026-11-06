package com.Group13_65CNTT.AutoGarage.vehicle.mapper;

import com.Group13_65CNTT.AutoGarage.vehicle.dto.CreateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.VehicleResponse;
import com.Group13_65CNTT.AutoGarage.vehicle.dto.UpdateVehicleRequest;
import com.Group13_65CNTT.AutoGarage.vehicle.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public static String formatPlate(String plate) {
        if (plate == null || plate.isBlank()) {
            return "";
        }
        String clean = plate.trim().replaceAll("\\s+", "").toUpperCase();
        java.util.regex.Matcher car5 = java.util.regex.Pattern.compile("^(\\d{2}[A-Z]{1,2})[-.]?(\\d{3})[-.]?(\\d{2})$").matcher(clean);
        if (car5.matches()) {
            return car5.group(1) + "-" + car5.group(2) + "." + car5.group(3);
        }
        java.util.regex.Matcher car4 = java.util.regex.Pattern.compile("^(\\d{2}[A-Z]{1,2})[-.]?(\\d{4})$").matcher(clean);
        if (car4.matches()) {
            return car4.group(1) + "-" + car4.group(2);
        }
        java.util.regex.Matcher moto5 = java.util.regex.Pattern.compile("^(\\d{2}[A-Z]\\d|\\d{2}-[A-Z]\\d)[-.]?(\\d{3})[-.]?(\\d{2})$").matcher(clean);
        if (moto5.matches()) {
            String prefix = moto5.group(1).replace("-", "");
            return prefix + "-" + moto5.group(2) + "." + moto5.group(3);
        }
        return clean;
    }


    public Vehicle toEntity(CreateVehicleRequest request) {
        if (request == null) {
            return null;
        }
        Vehicle vehicle = new Vehicle();
        vehicle.setCustomerId(request.getCustomerId());
        vehicle.setLicensePlate(formatPlate(request.getLicensePlate()));
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
        vehicle.setLicensePlate(formatPlate(request.getLicensePlate()));
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
        response.setLicensePlate(formatPlate(vehicle.getLicensePlate()));
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

package com.Group13_65CNTT.AutoGarage.vehicle.dto;

import java.util.ArrayList;
import java.util.List;

public class VehicleTimelineResponse {

    private Long vehicleId;
    private String licensePlate;
    private String brand;
    private String model;
    private Integer currentOdometer;
    private Long currentCustomerId;
    private String currentCustomerName;
    private List<VehicleHistoryEntryDto> historyEntries = new ArrayList<>();

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getCurrentOdometer() {
        return currentOdometer;
    }

    public void setCurrentOdometer(Integer currentOdometer) {
        this.currentOdometer = currentOdometer;
    }

    public Long getCurrentCustomerId() {
        return currentCustomerId;
    }

    public void setCurrentCustomerId(Long currentCustomerId) {
        this.currentCustomerId = currentCustomerId;
    }

    public String getCurrentCustomerName() {
        return currentCustomerName;
    }

    public void setCurrentCustomerName(String currentCustomerName) {
        this.currentCustomerName = currentCustomerName;
    }

    public List<VehicleHistoryEntryDto> getHistoryEntries() {
        return historyEntries;
    }

    public void setHistoryEntries(List<VehicleHistoryEntryDto> historyEntries) {
        this.historyEntries = historyEntries;
    }
}

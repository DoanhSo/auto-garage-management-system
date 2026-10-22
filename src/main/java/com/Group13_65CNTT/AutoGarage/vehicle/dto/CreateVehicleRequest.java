package com.Group13_65CNTT.AutoGarage.vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateVehicleRequest {

    @NotNull(message = "ID khách hàng sở hữu không được để trống")
    private Long customerId;

    @NotBlank(message = "Biển số xe không được để trống")
    @Size(max = 20, message = "Biển số xe tối đa 20 ký tự")
    private String licensePlate;

    @Size(max = 17, message = "Số VIN tối đa 17 ký tự")
    private String vin;

    @Size(max = 50, message = "Số máy tối đa 50 ký tự")
    private String engineNumber;

    @NotBlank(message = "Hãng xe không được để trống")
    @Size(max = 80, message = "Hãng xe tối đa 80 ký tự")
    private String brand;

    @NotBlank(message = "Dòng xe (model) không được để trống")
    @Size(max = 80, message = "Dòng xe tối đa 80 ký tự")
    private String model;

    private Short manufactureYear;

    @Size(max = 50, message = "Màu xe tối đa 50 ký tự")
    private String color;

    @Size(max = 30, message = "Loại nhiên liệu tối đa 30 ký tự")
    private String fuelType;

    @Min(value = 0, message = "Số km đã đi (odometer) không được âm")
    private Integer odometer = 0;

    private LocalDate inspectionDueDate;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getEngineNumber() {
        return engineNumber;
    }

    public void setEngineNumber(String engineNumber) {
        this.engineNumber = engineNumber;
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

    public Short getManufactureYear() {
        return manufactureYear;
    }

    public void setManufactureYear(Short manufactureYear) {
        this.manufactureYear = manufactureYear;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getFuelType() {
        return fuelType;
    }

    public void setFuelType(String fuelType) {
        this.fuelType = fuelType;
    }

    public Integer getOdometer() {
        return odometer;
    }

    public void setOdometer(Integer odometer) {
        this.odometer = odometer;
    }

    public LocalDate getInspectionDueDate() {
        return inspectionDueDate;
    }

    public void setInspectionDueDate(LocalDate inspectionDueDate) {
        this.inspectionDueDate = inspectionDueDate;
    }
}

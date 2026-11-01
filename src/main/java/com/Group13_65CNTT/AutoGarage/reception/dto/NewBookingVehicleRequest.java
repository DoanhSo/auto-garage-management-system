package com.Group13_65CNTT.AutoGarage.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NewBookingVehicleRequest {

    @NotBlank(message = "Biển số không được để trống")
    @Size(max = 20, message = "Biển số tối đa 20 ký tự")
    private String licensePlate;

    @NotBlank(message = "Hãng xe không được để trống")
    @Size(max = 80, message = "Hãng xe tối đa 80 ký tự")
    private String brand;

    @NotBlank(message = "Dòng xe không được để trống")
    @Size(max = 80, message = "Dòng xe tối đa 80 ký tự")
    private String model;

    private Short manufactureYear;

    @Size(max = 50, message = "Màu xe tối đa 50 ký tự")
    private String color;

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
}

package com.Group13_65CNTT.AutoGarage.reception.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CheckInVehicleRequest {

    private Long bookingId;

    @NotNull(message = "ID phương tiện không được để trống")
    private Long vehicleId;

    @NotNull(message = "Số km tiếp nhận (odometer) không được để trống")
    @Min(value = 0, message = "Số km tiếp nhận không được âm")
    private Integer odometerIn;

    @DecimalMin(value = "0.0", message = "Mức nhiên liệu tối thiểu 0%")
    @DecimalMax(value = "100.0", message = "Mức nhiên liệu tối đa 100%")
    private BigDecimal fuelLevelPercent;

    @Size(max = 1000, message = "Ý kiến khách hàng tối đa 1000 ký tự")
    private String customerComplaint;

    private Long actorUserId;

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public Integer getOdometerIn() {
        return odometerIn;
    }

    public void setOdometerIn(Integer odometerIn) {
        this.odometerIn = odometerIn;
    }

    public BigDecimal getFuelLevelPercent() {
        return fuelLevelPercent;
    }

    public void setFuelLevelPercent(BigDecimal fuelLevelPercent) {
        this.fuelLevelPercent = fuelLevelPercent;
    }

    public String getCustomerComplaint() {
        return customerComplaint;
    }

    public void setCustomerComplaint(String customerComplaint) {
        this.customerComplaint = customerComplaint;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }
}

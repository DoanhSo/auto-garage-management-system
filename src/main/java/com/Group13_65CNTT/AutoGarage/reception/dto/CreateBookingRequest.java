package com.Group13_65CNTT.AutoGarage.reception.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CreateBookingRequest {

    private Long customerId;

    @Valid
    private NewBookingCustomerRequest newCustomer;

    private Long vehicleId;

    @Valid
    private NewBookingVehicleRequest newVehicle;

    private Long serviceId;

    private Long bayId;

    @NotNull(message = "Thời gian hẹn không được để trống")
    @Future(message = "Thời gian hẹn phải ở trong tương lai")
    private Instant appointmentAt;

    @Size(max = 1000, message = "Mô tả yêu cầu tối đa 1000 ký tự")
    private String requestDescription;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public NewBookingCustomerRequest getNewCustomer() {
        return newCustomer;
    }

    public void setNewCustomer(NewBookingCustomerRequest newCustomer) {
        this.newCustomer = newCustomer;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
    }

    public NewBookingVehicleRequest getNewVehicle() {
        return newVehicle;
    }

    public void setNewVehicle(NewBookingVehicleRequest newVehicle) {
        this.newVehicle = newVehicle;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getBayId() {
        return bayId;
    }

    public void setBayId(Long bayId) {
        this.bayId = bayId;
    }

    public Instant getAppointmentAt() {
        return appointmentAt;
    }

    public void setAppointmentAt(Instant appointmentAt) {
        this.appointmentAt = appointmentAt;
    }

    public String getRequestDescription() {
        return requestDescription;
    }

    public void setRequestDescription(String requestDescription) {
        this.requestDescription = requestDescription;
    }
}

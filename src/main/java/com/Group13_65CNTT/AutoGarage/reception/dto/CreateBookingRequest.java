package com.Group13_65CNTT.AutoGarage.reception.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class CreateBookingRequest {

    @NotNull(message = "ID khách hàng không được để trống")
    private Long customerId;

    @NotNull(message = "ID phương tiện không được để trống")
    private Long vehicleId;

    @NotNull(message = "Dịch vụ chính không được để trống")
    private Long serviceId;

    private Long bayId;

    @NotNull(message = "Thời gian hẹn không được để trống")
    @Future(message = "Thời gian hẹn phải ở trong tương lai")
    private Instant appointmentAt;

    @Size(max = 1000, message = "Mô tả yêu cầu tối đa 1000 ký tự")
    private String requestDescription;

    private Long createdBy;

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
        this.vehicleId = vehicleId;
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

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}

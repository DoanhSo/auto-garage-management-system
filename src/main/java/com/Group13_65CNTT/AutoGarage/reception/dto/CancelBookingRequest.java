package com.Group13_65CNTT.AutoGarage.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CancelBookingRequest {

    @NotBlank(message = "Lý do hủy lịch không được để trống")
    @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
    private String cancellationReason;

    private Long actorUserId;

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }
}

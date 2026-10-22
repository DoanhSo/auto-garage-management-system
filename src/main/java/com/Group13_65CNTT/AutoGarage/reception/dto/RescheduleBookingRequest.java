package com.Group13_65CNTT.AutoGarage.reception.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class RescheduleBookingRequest {

    @NotNull(message = "Thời gian hẹn mới không được để trống")
    @Future(message = "Thời gian hẹn mới phải ở trong tương lai")
    private Instant newAppointmentAt;

    private Long newBayId;

    private Long actorUserId;

    public Instant getNewAppointmentAt() {
        return newAppointmentAt;
    }

    public void setNewAppointmentAt(Instant newAppointmentAt) {
        this.newAppointmentAt = newAppointmentAt;
    }

    public Long getNewBayId() {
        return newBayId;
    }

    public void setNewBayId(Long newBayId) {
        this.newBayId = newBayId;
    }

    public Long getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(Long actorUserId) {
        this.actorUserId = actorUserId;
    }
}

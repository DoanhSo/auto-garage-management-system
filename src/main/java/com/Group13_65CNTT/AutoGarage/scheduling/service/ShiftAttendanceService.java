package com.Group13_65CNTT.AutoGarage.scheduling.service;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftReconciliationResponse;
import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.ShiftResponse;

import java.time.LocalDate;
import java.util.List;

public interface ShiftAttendanceService {

    ShiftResponse checkIn(Long actorUserId, Long shiftId);

    ShiftResponse checkOut(Long actorUserId, Long shiftId);

    /** Scheduled hours versus timer hours of every shift starting on the given local date. */
    List<ShiftReconciliationResponse> reconcile(LocalDate date);
}

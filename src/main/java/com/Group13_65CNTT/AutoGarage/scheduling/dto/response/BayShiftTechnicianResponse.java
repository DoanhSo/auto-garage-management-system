package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.AttendanceStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;

import java.time.Instant;
import java.util.List;

/** A technician rostered on a bay during a given day, with whether the shift covers the current moment. */
public record BayShiftTechnicianResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long shiftId,
        ShiftType shiftType,
        AttendanceStatus attendanceStatus,
        Instant shiftStartsAt,
        Instant shiftEndsAt,
        List<String> skills,
        boolean canQc,
        boolean onShiftNow) {}

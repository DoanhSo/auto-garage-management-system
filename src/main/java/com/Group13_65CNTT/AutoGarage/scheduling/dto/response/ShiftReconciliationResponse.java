package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.AttendanceStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;

import java.math.BigDecimal;
import java.time.Instant;

public record ShiftReconciliationResponse(
        Long shiftId,
        Long employeeId,
        String employeeCode,
        String employeeName,
        ShiftType shiftType,
        Instant startsAt,
        Instant endsAt,
        AttendanceStatus attendanceStatus,
        Instant actualStartsAt,
        Instant actualEndsAt,
        BigDecimal scheduledHours,
        BigDecimal laborHours,
        BigDecimal idleHours,
        BigDecimal overShiftHours,
        int utilizationPercent) {}

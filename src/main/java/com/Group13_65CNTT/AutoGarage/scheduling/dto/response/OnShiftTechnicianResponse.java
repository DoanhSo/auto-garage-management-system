package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.AttendanceStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;

import java.time.Instant;
import java.util.List;

public record OnShiftTechnicianResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        Long shiftId,
        ShiftType shiftType,
        AttendanceStatus attendanceStatus,
        Instant shiftStartsAt,
        Instant shiftEndsAt,
        Long serviceBayId,
        String bayCode,
        String bayName,
        List<String> skills,
        boolean canQc,
        int activeOrdersCount,
        int completedOrdersInShift,
        java.math.BigDecimal currentShiftBonus,
        boolean hasActiveTimer,
        Long activeTaskId,
        Availability availabilityStatus) {

    public enum Availability {
        AVAILABLE,
        BUSY,
        OVERLOADED
    }
}

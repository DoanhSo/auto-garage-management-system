package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentStage;
import com.Group13_65CNTT.AutoGarage.repair.entity.WorkResultStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;

import java.time.Instant;

public record VehicleWorkHistoryResponse(
        Long id,
        Long repairOrderId,
        String orderCode,
        String licensePlate,
        AssignmentStage stage,
        String taskName,
        Long employeeId,
        String employeeName,
        ShiftType shiftType,
        Instant startedAt,
        Instant completedAt,
        long actualMinutes,
        WorkResultStatus resultStatus,
        String notes) {}

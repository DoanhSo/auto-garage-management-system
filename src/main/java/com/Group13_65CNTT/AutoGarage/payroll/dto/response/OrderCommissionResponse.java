package com.Group13_65CNTT.AutoGarage.payroll.dto.response;

import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderCommissionResponse(
        Long id,
        Long repairOrderId,
        Long taskId,
        Long employeeId,
        Long shiftId,
        String shiftType,
        Long serviceBayId,
        String bayCode,
        String licensePlate,
        CommissionType commissionType,
        BigDecimal baseAmount,
        BigDecimal ratePercent,
        BigDecimal commissionAmount,
        CommissionStatus status,
        Instant earnedAt,
        Long payrollItemId,
        String notes) {}

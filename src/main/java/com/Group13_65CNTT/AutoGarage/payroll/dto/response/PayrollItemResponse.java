package com.Group13_65CNTT.AutoGarage.payroll.dto.response;

import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollItemStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PayrollItemResponse(
        Long id,
        Long payrollPeriodId,
        Long employeeId,
        String employeeCode,
        String employeeName,
        BigDecimal baseSalarySnapshot,
        BigDecimal hourlyRateSnapshot,
        BigDecimal regularHours,
        BigDecimal overtimeHours,
        Integer shiftCount,
        Integer completedOrderCount,
        BigDecimal hourlyPayAmount,
        BigDecimal commissionAmount,
        BigDecimal orderBonusAmount,
        BigDecimal allowanceAmount,
        BigDecimal deductionAmount,
        BigDecimal grossPay,
        BigDecimal netPay,
        PayrollItemStatus status,
        Instant paidAt,
        String notes) {}

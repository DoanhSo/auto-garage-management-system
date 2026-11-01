package com.Group13_65CNTT.AutoGarage.payroll.dto.response;

import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriodStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PayrollPeriodResponse(
        Long id,
        String periodCode,
        String periodMonth,
        LocalDate fromDate,
        LocalDate toDate,
        PayrollPeriodStatus status,
        Integer totalEmployees,
        BigDecimal totalRegularHours,
        BigDecimal totalOvertimeHours,
        BigDecimal totalBasePay,
        BigDecimal totalCommissions,
        BigDecimal totalAllowances,
        BigDecimal totalDeductions,
        BigDecimal totalNetPay,
        Integer completedOrderCount,
        Long approvedBy,
        Instant approvedAt,
        Long paymentId,
        String notes,
        Instant createdAt,
        Long version) {}

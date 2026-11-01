package com.Group13_65CNTT.AutoGarage.payroll.dto.response;

import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriodStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PayslipResponse(
        PayrollItemResponse item,
        String periodCode,
        String periodMonth,
        LocalDate fromDate,
        LocalDate toDate,
        PayrollPeriodStatus periodStatus,
        List<LaborDayLine> laborDays,
        List<OrderCommissionResponse> commissions) {

    public record LaborDayLine(LocalDate date, BigDecimal regularHours, BigDecimal overtimeHours) {}
}

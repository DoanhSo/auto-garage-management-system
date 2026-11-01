package com.Group13_65CNTT.AutoGarage.payroll.dto.response;

import java.util.List;

public record PayrollPeriodDetailResponse(PayrollPeriodResponse period, List<PayrollItemResponse> items) {}

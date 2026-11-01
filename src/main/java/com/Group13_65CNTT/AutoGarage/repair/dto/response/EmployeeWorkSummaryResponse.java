package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import java.util.List;

public record EmployeeWorkSummaryResponse(
        Long employeeId,
        String month,
        long totalMinutes,
        int orderCount,
        int passedOrderCount,
        List<VehicleWorkHistoryResponse> entries) {}

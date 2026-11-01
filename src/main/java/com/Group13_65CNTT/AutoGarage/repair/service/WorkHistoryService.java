package com.Group13_65CNTT.AutoGarage.repair.service;

import com.Group13_65CNTT.AutoGarage.repair.dto.response.VehicleWorkHistoryResponse;

import java.util.List;

public interface WorkHistoryService {

    /** Every diagnosis, repair and QC row recorded for a vehicle, newest first. */
    List<VehicleWorkHistoryResponse> getVehicleWorkHistory(Long vehicleId);

    /** Work rows of one technician in a YYYY-MM month, for payroll reconciliation. */
    com.Group13_65CNTT.AutoGarage.repair.dto.response.EmployeeWorkSummaryResponse getEmployeeWorkSummary(
            Long employeeId, String month);
}

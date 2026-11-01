package com.Group13_65CNTT.AutoGarage.scheduling.service;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse;

import java.util.List;

public interface DispatchService {

    /** Technicians whose active shift covers the current instant, with bay, skills and workload. */
    List<OnShiftTechnicianResponse> getOnShiftTechnicians();
}

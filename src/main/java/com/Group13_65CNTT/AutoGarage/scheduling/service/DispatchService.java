package com.Group13_65CNTT.AutoGarage.scheduling.service;

import com.Group13_65CNTT.AutoGarage.scheduling.dto.response.OnShiftTechnicianResponse;

import java.util.List;

public interface DispatchService {

    /** Technicians whose active shift covers the current instant, with bay, skills and workload. */
    List<OnShiftTechnicianResponse> getOnShiftTechnicians();

    /** Technicians rostered on one bay during the given local day (any time of that day). */
    List<com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayShiftTechnicianResponse> getBayDayRoster(
            Long bayId, java.time.LocalDate date);

    /** Every active bay with the technicians currently on shift there. */
    List<com.Group13_65CNTT.AutoGarage.scheduling.dto.response.BayRosterResponse> getBaysRoster();
}

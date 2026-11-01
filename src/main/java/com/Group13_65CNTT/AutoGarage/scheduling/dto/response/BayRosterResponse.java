package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import java.util.List;

public record BayRosterResponse(
        Long bayId, String bayCode, String bayName, String bayType, List<OnShiftTechnicianResponse> technicians) {}

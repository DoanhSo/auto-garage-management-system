package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import java.time.Instant;
import java.util.List;

/** A QC-capable technician on shift right now, with whether they can be picked for this order. */
public record QcCandidateResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        Instant shiftEndsAt,
        Long serviceBayId,
        String bayCode,
        String bayName,
        List<String> skills,
        boolean eligible,
        Reason reason) {

    public enum Reason {
        MAIN_REPAIRER
    }
}

package com.Group13_65CNTT.AutoGarage.scheduling.entity;

public enum ShiftStatus {
    ACTIVE,
    CANCELLED,
    /** Registered by the technician, waiting for a manager's decision; never counts as a working shift. */
    PENDING,
    REJECTED
}

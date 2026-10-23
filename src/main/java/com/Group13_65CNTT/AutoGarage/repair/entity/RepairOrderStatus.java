package com.Group13_65CNTT.AutoGarage.repair.entity;

public enum RepairOrderStatus {
    NEW,
    DIAGNOSING,
    WAITING_APPROVAL,
    APPROVED,
    IN_PROGRESS,
    WAITING_PARTS,
    WAITING_QC,
    QC_FAILED,
    COMPLETED,
    INVOICED,
    CLOSED,
    CANCELLED
}

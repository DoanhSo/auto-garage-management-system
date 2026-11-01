package com.Group13_65CNTT.AutoGarage.shared.contracts;

/** Payroll hooks invoked by the repair workflow. */
public interface PayrollOperations {
    /** Idempotently records commissions for a closed repair order; returns the number of rows created. */
    int recordOrderCommissions(long actorUserId, long repairOrderId);
}

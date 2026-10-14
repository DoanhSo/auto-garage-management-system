package com.Group13_65CNTT.AutoGarage.shared.contracts;

/** I03 (TV3): called by reception/QC; must join the caller's transaction. */
public interface RepairWorkflow {
    long receive(long actorUserId, Long bookingId, long customerId, long vehicleId, int odometer);
    long createWarrantyOrder(long actorUserId, long warrantyClaimId);
    void requireInvoiceReady(long actorUserId, long repairOrderId);
    void handOver(long actorUserId, long repairOrderId, int odometerOut, String requestKey);
}

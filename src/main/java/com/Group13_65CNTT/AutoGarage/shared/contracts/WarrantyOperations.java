package com.Group13_65CNTT.AutoGarage.shared.contracts;

/** I07 (TV3): snapshot invoice policy on handover; approved claims create new orders. */
public interface WarrantyOperations {
    void createFromInvoice(long actorUserId, long invoiceId, int odometerOut);
    long openClaim(long actorUserId, long warrantyId, int currentOdometer, String description, String requestKey);
    void decideClaim(long actorUserId, long claimId, boolean approved, String reason, String requestKey);
}

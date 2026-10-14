package com.Group13_65CNTT.AutoGarage.shared.contracts;

import java.math.BigDecimal;

/** I06 (TV4): immutable snapshots and locked balance checks. */
public interface BillingOperations {
    enum PaymentMethod { CASH, CARD, BANK_TRANSFER, VIETQR }
    record PaymentCommand(BigDecimal amount, PaymentMethod method, String providerReference, String requestKey) {}
    long issueInvoice(long actorUserId, long repairOrderId, String requestKey);
    long receivePayment(long actorUserId, long invoiceId, PaymentCommand command);
    long paySupplier(long actorUserId, long inventoryDocumentId, PaymentCommand command);
    void requireHandoverAllowed(long actorUserId, long repairOrderId);
}

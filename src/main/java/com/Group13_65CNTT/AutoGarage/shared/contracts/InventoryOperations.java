package com.Group13_65CNTT.AutoGarage.shared.contracts;

import java.math.BigDecimal;

/** I05 (TV4): lock parts in ascending ID order; post a document at most once. */
public interface InventoryOperations {
    void reserve(long actorUserId, long estimateItemId, BigDecimal quantity);
    void releaseForOrder(long actorUserId, long repairOrderId);
    void post(long actorUserId, long documentId, String requestKey);
}

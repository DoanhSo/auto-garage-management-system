package com.Group13_65CNTT.AutoGarage.shared.contracts;

import java.math.BigDecimal;

/** I05 (TV4): lock parts in ascending ID order; post a document at most once. */
public interface InventoryOperations {
    void requireIssueComplete(long repairOrderId);
    /** Blocks starting work while approved parts of the order are reserved but not yet issued. */
    void requireIssuedForWorkStart(long repairOrderId);
    /** Blocks completing a repair task while parts of the order are reserved but not yet issued. */
    void requireIssuedBeforeCompletion(long repairOrderId);
    /** Blocks sending the order to QC while parts are reserved but not issued or stock documents are still drafts. */
    void requireIssuedBeforeQc(long repairOrderId);
    void reserve(long actorUserId, long estimateItemId, BigDecimal quantity);
    void releaseForOrder(long actorUserId, long repairOrderId);
    void post(long actorUserId, long documentId, String requestKey);
}

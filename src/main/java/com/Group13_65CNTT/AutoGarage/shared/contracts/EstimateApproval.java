package com.Group13_65CNTT.AutoGarage.shared.contracts;

import java.util.List;

/** I04 (TV3): approve lines, create tasks, reserve parts and enqueue notices atomically. */
public interface EstimateApproval {
    enum Decision { APPROVED, REJECTED }
    record LineDecision(long estimateItemId, Decision decision, String note) {}
    void decide(long actorUserId, long estimateId, List<LineDecision> lines, String requestKey);
}

package com.Group13_65CNTT.AutoGarage.shared.contracts;

import java.time.Instant;

/** I02 (TV3): lock the bay before testing half-open interval overlap. */
public interface BayScheduling {
    long reserveBooking(long actorUserId, long bookingId, long bayId, Instant startsAt, Instant endsAt);
    long reserveRepair(long actorUserId, long repairOrderId, long bayId, Instant startsAt, Instant endsAt);
    void releaseForBooking(long actorUserId, long bookingId);
    void releaseForRepair(long actorUserId, long repairOrderId);
}

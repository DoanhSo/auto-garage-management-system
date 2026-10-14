package com.Group13_65CNTT.AutoGarage.shared.contracts;

/** I01 (TV1): implementation must check both role and object ownership. */
public interface SecurityAccess {
    void requireRole(long actorUserId, String... allowedRoles);
    void requireVehicleAccess(long actorUserId, long vehicleId);
    void requireRepairOrderAccess(long actorUserId, long repairOrderId);
}

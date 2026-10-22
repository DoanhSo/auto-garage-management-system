package com.Group13_65CNTT.AutoGarage.identity.service;

import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.shared.contracts.SecurityAccess;

public interface ObjectAccessService extends SecurityAccess {
    void requireResourceAccess(long actorId, AccessResource resource, long id);

    void requireQualityCheckAccess(long actorId, long orderId);
}

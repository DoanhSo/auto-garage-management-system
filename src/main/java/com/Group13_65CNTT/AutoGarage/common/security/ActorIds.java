package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

/** Validates the actor ID supplied by an authenticated application boundary. */
public final class ActorIds {
    private ActorIds() {}

    public static long require(Long actorId) {
        if (actorId == null || actorId <= 0) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        return actorId;
    }
}

package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    public long userId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        try {
            long id = Long.parseLong(jwt.getToken().getSubject());
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException invalid) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
    }

    public Long userIdOrNull() {
        try {
            return userId();
        } catch (BusinessException unauthenticated) {
            if (unauthenticated.getResponseCode() != ResponseCode.UNAUTHORIZED)
                throw unauthenticated;
            return null;
        }
    }
}

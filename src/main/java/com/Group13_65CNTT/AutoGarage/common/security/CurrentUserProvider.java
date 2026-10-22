package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public long userId() {
        Jwt jwt = currentJwt();
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        if (userRepository != null) {
            Optional<User> resolved = AccountTokenValidator.resolveUser(userRepository, jwt);
            if (resolved.isPresent() && resolved.get().getId() != null) {
                return resolved.get().getId();
            }
        }
        if (subject.matches(SecurityConstants.POSITIVE_LONG_ID_PATTERN)) {
            try {
                long id = Long.parseLong(subject);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
                // Fall through to UNAUTHORIZED
            }
        }
        throw new BusinessException(ResponseCode.UNAUTHORIZED);
    }

    public String subjectOrNull() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            return null;
        }
        String subject = jwt.getToken().getSubject();
        return subject == null || subject.isBlank() ? null : subject;
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

    private Jwt currentJwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwt) || !jwt.isAuthenticated()) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        return jwt.getToken();
    }
}

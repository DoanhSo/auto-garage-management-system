package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;

public final class AccountTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2TokenValidatorResult INVALID =
            OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid or revoked token", null));
    private final UserRepository users;
    private final AuthProperties properties;
    private final Clock clock;

    public AccountTokenValidator(UserRepository users, AuthProperties properties, Clock clock) {
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    public OAuth2TokenValidatorResult validate(Jwt token) {
        long id, version;
        Role role;
        try {
            if (token.getSubject() == null
                    || !token.getSubject().matches("[1-9][0-9]{0,18}")
                    || token.getExpiresAt() == null
                    || token.getIssuedAt() == null
                    || token.getId() == null
                    || token.getId().isBlank()) return INVALID;
            Object rawVersion = token.getClaim(SecurityConstants.AUTH_VERSION_CLAIM);
            if (!(rawVersion instanceof Number) || !rawVersion.toString().matches("[0-9]{1,19}"))
                return INVALID;
            id = Long.parseLong(token.getSubject());
            version = Long.parseLong(rawVersion.toString());
            role = Role.valueOf(token.getClaimAsString(SecurityConstants.ROLE_CLAIM));
            if (!token.getAudience().contains(properties.audience())
                    || !token.getExpiresAt().isAfter(token.getIssuedAt())
                    || token.getIssuedAt().isAfter(clock.instant().plus(properties.clockSkew())))
                return INVALID;
        } catch (IllegalArgumentException | NullPointerException | ClassCastException invalid) {
            return INVALID;
        }
        // Do not disguise a database outage as invalid credentials.
        return users.findById(id)
                .filter(u -> u.isActive() && u.getAuthVersion() == version && u.getRole() == role)
                .map(u -> OAuth2TokenValidatorResult.success())
                .orElse(INVALID);
    }
}

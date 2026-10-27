package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
public final class AccountTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2TokenValidatorResult INVALID =
            OAuth2TokenValidatorResult.failure(
                    new OAuth2Error(
                            SecurityConstants.OAUTH2_INVALID_TOKEN_ERROR,
                            SecurityConstants.OAUTH2_INVALID_TOKEN_DESCRIPTION,
                            null));
    private final UserRepository users;
    private final AuthProperties properties;
    private final Clock clock;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        long version;
        Role role;
        try {
            if (token.getSubject() == null
                    || token.getSubject().isBlank()
                    || token.getSubject().length() > SecurityConstants.MAX_SUBJECT_LENGTH
                    || token.getExpiresAt() == null
                    || token.getIssuedAt() == null
                    || token.getId() == null
                    || token.getId().isBlank()
                    || token.getAudience() == null) return INVALID;
            if (token.hasClaim(SecurityConstants.AUTH_VERSION_CLAIM)) {
                Object rawVersion = token.getClaim(SecurityConstants.AUTH_VERSION_CLAIM);
                if (!(rawVersion instanceof Number)
                        || !rawVersion.toString().matches(SecurityConstants.AUTH_VERSION_PATTERN))
                    return INVALID;
                version = Long.parseLong(rawVersion.toString());
            } else {
                version = SecurityConstants.DEFAULT_AUTH_VERSION;
            }
            role = KeycloakJwtAuthenticationConverter.extractSingleRole(token).orElse(null);
            if (role == null
                    || !token.getAudience().contains(properties.audience())
                    || !token.getExpiresAt().isAfter(token.getIssuedAt())
                    || token.getIssuedAt().isAfter(clock.instant().plus(properties.clockSkew())))
                return INVALID;
        } catch (IllegalArgumentException | NullPointerException | ClassCastException invalid) {
            return INVALID;
        }
        // Do not disguise a database outage as invalid credentials.
        Optional<User> resolved = resolveUser(users, token);
        if (resolved.isPresent()) {
            return resolved.filter(
                            u ->
                                    u.isActive()
                                            && u.getDeletedAt() == null
                                            && u.getAuthVersion() == version
                                            && u.getRole() == role)
                    .map(u -> OAuth2TokenValidatorResult.success())
                    .orElse(INVALID);
        }
        // Allow JIT provisioning exclusively for self-registered or Google-authenticated CUSTOMER
        // tokens issued by Keycloak (never for staff or administrator roles).
        if (role == Role.CUSTOMER
                && version == SecurityConstants.DEFAULT_AUTH_VERSION
                && token.getSubject().matches(SecurityConstants.IDENTITY_ID_PATTERN)
                && !token.getSubject().matches(SecurityConstants.POSITIVE_LONG_ID_PATTERN)) {
            String preferredUsername =
                    token.getClaimAsString(SecurityConstants.PREFERRED_USERNAME_CLAIM);
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return OAuth2TokenValidatorResult.success();
            }
        }
        return INVALID;
    }

    public static Optional<User> resolveUser(UserRepository users, Jwt token) {
        String subject = token.getSubject();
        if (subject == null || subject.isBlank()) return Optional.empty();
        Optional<User> byIdentity = users.findByIdentityId(subject);
        if (byIdentity.isPresent()) return byIdentity;
        if (subject.matches(SecurityConstants.POSITIVE_LONG_ID_PATTERN)) {
            final long id;
            try {
                id = Long.parseLong(subject);
            } catch (NumberFormatException invalidIdentifier) {
                return Optional.empty();
            }
            Optional<User> byId =
                    users.findById(id)
                            .filter(
                                    u ->
                                            u.getIdentityId() == null
                                                    || u.getIdentityId().equals(subject));
            if (byId.isPresent()) return byId;
        }
        String preferredUsername =
                token.getClaimAsString(SecurityConstants.PREFERRED_USERNAME_CLAIM);
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            // Customer usernames are self-service input in Keycloak. Matching an unbound local
            // customer by name would let a new registration take over that customer's records.
            if (KeycloakJwtAuthenticationConverter.extractSingleRole(token).orElse(null) == Role.CUSTOMER) {
                return Optional.empty();
            }
            return users.findByUsername(preferredUsername.strip().toLowerCase(Locale.ROOT))
                    .filter(u -> u.getIdentityId() == null || u.getIdentityId().equals(subject));
        }
        return Optional.empty();
    }
}

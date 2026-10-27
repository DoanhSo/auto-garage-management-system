package com.Group13_65CNTT.AutoGarage.common.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class AccountTokenValidatorTest {
    private final UserRepository users = mock(UserRepository.class);
    private final Instant now = Instant.parse("2026-10-24T00:00:00Z");
    private final AuthProperties properties = new AuthProperties(
            "https://identity.example.test/realms/garage", "garage-api",
            "https://identity.example.test/certs", Duration.ZERO, List.of(), Duration.ofHours(1));

    @Test
    void selfRegistrationCannotClaimUnboundLocalCustomerByUsername() {
        Jwt token = token(0);
        when(users.findByIdentityId(token.getSubject())).thenReturn(Optional.empty());
        assertThat(AccountTokenValidator.resolveUser(users, token)).isEmpty();
        verify(users, never()).findByUsername(anyString());
    }

    @Test
    void revokedAccessTokenFailsAndFreshTokenWithSynchronizedVersionPasses() {
        User user = User.builder().identityId("customer-subject").username("customer.one")
                .role(Role.CUSTOMER).active(true).authVersion(1).build();
        when(users.findByIdentityId("customer-subject")).thenReturn(Optional.of(user));
        var validator = new AccountTokenValidator(users, properties, Clock.fixed(now, ZoneOffset.UTC));
        assertThat(validator.validate(token(0)).hasErrors()).isTrue();
        assertThat(validator.validate(token(1)).hasErrors()).isFalse();
    }

    @Test
    void oversizedNumericSubjectIsInvalidInsteadOfThrowingServerError() {
        var validator = new AccountTokenValidator(users, properties, Clock.fixed(now, ZoneOffset.UTC));
        assertThat(validator.validate(token("9999999999999999999", 0)).hasErrors()).isTrue();
        verify(users, never()).findById(anyLong());
    }

    private Jwt token(long version) {
        return token("customer-subject", version);
    }

    private Jwt token(String subject, long version) {
        return Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject(subject).audience(List.of("garage-api"))
                .issuedAt(now.minusSeconds(1)).expiresAt(now.plusSeconds(600)).jti("test-id")
                .claim("preferred_username", "customer.one")
                .claim("realm_access", Map.of("roles", List.of("CUSTOMER")))
                .claim("auth_version", version).build();
    }
}

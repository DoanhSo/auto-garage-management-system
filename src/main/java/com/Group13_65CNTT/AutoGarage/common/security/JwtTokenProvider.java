package com.Group13_65CNTT.AutoGarage.common.security;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;

import lombok.RequiredArgsConstructor;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final Clock clock;

    public record IssuedToken(String value, Instant expiresAt) {
        @Override
        public String toString() {
            return "IssuedToken[redacted]";
        }
    }

    public IssuedToken issue(User user) {
        Instant now = clock.instant();
        Instant expires = now.plus(properties.ttl());
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .audience(List.of(properties.audience()))
                        .subject(user.getId().toString())
                        .issuedAt(now)
                        .notBefore(now)
                        .expiresAt(expires)
                        .id(UUID.randomUUID().toString())
                        .claim(SecurityConstants.ROLE_CLAIM, user.getRole().name())
                        .claim(SecurityConstants.AUTH_VERSION_CLAIM, user.getAuthVersion())
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(),
                                        claims))
                        .getTokenValue();
        return new IssuedToken(token, expires);
    }
}

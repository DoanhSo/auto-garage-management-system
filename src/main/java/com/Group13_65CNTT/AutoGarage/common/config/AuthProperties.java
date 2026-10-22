package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties("security.auth")
public record AuthProperties(
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotBlank String jwkSetUri,
        @NotNull Duration clockSkew,
        List<String> allowedOrigins,
        @DefaultValue("1h") @NotNull Duration corsMaxAge) {
    @AssertTrue(message = "CORS cache duration must not be negative")
    public boolean isCorsMaxAgeValid() {
        return corsMaxAge != null && !corsMaxAge.isNegative();
    }

    @AssertTrue(message = "Keycloak JWKS URI must be an explicit http or https URI")
    public boolean isJwkSetUriValid() {
        if (jwkSetUri == null || jwkSetUri.isBlank()) return false;
        try {
            URI uri = URI.create(jwkSetUri);
            return (SecurityConstants.HTTP_SCHEME.equals(uri.getScheme())
                            || SecurityConstants.HTTPS_SCHEME.equals(uri.getScheme()))
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && uri.getFragment() == null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    @AssertTrue(message = "Clock skew must be between zero and 60 seconds")
    public boolean isClockSkewValid() {
        return clockSkew != null
                && !clockSkew.isNegative()
                && clockSkew.compareTo(Duration.ofSeconds(SecurityConstants.MAX_CLOCK_SKEW_SECONDS))
                        <= 0;
    }

    @AssertTrue(
            message = "CORS origins must be explicit http/https origins without paths or wildcards")
    public boolean isOriginsValid() {
        if (allowedOrigins == null) return true;
        return allowedOrigins.stream()
                .filter(s -> !s.isBlank())
                .allMatch(
                        s -> {
                            try {
                                URI uri = URI.create(s);
                                return (SecurityConstants.HTTP_SCHEME.equals(uri.getScheme())
                                                || SecurityConstants.HTTPS_SCHEME.equals(
                                                        uri.getScheme()))
                                        && uri.getHost() != null
                                        && uri.getUserInfo() == null
                                        && uri.getQuery() == null
                                        && uri.getFragment() == null
                                        && (uri.getPath() == null || uri.getPath().isEmpty());
                            } catch (IllegalArgumentException invalid) {
                                return false;
                            }
                        });
    }

    @Override
    public String toString() {
        return "AuthProperties[issuer=" + issuer + ", audience=" + audience + "]";
    }
}

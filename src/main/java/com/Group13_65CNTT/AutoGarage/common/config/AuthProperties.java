package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
        @NotBlank String secretBase64,
        @NotBlank String issuer,
        @NotBlank String audience,
        @NotNull Duration ttl,
        @NotNull Duration clockSkew,
        @Min(4) @Max(16) int bcryptStrength,
        List<String> allowedOrigins,
        @DefaultValue("1h") @NotNull Duration corsMaxAge) {
    @AssertTrue(message = "CORS cache duration must not be negative")
    public boolean isCorsMaxAgeValid() {
        return corsMaxAge != null && !corsMaxAge.isNegative();
    }

    @AssertTrue(message = "JWT TTL must be positive and no more than 24 hours")
    public boolean isTtlValid() {
        return ttl != null
                && !ttl.isNegative()
                && !ttl.isZero()
                && ttl.compareTo(Duration.ofHours(24)) <= 0;
    }

    @AssertTrue(message = "Clock skew must be between zero and 60 seconds")
    public boolean isClockSkewValid() {
        return clockSkew != null
                && !clockSkew.isNegative()
                && clockSkew.compareTo(Duration.ofSeconds(60)) <= 0;
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
                                return ("http".equals(uri.getScheme())
                                                || "https".equals(uri.getScheme()))
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
        return "AuthProperties[secret=redacted]";
    }
}

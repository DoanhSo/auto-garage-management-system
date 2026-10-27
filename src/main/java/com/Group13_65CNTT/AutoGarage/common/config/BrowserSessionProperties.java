package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties("security.browser-session")
public record BrowserSessionProperties(
        @NotNull URI tokenUri,
        @NotBlank String clientId,
        @NotNull Duration requestTimeout,
        boolean secureCookie) {
    @AssertTrue(message = "Browser session requires an explicit http/https token endpoint and a positive timeout")
    public boolean isValid() {
        return tokenUri != null && tokenUri.getHost() != null
                && ("https".equals(tokenUri.getScheme()) || "http".equals(tokenUri.getScheme()))
                && tokenUri.getUserInfo() == null && tokenUri.getQuery() == null
                && tokenUri.getFragment() == null && tokenUri.getPath().endsWith("/token")
                && requestTimeout != null && !requestTimeout.isNegative() && !requestTimeout.isZero();
    }
}

package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("security.keycloak-admin")
public class KeycloakAdminProperties {
    private boolean enabled = true;
    private String serverUrl;
    private String realm;
    private String username;
    private String password;
    @NotNull private Duration connectTimeout = Duration.ofSeconds(5);
    @NotNull private Duration requestTimeout = Duration.ofSeconds(10);

    @AssertTrue(message = "Keycloak admin requires a server URL, realm and admin credentials when enabled")
    public boolean isConnectionConfigured() {
        if (!enabled) return true;
        if (serverUrl == null || realm == null || !realm.matches("[A-Za-z0-9._-]+")
                || username == null || username.isBlank() || password == null || password.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(serverUrl);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null
                    && uri.getQuery() == null && uri.getFragment() == null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    @AssertTrue(message = "Keycloak admin timeouts must be positive and connect timeout must not exceed request timeout")
    public boolean isTimeoutOrderValid() {
        return connectTimeout != null && requestTimeout != null
                && !connectTimeout.isNegative() && !connectTimeout.isZero()
                && !requestTimeout.isNegative() && !requestTimeout.isZero()
                && connectTimeout.compareTo(requestTimeout) <= 0;
    }
}

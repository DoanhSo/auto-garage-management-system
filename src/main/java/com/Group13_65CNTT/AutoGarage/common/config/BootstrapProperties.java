package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.common.util.PasswordPolicy;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;

@Validated
@ConfigurationProperties("security.bootstrap")
public record BootstrapProperties(
        boolean enabled, String username, String fullName, String password) {
    @AssertTrue(
            message = "Enabled admin bootstrap requires a valid username, full name and password")
    public boolean isConfigured() {
        return !enabled
                || (username != null
                        && username.matches("[A-Za-z0-9._-]{3,100}")
                        && fullName != null
                        && !fullName.isBlank()
                        && fullName.length() <= 150
                        && password != null
                        && !password.isBlank()
                        && password.getBytes(StandardCharsets.UTF_8).length
                                <= PasswordPolicy.BCRYPT_MAX_BYTES);
    }

    @Override
    public String toString() {
        return "BootstrapProperties[enabled=" + enabled + ", password=redacted]";
    }
}

package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;

import jakarta.validation.constraints.AssertTrue;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("security.bootstrap")
public record BootstrapProperties(
        boolean enabled, String username, String fullName, String identityId) {
    @AssertTrue(
            message =
                    "Enabled admin bootstrap requires a valid username, full name and optional identity ID")
    public boolean isConfigured() {
        return !enabled
                || (username != null
                        && username.matches(SecurityConstants.USERNAME_PATTERN)
                        && fullName != null
                        && !fullName.isBlank()
                        && fullName.length() <= SecurityConstants.FULL_NAME_MAX_LENGTH
                        && (identityId == null
                                || identityId.isBlank()
                                || identityId.matches(SecurityConstants.IDENTITY_ID_PATTERN)));
    }

    @Override
    public String toString() {
        return "BootstrapProperties[enabled=" + enabled + ", username=" + username + "]";
    }
}

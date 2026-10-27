package com.Group13_65CNTT.AutoGarage.common.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class KeycloakAdminPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues("security.keycloak-admin.server-url=https://identity.example.test",
                    "security.keycloak-admin.realm=garage", "security.keycloak-admin.username=test-admin",
                    "security.keycloak-admin.password=test-only-password");

    @Test
    void configuredAdminBindsAndDisabledAdapterDoesNotRequireCredentials() {
        context.run(c -> assertThat(c.getStartupFailure()).isNull());
        new ApplicationContextRunner().withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues("security.keycloak-admin.enabled=false")
                .run(c -> assertThat(c.getStartupFailure()).isNull());
    }

    @Test
    void enabledAdminRequiresCredentialsAndSafeServerUri() {
        context.withPropertyValues("security.keycloak-admin.password=")
                .run(c -> assertThat(c.getStartupFailure()).isNotNull());
        context.withPropertyValues("security.keycloak-admin.server-url=https://user:pass@identity.example.test")
                .run(c -> assertThat(c.getStartupFailure()).isNotNull());
        context.withPropertyValues("security.keycloak-admin.realm=garage/other")
                .run(c -> assertThat(c.getStartupFailure()).isNotNull());
    }

    @Test
    void nonpositiveOrInvertedTimeoutsFailStartup() {
        context.withPropertyValues("security.keycloak-admin.connect-timeout=0s")
                .run(c -> assertThat(c.getStartupFailure()).isNotNull());
        context.withPropertyValues("security.keycloak-admin.request-timeout=1s")
                .run(c -> assertThat(c.getStartupFailure()).isNotNull());
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties(KeycloakAdminProperties.class)
    static class PropertiesConfiguration {}
}

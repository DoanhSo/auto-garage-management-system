package com.Group13_65CNTT.AutoGarage.common.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

class AuthConfigurationTests {
    private static String randomKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class)
                    .withPropertyValues(
                            "security.auth.secret-base64=" + randomKey(),
                            "security.auth.issuer=test",
                            "security.auth.audience=test-api",
                            "security.auth.ttl=15m",
                            "security.auth.clock-skew=0s",
                            "security.auth.bcrypt-strength=4",
                            "security.bootstrap.enabled=false");

    @Test
    void completePropertiesBindAndSecretsAreRedacted() {
        context.run(
                c -> {
                    assertNull(c.getStartupFailure());
                    assertNotNull(c.getBean(AuthProperties.class));
                    assertFalse(
                            c.getBean(AuthProperties.class)
                                    .toString()
                                    .contains(c.getBean(AuthProperties.class).secretBase64()));
                });
    }

    @Test
    void emptySecretFailsBinding() {
        context.withPropertyValues("security.auth.secret-base64=")
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void missingIssuerOrAudienceFailsBinding() {
        context.withPropertyValues("security.auth.issuer=")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues("security.auth.audience=")
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void invalidTtlAndClockSkewFailBinding() {
        for (String ttl : List.of("0s", "-1s", "25h"))
            context.withPropertyValues("security.auth.ttl=" + ttl)
                    .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues("security.auth.clock-skew=61s")
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void corsDoesNotAcceptWildcardsOrUrlPaths() {
        context.withPropertyValues("security.auth.cors-max-age=-1s")
                .run(c -> assertNotNull(c.getStartupFailure()));
        for (String origin :
                List.of(
                        "*",
                        "https://example.test/path",
                        "https://user:pass@example.test",
                        "file:///tmp"))
            context.withPropertyValues("security.auth.allowed-origins=" + origin)
                    .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void badEncodingAndShortSigningKeysAreRejected() {
        var config = new IdentityConfiguration();
        assertThrows(
                IllegalArgumentException.class,
                () -> config.jwtSecretKey(properties("not base64!")));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        config.jwtSecretKey(
                                properties(Base64.getEncoder().encodeToString(new byte[8]))));
        assertNotNull(config.jwtSecretKey(properties(randomKey())));
    }

    @Test
    void bootstrapOnlyAcceptsExplicitCompleteCredentials() {
        context.withPropertyValues("security.bootstrap.enabled=true")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues(
                        "security.bootstrap.enabled=true",
                        "security.bootstrap.username=initial.admin",
                        "security.bootstrap.full-name=Initial Admin",
                        "security.bootstrap.password=TestBootstrap!123")
                .run(
                        c -> {
                            assertNull(c.getStartupFailure());
                            assertFalse(
                                    c.getBean(BootstrapProperties.class)
                                            .toString()
                                            .contains("TestBootstrap!123"));
                        });
    }

    @Test
    void unsupportedBcryptCostsFailConfiguration() {
        context.withPropertyValues("security.auth.bcrypt-strength=3")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues("security.auth.bcrypt-strength=17")
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    private static AuthProperties properties(String secret) {
        return new AuthProperties(
                secret,
                "test",
                "api",
                Duration.ofMinutes(15),
                Duration.ZERO,
                4,
                List.of(),
                Duration.ofHours(1));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AuthProperties.class, BootstrapProperties.class})
    static class PropertiesConfiguration {}
}

package com.Group13_65CNTT.AutoGarage.common.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

class AuthConfigurationTests {
    private static final String VALID_JWKS =
            "http://localhost:8081/realms/autogarage-realm/protocol/openid-connect/certs";

    private final ApplicationContextRunner context =
            new ApplicationContextRunner()
                    .withUserConfiguration(PropertiesConfiguration.class)
                    .withPropertyValues(
                            "security.auth.issuer=http://localhost:8081/realms/autogarage-realm",
                            "security.auth.audience=autogarage-api",
                            "security.auth.jwk-set-uri=" + VALID_JWKS,
                            "security.auth.clock-skew=0s",
                            "security.bootstrap.enabled=false");

    @Test
    void completeKeycloakPropertiesBindCleanly() {
        context.run(
                c -> {
                    assertNull(c.getStartupFailure());
                    assertNotNull(c.getBean(AuthProperties.class));
                    assertEquals(VALID_JWKS, c.getBean(AuthProperties.class).jwkSetUri());
                });
    }

    @Test
    void missingIssuerAudienceOrJwksUriFailsBinding() {
        context.withPropertyValues("security.auth.issuer=")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues("security.auth.audience=")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues("security.auth.jwk-set-uri=")
                .run(c -> assertNotNull(c.getStartupFailure()));
    }

    @Test
    void invalidJwksUriAndClockSkewFailBinding() {
        for (String uri :
                List.of(
                        "not a uri",
                        "ftp://localhost:8081/certs",
                        "https://user:pass@localhost:8081/certs",
                        "http://localhost:8081/certs#frag")) {
            context.withPropertyValues("security.auth.jwk-set-uri=" + uri)
                    .run(c -> assertNotNull(c.getStartupFailure()));
        }
        context.withPropertyValues("security.auth.clock-skew=-1s")
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
    void identityConfigurationCreatesRs256JwtDecoderFromJwksUri() {
        var config = new IdentityConfiguration();
        assertThrows(
                IllegalArgumentException.class,
                () -> config.jwtDecoder(properties("ftp://bad"), null, Clock.systemUTC()));
        assertNotNull(config.jwtDecoder(properties(VALID_JWKS), null, Clock.systemUTC()));
    }

    @Test
    void bootstrapOnlyAcceptsValidAdministratorProfile() {
        context.withPropertyValues("security.bootstrap.enabled=true")
                .run(c -> assertNotNull(c.getStartupFailure()));
        context.withPropertyValues(
                        "security.bootstrap.enabled=true",
                        "security.bootstrap.username=initial.admin",
                        "security.bootstrap.full-name=Initial Admin",
                        "security.bootstrap.identity-id=550e8400-e29b-41d4-a716-446655440000")
                .run(c -> assertNull(c.getStartupFailure()));
    }

    private static AuthProperties properties(String jwksUri) {
        return new AuthProperties(
                "http://localhost:8081/realms/autogarage-realm",
                "autogarage-api",
                jwksUri,
                Duration.ZERO,
                List.of(),
                Duration.ofHours(1));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableConfigurationProperties({AuthProperties.class, BootstrapProperties.class})
    static class PropertiesConfiguration {}
}

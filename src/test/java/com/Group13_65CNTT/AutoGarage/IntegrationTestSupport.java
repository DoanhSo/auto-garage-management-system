package com.Group13_65CNTT.AutoGarage;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sun.net.httpserver.HttpServer;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestConstructor;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ephemeral loopback JWKS endpoint and RS256 token issuer shared only by this test JVM. Exercises
 * the production {@code NimbusJwtDecoder.withJwkSetUri(...)} path offline.
 */
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public abstract class IntegrationTestSupport {
    protected static final String KEY_ID = "keycloak-test-rsa-key";
    private static final JwtEncoder ENCODER;
    private static final String JWKS_URI;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            RSAKey rsaKey =
                    new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                            .privateKey((RSAPrivateKey) pair.getPrivate())
                            .keyUse(KeyUse.SIGNATURE)
                            .algorithm(JWSAlgorithm.RS256)
                            .keyID(KEY_ID)
                            .build();
            ENCODER = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
            byte[] jwksBytes =
                    new JWKSet(rsaKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(
                    "/realms/autogarage-realm/protocol/openid-connect/certs",
                    exchange -> {
                        exchange.getResponseHeaders()
                                .set("Content-Type", "application/json; charset=UTF-8");
                        exchange.sendResponseHeaders(200, jwksBytes.length);
                        try (OutputStream out = exchange.getResponseBody()) {
                            out.write(jwksBytes);
                        }
                    });
            server.start();
            JWKS_URI =
                    "http://127.0.0.1:"
                            + server.getAddress().getPort()
                            + "/realms/autogarage-realm/protocol/openid-connect/certs";
        } catch (Exception failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    @DynamicPropertySource
    static void testKeycloakJwksUri(DynamicPropertyRegistry properties) {
        properties.add("security.auth.jwk-set-uri", () -> JWKS_URI);
    }

    protected static JwtEncoder testJwtEncoder() {
        return ENCODER;
    }

    protected static String testJwksUri() {
        return JWKS_URI;
    }

    protected String issueKeycloakToken(AuthProperties properties, User user) {
        String subject =
                user.getIdentityId() != null && !user.getIdentityId().isBlank()
                        ? user.getIdentityId()
                        : user.getId().toString();
        return issueKeycloakToken(
                properties,
                subject,
                user.getUsername(),
                user.getRole().name(),
                user.getAuthVersion());
    }

    protected String issueKeycloakToken(
            AuthProperties properties,
            String subject,
            String preferredUsername,
            String role,
            long authVersion) {
        Instant now = Instant.now();
        var builder =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .audience(List.of(properties.audience()))
                        .subject(subject)
                        .issuedAt(now.minusSeconds(1))
                        .notBefore(now.minusSeconds(1))
                        .expiresAt(now.plusSeconds(900))
                        .id(UUID.randomUUID().toString())
                        .claim(
                                SecurityConstants.REALM_ACCESS_CLAIM,
                                Map.of(
                                        SecurityConstants.ROLES_CLAIM,
                                        List.of(role, "default-roles-autogarage")))
                        .claim(SecurityConstants.AUTH_VERSION_CLAIM, authVersion);
        if (preferredUsername != null) {
            builder.claim(SecurityConstants.PREFERRED_USERNAME_CLAIM, preferredUsername);
        }
        JwsHeader header =
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).type("JWT").build();
        return ENCODER.encode(JwtEncoderParameters.from(header, builder.build())).getTokenValue();
    }
}

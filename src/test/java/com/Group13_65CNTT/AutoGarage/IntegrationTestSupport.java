package com.Group13_65CNTT.AutoGarage;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.SecureRandom;
import java.util.Base64;

/** Random signing key shared only by this test JVM. Never a deployment secret. */
public abstract class IntegrationTestSupport {
    private static final String KEY;

    static {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        KEY = Base64.getEncoder().encodeToString(bytes);
    }

    @DynamicPropertySource
    static void testSigningKey(DynamicPropertyRegistry properties) {
        properties.add("security.auth.secret-base64", () -> KEY);
    }
}

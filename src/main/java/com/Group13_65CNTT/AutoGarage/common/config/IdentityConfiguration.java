package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.auth.service.BootstrapService;
import com.Group13_65CNTT.AutoGarage.common.security.AccountTokenValidator;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Clock;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableTransactionManagement(order = 0)
@EnableConfigurationProperties({AuthProperties.class, BootstrapProperties.class})
public class IdentityConfiguration {
    @Bean
    Clock identityClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordEncoder passwordEncoder(AuthProperties properties) {
        return new BCryptPasswordEncoder(properties.bcryptStrength());
    }

    @Bean
    SecretKeySpec jwtSecretKey(AuthProperties properties) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.secretBase64());
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("AUTH_JWT_SECRET must be Base64-encoded", invalid);
        }
        if (bytes.length < 32)
            throw new IllegalArgumentException(
                    "AUTH_JWT_SECRET must decode to at least 32 random bytes");
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKeySpec key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKeySpec key, AuthProperties properties, UserRepository users, Clock clock) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamp = new JwtTimestampValidator(properties.clockSkew());
        timestamp.setClock(clock);
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        timestamp,
                        new JwtIssuerValidator(properties.issuer()),
                        new AccountTokenValidator(users, properties, clock)));
        return decoder;
    }

    @Bean
    ApplicationRunner initialAdministrator(
            BootstrapProperties properties, BootstrapService service) {
        return args -> service.initialize(properties);
    }
}

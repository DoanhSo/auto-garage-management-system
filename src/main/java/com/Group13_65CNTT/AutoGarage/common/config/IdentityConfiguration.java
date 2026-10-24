package com.Group13_65CNTT.AutoGarage.common.config;

import com.Group13_65CNTT.AutoGarage.auth.service.BootstrapService;
import com.Group13_65CNTT.AutoGarage.common.security.AccountTokenValidator;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.time.Clock;

@Configuration
@EnableTransactionManagement(order = 0)
@EnableConfigurationProperties({AuthProperties.class, BootstrapProperties.class, KeycloakAdminProperties.class})
public class IdentityConfiguration {
    @Bean
    Clock identityClock() {
        return Clock.systemUTC();
    }

    @Bean
    JwtDecoder jwtDecoder(AuthProperties properties, UserRepository users, Clock clock) {
        if (!properties.isJwkSetUriValid()) {
            throw new IllegalArgumentException(
                    "KEYCLOAK_JWKS_URI must be a valid http or https URI");
        }
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri())
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();
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

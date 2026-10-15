package com.Group13_65CNTT.AutoGarage;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityInfrastructureTests extends IntegrationTestSupport {
    @Autowired MockMvc mvc;
    @Autowired JwtEncoder encoder;
    @Autowired AuthProperties properties;
    @MockitoBean UserRepository users;

    @Test
    void databaseOutageReturns503WithoutDisguisingItAsInvalidCredentials() throws Exception {
        when(users.findById(anyLong()))
                .thenThrow(new DataAccessResourceFailureException("Injected database outage"));
        var now = Instant.now();
        var claims =
                JwtClaimsSet.builder()
                        .issuer(properties.issuer())
                        .audience(List.of(properties.audience()))
                        .subject("950005")
                        .issuedAt(now.minusSeconds(1))
                        .expiresAt(now.plusSeconds(300))
                        .id("infrastructure-test")
                        .claim("role", "CUSTOMER")
                        .claim("auth_version", 0L)
                        .build();
        String token =
                encoder.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                        .getTokenValue();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.message").value("Authentication service is unavailable"));
    }
}

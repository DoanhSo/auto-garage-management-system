package com.Group13_65CNTT.AutoGarage;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RequiredArgsConstructor
class SecurityInfrastructureTests extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final AuthProperties properties;
    @MockitoBean UserRepository users;

    @Test
    void databaseOutageReturns503WithoutDisguisingItAsInvalidCredentials() throws Exception {
        when(users.findByIdentityId(anyString()))
                .thenThrow(new DataAccessResourceFailureException("Injected database outage"));
        String token =
                issueKeycloakToken(
                        properties,
                        "550e8400-e29b-41d4-a716-446655440005",
                        "customer",
                        "CUSTOMER",
                        0L);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(4002))
                .andExpect(jsonPath("$.message").value("Authentication service is unavailable"));
    }
}

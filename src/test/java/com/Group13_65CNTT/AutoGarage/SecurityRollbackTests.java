package com.Group13_65CNTT.AutoGarage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.audit.service.AuditService;
import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RequiredArgsConstructor
class SecurityRollbackTests extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final UserRepository users;
    private final AuthProperties properties;
    @MockitoBean AuditService audit;

    @BeforeEach
    void fixtures() {
        assertTrue(
                jdbc.queryForObject("SELECT DATABASE()", String.class)
                        .startsWith("autogarage_test"));
        jdbc.update(
                "INSERT INTO users(id,identity_id,username,full_name,role)"
                    + " VALUES(950001,'550e8400-e29b-41d4-a716-446655950001','rollback.admin','Admin','ADMIN'),"
                    + "(950002,'550e8400-e29b-41d4-a716-446655950002','rollback.customer','Customer','CUSTOMER')");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM users WHERE username LIKE 'rollback.%'");
    }

    @Test
    void accountCreationRollsBackWhenAuditFails() throws Exception {
        String admin = token("rollback.admin");
        failAudit("USER_CREATED");
        mvc.perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "identityId", "rollback-created",
                                                        "username",
                                                        "rollback.created",
                                                        "fullName",
                                                        "Created",
                                                        "role",
                                                        "CUSTOMER"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unable to complete the request"));
        assertTrue(users.findByIdentifier("rollback.created").isEmpty());
    }

    @Test
    void roleAndTokenVersionRollBackWhenAuditFails() throws Exception {
        String admin = token("rollback.admin"), customer = token("rollback.customer");
        var before = users.findById(950002L).orElseThrow();
        failAudit("USER_UPDATED");
        mvc.perform(
                        put("/api/v1/admin/users/950002")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "fullName",
                                                        "Changed",
                                                        "role",
                                                        "RECEPTIONIST",
                                                        "active",
                                                        true,
                                                        "version",
                                                        before.getVersion()))))
                .andExpect(status().isInternalServerError());
        var after = users.findById(950002L).orElseThrow();
        assertEquals(Role.CUSTOMER, after.getRole());
        assertEquals(before.getAuthVersion(), after.getAuthVersion());
        assertEquals(before.getVersion(), after.getVersion());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk());
    }

    @Test
    void failedLogoutDoesNotPartiallyRevokeTokens() throws Exception {
        String customer = token("rollback.customer");
        failAudit("AUTH_LOGOUT_ALL");
        mvc.perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + customer))
                .andExpect(status().isInternalServerError());
        assertEquals(0, users.findById(950002L).orElseThrow().getAuthVersion());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk());
    }

    private void failAudit(String action) {
        doThrow(new IllegalStateException("Injected audit storage failure"))
                .when(audit)
                .record(any(), eq(action), any(), anyMap());
    }

    private String token(String username) {
        return issueKeycloakToken(properties, users.findByIdentifier(username).orElseThrow());
    }
}

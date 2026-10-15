package com.Group13_65CNTT.AutoGarage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.audit.service.AuditService;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityRollbackTests extends IntegrationTestSupport {
    private static final String PASSWORD = "RollbackTest!123";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @MockitoBean AuditService audit;

    @BeforeEach
    void fixtures() {
        assertTrue(
                jdbc.queryForObject("SELECT DATABASE()", String.class)
                        .startsWith("autogarage_test"));
        String hash = encoder.encode(PASSWORD);
        jdbc.update(
                "INSERT INTO users(id,username,password_hash,full_name,role)"
                    + " VALUES(950001,'rollback.admin',?,'Admin','ADMIN'),(950002,'rollback.customer',?,'Customer','CUSTOMER')",
                hash,
                hash);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM users WHERE username LIKE 'rollback.%'");
    }

    @Test
    void accountCreationRollsBackWhenAuditFails() throws Exception {
        String admin = login("rollback.admin");
        failAudit("USER_CREATED");
        mvc.perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "username",
                                                        "rollback.created",
                                                        "fullName",
                                                        "Created",
                                                        "role",
                                                        "CUSTOMER",
                                                        "password",
                                                        PASSWORD))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unable to complete the request"));
        assertTrue(users.findByIdentifier("rollback.created").isEmpty());
    }

    @Test
    void roleAndTokenVersionRollBackWhenAuditFails() throws Exception {
        String admin = login("rollback.admin"), customer = login("rollback.customer");
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
        String customer = login("rollback.customer");
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

    private String login(String username) throws Exception {
        var response =
                mvc.perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                json.writeValueAsString(
                                                        Map.of(
                                                                "identifier",
                                                                username,
                                                                "password",
                                                                PASSWORD))))
                        .andExpect(status().isOk())
                        .andReturn();
        return json.readTree(response.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }
}

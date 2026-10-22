package com.Group13_65CNTT.AutoGarage;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
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
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Intentionally NOT @Transactional: each HTTP thread uses an independent committed transaction. */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@RequiredArgsConstructor
class SecurityConcurrencyTests extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final UserRepository users;
    private final AuthProperties properties;

    @BeforeEach
    void fixtures() {
        assertTrue(
                jdbc.queryForObject("SELECT DATABASE()", String.class)
                        .startsWith("autogarage_test"),
                "Concurrency fixtures require a dedicated test database");
        jdbc.update(
                "INSERT INTO users(id,identity_id,username,full_name,role)"
                    + " VALUES(940001,'550e8400-e29b-41d4-a716-446655940001','concurrency.admin_a','A','ADMIN'),"
                    + "(940002,'550e8400-e29b-41d4-a716-446655940002','concurrency.admin_b','B','ADMIN'),"
                    + "(940003,'550e8400-e29b-41d4-a716-446655940003','concurrency.customer','C','CUSTOMER')");
    }

    @AfterEach
    void cleanup() {
        List<Long> ids =
                jdbc.queryForList(
                        "SELECT id FROM users WHERE username LIKE 'concurrency.%'", Long.class);
        for (Long id : ids)
            jdbc.update(
                    "DELETE FROM audit_events WHERE actor_user_id=? OR (entity_type='USER' AND"
                            + " entity_id=?)",
                    id,
                    id);
        for (Long id : ids) jdbc.update("DELETE FROM users WHERE id=?", id);
    }

    @Test
    void concurrentAdminDemotionsCannotRemoveTheLastAdministrator() throws Exception {
        String a = token("concurrency.admin_a"), b = token("concurrency.admin_b");
        long va = users.findById(940001L).orElseThrow().getVersion(),
                vb = users.findById(940002L).orElseThrow().getVersion();
        List<Integer> statuses = race(() -> update(a, 940001, va), () -> update(b, 940002, vb));
        assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE role='ADMIN' AND is_active=TRUE",
                        Integer.class));
    }

    @Test
    void concurrentUpdatesWithSameVersionHaveOnlyOneWinner() throws Exception {
        String admin = token("concurrency.admin_a");
        List<Integer> statuses =
                race(() -> update(admin, 940003, 0), () -> update(admin, 940003, 0));
        assertEquals(List.of(200, 409), statuses.stream().sorted().toList());
        assertEquals(1, users.findById(940003L).orElseThrow().getVersion());
    }

    @Test
    void concurrentDuplicateRegistrationCreatesOneAccountAndOneAudit() throws Exception {
        String admin = token("concurrency.admin_a");
        List<Integer> statuses = race(() -> create(admin), () -> create(admin));
        assertEquals(List.of(201, 409), statuses.stream().sorted().toList());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE username='concurrency.created'",
                        Integer.class));
        long created = users.findByIdentifier("concurrency.created").orElseThrow().getId();
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_events WHERE action='USER_CREATED' AND"
                                + " entity_id=?",
                        Integer.class,
                        created));
    }

    private String token(String username) {
        return issueKeycloakToken(properties, users.findByIdentifier(username).orElseThrow());
    }

    private int update(String token, long id, long version) throws Exception {
        return mvc.perform(
                        put("/api/v1/admin/users/" + id)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "fullName",
                                                        "Changed",
                                                        "role",
                                                        "CUSTOMER",
                                                        "active",
                                                        true,
                                                        "version",
                                                        version))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private int create(String token) throws Exception {
        return mvc.perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "username",
                                                        "concurrency.created",
                                                        "fullName",
                                                        "Created",
                                                        "role",
                                                        "CUSTOMER"))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    private List<Integer> race(CheckedRequest first, CheckedRequest second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                assertTrue(start.await(10, TimeUnit.SECONDS));
                                return first.run();
                            });
            var b =
                    executor.submit(
                            () -> {
                                ready.countDown();
                                assertTrue(start.await(10, TimeUnit.SECONDS));
                                return second.run();
                            });
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    @FunctionalInterface
    interface CheckedRequest {
        int run() throws Exception;
    }
}

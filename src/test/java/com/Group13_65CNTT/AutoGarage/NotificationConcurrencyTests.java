package com.Group13_65CNTT.AutoGarage;

import static org.assertj.core.api.Assertions.*;

import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.service.*;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Committed MySQL outbox and idempotency transactions")
@RequiredArgsConstructor
class NotificationConcurrencyTests extends IntegrationTestSupport {
    private final JdbcTemplate jdbc;
    private final NotificationService notifications;
    private final OutboxService outbox;
    private final NotificationAudit replay;
    private final PlatformTransactionManager manager;
    private static final String HASH = "a".repeat(64);

    @BeforeEach
    void fixture() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class))
                .startsWith("autogarage_test");
        jdbc.update(
                "INSERT INTO users(id,username,password_hash,full_name,role)"
                        + " VALUES(970001,'outbox.user','test-only','Outbox','ADMIN')");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.update(
                "DELETE FROM audit_events WHERE actor_user_id=970001 OR request_key LIKE 'outbox.%'"
                        + " OR (entity_type='NOTIFICATION' AND entity_id IN (SELECT id FROM"
                        + " notifications WHERE event_key LIKE 'outbox.%'))");
        jdbc.update("DELETE FROM notifications WHERE event_key LIKE 'outbox.%'");
        jdbc.update("DELETE FROM users WHERE id IN (970001,970002)");
    }

    @Test
    void workersClaimDisjointCommittedBatches() throws Exception {
        for (int i = 0; i < 4; i++) enqueue("outbox.race." + i);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> outbox.claim("worker-a"));
            var b = pool.submit(() -> outbox.claim("worker-b"));
            var first = a.get(10, TimeUnit.SECONDS);
            var second = b.get(10, TimeUnit.SECONDS);
            var ids = new HashSet<Long>();
            first.forEach(n -> ids.add(n.getId()));
            assertThat(second).noneMatch(n -> ids.contains(n.getId()));
            assertThat(first.size() + second.size()).isEqualTo(4);
        }
    }

    @Test
    void recoveredLeaseFencesAnOldWorkerAndFailureRetriesAreBounded() {
        long id = enqueue("outbox.recovery");
        outbox.claim("old-worker");
        jdbc.update(
                "UPDATE notifications SET locked_until=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 MINUTE)"
                        + " WHERE id=?",
                id);
        outbox.claim("new-worker");
        outbox.complete(id, "old-worker", "stale");
        assertThat(status(id)).isEqualTo("PROCESSING");
        outbox.fail(id, "new-worker");
        assertThat(status(id)).isEqualTo("PENDING");
        jdbc.update(
                "UPDATE notifications SET next_attempt_at=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1"
                        + " MINUTE) WHERE id=?",
                id);
        outbox.claim("final-worker");
        outbox.fail(id, "final-worker");
        assertThat(status(id)).isEqualTo("FAILED");
    }

    @Test
    void enqueueAndCompletionRecordRollBackTogether() {
        actor();
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(manager)
                                        .executeWithoutResult(
                                                tx -> {
                                                    enqueue("outbox.rollback");
                                                    replay.recordCompletedRequest(
                                                            970001,
                                                            "TEST",
                                                            "USER",
                                                            970001,
                                                            "outbox.rollback",
                                                            HASH,
                                                            "{\"id\":1}");
                                                    throw new IllegalStateException(
                                                            "rollback probe");
                                                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM notifications WHERE"
                                        + " event_key='outbox.rollback'",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_events WHERE"
                                        + " request_key='outbox.rollback'",
                                Integer.class))
                .isZero();
    }

    @Test
    void identicalConcurrentRequestsReplayOneCommittedMutation() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<String> call =
                    () -> {
                        actor();
                        try {
                            return new TransactionTemplate(manager)
                                    .execute(
                                            tx -> {
                                                String completed =
                                                        replay.findCompletedRequest(
                                                                "outbox.same", HASH);
                                                if (completed != null) return completed;
                                                jdbc.update(
                                                        "UPDATE users SET"
                                                            + " auth_version=auth_version+1 WHERE"
                                                            + " id=970001");
                                                replay.recordCompletedRequest(
                                                        970001,
                                                        "TEST",
                                                        "USER",
                                                        970001,
                                                        "outbox.same",
                                                        HASH,
                                                        "{\"id\":1}");
                                                return "{\"id\":1}";
                                            });
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    };
            var a = pool.submit(call);
            var b = pool.submit(call);
            assertThat(a.get(10, TimeUnit.SECONDS).replace(" ", ""))
                    .isEqualTo(b.get(10, TimeUnit.SECONDS).replace(" ", ""));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT auth_version FROM users WHERE id=970001", Long.class))
                    .isEqualTo(1);
        }
    }

    @Test
    void differentHashOrActorCannotReplayAnotherRequest() {
        actor();
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        tx ->
                                replay.recordCompletedRequest(
                                        970001, "TEST", "USER", 970001, "outbox.hash", HASH, "{}"));
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(manager)
                                        .execute(
                                                tx ->
                                                        replay.findCompletedRequest(
                                                                "outbox.hash", "b".repeat(64))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void anotherActorCannotReadACompletedRequestWithTheSameHash() {
        actor();
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        tx ->
                                replay.recordCompletedRequest(
                                        970001,
                                        "TEST",
                                        "USER",
                                        970001,
                                        "outbox.actor",
                                        HASH,
                                        "{}"));
        jdbc.update(
                "INSERT INTO users(id,username,password_hash,full_name,role)"
                        + " VALUES(970002,'outbox.other','test-only','Other','CUSTOMER')");
        Jwt other =
                Jwt.withTokenValue("test-only").header("alg", "HS256").subject("970002").build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                other, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(manager)
                                        .execute(
                                                tx ->
                                                        replay.findCompletedRequest(
                                                                "outbox.actor", HASH)))
                .isInstanceOf(BusinessException.class);
    }

    private long enqueue(String event) {
        return notifications
                .enqueue(
                        NotificationCreateRequest.builder()
                                .userId(970001L)
                                .eventKey(event)
                                .channel("WEB")
                                .destination("web")
                                .payloadJson("{}")
                                .build())
                .getId();
    }

    private String status(long id) {
        return jdbc.queryForObject("SELECT status FROM notifications WHERE id=?", String.class, id);
    }

    private void actor() {
        Jwt jwt = Jwt.withTokenValue("test-only").header("alg", "HS256").subject("970001").build();
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new JwtAuthenticationToken(
                                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }
}

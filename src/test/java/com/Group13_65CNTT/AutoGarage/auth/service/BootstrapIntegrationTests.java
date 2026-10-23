package com.Group13_65CNTT.AutoGarage.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.common.config.BootstrapProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class BootstrapIntegrationTests extends IntegrationTestSupport {
    private final BootstrapService bootstrap;
    private final UserRepository users;
    private final JdbcTemplate jdbc;

    @Test
    void disabledBootstrapCreatesNothing() {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        run(false, "bootstrap.disabled", "550e8400-e29b-41d4-a716-446655440001");
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
    }

    @Test
    void explicitBootstrapCreatesFirstKeycloakAdministratorWithoutLocalPassword() {
        run(true, "bootstrap.admin", "550e8400-e29b-41d4-a716-446655440002");
        var admin = users.findByIdentifier("bootstrap.admin").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isActive());
        assertNull(admin.getPasswordHash());
        assertEquals("550e8400-e29b-41d4-a716-446655440002", admin.getIdentityId());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_events WHERE action='ADMIN_BOOTSTRAPPED' AND"
                                + " entity_id=?",
                        Integer.class,
                        admin.getId()));
    }

    @Test
    void existingAdministratorIsIdempotentlyPreserved() {
        run(true, "bootstrap.admin", "550e8400-e29b-41d4-a716-446655440003");
        String identityId = users.findByIdentifier("bootstrap.admin").orElseThrow().getIdentityId();
        run(true, "bootstrap.admin", "550e8400-e29b-41d4-a716-446655440099");
        assertEquals(
                identityId,
                users.findByIdentifier("bootstrap.admin").orElseThrow().getIdentityId());
    }

    @Test
    void bootstrapRefusesToElevateAnExistingCustomer() {
        users.saveAndFlush(
                User.builder()
                        .username("bootstrap.customer")
                        .fullName("Customer")
                        .role(Role.CUSTOMER)
                        .build());
        var failure =
                assertThrows(
                        BusinessException.class,
                        () -> run(true, "bootstrap.customer", null));
        assertEquals(ResponseCode.BOOTSTRAP_ACCOUNT_CONFLICT, failure.getResponseCode());
        assertEquals(
                Role.CUSTOMER,
                users.findByIdentifier("bootstrap.customer").orElseThrow().getRole());
    }

    @Test
    void bootstrapDoesNotCreateAnotherAccountWhenAnAdminAlreadyExists() {
        users.saveAndFlush(
                User.builder()
                        .username("bootstrap.existing")
                        .fullName("Admin")
                        .role(Role.ADMIN)
                        .build());
        run(true, "bootstrap.new", null);
        assertTrue(users.findByIdentifier("bootstrap.new").isEmpty());
    }

    private void run(boolean enabled, String username, String identityId) {
        bootstrap.initialize(
                new BootstrapProperties(enabled, username, "Bootstrap Admin", identityId));
    }
}

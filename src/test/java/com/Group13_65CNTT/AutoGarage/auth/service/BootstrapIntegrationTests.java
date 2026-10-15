package com.Group13_65CNTT.AutoGarage.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.common.config.BootstrapProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BootstrapIntegrationTests extends IntegrationTestSupport {
    @Autowired BootstrapService bootstrap;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired JdbcTemplate jdbc;

    @Test
    void disabledBootstrapCreatesNothing() throws Exception {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        run(false, "bootstrap.disabled", "BootstrapPassword!123");
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
    }

    @Test
    void explicitBootstrapCreatesAHashedFirstAdministrator() throws Exception {
        run(true, "bootstrap.admin", "BootstrapPassword!123");
        var admin = users.findByIdentifier("bootstrap.admin").orElseThrow();
        assertEquals(Role.ADMIN, admin.getRole());
        assertTrue(admin.isActive());
        assertTrue(passwords.matches("BootstrapPassword!123", admin.getPasswordHash()));
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_events WHERE action='ADMIN_BOOTSTRAPPED' AND"
                                + " entity_id=?",
                        Integer.class,
                        admin.getId()));
    }

    @Test
    void existingAdministratorPasswordIsNeverReset() throws Exception {
        run(true, "bootstrap.admin", "BootstrapPassword!123");
        String hash = users.findByIdentifier("bootstrap.admin").orElseThrow().getPasswordHash();
        run(true, "bootstrap.admin", "DifferentPassword!123");
        assertEquals(
                hash, users.findByIdentifier("bootstrap.admin").orElseThrow().getPasswordHash());
    }

    @Test
    void bootstrapRefusesToElevateAnExistingCustomer() {
        users.saveAndFlush(
                User.builder()
                        .username("bootstrap.customer")
                        .passwordHash(passwords.encode("BootstrapPassword!123"))
                        .fullName("Customer")
                        .role(Role.CUSTOMER)
                        .build());
        var failure =
                assertThrows(
                        BusinessException.class,
                        () -> run(true, "bootstrap.customer", "DifferentPassword!123"));
        assertEquals(ResponseCode.BOOTSTRAP_ACCOUNT_CONFLICT, failure.getResponseCode());
        assertEquals(
                Role.CUSTOMER,
                users.findByIdentifier("bootstrap.customer").orElseThrow().getRole());
    }

    @Test
    void bootstrapDoesNotCreateAnotherAccountWhenAnAdminAlreadyExists() throws Exception {
        users.saveAndFlush(
                User.builder()
                        .username("bootstrap.existing")
                        .passwordHash(passwords.encode("BootstrapPassword!123"))
                        .fullName("Admin")
                        .role(Role.ADMIN)
                        .build());
        run(true, "bootstrap.new", "BootstrapPassword!123");
        assertTrue(users.findByIdentifier("bootstrap.new").isEmpty());
    }

    private void run(boolean enabled, String username, String password) throws Exception {
        bootstrap.initialize(
                new BootstrapProperties(enabled, username, "Bootstrap Admin", password));
    }
}

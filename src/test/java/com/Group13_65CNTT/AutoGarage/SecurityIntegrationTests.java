package com.Group13_65CNTT.AutoGarage;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SecurityIntegrationTests.AccessProbe.class)
@Transactional
class SecurityIntegrationTests extends IntegrationTestSupport {
    private static final String PASSWORD = "SecurityTest!123";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    @Autowired AuthProperties properties;
    @Autowired UserRepository users;
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @BeforeEach
    void fixtures() {
        cleanupFailedAttempts();
        String hash = passwords.encode(PASSWORD);
        String[] names = {
            "security_admin",
            "security_reception",
            "security_warehouse",
            "security_tech",
            "security_customer_a",
            "security_customer_b"
        };
        String[] roles = {
            "ADMIN", "RECEPTIONIST", "WAREHOUSE_STAFF", "TECHNICIAN", "CUSTOMER", "CUSTOMER"
        };
        for (int i = 0; i < names.length; i++)
            jdbc.update(
                    "INSERT INTO users(id,username,email,password_hash,full_name,role)"
                            + " VALUES(?,?,?,?,?,?)",
                    920001 + i,
                    names[i],
                    names[i] + "@example.test",
                    hash,
                    names[i],
                    roles[i]);
        jdbc.update(
                "INSERT INTO customers(id,user_id,customer_code,full_name,phone)"
                    + " VALUES(920001,920005,'SEC_A','A','01'),(920002,920006,'SEC_B','B','02')");
        jdbc.update(
                "INSERT INTO vehicles(id,customer_id,license_plate,brand,model)"
                    + " VALUES(920001,920001,'SEC_A','Test','Test'),(920002,920002,'SEC_B','Test','Test')");
        jdbc.update(
                "INSERT INTO services(id,service_code,service_name,unit_price,standard_minutes)"
                        + " VALUES(920001,'SEC_S','Service',100,30)");
        jdbc.update(
                "INSERT INTO"
                    + " bookings(id,booking_code,customer_id,vehicle_id,service_id,appointment_at,expected_end_at)"
                    + " VALUES(920001,'SEC_BKA',920001,920001,920001,'2026-10-14"
                    + " 01:00:00','2026-10-14"
                    + " 02:00:00'),(920002,'SEC_BKB',920002,920002,920001,'2026-10-14"
                    + " 01:00:00','2026-10-14 02:00:00')");
        jdbc.update(
                "INSERT INTO"
                    + " repair_orders(id,order_code,customer_id,vehicle_id,odometer_in,received_by)"
                    + " VALUES(920001,'SEC_RA',920001,920001,100,920002),(920002,'SEC_RB',920002,920002,100,920002)");
        jdbc.update(
                "INSERT INTO estimates(id,estimate_code,repair_order_id,version_no,created_by)"
                        + " VALUES(920001,'SEC_E',920001,1,920002)");
        jdbc.update(
                "INSERT INTO"
                    + " estimate_items(id,estimate_id,line_no,item_type,service_id,item_name,unit,quantity,unit_price,line_total,approval_status)"
                    + " VALUES(920001,920001,1,'SERVICE',920001,'Service','job',1,100,100,'APPROVED')");
        jdbc.update(
                "INSERT INTO repair_tasks(id,repair_order_id,estimate_item_id,service_id,task_name)"
                        + " VALUES(920001,920001,920001,920001,'Task')");
        jdbc.update(
                "INSERT INTO employees(id,user_id,employee_code) VALUES(920001,920004,'SEC_TECH')");
        jdbc.update(
                "INSERT INTO"
                    + " task_assignments(id,task_id,employee_id,assignment_role,starts_at,ends_at,assigned_by)"
                    + " VALUES(920001,920001,920001,'MAIN','2026-10-14 01:00:00','2026-10-14"
                    + " 02:00:00',920002)");
        jdbc.update(
                "INSERT INTO"
                    + " invoices(id,invoice_code,repair_order_id,customer_id,customer_snapshot,vehicle_snapshot,subtotal,total_amount,issued_by)"
                    + " VALUES(920001,'SEC_IA',920001,920001,'{}','{}',100,100,920002),(920002,'SEC_IB',920002,920002,'{}','{}',100,100,920002)");
        jdbc.update(
                "INSERT INTO"
                    + " invoice_items(id,invoice_id,line_no,item_type,repair_task_id,item_name,unit,quantity,unit_price,unit_cost,line_total)"
                    + " VALUES(920001,920001,1,'SERVICE',920001,'Service','job',1,100,20,100)");
        jdbc.update(
                "INSERT INTO"
                    + " warranties(id,warranty_code,invoice_item_id,vehicle_id,customer_id,starts_at,expires_at,start_odometer,warranty_days)"
                    + " VALUES(920001,'SEC_W',920001,920001,920001,'2026-10-14','2026-11-13',100,30)");
        jdbc.update(
                "INSERT INTO"
                        + " warranty_claims(id,claim_code,warranty_id,current_odometer,description)"
                        + " VALUES(920001,'SEC_CL',920001,150,'Test')");
        jdbc.update(
                "INSERT INTO"
                    + " attachments(id,repair_order_id,storage_key,original_filename,content_type,size_bytes,category,uploaded_by)"
                    + " VALUES(920001,920001,'sec/a.jpg','a.jpg','image/jpeg',10,'CHECK_IN',920002),(920002,920002,'sec/b.jpg','b.jpg','image/jpeg',10,'CHECK_IN',920002)");
        jdbc.update(
                "INSERT INTO"
                    + " attachments(id,warranty_claim_id,storage_key,original_filename,content_type,size_bytes,category,uploaded_by)"
                    + " VALUES(920003,920001,'sec/claim.jpg','claim.jpg','image/jpeg',10,'WARRANTY',920005)");
        jdbc.update(
                "INSERT INTO"
                    + " notifications(id,user_id,recipient_key,destination,event_key,channel,payload_json)"
                    + " VALUES(920001,920005,'user:920005','web','SEC_NOTICE_A','WEB','{}'),(920002,920006,'user:920006','web','SEC_NOTICE_B','WEB','{}')");
    }

    @Test
    void loginAndMeWorkWithoutLeakingCredentials() throws Exception {
        var result =
                perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                body(
                                                        Map.of(
                                                                "identifier",
                                                                "SECURITY_CUSTOMER_A",
                                                                "password",
                                                                PASSWORD))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                        .andExpect(jsonPath("$.data.user.role").value("CUSTOMER"))
                        .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist())
                        .andReturn();
        String token =
                json.readTree(result.getResponse().getContentAsString())
                        .path("data")
                        .path("accessToken")
                        .asText();
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(920005));
        assertFalse(result.getResponse().getContentAsString().contains(PASSWORD));
        assertFalse(result.getResponse().getContentAsString().contains("$2a$"));
    }

    @Test
    void emailLoginIsSupported() throws Exception {
        assertFalse(login("SECURITY_CUSTOMER_A@EXAMPLE.TEST", PASSWORD).isBlank());
    }

    @Test
    void unknownWrongPasswordAndInactiveHaveSamePublicError() throws Exception {
        String wrong = loginFailure("security_customer_a", "wrong-password");
        assertEquals(wrong, loginFailure("missing_user", "wrong-password"));
        jdbc.update("UPDATE users SET is_active=FALSE WHERE id=920005");
        assertEquals(wrong, loginFailure("security_customer_a", PASSWORD));
        var independent =
                new org.springframework.transaction.support.TransactionTemplate(transactions);
        independent.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Integer attempts =
                independent.execute(
                        status ->
                                jdbc.queryForObject(
                                        "SELECT COUNT(*) FROM audit_events WHERE"
                                                + " action='AUTH_LOGIN_FAILED'",
                                        Integer.class));
        assertEquals(3, attempts.intValue());
    }

    @Test
    void anonymousAndMalformedBearerAreRejectedWithJson() throws Exception {
        perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(3003));
        perform(get("/api/v1/auth/me").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenCannotBeSentAsQueryParameter() throws Exception {
        perform(
                        get("/api/v1/auth/me")
                                .param("access_token", login("security_customer_a", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJsonAndBlankLoginAreBadRequests() throws Exception {
        perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{broken"))
                .andExpect(status().isBadRequest());
        perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(Map.of("identifier", " ", "password", " "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expiredFutureWrongIssuerAndAudienceTokensAreRejected() throws Exception {
        rejectToken(
                token(
                        Map.of(
                                "exp",
                                Instant.now().minusSeconds(100),
                                "iat",
                                Instant.now().minusSeconds(200),
                                "nbf",
                                Instant.now().minusSeconds(200))));
        rejectToken(
                token(
                        Map.of(
                                "iat",
                                Instant.now().plusSeconds(120),
                                "nbf",
                                Instant.now().plusSeconds(120),
                                "exp",
                                Instant.now().plusSeconds(600))));
        rejectToken(token(Map.of("iss", "wrong-issuer")));
        rejectToken(token(Map.of("aud", List.of("wrong-audience"))));
    }

    @Test
    void requiredClaimsAndClaimTypesAreValidated() throws Exception {
        for (String missing : List.of("exp", "iat", "jti", "role", "auth_version", "sub")) {
            var changes = new HashMap<String, Object>();
            changes.put(missing, null);
            rejectToken(token(changes));
        }
        rejectToken(token(Map.of("sub", "not-a-number")));
        rejectToken(token(Map.of("role", "MANAGER")));
        rejectToken(token(Map.of("auth_version", "0")));
        rejectToken(token(Map.of("auth_version", 0.5)));
    }

    @Test
    void wrongSignatureAndAlgorithmAreRejected() throws Exception {
        byte[] other = new byte[64];
        new java.security.SecureRandom().nextBytes(other);
        JwtEncoder wrong =
                NimbusJwtEncoder.withSecretKey(new SecretKeySpec(other, "HmacSHA256"))
                        .algorithm(MacAlgorithm.HS256)
                        .build();
        rejectToken(
                wrong.encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(),
                                        claims(Map.of())))
                        .getTokenValue());
        JwtEncoder algorithm =
                NimbusJwtEncoder.withSecretKey(new SecretKeySpec(other, "HmacSHA512"))
                        .algorithm(MacAlgorithm.HS512)
                        .build();
        rejectToken(
                algorithm
                        .encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS512).build(),
                                        claims(Map.of())))
                        .getTokenValue());
    }

    @Test
    void logoutRevokesAllTokensButDoesNotAffectAnotherUser() throws Exception {
        String first = login("security_customer_a", PASSWORD),
                second = login("security_customer_a", PASSWORD),
                other = login("security_customer_b", PASSWORD);
        perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        rejectToken(first);
        rejectToken(second);
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk());
        assertFalse(login("security_customer_a", PASSWORD).isBlank());
    }

    @Test
    void passwordChangeHashesPasswordAndRevokesTokens() throws Exception {
        String token = login("security_customer_a", PASSWORD), next = "NewSecurePassword!123";
        perform(
                        post("/api/v1/auth/password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body(
                                                Map.of(
                                                        "currentPassword",
                                                        PASSWORD,
                                                        "newPassword",
                                                        next))))
                .andExpect(status().isOk());
        rejectToken(token);
        String hash =
                jdbc.queryForObject(
                        "SELECT password_hash FROM users WHERE id=920005", String.class);
        assertNotEquals(next, hash);
        assertTrue(passwords.matches(next, hash));
        loginFailure("security_customer_a", PASSWORD);
        assertFalse(login("security_customer_a", next).isBlank());
    }

    @Test
    void wrongOldPasswordAndOverlongUtf8CannotMutatePassword() throws Exception {
        String token = login("security_customer_a", PASSWORD),
                hash = users.findById(920005L).orElseThrow().getPasswordHash();
        perform(
                        post("/api/v1/auth/password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body(
                                                Map.of(
                                                        "currentPassword",
                                                        "wrong-password",
                                                        "newPassword",
                                                        "AnotherSecure!123"))))
                .andExpect(status().isUnauthorized());
        perform(
                        post("/api/v1/auth/password")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        body(
                                                Map.of(
                                                        "currentPassword",
                                                        PASSWORD,
                                                        "newPassword",
                                                        "密".repeat(30)))))
                .andExpect(status().isBadRequest());
        assertEquals(hash, users.findById(920005L).orElseThrow().getPasswordHash());
    }

    @Test
    void everyNonAdminRoleCannotManageAccounts() throws Exception {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        for (String name :
                List.of(
                        "security_reception",
                        "security_warehouse",
                        "security_tech",
                        "security_customer_a")) {
            String token = login(name, PASSWORD);
            perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
            perform(
                            post("/api/v1/admin/users")
                                    .header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(createBody("blocked")))
                    .andExpect(status().isForbidden());
            perform(
                            delete("/api/v1/admin/users/920006")
                                    .param("version", "0")
                                    .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        assertTrue(users.findById(920006L).orElseThrow().isActive());
    }

    @Test
    void adminCanCreatePageAndReadUsersWithSafeDtos() throws Exception {
        String token = login("security_admin", PASSWORD);
        perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody("Created_User")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.username").value("created_user"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        assertTrue(
                passwords.matches(
                        PASSWORD,
                        users.findByIdentifier("created_user").orElseThrow().getPasswordHash()));
        perform(
                        get("/api/v1/admin/users")
                                .param("size", "2")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
        perform(get("/api/v1/admin/users/920005").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void duplicateInvalidRoleBadIdAndBadPageAreControlledErrors() throws Exception {
        String token = login("security_admin", PASSWORD);
        perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody("SECURITY_CUSTOMER_A")))
                .andExpect(status().isConflict());
        perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody("bad_role").replace("CUSTOMER", "MANAGER")))
                .andExpect(status().isBadRequest());
        perform(get("/api/v1/admin/users/not-an-id").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        perform(get("/api/v1/admin/users/0").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        perform(get("/api/v1/admin/users/999999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        perform(
                        get("/api/v1/admin/users")
                                .param("size", "101")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void roleChangeImmediatelyRevokesOldTokensAndAllowsNewRole() throws Exception {
        String admin = login("security_admin", PASSWORD),
                customer = login("security_customer_a", PASSWORD);
        long version = users.findById(920005L).orElseThrow().getVersion();
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("RECEPTIONIST", true, version)))
                .andExpect(status().isOk());
        rejectToken(customer);
        String refreshed = login("security_customer_a", PASSWORD);
        access(refreshed, "INVOICE", 920002, 200);
    }

    @Test
    void profileUpdateKeepsPasswordAndDetectsStaleVersion() throws Exception {
        String admin = login("security_admin", PASSWORD);
        var original = users.findById(920005L).orElseThrow();
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", true, original.getVersion())))
                .andExpect(status().isOk());
        assertEquals(
                original.getPasswordHash(),
                users.findById(920005L).orElseThrow().getPasswordHash());
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", true, original.getVersion())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2005));
    }

    @Test
    void lastAdminCannotBeDisabledOrDemoted() throws Exception {
        String admin = login("security_admin", PASSWORD);
        long version = users.findById(920001L).orElseThrow().getVersion();
        perform(
                        delete("/api/v1/admin/users/920001")
                                .param("version", Long.toString(version))
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2006));
        perform(
                        put("/api/v1/admin/users/920001")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", true, version)))
                .andExpect(status().isConflict());
    }

    @Test
    void deletionPreservesHistoryButRemovesManagementAccessAndInvalidatesToken() throws Exception {
        String admin = login("security_admin", PASSWORD),
                customer = login("security_customer_b", PASSWORD);
        perform(
                        delete("/api/v1/admin/users/920006")
                                .param(
                                        "version",
                                        Long.toString(
                                                users.findById(920006L).orElseThrow().getVersion()))
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        assertFalse(users.findById(920006L).orElseThrow().isActive());
        var deleted = users.findById(920006L).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM customers WHERE user_id=920006", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action='USER_DELETED' AND entity_id=920006", Long.class));
        perform(get("/api/v1/admin/users/920006").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        perform(get("/api/v1/admin/users").param("keyword", "security_customer_b")
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        perform(put("/api/v1/admin/users/920006").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("CUSTOMER", true, deleted.getVersion())))
                .andExpect(status().isNotFound());
        perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(body(Map.of("identifier", "security_customer_b", "password", PASSWORD))))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(3005));
        rejectToken(customer);
    }

    @Test
    void lockingCanBeReversedWithoutDeletingTheAccount() throws Exception {
        String admin = login("security_admin", PASSWORD), customer = login("security_customer_b", PASSWORD);
        long version = users.findById(920006L).orElseThrow().getVersion();
        perform(put("/api/v1/admin/users/920006").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("CUSTOMER", false, version)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(false));
        var locked = users.findById(920006L).orElseThrow();
        assertNull(locked.getDeletedAt());
        rejectToken(customer);
        perform(get("/api/v1/admin/users").param("keyword", "security_customer_b").param("active", "false")
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        perform(put("/api/v1/admin/users/920006").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("CUSTOMER", true, locked.getVersion())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.active").value(true));
        assertNotNull(login("security_customer_b", PASSWORD));
    }

    @Test
    void staleDeleteCannotRemoveAnUpdatedAccount() throws Exception {
        String admin = login("security_admin", PASSWORD);
        long stale = users.findById(920006L).orElseThrow().getVersion();
        perform(put("/api/v1/admin/users/920006").header("Authorization", "Bearer " + admin)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody("CUSTOMER", false, stale)))
                .andExpect(status().isOk());
        perform(delete("/api/v1/admin/users/920006").param("version", Long.toString(stale))
                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(2005));
        assertNull(users.findById(920006L).orElseThrow().getDeletedAt());
    }

    @Test
    void customerCannotAccessAnotherCustomersResources() throws Exception {
        String customer = login("security_customer_a", PASSWORD);
        for (String type :
                List.of(
                        "CUSTOMER",
                        "VEHICLE",
                        "BOOKING",
                        "REPAIR_ORDER",
                        "INVOICE",
                        "ATTACHMENT",
                        "NOTIFICATION")) {
            access(customer, type, 920001, 200);
            access(customer, type, 920002, 403);
        }
        for (String type : List.of("ESTIMATE", "TASK", "WARRANTY", "WARRANTY_CLAIM"))
            access(customer, type, 920001, 200);
        access(customer, "ATTACHMENT", 920003, 200);
    }

    @Test
    void ownershipTransferDoesNotExposeOldInvoiceOrPhotos() throws Exception {
        String oldOwner = login("security_customer_a", PASSWORD),
                newOwner = login("security_customer_b", PASSWORD);
        jdbc.update("UPDATE vehicles SET customer_id=920002 WHERE id=920001");
        access(oldOwner, "VEHICLE", 920001, 403);
        access(newOwner, "VEHICLE", 920001, 200);
        access(oldOwner, "INVOICE", 920001, 200);
        access(newOwner, "INVOICE", 920001, 403);
        access(newOwner, "ATTACHMENT", 920001, 403);
        access(newOwner, "REPAIR_ORDER", 920001, 403);
    }

    @Test
    void technicianNeedsAnActiveAssignmentAndCannotReadFinance() throws Exception {
        String technician = login("security_tech", PASSWORD);
        access(technician, "REPAIR_ORDER", 920001, 200);
        access(technician, "REPAIR_ORDER", 920002, 403);
        access(technician, "TASK", 920001, 200);
        access(technician, "ATTACHMENT", 920001, 200);
        access(technician, "VEHICLE", 920001, 200);
        access(technician, "INVOICE", 920001, 403);
        jdbc.update("UPDATE task_assignments SET is_active=FALSE WHERE id=920001");
        access(technician, "REPAIR_ORDER", 920001, 403);
    }

    @Test
    void qcRequiresBothCapabilityAndQcAssignment() throws Exception {
        String technician = login("security_tech", PASSWORD);
        qc(technician, 403);
        jdbc.update("UPDATE employees SET can_qc=TRUE WHERE id=920001");
        qc(technician, 403);
        jdbc.update(
                "INSERT INTO"
                    + " task_assignments(task_id,employee_id,assignment_role,starts_at,ends_at,assigned_by)"
                    + " VALUES(920001,920001,'QC','2026-10-14 01:00:00','2026-10-14"
                    + " 02:00:00',920002)");
        qc(technician, 200);
        jdbc.update("UPDATE employees SET can_qc=FALSE WHERE id=920001");
        qc(technician, 403);
    }

    @Test
    void warehouseCannotReadCustomerFinanceOrEvidence() throws Exception {
        String warehouse = login("security_warehouse", PASSWORD);
        access(warehouse, "REPAIR_ORDER", 920001, 200);
        for (String type : List.of("CUSTOMER", "INVOICE", "ATTACHMENT", "VEHICLE"))
            access(warehouse, type, 920001, 403);
    }

    @Test
    void actorParameterCannotImpersonateAnotherUser() throws Exception {
        String customer = login("security_customer_a", PASSWORD);
        perform(
                        get("/api/v1/auth/testing/spoof/920001")
                                .header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void noticesStayPrivateEvenForAdminAndRecipientsCannotBeConfused() throws Exception {
        String admin = login("security_admin", PASSWORD),
                customer = login("security_customer_b", PASSWORD);
        access(admin, "NOTIFICATION", 920001, 403);
        jdbc.update("UPDATE notifications SET customer_id=920002 WHERE id=920001");
        access(customer, "NOTIFICATION", 920001, 403); // explicit user recipient takes precedence
    }

    @Test
    void corsOnlyAllowsConfiguredOriginsAndUnknownRoutesStayDenied() throws Exception {
        perform(
                        options("/api/v1/auth/login")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        perform(
                        options("/api/v1/auth/login")
                                .header("Origin", "https://untrusted.example")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(3002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
        perform(
                        get("/api/v1/unregistered")
                                .header(
                                        "Authorization",
                                        "Bearer " + login("security_admin", PASSWORD)))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityAuditNeverContainsPasswordsOrBearerTokens() throws Exception {
        String token = login("security_customer_a", PASSWORD);
        perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        String data = jdbc.queryForList("SELECT action,after_json FROM audit_events").toString();
        assertFalse(data.contains(PASSWORD));
        assertFalse(data.contains(token));
        assertFalse(data.contains("$2a$"));
    }

    @Test
    void adminCanCreateAllFiveRolesButNoOtherRole() throws Exception {
        String admin = login("security_admin", PASSWORD);
        for (String role :
                List.of("ADMIN", "RECEPTIONIST", "WAREHOUSE_STAFF", "TECHNICIAN", "CUSTOMER")) {
            perform(
                            post("/api/v1/admin/users")
                                    .header("Authorization", "Bearer " + admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            body(
                                                    Map.of(
                                                            "username",
                                                            "role_" + role,
                                                            "fullName",
                                                            "Role test",
                                                            "role",
                                                            role,
                                                            "password",
                                                            PASSWORD))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.role").value(role));
        }
    }

    @Test
    void unknownWritableFieldsAndPartialPutCannotChangeCredentials() throws Exception {
        String admin = login("security_admin", PASSWORD);
        var user = users.findById(920005L).orElseThrow();
        var payload = new HashMap<String, Object>();
        payload.put("fullName", "Profile");
        payload.put("role", "CUSTOMER");
        payload.put("active", true);
        payload.put("version", user.getVersion());
        payload.put("password", "UnwantedPassword!123");
        payload.put("id", 920006);
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(payload)))
                .andExpect(status().isBadRequest());
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(Map.of("fullName", "Incomplete"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
        assertEquals(
                user.getPasswordHash(), users.findById(920005L).orElseThrow().getPasswordHash());
        assertEquals(user.getFullName(), users.findById(920005L).orElseThrow().getFullName());
    }

    @Test
    void missingParametersWrongMethodsAndUnsupportedMediaHaveCorrectStatuses() throws Exception {
        String admin = login("security_admin", PASSWORD);
        perform(delete("/api/v1/admin/users/920005").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        perform(get("/api/v1/auth/login").header("Authorization", "Bearer " + admin))
                .andExpect(status().isMethodNotAllowed());
        perform(get("/api/v1/auth/missing").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_XML)
                                .content("<login/>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    private org.springframework.test.web.servlet.ResultActions perform(
            org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
        entityManager.flush();
        entityManager.clear();
        return mvc.perform(request);
    }

    private String body(Object value) {
        return json.writeValueAsString(value);
    }

    @org.junit.jupiter.api.AfterEach
    void cleanupFailedAttempts() {
        var independent =
                new org.springframework.transaction.support.TransactionTemplate(transactions);
        independent.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        independent.executeWithoutResult(
                status -> {
                    assertTrue(
                            jdbc.queryForObject("SELECT DATABASE()", String.class)
                                    .startsWith("autogarage_test"));
                    jdbc.update(
                            "DELETE FROM audit_events WHERE action='AUTH_LOGIN_FAILED' AND"
                                    + " actor_user_id IS NULL");
                });
    }

    private String createBody(String name) {
        return body(
                Map.of(
                        "username",
                        name,
                        "email",
                        name + "@example.test",
                        "fullName",
                        "Created User",
                        "role",
                        "CUSTOMER",
                        "password",
                        PASSWORD));
    }

    private String updateBody(String role, boolean active, long version) {
        return body(
                Map.of(
                        "fullName",
                        "Updated User",
                        "role",
                        role,
                        "active",
                        active,
                        "version",
                        version));
    }

    private String login(String name, String password) throws Exception {
        MvcResult response =
                perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                body(
                                                        Map.of(
                                                                "identifier",
                                                                name,
                                                                "password",
                                                                password))))
                        .andExpect(status().isOk())
                        .andReturn();
        return json.readTree(response.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
    }

    private String loginFailure(String name, String password) throws Exception {
        String response =
                perform(
                                post("/api/v1/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                body(
                                                        Map.of(
                                                                "identifier",
                                                                name,
                                                                "password",
                                                                password))))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code").value(3005))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.readTree(response).path("message").asText();
    }

    private JwtClaimsSet claims(Map<String, Object> changes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", properties.issuer());
        claims.put("aud", List.of(properties.audience()));
        claims.put("sub", "920005");
        claims.put("iat", Instant.now().minusSeconds(1));
        claims.put("nbf", Instant.now().minusSeconds(1));
        claims.put("exp", Instant.now().plusSeconds(300));
        claims.put("jti", "test-id");
        claims.put("role", "CUSTOMER");
        claims.put("auth_version", 0L);
        claims.putAll(changes);
        claims.values().removeIf(java.util.Objects::isNull);
        return JwtClaimsSet.builder().claims(c -> c.putAll(claims)).build();
    }

    private String token(Map<String, Object> changes) {
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims(changes)))
                .getTokenValue();
    }

    private void rejectToken(String token) throws Exception {
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private void access(String token, String resource, long id, int status) throws Exception {
        perform(
                        get("/api/v1/auth/testing/access/" + resource + "/" + id)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().is(status));
    }

    private void qc(String token, int status) throws Exception {
        perform(get("/api/v1/auth/testing/qc/920001").header("Authorization", "Bearer " + token))
                .andExpect(status().is(status));
    }

    /** Test-only endpoints to exercise I01 through the actual bearer filter. */
    @RestController
    static class AccessProbe {
        private final ObjectAccessService access;
        private final CurrentUserProvider actor;

        AccessProbe(ObjectAccessService access, CurrentUserProvider actor) {
            this.access = access;
            this.actor = actor;
        }

        @GetMapping("/api/v1/auth/testing/access/{type}/{id}")
        ApiResponse<Void> access(@PathVariable AccessResource type, @PathVariable long id) {
            access.requireResourceAccess(actor.userId(), type, id);
            return ApiResponse.success(ResponseCode.SUCCESS, null, "/api/v1/auth/testing");
        }

        @GetMapping("/api/v1/auth/testing/qc/{id}")
        ApiResponse<Void> qc(@PathVariable long id) {
            access.requireQualityCheckAccess(actor.userId(), id);
            return ApiResponse.success(ResponseCode.SUCCESS, null, "/api/v1/auth/testing");
        }

        @GetMapping("/api/v1/auth/testing/spoof/{id}")
        ApiResponse<Void> spoof(@PathVariable long id) {
            access.requireRole(id, "ADMIN");
            return ApiResponse.success(ResponseCode.SUCCESS, null, "/api/v1/auth/testing");
        }
    }
}

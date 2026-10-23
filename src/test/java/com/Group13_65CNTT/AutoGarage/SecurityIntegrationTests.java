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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import tools.jackson.databind.ObjectMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
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
@RequiredArgsConstructor
class SecurityIntegrationTests extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    private final AuthProperties properties;
    private final UserRepository users;
    private final jakarta.persistence.EntityManager entityManager;

    @BeforeEach
    void fixtures() {
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
                    "INSERT INTO users(id,identity_id,username,email,full_name,role)"
                            + " VALUES(?,?,?,?,?,?)",
                    920001 + i,
                    "550e8400-e29b-41d4-a716-44665592000" + (i + 1),
                    names[i],
                    names[i] + "@example.test",
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
    void keycloakBearerTokenAuthenticatesAndSynchronizesIdentityIdOnFirstAccess()
            throws Exception {
        jdbc.update(
                "INSERT INTO users(id,identity_id,username,email,full_name,role)"
                        + " VALUES(920099,NULL,'unlinked_customer','unlinked@example.test','Unlinked','CUSTOMER')");
        String firstToken =
                issueKeycloakToken(
                        properties,
                        "550e8400-e29b-41d4-a716-446655920099",
                        "unlinked_customer",
                        "CUSTOMER",
                        0L);
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(920099))
                .andExpect(
                        jsonPath("$.data.identityId")
                                .value("550e8400-e29b-41d4-a716-446655920099"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        var linked = users.findById(920099L).orElseThrow();
        assertEquals("550e8400-e29b-41d4-a716-446655920099", linked.getIdentityId());
        assertNotNull(linked.getLastLoginAt());
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
        perform(get("/api/v1/auth/me").param("access_token", token("security_customer_a")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredFutureWrongIssuerAndAudienceTokensAreRejected() throws Exception {
        rejectToken(
                customToken(
                        Map.of(
                                "exp",
                                Instant.now().minusSeconds(100),
                                "iat",
                                Instant.now().minusSeconds(200),
                                "nbf",
                                Instant.now().minusSeconds(200))));
        rejectToken(
                customToken(
                        Map.of(
                                "iat",
                                Instant.now().plusSeconds(120),
                                "nbf",
                                Instant.now().plusSeconds(120),
                                "exp",
                                Instant.now().plusSeconds(600))));
        rejectToken(customToken(Map.of("iss", "http://wrong-issuer.example/realms/test")));
        rejectToken(customToken(Map.of("aud", List.of("wrong-audience"))));
    }

    @Test
    void requiredClaimsAndKeycloakRoleClaimsAreValidated() throws Exception {
        for (String missing : List.of("exp", "iat", "jti", "realm_access", "sub")) {
            var changes = new HashMap<String, Object>();
            changes.put(missing, null);
            rejectToken(customToken(changes));
        }
        rejectToken(customToken(Map.of("sub", "   ")));
        rejectToken(customToken(Map.of("sub", "550e8400-e29b-41d4-a716-unknown-user")));
        rejectToken(
                customToken(
                        Map.of("realm_access", Map.of("roles", List.of("MANAGER")))));
        rejectToken(
                customToken(
                        Map.of("realm_access", Map.of("roles", List.of("ADMIN", "CUSTOMER")))));
        rejectToken(customToken(Map.of("auth_version", "0")));
        rejectToken(customToken(Map.of("auth_version", 0.5)));
    }

    @Test
    void wrongSignatureAndAlgorithmAreRejected() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair wrongPair = generator.generateKeyPair();
        RSAKey wrongRsa =
                new RSAKey.Builder((RSAPublicKey) wrongPair.getPublic())
                        .privateKey((RSAPrivateKey) wrongPair.getPrivate())
                        .keyUse(KeyUse.SIGNATURE)
                        .algorithm(JWSAlgorithm.RS256)
                        .keyID(KEY_ID)
                        .build();
        JwtEncoder wrongRsaEncoder =
                new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(wrongRsa)));
        rejectToken(
                wrongRsaEncoder
                        .encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(SignatureAlgorithm.RS256)
                                                .keyId(KEY_ID)
                                                .build(),
                                        claims(Map.of())))
                        .getTokenValue());

        byte[] symmetric = new byte[64];
        new java.security.SecureRandom().nextBytes(symmetric);
        JwtEncoder hs256Encoder =
                NimbusJwtEncoder.withSecretKey(new SecretKeySpec(symmetric, "HmacSHA256"))
                        .algorithm(MacAlgorithm.HS256)
                        .build();
        rejectToken(
                hs256Encoder
                        .encode(
                                JwtEncoderParameters.from(
                                        JwsHeader.with(MacAlgorithm.HS256).build(),
                                        claims(Map.of())))
                        .getTokenValue());
    }

    @Test
    void logoutRevokesAllTokensButDoesNotAffectAnotherUser() throws Exception {
        String first = token("security_customer_a"),
                second = token("security_customer_a"),
                other = token("security_customer_b");
        perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + first))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
        rejectToken(first);
        rejectToken(second);
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk());
        String reissued = token("security_customer_a");
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + reissued))
                .andExpect(status().isOk());
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
            String token = token(name);
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
        String token = token("security_admin");
        perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createBody("Created_User")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.username").value("created_user"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
        assertNull(users.findByIdentifier("created_user").orElseThrow().getPasswordHash());
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
        String token = token("security_admin");
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
        String admin = token("security_admin"), customer = token("security_customer_a");
        long version = users.findById(920005L).orElseThrow().getVersion();
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("RECEPTIONIST", true, version)))
                .andExpect(status().isOk());
        rejectToken(customer);
        String refreshed = token("security_customer_a");
        access(refreshed, "INVOICE", 920002, 200);
    }

    @Test
    void profileUpdatePreservesIdentityAndDetectsStaleVersion() throws Exception {
        String admin = token("security_admin");
        var original = users.findById(920005L).orElseThrow();
        perform(
                        put("/api/v1/admin/users/920005")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", true, original.getVersion())))
                .andExpect(status().isOk());
        assertEquals(
                original.getIdentityId(),
                users.findById(920005L).orElseThrow().getIdentityId());
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
        String admin = token("security_admin");
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
        String admin = token("security_admin"), customer = token("security_customer_b");
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
        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM customers WHERE user_id=920006", Long.class));
        assertEquals(
                1L,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_events WHERE action='USER_DELETED' AND"
                                + " entity_id=920006",
                        Long.class));
        perform(get("/api/v1/admin/users/920006").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        perform(
                        get("/api/v1/admin/users")
                                .param("keyword", "security_customer_b")
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        perform(
                        put("/api/v1/admin/users/920006")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", true, deleted.getVersion())))
                .andExpect(status().isNotFound());
        rejectToken(customer);
    }

    @Test
    void lockingCanBeReversedWithoutDeletingTheAccount() throws Exception {
        String admin = token("security_admin"), customer = token("security_customer_b");
        long version = users.findById(920006L).orElseThrow().getVersion();
        perform(
                        put("/api/v1/admin/users/920006")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", false, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        var locked = users.findById(920006L).orElseThrow();
        assertNull(locked.getDeletedAt());
        rejectToken(customer);
        perform(
                        get("/api/v1/admin/users")
                                .param("keyword", "security_customer_b")
                                .param("active", "false")
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        perform(
                        put("/api/v1/admin/users/920006")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", true, locked.getVersion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));
        String unlockedToken = token("security_customer_b");
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + unlockedToken))
                .andExpect(status().isOk());
    }

    @Test
    void staleDeleteCannotRemoveAnUpdatedAccount() throws Exception {
        String admin = token("security_admin");
        long stale = users.findById(920006L).orElseThrow().getVersion();
        perform(
                        put("/api/v1/admin/users/920006")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateBody("CUSTOMER", false, stale)))
                .andExpect(status().isOk());
        perform(
                        delete("/api/v1/admin/users/920006")
                                .param("version", Long.toString(stale))
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(2005));
        assertNull(users.findById(920006L).orElseThrow().getDeletedAt());
    }

    @Test
    void customerCannotAccessAnotherCustomersResources() throws Exception {
        String customer = token("security_customer_a");
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
        String oldOwner = token("security_customer_a"), newOwner = token("security_customer_b");
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
        String technician = token("security_tech");
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
        String technician = token("security_tech");
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
        String warehouse = token("security_warehouse");
        access(warehouse, "REPAIR_ORDER", 920001, 200);
        for (String type : List.of("CUSTOMER", "INVOICE", "ATTACHMENT", "VEHICLE"))
            access(warehouse, type, 920001, 403);
    }

    @Test
    void actorParameterCannotImpersonateAnotherUser() throws Exception {
        String customer = token("security_customer_a");
        perform(
                        get("/api/v1/auth/testing/spoof/920001")
                                .header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void noticesStayPrivateEvenForAdminAndRecipientsCannotBeConfused() throws Exception {
        String admin = token("security_admin"), customer = token("security_customer_b");
        access(admin, "NOTIFICATION", 920001, 403);
        jdbc.update("UPDATE notifications SET customer_id=920002 WHERE id=920001");
        access(customer, "NOTIFICATION", 920001, 403);
    }

    @Test
    void corsOnlyAllowsConfiguredOriginsAndUnknownRoutesStayDenied() throws Exception {
        perform(
                        options("/api/v1/auth/me")
                                .header("Origin", "http://localhost:5173")
                                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(
                        header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
        perform(
                        options("/api/v1/auth/me")
                                .header("Origin", "https://untrusted.example")
                                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(3002))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"));
        perform(
                        get("/api/v1/unregistered")
                                .header("Authorization", "Bearer " + token("security_admin")))
                .andExpect(status().isForbidden());
    }

    @Test
    void securityAuditNeverContainsBearerTokens() throws Exception {
        String token = token("security_customer_a");
        perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        String data = jdbc.queryForList("SELECT action,after_json FROM audit_events").toString();
        assertFalse(data.contains(token));
    }

    @Test
    void adminCanCreateAllFiveRolesButNoOtherRole() throws Exception {
        String admin = token("security_admin");
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
                                                            role))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.role").value(role));
        }
    }

    @Test
    void unknownWritableFieldsAndPartialPutAreRejected() throws Exception {
        String admin = token("security_admin");
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
        assertEquals(user.getFullName(), users.findById(920005L).orElseThrow().getFullName());
    }

    @Test
    void missingParametersWrongMethodsAndUnsupportedMediaHaveCorrectStatuses() throws Exception {
        String admin = token("security_admin");
        perform(delete("/api/v1/admin/users/920005").header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        perform(get("/api/v1/auth/logout").header("Authorization", "Bearer " + admin))
                .andExpect(status().isMethodNotAllowed());
        perform(get("/api/v1/auth/missing").header("Authorization", "Bearer " + admin))
                .andExpect(status().isNotFound());
        perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_XML)
                                .content("<user/>"))
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
                        "CUSTOMER"));
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

    private String token(String name) {
        return issueKeycloakToken(properties, users.findByIdentifier(name).orElseThrow());
    }

    private JwtClaimsSet claims(Map<String, Object> changes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", properties.issuer());
        claims.put("aud", List.of(properties.audience()));
        claims.put("sub", "550e8400-e29b-41d4-a716-446655920005");
        claims.put("iat", Instant.now().minusSeconds(1));
        claims.put("nbf", Instant.now().minusSeconds(1));
        claims.put("exp", Instant.now().plusSeconds(300));
        claims.put("jti", "test-id");
        claims.put("realm_access", Map.of("roles", List.of("CUSTOMER")));
        claims.put("auth_version", 0L);
        claims.putAll(changes);
        claims.values().removeIf(java.util.Objects::isNull);
        return JwtClaimsSet.builder().claims(c -> c.putAll(claims)).build();
    }

    private String customToken(Map<String, Object> changes) {
        return testJwtEncoder()
                .encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY_ID).build(),
                                claims(changes)))
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

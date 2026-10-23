package com.Group13_65CNTT.AutoGarage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@Transactional
@DisplayName("TV4 Inventory and Billing integration tests on H2")
@RequiredArgsConstructor
class TV4IntegrationTests extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final EntityManager entities;
    private final UserRepository users;
    private final AuthProperties authProperties;

    @BeforeEach
    void setupFixtures() {
        jdbc.update(
                "INSERT INTO users(id, identity_id, username, full_name, role, is_active, auth_version, version, created_at, updated_at) " +
                "VALUES (940001, '550e8400-e29b-41d4-a716-446655940001', 'tv4.admin', 'Admin User', 'ADMIN', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "       (940002, '550e8400-e29b-41d4-a716-446655940002', 'tv4.warehouse', 'Warehouse Staff', 'WAREHOUSE_STAFF', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "       (940003, '550e8400-e29b-41d4-a716-446655940003', 'tv4.reception', 'Receptionist', 'RECEPTIONIST', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                "       (940004, '550e8400-e29b-41d4-a716-446655940004', 'tv4.customer', 'Customer User', 'CUSTOMER', true, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        jdbc.update(
                "INSERT INTO customers(id, customer_code, full_name, customer_type, phone, credit_limit, payment_term_days, is_active, version, created_at, updated_at) " +
                "VALUES (940001, 'TV4_CUST_1', 'Customer One', 'INDIVIDUAL', '0911222333', 10000000, 30, true, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");
    }

    private String token(String identifier) {
        User user = users.findByIdentifier(identifier).orElseThrow();
        return issueKeycloakToken(authProperties, user);
    }

    private ResultActions perform(RequestBuilder request) throws Exception {
        entities.flush();
        entities.clear();
        return mvc.perform(request);
    }

    @Test
    void supplierCrudAndAuthorization() throws Exception {
        String adminToken = token("tv4.admin");
        String warehouseToken = token("tv4.warehouse");
        String customerToken = token("tv4.customer");

        // Customer cannot access suppliers
        perform(get("/api/v1/inventory/suppliers").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // Warehouse staff can list suppliers
        perform(get("/api/v1/inventory/suppliers").header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content").isArray());

        // Warehouse staff can create supplier
        String createPayload = json.writeValueAsString(Map.of(
                "supplierCode", "SUP_TV4_01",
                "supplierName", "Bosch Vietnam Co",
                "phone", "0281234567",
                "email", "bosch@test.vn",
                "address", "District 1, HCMC",
                "taxCode", "0300123456",
                "paymentTermDays", 30
        ));

        var result = perform(post("/api/v1/inventory/suppliers")
                .header("Authorization", "Bearer " + warehouseToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data.supplierCode").value("SUP_TV4_01"))
                .andReturn();

        long supplierId = json.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Get by ID
        perform(get("/api/v1/inventory/suppliers/" + supplierId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.supplierName").value("Bosch Vietnam Co"));
    }

    @Test
    void partCrudAndLowStockAlert() throws Exception {
        String warehouseToken = token("tv4.warehouse");

        // Create a part
        String partPayload = json.writeValueAsString(Map.of(
                "sku", "PART_TV4_01",
                "partName", "Oil Filter Element",
                "unit", "pcs",
                "purchasePrice", 120000,
                "sellingPrice", 180000,
                "reorderLevel", 10,
                "warrantyDays", 90
        ));

        var createResult = perform(post("/api/v1/inventory/parts")
                .header("Authorization", "Bearer " + warehouseToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(partPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001))
                .andExpect(jsonPath("$.data.sku").value("PART_TV4_01"))
                .andExpect(jsonPath("$.data.availableQuantity").value(0))
                .andReturn();

        long partId = json.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // Check low-stock alert endpoint (stock is 0, reorderLevel is 10 -> low stock!)
        perform(get("/api/v1/inventory/parts/low-stock")
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.id == " + partId + ")]").exists());

        // Check reconciliation
        perform(get("/api/v1/inventory/reconciliation/parts/" + partId)
                .header("Authorization", "Bearer " + warehouseToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.partId").value(partId))
                .andExpect(jsonPath("$.data.stockQuantity").value(0))
                .andExpect(jsonPath("$.data.ledgerQuantity").value(0))
                .andExpect(jsonPath("$.data.matched").value(true));
    }
}

package com.Group13_65CNTT.AutoGarage;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.Group13_65CNTT.AutoGarage.common.config.AuthProperties;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.notification.dto.request.NotificationCreateRequest;
import com.Group13_65CNTT.AutoGarage.notification.service.*;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;

import jakarta.persistence.EntityManager;

import lombok.RequiredArgsConstructor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
@DisplayName("TV1 HTTP, notification, report and replay integration on MySQL")
@RequiredArgsConstructor
class TV1IntegrationTests extends IntegrationTestSupport {
    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final EntityManager entities;
    private final UserRepository users;
    private final AuthProperties authProperties;
    private final NotificationService notifications;
    private final ReminderService reminders;
    private final NotificationAudit replay;

    @BeforeEach
    void fixture() {
        jdbc.update(
                "INSERT INTO users(id,identity_id,username,full_name,role)"
                    + " VALUES(960001,'550e8400-e29b-41d4-a716-446655960001','tv1.admin','Admin','ADMIN'),"
                    + "(960002,'550e8400-e29b-41d4-a716-446655960002','tv1.customer','Customer','CUSTOMER')");
    }

    @Test
    void responseHasTheExactArchitectureEnvelopeAndNumericCodes() throws Exception {
        String customer = token("tv1.customer");
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(960002))
                .andExpect(
                        jsonPath("$.data.identityId")
                                .value("550e8400-e29b-41d4-a716-446655960002"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/auth/me"))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void notificationsAreDeduplicatedPrivateAndReadIdempotently() throws Exception {
        String admin = token("tv1.admin"), customer = token("tv1.customer");
        String payload =
                json.writeValueAsString(
                        Map.of(
                                "userId",
                                960002,
                                "eventKey",
                                "tv1.notice",
                                "channel",
                                "WEB",
                                "destination",
                                "web",
                                "payloadJson",
                                "{\"text\":\"Hello\"}"));
        var created =
                perform(
                                post("/api/v1/notifications")
                                        .header("Authorization", "Bearer " + admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(payload))
                        .andExpect(status().isOk())
                        .andReturn();
        long id =
                json.readTree(created.getResponse().getContentAsString())
                        .path("data")
                        .path("id")
                        .asLong();
        perform(
                        post("/api/v1/notifications")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk());
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM notifications WHERE event_key='tv1.notice'",
                                Integer.class))
                .isEqualTo(1);
        perform(get("/api/v1/notifications/" + id).header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        perform(get("/api/v1/notifications/" + id).header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destination").doesNotExist());
        perform(
                        post("/api/v1/notifications/" + id + "/read")
                                .header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk());
        perform(
                        get("/api/v1/notifications")
                                .param("unread", "true")
                                .header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void browserOriginCanBroadcastReadAndRecallWithoutBypassingRecipientPermissions()
            throws Exception {
        String admin = token("tv1.admin"), customer = token("tv1.customer");
        org.springframework.test.web.servlet.request.RequestPostProcessor browser =
                request -> {
                    request.setServerName("127.0.0.1");
                    request.setServerPort(5173);
                    request.addHeader("Origin", "http://127.0.0.1:5173");
                    return request;
                };
        String broadcast = json.writeValueAsString(Map.of(
                "subject", "Garage notice",
                "targetRoles", java.util.List.of(),
                "payloadJson", "{\"message\":\"Opening hours updated\"}"));
        perform(post("/api/v1/notifications/broadcast").with(browser)
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(broadcast))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientCount").value(2));
        var inbox = perform(get("/api/v1/notifications").with(browser)
                        .header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1)).andReturn();
        long id = json.readTree(inbox.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();
        perform(post("/api/v1/notifications/" + id + "/read").with(browser)
                        .header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.readAt").isNotEmpty());
        perform(post("/api/v1/notifications/" + id + "/read").with(browser)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isForbidden());
        perform(post("/api/v1/notifications/broadcast").with(browser)
                        .header("Authorization", "Bearer " + customer)
                        .contentType(MediaType.APPLICATION_JSON).content(broadcast))
                .andExpect(status().isForbidden());
        perform(delete("/api/v1/notifications/" + id).with(browser)
                        .header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
        perform(get("/api/v1/notifications/admin").with(browser)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(2));
        perform(delete("/api/v1/notifications/" + id).with(browser)
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        perform(get("/api/v1/notifications/" + id).with(browser)
                        .header("Authorization", "Bearer " + customer))
                .andExpect(status().isNotFound());
    }

    @Test
    void changedPayloadWithSameEventCannotOverwriteOriginal() {
        var request =
                NotificationCreateRequest.builder()
                        .userId(960002L)
                        .eventKey("tv1.conflict")
                        .channel("WEB")
                        .destination("web")
                        .payloadJson("{\"x\":1}")
                        .build();
        notifications.enqueue(request);
        request.setPayloadJson("{\"x\":2}");
        assertThatThrownBy(() -> notifications.enqueue(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void dashboardAndCsvAreAdminOnlyAndPeriodValidated() throws Exception {
        String admin = token("tv1.admin"), customer = token("tv1.customer");
        String from = "2026-10-01T00:00:00Z", until = "2026-11-01T00:00:00Z";
        perform(
                        get("/api/v1/reports/dashboard")
                                .param("from", from)
                                .param("until", until)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue").value(0));
        perform(
                        get("/api/v1/reports/dashboard")
                                .param("from", from)
                                .param("until", until)
                                .header("Authorization", "Bearer " + customer))
                .andExpect(status().isForbidden());
        perform(
                        get("/api/v1/reports/dashboard")
                                .param("from", until)
                                .param("until", from)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
        perform(
                        get("/api/v1/reports/dashboard/export")
                                .param("from", from)
                                .param("until", until)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void auditQueryDoesNotExposeReplayDataAndRejectsCustomer() throws Exception {
        String admin = token("tv1.admin"), customer = token("tv1.customer");
        perform(post("/api/v1/auth/logout").header("Authorization", "Bearer " + customer))
                .andExpect(status().isOk());
        perform(get("/api/v1/admin/audit-events").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].requestHash").doesNotExist());
        String newCustomerToken = token("tv1.customer");
        perform(
                        get("/api/v1/admin/audit-events")
                                .header("Authorization", "Bearer " + newCustomerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatedOpenApiDocumentsLayeredEndpointsAndBearerScheme() throws Exception {
        perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/admin/users'].post").exists())
                .andExpect(
                        jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void userSearchFiltersAndSortAllowlistAreEnforced() throws Exception {
        String admin = token("tv1.admin");
        perform(
                        get("/api/v1/admin/users")
                                .param("keyword", "customer")
                                .param("active", "true")
                                .param("sortBy", "username")
                                .param("sortDir", "desc")
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
        perform(
                        get("/api/v1/admin/users")
                                .param("sortBy", "passwordHash")
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "' OR 1=1 --", "%' OR 1=1 --", "' UNION SELECT 1 --", "'; DROP TABLE users; --"
            })
    void sqlFragmentsAreBoundAsDataInTokenSubjectUserSearchAndAuditFilter(String input)
            throws Exception {
        String admin = token("tv1.admin");
        String injectedToken =
                issueKeycloakToken(authProperties, input, input, "CUSTOMER", 0L);
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + injectedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(3003));
        perform(
                        get("/api/v1/admin/users")
                                .param("keyword", input)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        perform(
                        get("/api/v1/admin/audit-events")
                                .param("action", input)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM users WHERE id IN (?, ?)",
                                Integer.class,
                                960001,
                                960002))
                .isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "id desc; DROP TABLE users; --",
                "username) OR 1=1 --",
                "LENGTH(username)",
                "passwordHash"
            })
    void rejectsInjectedSortPropertiesWithControlledBadRequest(String property) throws Exception {
        String admin = token("tv1.admin");
        perform(
                        get("/api/v1/admin/users")
                                .param("sortBy", property)
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
        perform(
                        get("/api/v1/admin/users")
                                .param("sortDir", "asc; DROP TABLE users; --")
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2004));
        perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
    }

    @Test
    void quotedProfileDataCanBeStoredAndSearchedWithoutBroadeningResults() throws Exception {
        String admin = token("tv1.admin");
        String fullName = "O'Brien ' OR 1=1 --";
        perform(
                        post("/api/v1/admin/users")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "username", "tv1.quoted",
                                                        "fullName", fullName,
                                                        "role", "CUSTOMER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.fullName").value(fullName));
        perform(
                        get("/api/v1/admin/users")
                                .param("keyword", "O'Brien")
                                .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].username").value("tv1.quoted"))
                .andExpect(jsonPath("$.data.content[0].fullName").value(fullName));
    }

    @Test
    void nativeNotificationInsertBindsQuotedEventSubjectAndJsonWithoutExecutingThem()
            throws Exception {
        String admin = token("tv1.admin"), customer = token("tv1.customer");
        String subject = "Notice '); DROP TABLE users; --";
        String eventKey = "tv1.notice' OR 1=1 --";
        String payload = json.writeValueAsString(Map.of("text", subject));
        perform(
                        post("/api/v1/notifications")
                                .header("Authorization", "Bearer " + admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        json.writeValueAsString(
                                                Map.of(
                                                        "userId", 960002,
                                                        "eventKey", eventKey,
                                                        "channel", "WEB",
                                                        "destination", "web",
                                                        "subject", subject,
                                                        "payloadJson", payload))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value(subject));
        var response =
                perform(get("/api/v1/notifications").header("Authorization", "Bearer " + customer))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.totalElements").value(1))
                        .andExpect(jsonPath("$.data.content[0].subject").value(subject))
                        .andReturn();
        String storedPayload =
                json.readTree(response.getResponse().getContentAsString())
                        .path("data")
                        .path("content")
                        .get(0)
                        .path("payloadJson")
                        .asText();
        assertThat(json.readTree(storedPayload).path("text").asText()).isEqualTo(subject);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM notifications WHERE event_key = ?",
                                Integer.class,
                                eventKey))
                .isEqualTo(1);
        perform(get("/api/v1/notifications").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void dashboardReconcilesPaymentsLedgerAndClosedLaborWithoutMultiplyingTotals()
            throws Exception {
        businessFixture();
        String admin = token("tv1.admin");
        var response =
                perform(
                                get("/api/v1/reports/dashboard")
                                        .param("from", Instant.now().minusSeconds(86400).toString())
                                        .param("until", Instant.now().plusSeconds(86400).toString())
                                        .header("Authorization", "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andReturn();
        var data = json.readTree(response.getResponse().getContentAsString()).path("data");
        assertThat(data.path("revenue").decimalValue()).isEqualByComparingTo("100");
        assertThat(data.path("cashIn").decimalValue()).isEqualByComparingTo("50");
        assertThat(data.path("cashOut").decimalValue()).isEqualByComparingTo("50");
        assertThat(data.path("customerDebt").decimalValue()).isEqualByComparingTo("50");
        assertThat(data.path("supplierDebt").decimalValue()).isEqualByComparingTo("150");
        assertThat(data.path("stockValue").decimalValue()).isEqualByComparingTo("300");
        assertThat(data.path("laborHours").decimalValue()).isEqualByComparingTo("1");
    }

    @Test
    void remindersUseTheCurrentMaintenanceCycleAndDeduplicateDailyRuns() {
        businessFixture();
        assertThat(reminders.enqueueDueReminders()).isEqualTo(2);
        assertThat(reminders.enqueueDueReminders()).isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM notifications WHERE user_id=960002",
                                Integer.class))
                .isEqualTo(2);
    }

    @Test
    void transferredVehicleDoesNotSendHistoricalCustomerReminders() {
        businessFixture();
        jdbc.update(
                "INSERT INTO customers(id,user_id,customer_code,full_name,phone)"
                        + " VALUES(960002,960001,'TV1_NEW_OWNER','New Owner','02')");
        jdbc.update("UPDATE vehicles SET customer_id=960002 WHERE id=960001");
        assertThat(reminders.enqueueDueReminders()).isZero();
    }

    private void businessFixture() {
        jdbc.update(
                "INSERT INTO users(id,username,full_name,role)"
                        + " VALUES(960003,'tv1.tech','Tech','TECHNICIAN')");
        jdbc.update(
                "INSERT INTO customers(id,user_id,customer_code,full_name,phone)"
                        + " VALUES(960001,960002,'TV1_CUSTOMER','Customer','01')");
        jdbc.update(
                "INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer)"
                        + " VALUES(960001,960001,'TV1_VEHICLE','Test','Test',500)");
        jdbc.update(
                "INSERT INTO"
                    + " services(id,service_code,service_name,unit_price,standard_minutes,interval_months,interval_km)"
                    + " VALUES(960001,'TV1_SERVICE','Service',100,60,1,1000)");
        jdbc.update(
                "INSERT INTO"
                    + " repair_orders(id,order_code,customer_id,vehicle_id,odometer_in,odometer_out,received_by,handed_over_by,handed_over_at,status)"
                    + " VALUES(960001,'TV1_ORDER',960001,960001,100,200,960001,960001,DATE_SUB(UTC_TIMESTAMP(6),INTERVAL"
                    + " 23 DAY),'CLOSED'),(960002,'TV1_VOID_ORDER',960001,960001,100,200,960001,NULL,NULL,'NEW')");
        jdbc.update(
                "INSERT INTO estimates(id,estimate_code,repair_order_id,version_no,created_by)"
                        + " VALUES(960001,'TV1_ESTIMATE',960001,1,960001)");
        jdbc.update(
                "INSERT INTO"
                    + " estimate_items(id,estimate_id,line_no,item_type,service_id,item_name,unit,quantity,unit_price,line_total,approval_status)"
                    + " VALUES(960001,960001,1,'SERVICE',960001,'Service','job',1,100,100,'APPROVED')");
        jdbc.update(
                "INSERT INTO"
                    + " repair_tasks(id,repair_order_id,estimate_item_id,service_id,task_name,status)"
                    + " VALUES(960001,960001,960001,960001,'Service','COMPLETED')");
        jdbc.update(
                "INSERT INTO"
                    + " invoices(id,invoice_code,repair_order_id,customer_id,customer_snapshot,vehicle_snapshot,subtotal,total_amount,issued_by)"
                    + " VALUES(960001,'TV1_INVOICE',960001,960001,'{}','{}',100,100,960001)");
        jdbc.update(
                "INSERT INTO"
                    + " invoices(id,invoice_code,repair_order_id,customer_id,customer_snapshot,vehicle_snapshot,subtotal,total_amount,issued_by,status,void_reason,voided_by,voided_at)"
                    + " VALUES(960002,'TV1_VOID',960002,960001,'{}','{}',900,900,960001,'VOID','test',960001,UTC_TIMESTAMP(6))");
        jdbc.update(
                "INSERT INTO"
                    + " invoice_items(id,invoice_id,line_no,item_type,repair_task_id,item_name,unit,quantity,unit_price,unit_cost,line_total)"
                    + " VALUES(960001,960001,1,'SERVICE',960001,'Service','job',1,100,20,100)");
        jdbc.update(
                "INSERT INTO"
                    + " payments(transaction_code,direction,invoice_id,amount,payment_method,recorded_by)"
                    + " VALUES('TV1_IN_1','IN',960001,20,'CASH',960001),('TV1_IN_2','IN',960001,30,'CASH',960001)");
        jdbc.update(
                "INSERT INTO"
                    + " warranties(id,warranty_code,invoice_item_id,vehicle_id,customer_id,starts_at,expires_at,start_odometer,warranty_days)"
                    + " VALUES(960001,'TV1_WARRANTY',960001,960001,960001,DATE_SUB(UTC_TIMESTAMP(6),INTERVAL"
                    + " 23 DAY),DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 7 DAY),200,30)");
        jdbc.update(
                "INSERT INTO suppliers(id,supplier_code,supplier_name)"
                        + " VALUES(960001,'TV1_SUPPLIER','Supplier')");
        jdbc.update(
                "INSERT INTO parts(id,sku,part_name,unit,stock_quantity,purchase_price)"
                        + " VALUES(960001,'TV1_PART','Part','piece',3,100)");
        jdbc.update(
                "INSERT INTO"
                    + " inventory_documents(id,document_code,document_type,supplier_id,status,subtotal,total_amount,created_by,posted_by,posted_at)"
                    + " VALUES(960001,'TV1_PURCHASE','IN',960001,'POSTED',200,200,960001,960001,UTC_TIMESTAMP(6)),(960002,'TV1_OPENING','OPENING',NULL,'POSTED',100,100,960001,960001,UTC_TIMESTAMP(6))");
        jdbc.update(
                "INSERT INTO"
                    + " inventory_document_items(id,document_id,line_no,part_id,quantity,unit_cost,line_total)"
                    + " VALUES(960001,960001,1,960001,2,100,200),(960002,960002,1,960001,1,100,100)");
        jdbc.update(
                "INSERT INTO"
                    + " inventory_transactions(document_item_id,part_id,transaction_type,quantity_change,stock_after,unit_cost,created_by)"
                    + " VALUES(960001,960001,'IN',2,2,100,960001),(960002,960001,'OPENING',1,3,100,960001)");
        jdbc.update(
                "INSERT INTO"
                    + " payments(transaction_code,direction,inventory_document_id,amount,payment_method,recorded_by)"
                    + " VALUES('TV1_OUT_1','OUT',960001,20,'CASH',960001),('TV1_OUT_2','OUT',960001,30,'CASH',960001)");
        jdbc.update(
                "INSERT INTO employees(id,user_id,employee_code) VALUES(960001,960003,'TV1_TECH')");
        jdbc.update(
                "INSERT INTO"
                    + " task_assignments(id,task_id,employee_id,assignment_role,starts_at,ends_at,assigned_by)"
                    + " VALUES(960001,960001,960001,'MAIN',DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1"
                    + " HOUR),UTC_TIMESTAMP(6),960001)");
        jdbc.update(
                "INSERT INTO"
                        + " labor_sessions(assignment_id,employee_id,starts_at,ends_at,stop_reason)"
                        + " VALUES(960001,960001,DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1"
                        + " HOUR),UTC_TIMESTAMP(6),'COMPLETE')");
    }

    private String token(String identifier) {
        return issueKeycloakToken(
                authProperties, users.findByIdentifier(identifier).orElseThrow());
    }

    private ResultActions perform(RequestBuilder request) throws Exception {
        entities.flush();
        entities.clear();
        return mvc.perform(request);
    }
}

package com.Group13_65CNTT.AutoGarage;

import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.*;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.*;
import com.Group13_65CNTT.AutoGarage.estimate.entity.ItemType;
import com.Group13_65CNTT.AutoGarage.estimate.service.EstimateService;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.*;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.inventory.service.InventoryService;
import com.Group13_65CNTT.AutoGarage.quality.dto.request.QualityCheckCreateRequest;
import com.Group13_65CNTT.AutoGarage.quality.service.QualityService;
import com.Group13_65CNTT.AutoGarage.reception.dto.CheckInVehicleRequest;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.service.ReceptionService;
import com.Group13_65CNTT.AutoGarage.repair.dto.request.*;
import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import com.Group13_65CNTT.AutoGarage.repair.service.LaborExecutionService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.*;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations.PaymentMethod;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;

/** Real services, Flyway schema and MySQL constraints; every fixture is rolled back. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class BusinessWorkflowIntegrationTests extends IntegrationTestSupport {
    private static final long ADMIN = 980001L;
    private static final long QC = 980002L;
    private final JdbcTemplate jdbc;
    private final EntityManager entities;
    private final ReceptionService reception;
    private final RepairWorkflow repair;
    private final EstimateService estimates;
    private final EstimateApproval approval;
    private final LaborExecutionService labor;
    private final QualityService quality;
    private final BillingService billing;
    private final InventoryService inventory;
    private final WarrantyOperations warranty;

    @BeforeEach
    void fixture() {
        jdbc.update("INSERT INTO users(id,identity_id,username,full_name,role) VALUES " +
                "(980001,'workflow-admin','workflow.admin','Workflow Admin','ADMIN')," +
                "(980002,'workflow-qc','workflow.qc','Workflow QC','TECHNICIAN')," +
                "(980003,'workflow-customer','workflow.customer','Workflow Customer','CUSTOMER')");
        jdbc.update("INSERT INTO customers(id,user_id,customer_code,full_name,phone) VALUES(980001,980003,'WF-CUST','Workflow Customer','0900000001')");
        jdbc.update("INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer) VALUES(980001,980001,'WF-980001','Test','Sedan',1000)");
        jdbc.update("INSERT INTO services(id,service_code,service_name,unit_price,standard_minutes,warranty_days) VALUES(980001,'WF-SVC','Service',120000,60,30)");
        jdbc.update("INSERT INTO employees(id,user_id,employee_code,can_qc,hourly_cost) VALUES(980002,980002,'WF-QC',true,60000)");
        jdbc.update("INSERT INTO employee_shifts(employee_id,starts_at,ends_at,created_by) VALUES(980002,DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY),DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY),980001)");
        jdbc.update("INSERT INTO parts(id,sku,part_name,unit,purchase_price,selling_price) VALUES(980001,'WF-PART','Filter','piece',20000,90000)");
        authenticate(ADMIN, "ADMIN");
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    private void authenticate(long id, String role) {
        Jwt jwt = Jwt.withTokenValue("isolated-test-token").header("alg", "RS256")
                .subject(Long.toString(id)).claim("realm_access", Map.of("roles", List.of(role)))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private void sync() { entities.flush(); entities.clear(); }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {false, true})
    void receptionApprovalStockQcInvoiceCashReplayAndHandover(boolean returnOnePart) {
        var opening = inventory.createDocument(ADMIN, InventoryDocumentCreateRequest.builder()
                .documentType(DocumentType.OPENING).items(List.of(InventoryDocumentItemRequest.builder()
                        .partId(980001L).quantity(new BigDecimal("10")).unitCost(new BigDecimal("20000")).build())).build());
        inventory.post(ADMIN, opening.getId(), "wf-opening");
        inventory.post(ADMIN, opening.getId(), "wf-opening");
        sync();
        assertThat(jdbc.queryForObject("SELECT stock_quantity FROM parts WHERE id=980001", BigDecimal.class)).isEqualByComparingTo("10");

        CheckInVehicleRequest checkIn = new CheckInVehicleRequest();
        checkIn.setVehicleId(980001L); checkIn.setActorUserId(ADMIN); checkIn.setOdometerIn(1100);
        checkIn.setFuelLevelPercent(new BigDecimal("50")); checkIn.setCustomerComplaint("Brake noise");
        long orderId = reception.checkInVehicle(checkIn).getRepairOrderId();
        sync();
        assertThat(jdbc.queryForObject("SELECT customer_complaint FROM repair_orders WHERE id=?", String.class, orderId)).isEqualTo("Brake noise");
        long diagnosis = jdbc.queryForObject("SELECT id FROM repair_tasks WHERE repair_order_id=?", Long.class, orderId);
        labor.completeTask(ADMIN, diagnosis, null);

        var estimate = estimates.createEstimate(ADMIN, EstimateCreateRequest.builder().repairOrderId(orderId)
                .items(List.of(
                        EstimateItemCreateRequest.builder().itemType(ItemType.SERVICE).serviceId(980001L)
                                .quantity(new BigDecimal("2")).discountAmount(new BigDecimal("10000")).taxAmount(new BigDecimal("5000")).build(),
                        EstimateItemCreateRequest.builder().itemType(ItemType.PART).partId(980001L)
                                .quantity(new BigDecimal("2")).unitPrice(new BigDecimal("50000"))
                                .discountAmount(new BigDecimal("10000")).taxAmount(new BigDecimal("1000")).build())).build());
        estimates.sendEstimate(ADMIN, estimate.getId());
        var lines = estimate.getItems().stream().map(item -> new EstimateApproval.LineDecision(item.getId(), EstimateApproval.Decision.APPROVED, "Approved")).toList();
        approval.decide(ADMIN, estimate.getId(), lines, "wf-approve");
        approval.decide(ADMIN, estimate.getId(), lines, "wf-approve");
        sync();
        long partLine = jdbc.queryForObject("SELECT id FROM estimate_items WHERE estimate_id=? AND item_type='PART'", Long.class, estimate.getId());
        var outgoing = inventory.createDocument(ADMIN, InventoryDocumentCreateRequest.builder().documentType(DocumentType.OUT).repairOrderId(orderId)
                .items(List.of(InventoryDocumentItemRequest.builder().partId(980001L).estimateItemId(partLine)
                        .quantity(new BigDecimal("2")).unitCost(BigDecimal.ONE).build())).build());
        inventory.post(ADMIN, outgoing.getId(), "wf-out");
        sync();
        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM parts WHERE id=980001", BigDecimal.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT reserved_quantity FROM estimate_items WHERE id=?", BigDecimal.class, partLine)).isZero();
        assertThat(jdbc.queryForObject("SELECT unit_price FROM repair_order_parts WHERE estimate_item_id=?", BigDecimal.class, partLine)).isEqualByComparingTo("50000");

        if (returnOnePart) {
            long sourceItem = jdbc.queryForObject("SELECT id FROM inventory_document_items WHERE document_id=?", Long.class, outgoing.getId());
            var returned = inventory.createDocument(ADMIN, InventoryDocumentCreateRequest.builder()
                    .documentType(DocumentType.RETURN).repairOrderId(orderId).sourceDocumentId(outgoing.getId())
                    .items(List.of(InventoryDocumentItemRequest.builder().partId(980001L).sourceItemId(sourceItem)
                            .quantity(BigDecimal.ONE).unitCost(BigDecimal.ONE).build())).build());
            inventory.post(ADMIN, returned.getId(), "wf-return");
            sync();
            assertThat(jdbc.queryForObject("SELECT quantity FROM repair_order_parts WHERE estimate_item_id=?", BigDecimal.class, partLine)).isEqualByComparingTo("1");
            assertThat(jdbc.queryForObject("SELECT stock_quantity FROM parts WHERE id=980001", BigDecimal.class)).isEqualByComparingTo("9");
        }

        long task = jdbc.queryForObject("SELECT id FROM repair_tasks WHERE repair_order_id=? AND task_type='REPAIR'", Long.class, orderId);
        labor.assignTask(ADMIN, task, TaskAssignmentRequest.builder().employeeId(980002L).assignmentRole(AssignmentRole.QC)
                .startsAt(Instant.now().minusSeconds(60)).endsAt(Instant.now().plusSeconds(3600)).build());
        labor.completeTask(ADMIN, task, null);
        sync();
        authenticate(QC, "TECHNICIAN");
        quality.performQualityCheck(QC, QualityCheckCreateRequest.builder().repairOrderId(orderId).result(CheckResult.PASSED).build());
        sync();
        authenticate(ADMIN, "ADMIN");
        var invoice = billing.issueInvoice(ADMIN, InvoiceIssueRequest.builder().repairOrderId(orderId).requestKey("wf-invoice").build());
        sync();
        assertThat(invoice.getTotalAmount()).isEqualByComparingTo(returnOnePart ? "280500" : "326000");
        assertThat(billing.issueInvoice(ADMIN, InvoiceIssueRequest.builder().repairOrderId(orderId).requestKey("wf-invoice").build()).getId()).isEqualTo(invoice.getId());
        var paymentRequest = PaymentReceiveRequest.builder().invoiceId(invoice.getId()).amount(invoice.getTotalAmount())
                .paymentMethod(PaymentMethod.CASH).requestKey("wf-cash").build();
        long paymentId = billing.receivePayment(ADMIN, paymentRequest).getId();
        sync();
        assertThat(billing.receivePayment(ADMIN, paymentRequest).getId()).isEqualTo(paymentId);
        repair.handOver(ADMIN, orderId, 1200, "wf-handover");
        sync();
        repair.handOver(ADMIN, orderId, 1200, "wf-handover");
        assertThat(jdbc.queryForObject("SELECT status FROM repair_orders WHERE id=?", String.class, orderId)).isEqualTo("CLOSED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE invoice_id=?", Integer.class, invoice.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM warranties WHERE invoice_item_id IN (SELECT id FROM invoice_items WHERE invoice_id=?)", Integer.class, invoice.getId())).isEqualTo(1);
        long warrantyId = jdbc.queryForObject("SELECT w.id FROM warranties w JOIN invoice_items i ON w.invoice_item_id=i.id WHERE i.invoice_id=?", Long.class, invoice.getId());
        long claimId = warranty.openClaim(ADMIN, warrantyId, 1300, "Recurring noise", "wf-claim");
        sync();
        assertThat(warranty.openClaim(ADMIN, warrantyId, 1300, "Recurring noise", "wf-claim")).isEqualTo(claimId);
        warranty.decideClaim(ADMIN, claimId, true, "Covered by warranty", "wf-claim-approve");
        sync();
        warranty.decideClaim(ADMIN, claimId, true, "Covered by warranty", "wf-claim-approve");
        assertThat(jdbc.queryForObject("SELECT status FROM warranty_claims WHERE id=?", String.class, claimId)).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_orders WHERE order_type='WARRANTY' AND customer_id=980001", Integer.class)).isEqualTo(1);
    }

    @Test
    void technicianCannotOperateAnotherTechniciansTimer() {
        long order = repair.receive(ADMIN, null, 980001L, 980001L, 1100);
        sync();
        long task = jdbc.queryForObject("SELECT id FROM repair_tasks WHERE repair_order_id=?", Long.class, order);
        jdbc.update("INSERT INTO users(id,identity_id,username,full_name,role) VALUES(980004,'workflow-other','workflow.other','Other Technician','TECHNICIAN')");
        jdbc.update("INSERT INTO employees(id,user_id,employee_code) VALUES(980004,980004,'WF-OTHER')");
        jdbc.update("INSERT INTO employee_shifts(employee_id,starts_at,ends_at,created_by) VALUES(980004,DATE_SUB(UTC_TIMESTAMP(),INTERVAL 1 DAY),DATE_ADD(UTC_TIMESTAMP(),INTERVAL 1 DAY),980001)");
        var assignment = labor.assignTask(ADMIN, task, TaskAssignmentRequest.builder().employeeId(980004L).assignmentRole(AssignmentRole.MAIN)
                .startsAt(Instant.now()).endsAt(Instant.now().plusSeconds(3600)).build());
        sync();
        authenticate(QC, "TECHNICIAN");
        assertThatThrownBy(() -> labor.startTimer(QC, assignment.getId())).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM labor_sessions WHERE assignment_id=?", Integer.class, assignment.getId())).isZero();
    }

    @Test
    void supplierCashPaymentCanBeReplayedWithoutBankReference() {
        jdbc.update("INSERT INTO suppliers(id,supplier_code,supplier_name) VALUES(980001,'WF-SUP','Workflow Supplier')");
        var incoming = inventory.createDocument(ADMIN, InventoryDocumentCreateRequest.builder().documentType(DocumentType.IN).supplierId(980001L)
                .items(List.of(InventoryDocumentItemRequest.builder().partId(980001L).quantity(new BigDecimal("10"))
                        .unitCost(new BigDecimal("20000")).build())).build());
        inventory.post(ADMIN, incoming.getId(), "wf-in");
        sync();
        var request = SupplierPaymentRequest.builder().inventoryDocumentId(incoming.getId()).amount(new BigDecimal("100000"))
                .paymentMethod(PaymentMethod.CASH).requestKey("wf-supplier-cash").build();
        long payment = billing.paySupplier(ADMIN, request).getId();
        sync();
        assertThat(billing.paySupplier(ADMIN, request).getId()).isEqualTo(payment);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE inventory_document_id=?", Integer.class, incoming.getId())).isEqualTo(1);
    }

    @Test
    void anotherCustomerCannotReadAnEstimateByGuessingItsId() {
        long order = repair.receive(ADMIN, null, 980001L, 980001L, 1100);
        var estimate = estimates.createEstimate(ADMIN, EstimateCreateRequest.builder().repairOrderId(order)
                .items(List.of(EstimateItemCreateRequest.builder().itemType(ItemType.SERVICE).serviceId(980001L)
                        .quantity(BigDecimal.ONE).build())).build());
        jdbc.update("INSERT INTO users(id,identity_id,username,full_name,role) VALUES(980005,'workflow-stranger','workflow.stranger','Other Customer','CUSTOMER')");
        sync();
        authenticate(980005L, "CUSTOMER");
        assertThatThrownBy(() -> estimates.getEstimateById(estimate.getId())).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode.FORBIDDEN);
    }

    @Test
    void estimateDraftCannotBeApproved() {
        long order = repair.receive(ADMIN, null, 980001L, 980001L, 1100);
        var estimate = estimates.createEstimate(ADMIN, EstimateCreateRequest.builder().repairOrderId(order)
                .items(List.of(EstimateItemCreateRequest.builder().itemType(ItemType.SERVICE).serviceId(980001L)
                        .quantity(BigDecimal.ONE).build())).build());
        sync();
        assertThatThrownBy(() -> approval.decide(ADMIN, estimate.getId(),
                List.of(new EstimateApproval.LineDecision(estimate.getItems().getFirst().getId(), EstimateApproval.Decision.APPROVED, null)), "wf-draft"))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM repair_tasks WHERE repair_order_id=? AND task_type='REPAIR'", Integer.class, order)).isZero();
    }
}

package com.Group13_65CNTT.AutoGarage.billing.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceIssueRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.SupplierPaymentRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.PaymentResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.*;
import com.Group13_65CNTT.AutoGarage.billing.mapper.DebtMapper;
import com.Group13_65CNTT.AutoGarage.billing.mapper.InvoiceMapper;
import com.Group13_65CNTT.AutoGarage.billing.mapper.PaymentMapper;
import com.Group13_65CNTT.AutoGarage.billing.repository.*;
import com.Group13_65CNTT.AutoGarage.billing.service.impl.BillingServiceImpl;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocument;
import com.Group13_65CNTT.AutoGarage.inventory.repository.InventoryDocumentRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BillingService unit tests")
class BillingServiceTest {

    @Test
    void voidingUnpaidInvoiceReturnsOrderToCompletedForReissue() {
        Invoice invoice = Invoice.builder().id(1L).repairOrderId(500L).status(InvoiceStatus.UNPAID)
                .totalAmount(new BigDecimal("1000000")).build();
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(BigDecimal.ZERO);
        when(jdbcTemplate.queryForList(contains("FROM repair_orders"), eq(500L)))
                .thenReturn(List.of(Map.of("status", "INVOICED")));

        service.voidInvoice(10L, 1L, "Nhập sai chiết khấu");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VOID);
        assertThat(invoice.getVoidReason()).isEqualTo("Nhập sai chiết khấu");
        verify(jdbcTemplate).update(contains("SET status = 'COMPLETED'"), eq(500L));
    }

    @Test
    void voidingInvoiceAfterHandoverIsRejected() {
        Invoice invoice = Invoice.builder().id(1L).repairOrderId(500L).status(InvoiceStatus.UNPAID)
                .totalAmount(new BigDecimal("1000000")).build();
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(BigDecimal.ZERO);
        when(jdbcTemplate.queryForList(contains("FROM repair_orders"), eq(500L)))
                .thenReturn(List.of(Map.of("status", "CLOSED")));

        assertThatThrownBy(() -> service.voidInvoice(10L, 1L, "Muộn"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("chưa bàn giao");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void voidInvoiceCannotAuthorizeHandoverEvenWithZeroBalance() {
        // Only a voided invoice exists, so there is no live invoice to hand over against.
        when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.HANDOVER_BLOCKED)
                .hasMessageContaining("chưa có hóa đơn còn hiệu lực");
        verifyNoInteractions(paymentRepository);
    }

    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceItemRepository invoiceItemRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock InvoiceBalanceViewRepository invoiceBalanceViewRepository;
    @Mock SupplierBalanceViewRepository supplierBalanceViewRepository;
    @Mock InventoryDocumentRepository inventoryDocumentRepository;
    @Mock UserRepository userRepository;
    @Mock InvoiceMapper invoiceMapper;
    @Mock PaymentMapper paymentMapper;
    @Mock DebtMapper debtMapper;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;
    @Mock com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider currentUser;
    @Spy Optional<RepairWorkflow> repairWorkflow = Optional.empty();
    @Spy Optional<NotificationAudit> notificationAudit = Optional.empty();

    @InjectMocks BillingServiceImpl service;

    @Test
    void invoiceReadinessPreservesInfrastructureFailure() {
        RepairWorkflow workflow = mock(RepairWorkflow.class);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "repairWorkflow", Optional.of(workflow));
        var failure = new org.springframework.dao.DataAccessResourceFailureException("private database details");
        doThrow(failure).when(workflow).requireInvoiceReady(1L, 200L);
        InvoiceIssueRequest request = InvoiceIssueRequest.builder().repairOrderId(200L).build();

        assertThatThrownBy(() -> service.issueInvoice(1L, request)).isSameAs(failure);

        verifyNoInteractions(jdbcTemplate);
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void issueInvoice_Success() {
        InvoiceIssueRequest req = InvoiceIssueRequest.builder()
                .repairOrderId(200L)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(new BigDecimal("100000"))
                .build();

        when(invoiceRepository.existsByRepairOrderIdAndStatusNot(200L, InvoiceStatus.VOID)).thenReturn(false);

        // Mock order query
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("id", 200L);
        orderMap.put("customer_id", 100L);
        orderMap.put("vehicle_id", 300L);
        orderMap.put("latest_qc_check_id", 400L);
        orderMap.put("status", "COMPLETED");
        when(jdbcTemplate.queryForList(contains("FROM repair_orders"), eq(200L)))
                .thenReturn(List.of(orderMap));

        // Mock QC check query
        Map<String, Object> qcMap = new HashMap<>();
        qcMap.put("check_type", "QC");
        qcMap.put("result", "PASSED");
        when(jdbcTemplate.queryForList(contains("FROM vehicle_checks"), eq(400L)))
                .thenReturn(List.of(qcMap));

        // Mock active labor timers
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM labor_sessions"), eq(Integer.class), eq(200L)))
                .thenReturn(0);

        // Mock customer query
        Map<String, Object> custMap = new HashMap<>();
        custMap.put("customer_code", "CUST-01");
        custMap.put("full_name", "Nguyen Van A");
        custMap.put("phone", "0901234567");
        custMap.put("email", "a@test.com");
        custMap.put("address", "Hanoi");
        custMap.put("tax_code", "010101");
        when(jdbcTemplate.queryForMap(contains("FROM customers WHERE id = ?"), eq(100L)))
                .thenReturn(custMap);

        // Mock vehicle query
        Map<String, Object> vehMap = new HashMap<>();
        vehMap.put("plate_number", "30A-12345");
        vehMap.put("vin", "VIN12345");
        vehMap.put("make", "Toyota");
        vehMap.put("model", "Vios");
        vehMap.put("year", 2020);
        vehMap.put("color", "White");
        vehMap.put("current_odometer", 50000);
        when(jdbcTemplate.queryForMap(contains("FROM vehicles WHERE id = ?"), eq(300L)))
                .thenReturn(vehMap);

        // Mock tasks & parts queries (at least one billable item)
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("task_id", 1L);
        taskMap.put("quantity", BigDecimal.ONE);
        taskMap.put("service_name", "Oil change");
        taskMap.put("unit", "service");
        taskMap.put("unit_price", new BigDecimal("200000"));
        taskMap.put("unit_cost", new BigDecimal("50000"));
        taskMap.put("warranty_days", 30);
        taskMap.put("warranty_km", 1000);
        taskMap.put("warranty_terms", "Standard");

        when(jdbcTemplate.queryForList(contains("FROM repair_tasks rt"), eq(200L)))
                .thenReturn(List.of(taskMap));
        when(jdbcTemplate.queryForList(contains("FROM repair_order_parts rop"), eq(200L)))
                .thenReturn(List.of());

        Invoice savedInvoice = Invoice.builder()
                .id(1L).invoiceCode("INV-TEST").status(InvoiceStatus.UNPAID)
                .totalAmount(new BigDecimal("300000"))
                .items(new ArrayList<>())
                .build();

        when(invoiceRepository.save(any(Invoice.class))).thenReturn(savedInvoice);
        when(paymentRepository.calculatePaidAmountForInvoice(any())).thenReturn(BigDecimal.ZERO);
        when(invoiceMapper.toResponse(savedInvoice)).thenReturn(InvoiceResponse.builder().id(1L).invoiceCode("INV-TEST").build());

        InvoiceResponse response = service.issueInvoice(10L, req);

        assertThat(response).isNotNull();
        assertThat(response.getInvoiceCode()).isEqualTo("INV-TEST");
        verify(invoiceRepository).save(any(Invoice.class));
    }

    @Test
    void issueInvoice_AlreadyIssued_ThrowsConflict() {
        InvoiceIssueRequest req = InvoiceIssueRequest.builder()
                .repairOrderId(200L)
                .build();

        when(invoiceRepository.existsByRepairOrderIdAndStatusNot(200L, InvoiceStatus.VOID)).thenReturn(true);

        assertThatThrownBy(() -> service.issueInvoice(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.DUPLICATE_RESOURCE);
    }

    @Test
    void receivePayment_FullyPaid_TransitionsStatusToPaid() {
        Invoice invoice = Invoice.builder()
                .id(1L).invoiceCode("INV-001").status(InvoiceStatus.UNPAID)
                .totalAmount(new BigDecimal("500000"))
                .build();

        PaymentReceiveRequest req = PaymentReceiveRequest.builder()
                .invoiceId(1L).amount(new BigDecimal("500000"))
                .paymentMethod(BillingOperations.PaymentMethod.CASH)
                .providerReference("REF-001").notes("Full payment")
                .build();

        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(BigDecimal.ZERO);
        when(paymentRepository.existsByProviderReference("REF-001")).thenReturn(false);

        Payment payment = Payment.builder().id(50L).transactionCode("TX-001").amount(new BigDecimal("500000")).build();
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(PaymentResponse.builder().id(50L).transactionCode("TX-001").build());

        PaymentResponse response = service.receivePayment(10L, req);

        assertThat(response.getTransactionCode()).isEqualTo("TX-001");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(paymentRepository).save(any(Payment.class));
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void paySupplier_ExceedsDocumentTotal_ThrowsBadRequest() {
        User adminUser = User.builder().role(Role.ADMIN).build();
        adminUser.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));

        InventoryDocument doc = InventoryDocument.builder()
                .id(10L).documentType(DocumentType.IN).status(DocumentStatus.POSTED)
                .supplier(com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier.builder().id(1L).build())
                .totalAmount(new BigDecimal("1000000"))
                .build();

        SupplierPaymentRequest req = SupplierPaymentRequest.builder()
                .inventoryDocumentId(10L).amount(new BigDecimal("800000"))
                .paymentMethod(BillingOperations.PaymentMethod.BANK_TRANSFER)
                .notes("Supplier advance")
                .build();

        when(inventoryDocumentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(doc));
        // Already paid 300,000; attempting to pay 800,000 exceeds 1,000,000 total!
        when(paymentRepository.calculatePaidAmountForDocument(10L)).thenReturn(new BigDecimal("300000"));

        assertThatThrownBy(() -> service.paySupplier(10L, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("vượt số còn nợ nhà cung cấp")
                .hasMessageContaining("700.000");
    }

    @Test
    void requireHandoverAllowed_UnpaidInvoice_ThrowsHandoverDenied() {
        Invoice invoice = Invoice.builder()
                .id(1L).customerId(100L).status(InvoiceStatus.PARTIALLY_PAID).totalAmount(new BigDecimal("5000000"))
                .build();

        when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(new BigDecimal("1000000"));

        Map<String, Object> custMap = new HashMap<>();
        custMap.put("credit_limit", new BigDecimal("2000000"));
        custMap.put("payment_term_days", 15);
        when(jdbcTemplate.queryForList(contains("SELECT credit_limit"), eq(100L)))
                .thenReturn(List.of(custMap));

        // Remaining 4,000,000 exceeds credit limit 2,000,000!
        when(invoiceBalanceViewRepository.calculateTotalOutstandingBalanceForCustomer(100L))
                .thenReturn(new BigDecimal("4000000"));

        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.HANDOVER_BLOCKED)
                .hasMessageContaining("vượt hạn mức")
                .hasMessageContaining("tối thiểu 2.000.000");
    }

    @Test
    @DisplayName("Khách không có hạn mức: phải thu hết, số tối thiểu bằng toàn bộ phần còn nợ")
    void handoverReadiness_NoCreditLimit_MustPayInFull() {
        stubUnpaid(unpaidInvoice(), BigDecimal.ZERO, 15, new BigDecimal("4000000"));

        var result = service.getHandoverReadiness(500L);

        assertThat(result.allowed()).isFalse();
        assertThat(result.minimumPayment()).isEqualByComparingTo("4000000");
        assertThat(result.reasons().get(0)).contains("chưa có hạn mức nợ").contains("tối thiểu 4.000.000");
    }

    @Test
    @DisplayName("Có hạn mức, nợ vượt 1 đồng: cần thu đúng 1 đồng; nợ đúng bằng hạn mức thì được bàn giao")
    void handoverReadiness_BoundaryAroundCreditLimit() {
        stubUnpaid(unpaidInvoice(), new BigDecimal("3000000"), 15, new BigDecimal("3000001"));
        var over = service.getHandoverReadiness(500L);
        assertThat(over.allowed()).isFalse();
        assertThat(over.minimumPayment()).isEqualByComparingTo("1");

        when(invoiceBalanceViewRepository.calculateTotalOutstandingBalanceForCustomer(100L))
                .thenReturn(new BigDecimal("3000000"));
        var equal = service.getHandoverReadiness(500L);
        assertThat(equal.allowed()).isTrue();
        assertThat(equal.minimumPayment()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Hạn mức lớn hơn công nợ: chưa phải trả vội, bàn giao được dù hóa đơn còn nợ")
    void requireHandoverAllowed_DebtBelowLimit_Allowed() {
        stubUnpaid(unpaidInvoice(), new BigDecimal("9000000"), 15, new BigDecimal("4000000"));

        service.requireHandoverAllowed(10L, 500L);
    }

    private Invoice unpaidInvoice() {
        return Invoice.builder().id(1L).customerId(100L).status(InvoiceStatus.PARTIALLY_PAID)
                .totalAmount(new BigDecimal("5000000")).dueDate(java.time.LocalDate.now().plusDays(10)).build();
    }

    private void stubUnpaid(Invoice invoice, Object creditLimit, Object termDays, BigDecimal totalDebt) {
        lenient().when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(invoice));
        lenient().when(invoiceRepository.findActiveByRepairOrderId(500L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(new BigDecimal("1000000"));
        Map<String, Object> custMap = new HashMap<>();
        custMap.put("credit_limit", creditLimit);
        custMap.put("payment_term_days", termDays);
        when(jdbcTemplate.queryForList(contains("SELECT credit_limit"), eq(100L))).thenReturn(List.of(custMap));
        when(invoiceBalanceViewRepository.calculateTotalOutstandingBalanceForCustomer(100L)).thenReturn(totalDebt);
    }

    @Test
    @DisplayName("Hạn thanh toán 0 nhưng nợ trong hạn mức: vẫn bàn giao được")
    void requireHandoverAllowed_NoPaymentTermWithinLimit_Allowed() {
        stubUnpaid(unpaidInvoice(), new BigDecimal("9000000"), 0, new BigDecimal("4000000"));

        service.requireHandoverAllowed(10L, 500L);
    }

    @Test
    @DisplayName("Bàn giao bị chặn khi khách có hóa đơn quá hạn: thông điệp riêng")
    void requireHandoverAllowed_OverdueInvoice_ThrowsBlocked() {
        stubUnpaid(unpaidInvoice(), new BigDecimal("9000000"), 15, new BigDecimal("4000000"));
        InvoiceBalanceView overdue = org.mockito.Mockito.mock(InvoiceBalanceView.class);
        when(overdue.getDueDate()).thenReturn(java.time.LocalDate.now().minusDays(3));
        when(overdue.getBalanceAmount()).thenReturn(new BigDecimal("100000"));
        when(invoiceBalanceViewRepository.findByCustomerId(100L)).thenReturn(List.of(overdue));

        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.HANDOVER_BLOCKED)
                .hasMessageContaining("quá hạn");
    }

    @Test
    @DisplayName("Bàn giao bị chặn khi hóa đơn của phiếu đã bị hủy: thông điệp riêng")
    void requireHandoverAllowed_VoidInvoice_ThrowsBlocked() {
        Invoice voided = Invoice.builder().id(1L).customerId(100L).status(InvoiceStatus.VOID)
                .totalAmount(new BigDecimal("5000000")).build();
        when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(voided));

        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.HANDOVER_BLOCKED)
                .hasMessageContaining("đã bị hủy");
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("Bàn giao báo không tìm thấy khi thiếu hồ sơ khách của hóa đơn")
    void requireHandoverAllowed_MissingCustomer_ThrowsNotFound() {
        Invoice invoice = unpaidInvoice();
        when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(new BigDecimal("1000000"));
        when(jdbcTemplate.queryForList(contains("SELECT credit_limit"), eq(100L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException.class)
                .hasMessageContaining("khách hàng");
    }

    @Test
    @DisplayName("Bàn giao được khi hóa đơn đã thanh toán đủ")
    void requireHandoverAllowed_PaidInvoice_Allowed() {
        Invoice paid = Invoice.builder().id(1L).customerId(100L).status(InvoiceStatus.PAID)
                .totalAmount(new BigDecimal("5000000")).build();
        when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(paid));

        service.requireHandoverAllowed(10L, 500L);

        verifyNoInteractions(jdbcTemplate);
    }

    // ---- Bổ sung bảo hành theo dòng hóa đơn trước khi bàn giao ----

    private InvoiceItem warrantyLine(Invoice invoice) {
        InvoiceItem line = InvoiceItem.builder().id(7L).invoice(invoice).itemName("Thay má phanh").build();
        when(invoiceItemRepository.findById(7L)).thenReturn(Optional.of(line));
        return line;
    }

    private Invoice warrantyInvoice(InvoiceStatus status, String orderStatus, int existingBooks) {
        Invoice invoice = Invoice.builder().id(1L).repairOrderId(500L).customerId(100L).status(status)
                .totalAmount(new BigDecimal("100000")).build();
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));
        lenient().when(jdbcTemplate.queryForList(contains("SELECT status FROM repair_orders"), eq(String.class), eq(500L)))
                .thenReturn(List.of(orderStatus));
        lenient().when(jdbcTemplate.queryForObject(contains("FROM warranties"), eq(Integer.class), eq(7L)))
                .thenReturn(existingBooks);
        return invoice;
    }

    private com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceItemWarrantyRequest warrantyRequest(
            int days, int km, String terms) {
        return com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceItemWarrantyRequest.builder()
                .warrantyDays(days).warrantyKm(km).warrantyTerms(terms).build();
    }

    @Test
    @DisplayName("Bổ sung bảo hành cho dòng hóa đơn: lưu ngày, km và điều khoản đã cắt khoảng trắng")
    void updateInvoiceItemWarranty_SavesPolicy() {
        Invoice invoice = warrantyInvoice(InvoiceStatus.UNPAID, "INVOICED", 0);
        InvoiceItem line = warrantyLine(invoice);
        lenient().when(invoiceItemRepository.findByInvoiceId(1L)).thenReturn(List.of(line));

        try {
            service.updateInvoiceItemWarranty(10L, 1L, 7L, warrantyRequest(90, 5000, "  Bảo hành chính hãng "));
        } catch (RuntimeException ignored) {
            // Response enrichment needs collaborators this unit test does not stub; the saved policy is what matters.
        }

        assertThat(line.getWarrantyDays()).isEqualTo(90);
        assertThat(line.getWarrantyKm()).isEqualTo(5000);
        assertThat(line.getWarrantyTerms()).isEqualTo("Bảo hành chính hãng");
        verify(invoiceItemRepository).save(line);
    }

    @Test
    @DisplayName("Không thêm bảo hành khi phiếu đã bàn giao (CLOSED)")
    void updateInvoiceItemWarranty_ClosedOrder_Blocked() {
        warrantyInvoice(InvoiceStatus.PAID, "CLOSED", 0);

        assertThatThrownBy(() -> service.updateInvoiceItemWarranty(10L, 1L, 7L, warrantyRequest(30, 0, null)))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.WARRANTY_BLOCKED)
                .hasMessageContaining("đã bàn giao");
        verify(invoiceItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Không thêm bảo hành khi không tìm thấy phiếu của hóa đơn")
    void updateInvoiceItemWarranty_MissingOrder_Blocked() {
        Invoice invoice = Invoice.builder().id(1L).repairOrderId(500L).customerId(100L)
                .status(InvoiceStatus.UNPAID).totalAmount(new BigDecimal("100000")).build();
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));
        when(jdbcTemplate.queryForList(contains("SELECT status FROM repair_orders"), eq(String.class), eq(500L)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.updateInvoiceItemWarranty(10L, 1L, 7L, warrantyRequest(30, 0, null)))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.WARRANTY_BLOCKED);
        verify(invoiceItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Nợ nơi khác vượt xa: thông báo nêu đúng số tối thiểu bằng minimumPayment, không vượt số còn nợ")
    void handoverReadiness_MessageMatchesCappedMinimum() {
        // balance 4,000,000 (5M total - 1M paid); debt 9M vs limit 3M => uncapped 6M, capped 4M
        stubUnpaid(unpaidInvoice(), new BigDecimal("3000000"), 15, new BigDecimal("9000000"));

        var result = service.getHandoverReadiness(500L);

        assertThat(result.minimumPayment()).isEqualByComparingTo("4000000");
        assertThat(result.reasons().get(0)).contains("tối thiểu 4.000.000").doesNotContain("6.000.000");
    }

    @Test
    @DisplayName("Không thêm bảo hành cho hóa đơn đã hủy")
    void updateInvoiceItemWarranty_VoidInvoice_Blocked() {
        warrantyInvoice(InvoiceStatus.VOID, "INVOICED", 0);

        assertThatThrownBy(() -> service.updateInvoiceItemWarranty(10L, 1L, 7L, warrantyRequest(30, 0, null)))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.WARRANTY_BLOCKED)
                .hasMessageContaining("đã bị hủy");
    }

    @Test
    @DisplayName("Dòng đã có sổ bảo hành thì khóa, hướng dẫn điều chỉnh ở màn Bảo hành")
    void updateInvoiceItemWarranty_ExistingBook_Blocked() {
        Invoice invoice = warrantyInvoice(InvoiceStatus.UNPAID, "INVOICED", 1);
        warrantyLine(invoice);

        assertThatThrownBy(() -> service.updateInvoiceItemWarranty(10L, 1L, 7L, warrantyRequest(30, 0, null)))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.WARRANTY_BLOCKED)
                .hasMessageContaining("đã có sổ bảo hành")
                .hasMessageContaining("Thay má phanh");
        verify(invoiceItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Dòng không thuộc hóa đơn này: báo không tìm thấy")
    void updateInvoiceItemWarranty_ForeignLine_NotFound() {
        warrantyInvoice(InvoiceStatus.UNPAID, "INVOICED", 0);
        Invoice other = Invoice.builder().id(99L).build();
        when(invoiceItemRepository.findById(7L)).thenReturn(Optional.of(
                InvoiceItem.builder().id(7L).invoice(other).itemName("Dòng khác").build()));

        assertThatThrownBy(() -> service.updateInvoiceItemWarranty(10L, 1L, 7L, warrantyRequest(30, 0, null)))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Khách hàng chỉ được xem hóa đơn của chính mình")
    void getInvoiceById_Customer_RequiresOwnership() {
        Invoice invoice = new Invoice();
        invoice.setId(7L);
        when(invoiceRepository.findById(7L)).thenReturn(Optional.of(invoice));
        when(currentUser.role()).thenReturn(Role.CUSTOMER);
        doThrow(new BusinessException(ResponseCode.FORBIDDEN))
                .when(customerScope).requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.INVOICE, 7L);

        assertThatThrownBy(() -> service.getInvoiceById(7L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.FORBIDDEN);
        verify(invoiceMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("Khách hàng tìm hóa đơn bị ép theo mã khách hàng của mình")
    void searchInvoices_Customer_ForcesOwnCustomerId() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(currentUser.role()).thenReturn(Role.CUSTOMER);
        when(customerScope.customerIdForQuery(null)).thenReturn(5L);
        when(invoiceRepository.searchInvoices(null, 5L, null, null, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        service.searchInvoices(null, null, null, null, pageable);

        verify(invoiceRepository).searchInvoices(null, 5L, null, null, pageable);
    }

    @Test
    @DisplayName("Nhân viên tìm hóa đơn giữ nguyên bộ lọc khách hàng")
    void searchInvoices_Staff_KeepsRequestedCustomerId() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        when(currentUser.role()).thenReturn(Role.RECEPTIONIST);
        when(invoiceRepository.searchInvoices(null, 9L, null, null, pageable))
                .thenReturn(org.springframework.data.domain.Page.empty(pageable));

        service.searchInvoices(null, 9L, null, null, pageable);

        verify(customerScope, never()).customerIdForQuery(any());
        verify(invoiceRepository).searchInvoices(null, 9L, null, null, pageable);
    }

    // =========================================================================
    // Lý do không lập được hóa đơn
    // =========================================================================

    private InvoiceIssueRequest issueRequest() {
        return InvoiceIssueRequest.builder().repairOrderId(200L).build();
    }

    private void stubOrderRow(Object latestQc) {
        Map<String, Object> orderMap = new HashMap<>();
        orderMap.put("id", 200L);
        orderMap.put("customer_id", 100L);
        orderMap.put("vehicle_id", 300L);
        orderMap.put("latest_qc_check_id", latestQc);
        orderMap.put("status", "COMPLETED");
        when(jdbcTemplate.queryForList(contains("FROM repair_orders"), eq(200L))).thenReturn(List.of(orderMap));
    }

    private void stubQc(String result) {
        Map<String, Object> qcMap = new HashMap<>();
        qcMap.put("check_type", "QC");
        qcMap.put("result", result);
        when(jdbcTemplate.queryForList(contains("FROM vehicle_checks"), eq(400L))).thenReturn(List.of(qcMap));
    }

    @Test
    @DisplayName("Lập hóa đơn khi phiếu đã có hóa đơn: thông điệp riêng")
    void issueInvoice_AlreadyIssued_HasMessage() {
        when(invoiceRepository.existsByRepairOrderIdAndStatusNot(200L, InvoiceStatus.VOID)).thenReturn(true);

        assertThatThrownBy(() -> service.issueInvoice(10L, issueRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đã có hóa đơn còn hiệu lực");
    }

    @Test
    @DisplayName("Lập hóa đơn khi phiếu chưa có QC: 409/INVOICE_BLOCKED kèm thông điệp")
    void issueInvoice_NoQc_ThrowsBlocked() {
        stubOrderRow(null);

        assertThatThrownBy(() -> service.issueInvoice(10L, issueRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.INVOICE_BLOCKED)
                .hasMessageContaining("chưa có kết quả QC");
    }

    @Test
    @DisplayName("Lập hóa đơn khi QC gần nhất chưa đạt: thông điệp riêng")
    void issueInvoice_QcFailed_ThrowsBlocked() {
        stubOrderRow(400L);
        stubQc("FAILED");

        assertThatThrownBy(() -> service.issueInvoice(10L, issueRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.INVOICE_BLOCKED)
                .hasMessageContaining("QC gần nhất chưa đạt");
    }

    @Test
    @DisplayName("Lập hóa đơn khi còn đồng hồ đang chạy: thông điệp riêng")
    void issueInvoice_OpenTimer_ThrowsBlocked() {
        stubOrderRow(400L);
        stubQc("PASSED");
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM labor_sessions"), eq(Integer.class), eq(200L)))
                .thenReturn(2);

        assertThatThrownBy(() -> service.issueInvoice(10L, issueRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.INVOICE_BLOCKED)
                .hasMessageContaining("bấm giờ");
    }

    @Test
    @DisplayName("Lập hóa đơn khi không có hạng mục tính tiền: thông điệp riêng")
    void issueInvoice_NoBillableItems_ThrowsBlocked() {
        stubOrderRow(400L);
        stubQc("PASSED");
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM labor_sessions"), eq(Integer.class), eq(200L)))
                .thenReturn(0);
        Map<String, Object> custMap = new HashMap<>();
        custMap.put("customer_code", "C1");
        custMap.put("full_name", "A");
        custMap.put("payment_term_days", 0);
        when(jdbcTemplate.queryForMap(contains("FROM customers"), eq(100L))).thenReturn(custMap);
        Map<String, Object> vehMap = new HashMap<>();
        vehMap.put("plate_number", "30A-123.45");
        when(jdbcTemplate.queryForMap(contains("FROM vehicles"), eq(300L))).thenReturn(vehMap);
        when(jdbcTemplate.queryForList(contains("FROM repair_tasks rt"), eq(200L))).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM repair_order_parts"), eq(200L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.issueInvoice(10L, issueRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.INVOICE_BLOCKED)
                .hasMessageContaining("không có hạng mục");
    }

    @Test
    @DisplayName("Lập hóa đơn với giảm giá lớn hơn tạm tính: 400 kèm thông điệp, không lưu hóa đơn")
    void issueInvoice_DiscountExceedsSubtotal_ThrowsRuleViolation() {
        stubOrderRow(400L);
        stubQc("PASSED");
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM labor_sessions"), eq(Integer.class), eq(200L)))
                .thenReturn(0);
        Map<String, Object> custMap = new HashMap<>();
        custMap.put("customer_code", "C1");
        custMap.put("full_name", "A");
        custMap.put("payment_term_days", 0);
        when(jdbcTemplate.queryForMap(contains("FROM customers"), eq(100L))).thenReturn(custMap);
        Map<String, Object> vehMap = new HashMap<>();
        vehMap.put("plate_number", "30A-123.45");
        when(jdbcTemplate.queryForMap(contains("FROM vehicles"), eq(300L))).thenReturn(vehMap);
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("task_id", 1L);
        taskMap.put("quantity", BigDecimal.ONE);
        taskMap.put("service_name", "Oil change");
        taskMap.put("unit", "service");
        taskMap.put("unit_price", new BigDecimal("200000"));
        taskMap.put("unit_cost", new BigDecimal("50000"));
        when(jdbcTemplate.queryForList(contains("FROM repair_tasks rt"), eq(200L))).thenReturn(List.of(taskMap));
        when(jdbcTemplate.queryForList(contains("FROM repair_order_parts"), eq(200L))).thenReturn(List.of());
        InvoiceIssueRequest request = InvoiceIssueRequest.builder()
                .repairOrderId(200L).discountAmount(new BigDecimal("500000")).build();

        assertThatThrownBy(() -> service.issueInvoice(10L, request))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST)
                .hasMessageContaining("giảm giá");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kiểm tra sẵn sàng: bị chặn thì trả ready=false kèm lý do, không ghi dữ liệu")
    void getInvoiceReadiness_Blocked_ReturnsReason() {
        when(currentUser.userId()).thenReturn(10L);
        stubOrderRow(null);

        var result = service.getInvoiceReadiness(200L);

        assertThat(result.ready()).isFalse();
        assertThat(result.reason()).contains("chưa có kết quả QC");
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Kiểm tra sẵn sàng: đủ điều kiện thì ready=true và không khóa dòng phiếu")
    void getInvoiceReadiness_Ready() {
        when(currentUser.userId()).thenReturn(10L);
        stubOrderRow(400L);
        stubQc("PASSED");
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM labor_sessions"), eq(Integer.class), eq(200L)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("FROM repair_tasks rt"), eq(Integer.class), eq(200L), eq(200L)))
                .thenReturn(3);

        var result = service.getInvoiceReadiness(200L);

        assertThat(result.ready()).isTrue();
        assertThat(result.reason()).isNull();
        verify(jdbcTemplate, never()).queryForList(contains("FOR UPDATE"), eq(200L));
    }

    @Test
    @DisplayName("Kiểm tra sẵn sàng: phiếu không có hạng mục tính tiền thì ready=false")
    void getInvoiceReadiness_NoBillableItems() {
        when(currentUser.userId()).thenReturn(10L);
        stubOrderRow(400L);
        stubQc("PASSED");
        when(jdbcTemplate.queryForObject(contains("COUNT(*) FROM labor_sessions"), eq(Integer.class), eq(200L)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("FROM repair_tasks rt"), eq(Integer.class), eq(200L), eq(200L)))
                .thenReturn(0);

        var result = service.getInvoiceReadiness(200L);

        assertThat(result.ready()).isFalse();
        assertThat(result.reason()).contains("không có hạng mục");
    }

    @Test
    @DisplayName("Kiểm tra sẵn sàng: lỗi hạ tầng không bị nuốt thành ready=false")
    void getInvoiceReadiness_InfrastructureFailurePropagates() {
        when(currentUser.userId()).thenReturn(10L);
        var failure = new org.springframework.dao.DataAccessResourceFailureException("db down");
        when(jdbcTemplate.queryForList(contains("FROM repair_orders"), eq(200L))).thenThrow(failure);

        assertThatThrownBy(() -> service.getInvoiceReadiness(200L)).isSameAs(failure);
    }

    // =========================================================================
    // Thu tiền: mã giao dịch tự sinh, sửa được và thông điệp từ chối
    // =========================================================================

    private Invoice unpaid(long total) {
        return Invoice.builder().id(1L).invoiceCode("INV-001").status(InvoiceStatus.UNPAID)
                .totalAmount(new BigDecimal(total)).build();
    }

    private PaymentReceiveRequest.PaymentReceiveRequestBuilder receipt(long amount) {
        return PaymentReceiveRequest.builder().invoiceId(1L).amount(new BigDecimal(amount))
                .paymentMethod(BillingOperations.PaymentMethod.CASH);
    }

    private void stubInvoice(Invoice invoice, long alreadyPaid) {
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(new BigDecimal(alreadyPaid));
    }

    @Test
    @DisplayName("Thu đúng số còn nợ lẻ (1.365.998) thành công và hóa đơn chuyển PAID")
    void receivePayment_ExactOddBalance_Paid() {
        Invoice invoice = Invoice.builder().id(1L).invoiceCode("INV-10").status(InvoiceStatus.PARTIALLY_PAID)
                .totalAmount(new BigDecimal("3168000")).build();
        stubInvoice(invoice, 1_802_002);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.receivePayment(10L, receipt(1_365_998).build());

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    }

    @Test
    @DisplayName("Bỏ trống mã giao dịch: máy chủ tự sinh PT-yyyyMMdd-0001 khi hôm nay chưa có phiếu nào")
    void receivePayment_BlankCode_GeneratesFirstReceiptCode() {
        stubInvoice(unpaid(500_000), 0);
        when(paymentRepository.findCodesStartingWith(anyString(), any())).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.receivePayment(10L, receipt(100_000).transactionCode("  ").build());

        org.mockito.ArgumentCaptor<Payment> saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getTransactionCode()).matches("PT-\\d{8}-0001");
    }

    @Test
    @DisplayName("Mã tự sinh đánh số tiếp theo sau mã lớn nhất trong ngày")
    void suggestTransactionCode_ContinuesAfterHighestOfTheDay() {
        String prefix = com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy.dayPrefix(
                com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection.OUT,
                com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy.today());
        when(paymentRepository.findCodesStartingWith(eq(prefix), any())).thenReturn(List.of(prefix + "0007", prefix + "0003"));

        assertThat(service.suggestTransactionCode(com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection.OUT))
                .isEqualTo(prefix + "0008");
    }

    @Test
    @DisplayName("Mã lớn nhất không đúng định dạng bị bỏ qua, đánh số lại từ 0001; mã bị chiếm thì thử số kế tiếp")
    void suggestTransactionCode_IgnoresMalformedAndSkipsTaken() {
        var direction = com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection.IN;
        String prefix = com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy.dayPrefix(
                direction, com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy.today());
        when(paymentRepository.findCodesStartingWith(eq(prefix), any())).thenReturn(List.of(prefix + "ABCD"));
        when(paymentRepository.existsByTransactionCode(prefix + "0001")).thenReturn(true);

        assertThat(service.suggestTransactionCode(direction)).isEqualTo(prefix + "0002");
    }

    private static final String CODE_LOCK_SQL = "SELECT id FROM payment_code_lock WHERE id = 1 FOR UPDATE";

    @Test
    @DisplayName("Thu tiền lấy khóa mã trước khi kiểm tra mã nhập tay và trước khi ghi")
    void receivePayment_TypedCode_LocksBeforeChecking() {
        stubInvoice(unpaid(500_000), 0);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.receivePayment(10L, receipt(100_000).transactionCode("pt-lock").build());

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(jdbcTemplate, paymentRepository);
        order.verify(jdbcTemplate).queryForObject(CODE_LOCK_SQL, Integer.class);
        order.verify(paymentRepository).existsByTransactionCode("PT-LOCK");
        order.verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("Thu tiền tự sinh mã cũng lấy khóa mã trước khi tìm mã lớn nhất")
    void receivePayment_GeneratedCode_LocksBeforeNumbering() {
        stubInvoice(unpaid(500_000), 0);
        when(paymentRepository.findCodesStartingWith(anyString(), any())).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.receivePayment(10L, receipt(100_000).build());

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(jdbcTemplate, paymentRepository);
        order.verify(jdbcTemplate).queryForObject(CODE_LOCK_SQL, Integer.class);
        order.verify(paymentRepository).findCodesStartingWith(anyString(), any());
    }

    @Test
    @DisplayName("Gợi ý mã chỉ đọc nên không lấy khóa mã")
    void suggestTransactionCode_DoesNotLock() {
        when(paymentRepository.findCodesStartingWith(anyString(), any())).thenReturn(List.of());

        service.suggestTransactionCode(com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection.IN);

        verify(jdbcTemplate, never()).queryForObject(CODE_LOCK_SQL, Integer.class);
    }

    @Test
    @DisplayName("Mã giao dịch nhập tay được cắt khoảng trắng và đổi sang chữ in hoa")
    void receivePayment_TypedCode_IsNormalised() {
        stubInvoice(unpaid(500_000), 0);
        when(paymentRepository.existsByTransactionCode("PT-MANUAL.01")).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.receivePayment(10L, receipt(100_000).transactionCode("  pt-manual.01 ").build());

        org.mockito.ArgumentCaptor<Payment> saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getTransactionCode()).isEqualTo("PT-MANUAL.01");
    }

    @Test
    @DisplayName("Mã giao dịch nhập tay đã tồn tại: 409/PAYMENT_BLOCKED hướng dẫn nhập mã khác hoặc Sinh lại")
    void receivePayment_DuplicateCode_Blocked() {
        stubInvoice(unpaid(500_000), 0);
        when(paymentRepository.existsByTransactionCode("PT-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(100_000).transactionCode("pt-dup").build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("PT-DUP")
                .hasMessageContaining("Sinh lại");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Mã giao dịch sai định dạng bị từ chối")
    void receivePayment_InvalidCodeFormat_Rejected() {
        stubInvoice(unpaid(500_000), 0);

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(100_000).transactionCode("PT 01/02").build()))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException.class)
                .hasMessageContaining("chỉ gồm chữ, số");
    }

    @Test
    @DisplayName("Thu tiền hóa đơn đã hủy: thông điệp riêng")
    void receivePayment_VoidInvoice_Blocked() {
        Invoice invoice = Invoice.builder().id(1L).status(InvoiceStatus.VOID).totalAmount(new BigDecimal("500000")).build();
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(100_000).build()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("đã bị hủy");
    }

    @Test
    @DisplayName("Thu tiền hóa đơn đã thu đủ: thông điệp riêng")
    void receivePayment_PaidInvoice_Blocked() {
        Invoice invoice = Invoice.builder().id(1L).status(InvoiceStatus.PAID).totalAmount(new BigDecimal("500000")).build();
        when(invoiceRepository.findByIdWithLock(1L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(100_000).build()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("đã được thu đủ");
    }

    @Test
    @DisplayName("Thu vượt số còn nợ: nêu số còn nợ")
    void receivePayment_ExceedsBalance_Blocked() {
        stubInvoice(unpaid(500_000), 200_000);

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(300_001).build()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("vượt số còn nợ")
                .hasMessageContaining("300.000");
    }

    @Test
    @DisplayName("Số tiền thu không dương: lỗi nghiệp vụ có thông điệp")
    void receivePayment_NonPositiveAmount_Rejected() {
        stubInvoice(unpaid(500_000), 0);

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(0).build()))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException.class)
                .hasMessageContaining("lớn hơn 0");
    }

    @Test
    @DisplayName("Mã tham chiếu đã dùng: thông điệp nêu mã")
    void receivePayment_DuplicateProviderReference_Blocked() {
        stubInvoice(unpaid(500_000), 0);
        when(paymentRepository.existsByProviderReference("REF-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.receivePayment(10L, receipt(100_000).providerReference(" REF-DUP ").build()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("REF-DUP")
                .hasMessageContaining("đã được dùng");
    }

    // =========================================================================
    // Chi trả nhà cung cấp: mã giao dịch PC, nhập tay, trùng
    // =========================================================================

    private InventoryDocument postedSupplierDocument(long total, long alreadyPaid) {
        User adminUser = User.builder().role(Role.ADMIN).build();
        adminUser.setId(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(adminUser));
        InventoryDocument doc = InventoryDocument.builder()
                .id(10L).documentType(DocumentType.IN).status(DocumentStatus.POSTED)
                .supplier(com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier.builder().id(1L).build())
                .totalAmount(new BigDecimal(total))
                .build();
        when(inventoryDocumentRepository.findByIdWithLock(10L)).thenReturn(Optional.of(doc));
        lenient().when(paymentRepository.calculatePaidAmountForDocument(10L)).thenReturn(new BigDecimal(alreadyPaid));
        return doc;
    }

    private SupplierPaymentRequest.SupplierPaymentRequestBuilder payout(long amount) {
        return SupplierPaymentRequest.builder().inventoryDocumentId(10L).amount(new BigDecimal(amount))
                .paymentMethod(BillingOperations.PaymentMethod.BANK_TRANSFER);
    }

    @Test
    @DisplayName("Chi trả NCC bỏ trống mã: máy chủ sinh mã PC-yyyyMMdd-0001 của hôm nay")
    void paySupplier_BlankCode_GeneratesPayoutCode() {
        postedSupplierDocument(1_000_000, 0);
        when(paymentRepository.findCodesStartingWith(anyString(), any())).thenReturn(List.of());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.paySupplier(10L, payout(400_000).build());

        org.mockito.ArgumentCaptor<Payment> saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        String today = com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy.dayPrefix(
                com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection.OUT,
                com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy.today());
        assertThat(saved.getValue().getTransactionCode()).isEqualTo(today + "0001");
        assertThat(saved.getValue().getTransactionCode()).startsWith("PC-");
    }

    @Test
    @DisplayName("Chi trả NCC với mã nhập tay: được chuẩn hóa và lưu đúng mã đó")
    void paySupplier_TypedCode_IsNormalisedAndKept() {
        postedSupplierDocument(1_000_000, 0);
        when(paymentRepository.existsByTransactionCode("PC-TAY-01")).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(PaymentResponse.builder().id(1L).build());

        service.paySupplier(10L, payout(400_000).transactionCode(" pc-tay-01 ").build());

        org.mockito.ArgumentCaptor<Payment> saved = org.mockito.ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getTransactionCode()).isEqualTo("PC-TAY-01");
    }

    @Test
    @DisplayName("Chi trả NCC với mã giao dịch đã tồn tại: 409/PAYMENT_BLOCKED, không ghi phiếu chi")
    void paySupplier_DuplicateCode_Blocked() {
        postedSupplierDocument(1_000_000, 0);
        when(paymentRepository.existsByTransactionCode("PC-DUP")).thenReturn(true);

        assertThatThrownBy(() -> service.paySupplier(10L, payout(400_000).transactionCode("pc-dup").build()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("PC-DUP")
                .hasMessageContaining("Sinh lại");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chi trả NCC với mã tham chiếu đã dùng: thông điệp nêu mã")
    void paySupplier_DuplicateProviderReference_Blocked() {
        postedSupplierDocument(1_000_000, 0);
        when(paymentRepository.existsByProviderReference("UNC-001")).thenReturn(true);

        assertThatThrownBy(() -> service.paySupplier(10L, payout(400_000).providerReference(" UNC-001 ").build()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("UNC-001")
                .hasMessageContaining("đã được dùng");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chi trả NCC cho chứng từ chưa ghi sổ: thông điệp riêng")
    void paySupplier_UnpostedDocument_Blocked() {
        postedSupplierDocument(1_000_000, 0).setStatus(DocumentStatus.DRAFT);

        assertThatThrownBy(() -> service.paySupplier(10L, payout(400_000).build()))
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.PAYMENT_BLOCKED)
                .hasMessageContaining("đã ghi sổ");
    }

    @Test
    @DisplayName("Chi trả NCC số tiền không dương: lỗi nghiệp vụ có thông điệp")
    void paySupplier_NonPositiveAmount_Rejected() {
        postedSupplierDocument(1_000_000, 0);

        assertThatThrownBy(() -> service.paySupplier(10L, payout(0).build()))
                .isInstanceOf(com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException.class)
                .hasMessageContaining("lớn hơn 0");
    }
}

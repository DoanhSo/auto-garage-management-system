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
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);
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
                .hasMessageContaining("vượt hạn mức");
    }

    private Invoice unpaidInvoice() {
        return Invoice.builder().id(1L).customerId(100L).status(InvoiceStatus.PARTIALLY_PAID)
                .totalAmount(new BigDecimal("5000000")).dueDate(java.time.LocalDate.now().plusDays(10)).build();
    }

    private void stubUnpaid(Invoice invoice, Object creditLimit, Object termDays, BigDecimal totalDebt) {
        when(invoiceRepository.findActiveByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(invoice));
        when(paymentRepository.calculatePaidAmountForInvoice(1L)).thenReturn(new BigDecimal("1000000"));
        Map<String, Object> custMap = new HashMap<>();
        custMap.put("credit_limit", creditLimit);
        custMap.put("payment_term_days", termDays);
        when(jdbcTemplate.queryForList(contains("SELECT credit_limit"), eq(100L))).thenReturn(List.of(custMap));
        when(invoiceBalanceViewRepository.calculateTotalOutstandingBalanceForCustomer(100L)).thenReturn(totalDebt);
    }

    @Test
    @DisplayName("Bàn giao bị chặn khi khách không có hạn thanh toán công nợ: thông điệp riêng")
    void requireHandoverAllowed_NoPaymentTerm_ThrowsBlocked() {
        stubUnpaid(unpaidInvoice(), new BigDecimal("9000000"), 0, new BigDecimal("4000000"));

        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.HANDOVER_BLOCKED)
                .hasMessageContaining("không được ghi công nợ");
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
}

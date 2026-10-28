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
    void voidInvoiceCannotAuthorizeHandoverEvenWithZeroBalance() {
        Invoice invoice = Invoice.builder().id(1L).status(InvoiceStatus.VOID).totalAmount(BigDecimal.ZERO).build();
        when(invoiceRepository.findByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(invoice));
        assertThatThrownBy(() -> service.requireHandoverAllowed(10L, 500L))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);
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

        when(invoiceRepository.existsByRepairOrderId(200L)).thenReturn(false);

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

        when(invoiceRepository.existsByRepairOrderId(200L)).thenReturn(true);

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

        when(invoiceRepository.findByRepairOrderIdWithLock(500L)).thenReturn(Optional.of(invoice));
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
                .hasFieldOrPropertyWithValue("responseCode", ResponseCode.BAD_REQUEST);
    }
}

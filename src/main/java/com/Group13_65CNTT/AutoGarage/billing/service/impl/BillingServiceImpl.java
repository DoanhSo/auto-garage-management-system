package com.Group13_65CNTT.AutoGarage.billing.service.impl;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceIssueRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.SupplierPaymentRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceItemResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.PaymentResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.SupplierBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceBalanceView;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceItem;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceItemType;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus;
import com.Group13_65CNTT.AutoGarage.billing.entity.Payment;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.billing.entity.SupplierBalanceView;
import com.Group13_65CNTT.AutoGarage.billing.mapper.DebtMapper;
import com.Group13_65CNTT.AutoGarage.billing.mapper.InvoiceMapper;
import com.Group13_65CNTT.AutoGarage.billing.mapper.PaymentMapper;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceBalanceViewRepository;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceItemRepository;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceRepository;
import com.Group13_65CNTT.AutoGarage.billing.repository.PaymentRepository;
import com.Group13_65CNTT.AutoGarage.billing.repository.SupplierBalanceViewRepository;
import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.identity.entity.User;
import com.Group13_65CNTT.AutoGarage.identity.repository.UserRepository;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocument;
import com.Group13_65CNTT.AutoGarage.inventory.repository.InventoryDocumentRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BillingServiceImpl implements BillingService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceBalanceViewRepository invoiceBalanceViewRepository;
    private final SupplierBalanceViewRepository supplierBalanceViewRepository;
    private final InventoryDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final InvoiceMapper invoiceMapper;
    private final PaymentMapper paymentMapper;
    private final DebtMapper debtMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Optional<RepairWorkflow> repairWorkflow;
    private final Optional<NotificationAudit> notificationAudit;

    @Override
    @Transactional
    public long issueInvoice(long actorUserId, long repairOrderId, String requestKey) {
        InvoiceIssueRequest req = InvoiceIssueRequest.builder()
                .repairOrderId(repairOrderId)
                .discountAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .requestKey(requestKey)
                .build();
        InvoiceResponse resp = issueInvoice(actorUserId, req);
        return resp.getId();
    }

    @Override
    @Transactional
    public InvoiceResponse issueInvoice(long actorUserId, InvoiceIssueRequest request) {
        String requestKey = request.getRequestKey();
        String requestHash = "ISSUE_INVOICE:" + request.getRepairOrderId();
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for issuing invoice for order: {}", request.getRepairOrderId());
                return getInvoiceByOrderId(request.getRepairOrderId());
            }
        }

        if (invoiceRepository.existsByRepairOrderId(request.getRepairOrderId())) {
            log.warn("Invoice already issued for repair order {}", request.getRepairOrderId());
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }

        // Check readiness via I03 contract if available, and also verify DB state directly
        if (repairWorkflow.isPresent()) {
            try {
                repairWorkflow.get().requireInvoiceReady(actorUserId, request.getRepairOrderId());
            } catch (Exception ex) {
                log.warn("RepairWorkflow.requireInvoiceReady rejected order {}: {}", request.getRepairOrderId(), ex.getMessage());
                throw new BusinessException(ResponseCode.BAD_REQUEST, ex);
            }
        }

        // Direct DB verification of order readiness
        List<Map<String, Object>> orderRows = jdbcTemplate.queryForList(
                "SELECT ro.id, ro.customer_id, ro.vehicle_id, ro.latest_qc_check_id, ro.status " +
                "FROM repair_orders ro WHERE ro.id = ?",
                request.getRepairOrderId());

        if (orderRows.isEmpty()) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        }

        Map<String, Object> order = orderRows.get(0);
        Long customerId = ((Number) order.get("customer_id")).longValue();
        Long vehicleId = ((Number) order.get("vehicle_id")).longValue();
        Object latestQcObj = order.get("latest_qc_check_id");

        if (latestQcObj == null) {
            log.warn("Cannot issue invoice: repair order {} has not passed QC", request.getRepairOrderId());
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        Long latestQcId = ((Number) latestQcObj).longValue();
        List<Map<String, Object>> qcRows = jdbcTemplate.queryForList(
                "SELECT check_type, conclusion FROM vehicle_checks WHERE id = ?", latestQcId);
        if (qcRows.isEmpty() || !"PASSED".equalsIgnoreCase((String) qcRows.get(0).get("conclusion"))) {
            log.warn("Cannot issue invoice: QC check {} is not PASSED", latestQcId);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        // Verify no active labor timers
        Integer openTimers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM labor_sessions ls " +
                "JOIN task_assignments ta ON ls.assignment_id = ta.id " +
                "JOIN repair_tasks rt ON ta.task_id = rt.id " +
                "WHERE rt.repair_order_id = ? AND ls.ends_at IS NULL",
                Integer.class, request.getRepairOrderId());
        if (openTimers != null && openTimers > 0) {
            log.warn("Cannot issue invoice: repair order {} has {} active labor timers", request.getRepairOrderId(), openTimers);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        // Capture immutable customer snapshot
        Map<String, Object> cust = jdbcTemplate.queryForMap(
                "SELECT customer_code, full_name, phone, email, address, tax_code, payment_term_days, credit_limit " +
                "FROM customers WHERE id = ?", customerId);
        String customerSnapshot = String.format(
                "{\"customerCode\":\"%s\",\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"taxCode\":\"%s\"}",
                escapeJson(cust.get("customer_code")),
                escapeJson(cust.get("full_name")),
                escapeJson(cust.get("phone")),
                escapeJson(cust.get("email")),
                escapeJson(cust.get("address")),
                escapeJson(cust.get("tax_code")));

        // Capture immutable vehicle snapshot
        Map<String, Object> veh = jdbcTemplate.queryForMap(
                "SELECT plate_number, vin, make, model, year, color, current_odometer " +
                "FROM vehicles WHERE id = ?", vehicleId);
        String vehicleSnapshot = String.format(
                "{\"plateNumber\":\"%s\",\"vin\":\"%s\",\"make\":\"%s\",\"model\":\"%s\",\"year\":%s,\"color\":\"%s\",\"odometer\":%s}",
                escapeJson(veh.get("plate_number")),
                escapeJson(veh.get("vin")),
                escapeJson(veh.get("make")),
                escapeJson(veh.get("model")),
                veh.get("year"),
                escapeJson(veh.get("color")),
                veh.get("current_odometer"));

        String invoiceCode = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        LocalDate dueDate = request.getDueDate();
        if (dueDate == null) {
            Number termDays = (Number) cust.get("payment_term_days");
            int days = termDays != null ? termDays.intValue() : 0;
            dueDate = LocalDate.now().plusDays(days);
        }

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .repairOrderId(request.getRepairOrderId())
                .customerId(customerId)
                .customerSnapshot(customerSnapshot)
                .vehicleSnapshot(vehicleSnapshot)
                .issuedBy(actorUserId)
                .issuedAt(Instant.now())
                .dueDate(dueDate)
                .status(InvoiceStatus.UNPAID)
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .build();

        List<InvoiceItem> items = new ArrayList<>();
        int lineNo = 1;
        BigDecimal subtotal = BigDecimal.ZERO;

        // 1. Gather completed service tasks
        List<Map<String, Object>> serviceTasks = jdbcTemplate.queryForList(
                "SELECT rt.id AS task_id, s.service_name, s.unit, rt.actual_price, rt.actual_cost, " +
                "s.warranty_days, s.warranty_km, s.warranty_terms " +
                "FROM repair_tasks rt " +
                "JOIN services s ON rt.service_id = s.id " +
                "WHERE rt.repair_order_id = ? AND rt.status = 'COMPLETED'",
                request.getRepairOrderId());

        for (Map<String, Object> st : serviceTasks) {
            BigDecimal price = (BigDecimal) st.get("actual_price");
            BigDecimal cost = (BigDecimal) st.get("actual_cost");
            if (price == null) price = BigDecimal.ZERO;
            if (cost == null) cost = BigDecimal.ZERO;
            BigDecimal lineTotal = price;
            subtotal = subtotal.add(lineTotal);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .lineNo(lineNo++)
                    .itemType(InvoiceItemType.SERVICE)
                    .repairTaskId(((Number) st.get("task_id")).longValue())
                    .itemName((String) st.get("service_name"))
                    .unit((String) st.get("unit"))
                    .quantity(BigDecimal.ONE)
                    .unitPrice(price)
                    .unitCost(cost)
                    .lineTotal(lineTotal)
                    .warrantyDays(st.get("warranty_days") != null ? ((Number) st.get("warranty_days")).intValue() : 0)
                    .warrantyKm(st.get("warranty_km") != null ? ((Number) st.get("warranty_km")).intValue() : 0)
                    .warrantyTerms((String) st.get("warranty_terms"))
                    .build();
            items.add(item);
        }

        // 2. Gather issued parts with quantity > 0
        List<Map<String, Object>> orderParts = jdbcTemplate.queryForList(
                "SELECT rop.id AS rop_id, rop.quantity, rop.unit_price, rop.unit_cost, " +
                "p.part_name, p.unit, p.warranty_days, p.warranty_km, p.warranty_terms " +
                "FROM repair_order_parts rop " +
                "JOIN parts p ON rop.part_id = p.id " +
                "WHERE rop.repair_order_id = ? AND rop.quantity > 0",
                request.getRepairOrderId());

        for (Map<String, Object> op : orderParts) {
            BigDecimal qty = (BigDecimal) op.get("quantity");
            BigDecimal price = (BigDecimal) op.get("unit_price");
            BigDecimal cost = (BigDecimal) op.get("unit_cost");
            BigDecimal lineTotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .lineNo(lineNo++)
                    .itemType(InvoiceItemType.PART)
                    .repairOrderPartId(((Number) op.get("rop_id")).longValue())
                    .itemName((String) op.get("part_name"))
                    .unit((String) op.get("unit"))
                    .quantity(qty)
                    .unitPrice(price)
                    .unitCost(cost)
                    .lineTotal(lineTotal)
                    .warrantyDays(op.get("warranty_days") != null ? ((Number) op.get("warranty_days")).intValue() : 0)
                    .warrantyKm(op.get("warranty_km") != null ? ((Number) op.get("warranty_km")).intValue() : 0)
                    .warrantyTerms((String) op.get("warranty_terms"))
                    .build();
            items.add(item);
        }

        if (items.isEmpty()) {
            log.warn("Cannot issue invoice: repair order {} has no billable service tasks or parts", request.getRepairOrderId());
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        if (invoice.getDiscountAmount().compareTo(subtotal) > 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        BigDecimal total = subtotal.subtract(invoice.getDiscountAmount()).add(invoice.getTaxAmount());
        invoice.setSubtotal(subtotal);
        invoice.setTotalAmount(total);
        invoice.setItems(items);

        // If total is zero, mark PAID directly without payment record
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        }

        Invoice saved = invoiceRepository.save(invoice);

        // Update repair order status to INVOICED
        jdbcTemplate.update("UPDATE repair_orders SET status = 'INVOICED' WHERE id = ?", request.getRepairOrderId());

        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            notificationAudit.get().recordCompletedRequest(actorUserId, "ISSUE_INVOICE", "INVOICE", saved.getId(), requestKey, requestHash, "{\"invoiceCode\":\"" + saved.getInvoiceCode() + "\"}");
        }

        log.info("Issued invoice id: {}, code: {} for order: {}", saved.getId(), saved.getInvoiceCode(), request.getRepairOrderId());
        return enrichInvoiceResponse(saved);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return enrichInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByCode(String code) {
        Invoice invoice = invoiceRepository.findByInvoiceCode(code.trim())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return enrichInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByOrderId(Long repairOrderId) {
        Invoice invoice = invoiceRepository.findByRepairOrderId(repairOrderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return enrichInvoiceResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> searchInvoices(
            InvoiceStatus status,
            Long customerId,
            Long orderId,
            String query,
            Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return invoiceRepository.searchInvoices(status, customerId, orderId, cleanQuery, pageable)
                .map(this::enrichInvoiceResponse);
    }

    @Override
    @Transactional
    public void voidInvoice(long actorUserId, Long invoiceId, String voidReason) {
        Invoice invoice = invoiceRepository.findByIdWithLock(invoiceId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            log.warn("Invoice {} is already voided", invoiceId);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        BigDecimal paid = paymentRepository.calculatePaidAmountForInvoice(invoiceId);
        if (paid.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Cannot void invoice {} that has received payments of {}", invoiceId, paid);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setVoidReason(voidReason != null ? voidReason.trim() : "Cancelled");
        invoice.setVoidedBy(actorUserId);
        invoice.setVoidedAt(Instant.now());
        invoiceRepository.save(invoice);
        log.info("Invoice {} voided by actor {}", invoiceId, actorUserId);
    }

    @Override
    @Transactional
    public long receivePayment(long actorUserId, long invoiceId, PaymentCommand command) {
        PaymentReceiveRequest req = PaymentReceiveRequest.builder()
                .invoiceId(invoiceId)
                .amount(command.amount())
                .paymentMethod(command.method())
                .providerReference(command.providerReference())
                .requestKey(command.requestKey())
                .build();
        PaymentResponse resp = receivePayment(actorUserId, req);
        return resp.getId();
    }

    @Override
    @Transactional
    public PaymentResponse receivePayment(long actorUserId, PaymentReceiveRequest request) {
        String requestKey = request.getRequestKey();
        String requestHash = "RECEIVE_PAYMENT:" + request.getInvoiceId() + ":" + request.getAmount();
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for receiving payment key: {}", requestKey);
                Payment existing = paymentRepository.findByProviderReference(request.getProviderReference())
                        .orElseThrow(() -> new BusinessException(ResponseCode.DUPLICATE_RESOURCE));
                return paymentMapper.toResponse(existing);
            }
        }

        Invoice invoice = invoiceRepository.findByIdWithLock(request.getInvoiceId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        BigDecimal currentPaid = paymentRepository.calculatePaidAmountForInvoice(invoice.getId());
        BigDecimal balance = invoice.getTotalAmount().subtract(currentPaid);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0 || request.getAmount().compareTo(balance) > 0) {
            log.warn("Payment amount {} is invalid for invoice balance {}", request.getAmount(), balance);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        if (request.getProviderReference() != null && !request.getProviderReference().isBlank()) {
            if (paymentRepository.existsByProviderReference(request.getProviderReference().trim())) {
                log.warn("Provider reference already exists: {}", request.getProviderReference());
                throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
            }
        }

        String txCode = "TX-IN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = Payment.builder()
                .transactionCode(txCode)
                .direction(PaymentDirection.IN)
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .providerReference(request.getProviderReference())
                .paidAt(Instant.now())
                .recordedBy(actorUserId)
                .notes(request.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);

        BigDecimal newPaid = currentPaid.add(request.getAmount());
        if (newPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            notificationAudit.get().recordCompletedRequest(actorUserId, "RECEIVE_PAYMENT", "PAYMENT", saved.getId(), requestKey, requestHash, "{\"transactionCode\":\"" + txCode + "\"}");
        }

        log.info("Received payment id: {}, code: {}, amount: {} for invoice: {}", saved.getId(), txCode, request.getAmount(), invoice.getId());
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public long paySupplier(long actorUserId, long inventoryDocumentId, PaymentCommand command) {
        SupplierPaymentRequest req = SupplierPaymentRequest.builder()
                .inventoryDocumentId(inventoryDocumentId)
                .amount(command.amount())
                .paymentMethod(command.method())
                .providerReference(command.providerReference())
                .requestKey(command.requestKey())
                .build();
        PaymentResponse resp = paySupplier(actorUserId, req);
        return resp.getId();
    }

    @Override
    @Transactional
    public PaymentResponse paySupplier(long actorUserId, SupplierPaymentRequest request) {
        // Enforce BR01: only ADMIN can make outgoing payments
        User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new BusinessException(ResponseCode.UNAUTHORIZED));
        if (actor.getRole() != Role.ADMIN) {
            log.warn("Actor {} with role {} attempted paySupplier; ADMIN required", actorUserId, actor.getRole());
            throw new BusinessException(ResponseCode.FORBIDDEN);
        }

        String requestKey = request.getRequestKey();
        String requestHash = "PAY_SUPPLIER:" + request.getInventoryDocumentId() + ":" + request.getAmount();
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for paying supplier key: {}", requestKey);
                Payment existing = paymentRepository.findByProviderReference(request.getProviderReference())
                        .orElseThrow(() -> new BusinessException(ResponseCode.DUPLICATE_RESOURCE));
                return paymentMapper.toResponse(existing);
            }
        }

        InventoryDocument doc = documentRepository.findByIdWithLock(request.getInventoryDocumentId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (doc.getDocumentType() != DocumentType.IN || doc.getStatus() != DocumentStatus.POSTED || doc.getSupplier() == null) {
            log.warn("Cannot pay document {}: type={}, status={}, supplier={}",
                    doc.getId(), doc.getDocumentType(), doc.getStatus(), doc.getSupplier());
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        BigDecimal currentPaid = paymentRepository.calculatePaidAmountForDocument(doc.getId());
        BigDecimal balance = doc.getTotalAmount().subtract(currentPaid);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0 || request.getAmount().compareTo(balance) > 0) {
            log.warn("Payment amount {} exceeds document balance {}", request.getAmount(), balance);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        if (request.getProviderReference() != null && !request.getProviderReference().isBlank()) {
            if (paymentRepository.existsByProviderReference(request.getProviderReference().trim())) {
                log.warn("Provider reference already exists: {}", request.getProviderReference());
                throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
            }
        }

        String txCode = "TX-OUT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment payment = Payment.builder()
                .transactionCode(txCode)
                .direction(PaymentDirection.OUT)
                .inventoryDocumentId(doc.getId())
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .providerReference(request.getProviderReference())
                .paidAt(Instant.now())
                .recordedBy(actorUserId)
                .notes(request.getNotes())
                .build();

        Payment saved = paymentRepository.save(payment);

        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            notificationAudit.get().recordCompletedRequest(actorUserId, "PAY_SUPPLIER", "PAYMENT", saved.getId(), requestKey, requestHash, "{\"transactionCode\":\"" + txCode + "\"}");
        }

        log.info("Recorded supplier payment id: {}, code: {}, amount: {} for document: {}", saved.getId(), txCode, request.getAmount(), doc.getId());
        return paymentMapper.toResponse(saved);
    }

    @Override
    public void requireHandoverAllowed(long actorUserId, long repairOrderId) {
        Invoice invoice = invoiceRepository.findByRepairOrderId(repairOrderId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return; // Handover allowed
        }

        BigDecimal currentPaid = paymentRepository.calculatePaidAmountForInvoice(invoice.getId());
        BigDecimal balance = invoice.getTotalAmount().subtract(currentPaid);

        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            return; // Completely paid
        }

        // Verify customer credit limits
        List<Map<String, Object>> custRows = jdbcTemplate.queryForList(
                "SELECT credit_limit, payment_term_days FROM customers WHERE id = ?",
                invoice.getCustomerId());

        if (custRows.isEmpty()) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        }

        BigDecimal creditLimit = (BigDecimal) custRows.get(0).get("credit_limit");
        if (creditLimit == null) creditLimit = BigDecimal.ZERO;

        BigDecimal totalCustomerDebt = invoiceBalanceViewRepository.calculateTotalOutstandingBalanceForCustomer(invoice.getCustomerId());

        if (totalCustomerDebt.compareTo(creditLimit) > 0) {
            log.warn("Customer {} total debt {} exceeds credit limit {}", invoice.getCustomerId(), totalCustomerDebt, creditLimit);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        // Check if customer has overdue invoices
        LocalDate today = LocalDate.now();
        List<InvoiceBalanceView> customerInvoices = invoiceBalanceViewRepository.findByCustomerId(invoice.getCustomerId());
        for (InvoiceBalanceView iv : customerInvoices) {
            if (iv.getDueDate() != null && iv.getDueDate().isBefore(today) && iv.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
                log.warn("Customer {} has overdue invoice {} with balance {}", invoice.getCustomerId(), iv.getInvoiceCode(), iv.getBalanceAmount());
                throw new BusinessException(ResponseCode.BAD_REQUEST);
            }
        }
    }

    @Override
    public Page<PaymentResponse> searchPayments(
            PaymentDirection direction,
            Long invoiceId,
            Long documentId,
            String query,
            Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return paymentRepository.searchPayments(direction, invoiceId, documentId, cleanQuery, pageable)
                .map(paymentMapper::toResponse);
    }

    @Override
    public List<PaymentResponse> getPaymentsForInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaidAtAsc(invoiceId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public List<PaymentResponse> getPaymentsForDocument(Long documentId) {
        return paymentRepository.findByInventoryDocumentIdOrderByPaidAtAsc(documentId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public Page<InvoiceBalanceResponse> getCustomerDebtLedger(
            Long customerId,
            Boolean hasBalance,
            String query,
            Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return invoiceBalanceViewRepository.searchInvoiceBalances(customerId, hasBalance, cleanQuery, pageable)
                .map(debtMapper::toInvoiceBalanceResponse);
    }

    @Override
    public Page<SupplierBalanceResponse> getSupplierDebtLedger(
            Boolean hasBalance,
            String query,
            Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return supplierBalanceViewRepository.searchSupplierBalances(hasBalance, cleanQuery, pageable)
                .map(debtMapper::toSupplierBalanceResponse);
    }

    private InvoiceResponse enrichInvoiceResponse(Invoice invoice) {
        InvoiceResponse resp = invoiceMapper.toResponse(invoice);
        BigDecimal paid = paymentRepository.calculatePaidAmountForInvoice(invoice.getId());
        if (paid == null) paid = BigDecimal.ZERO;
        BigDecimal total = invoice.getTotalAmount() != null ? invoice.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal bal = total.subtract(paid);
        if (bal.compareTo(BigDecimal.ZERO) < 0) bal = BigDecimal.ZERO;
        resp.setPaidAmount(paid);
        resp.setBalanceAmount(bal);
        return resp;
    }

    private String escapeJson(Object val) {
        if (val == null) return "";
        return val.toString().replace("\"", "\\\"");
    }
}

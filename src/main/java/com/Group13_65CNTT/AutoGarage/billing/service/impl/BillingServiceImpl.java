package com.Group13_65CNTT.AutoGarage.billing.service.impl;

import com.Group13_65CNTT.AutoGarage.common.util.RequestFingerprint;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceIssueRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.SupplierPaymentRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.HandoverReadinessResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceItemResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceReadinessResponse;
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
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ConflictException;
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
import org.springframework.data.domain.PageRequest;
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
import java.util.Locale;

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
    private final CustomerScope customerScope;
    private final CurrentUserProvider currentUser;
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
        String requestHash = RequestFingerprint.of("ISSUE_INVOICE", actorUserId, request.getRepairOrderId(), request.getDiscountAmount(), request.getTaxAmount(), request.getDueDate());
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for issuing invoice for order: {}", request.getRepairOrderId());
                return getInvoiceByOrderId(request.getRepairOrderId());
            }
        }

        Map<String, Object> order = requireIssueReady(actorUserId, request.getRepairOrderId(), true);
        Long customerId = ((Number) order.get("customer_id")).longValue();
        Long vehicleId = ((Number) order.get("vehicle_id")).longValue();

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
                "SELECT license_plate AS plate_number, vin, brand AS make, model, manufacture_year AS year, color, odometer AS current_odometer " +
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
                "SELECT rt.id AS task_id, ei.item_name AS service_name, ei.unit, rt.quantity, rt.unit_price, ei.unit_cost, " +
                "ei.discount_amount, ei.tax_amount, ei.warranty_days, ei.warranty_km, ei.warranty_terms " +
                "FROM repair_tasks rt " +
                "JOIN estimate_items ei ON rt.estimate_item_id = ei.id " +
                "WHERE rt.repair_order_id = ? AND rt.status = 'COMPLETED' AND rt.task_type = 'REPAIR' AND ei.approval_status = 'APPROVED'",
                request.getRepairOrderId());

        for (Map<String, Object> st : serviceTasks) {
            BigDecimal price = (BigDecimal) st.get("unit_price");
            BigDecimal cost = (BigDecimal) st.get("unit_cost");
            BigDecimal quantity = (BigDecimal) st.get("quantity");
            BigDecimal discount = (BigDecimal) st.getOrDefault("discount_amount", BigDecimal.ZERO);
            BigDecimal tax = (BigDecimal) st.getOrDefault("tax_amount", BigDecimal.ZERO);
            if (price == null) price = BigDecimal.ZERO;
            if (cost == null) cost = BigDecimal.ZERO;
            BigDecimal lineTotal = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP).subtract(discount).add(tax);
            subtotal = subtotal.add(lineTotal);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .lineNo(lineNo++)
                    .itemType(InvoiceItemType.SERVICE)
                    .repairTaskId(((Number) st.get("task_id")).longValue())
                    .itemName((String) st.get("service_name"))
                    .unit((String) st.get("unit"))
                    .quantity(quantity)
                    .discountAmount(discount)
                    .taxAmount(tax)
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
                "ei.item_name AS part_name, ei.unit, ei.quantity AS approved_quantity, ei.discount_amount, ei.tax_amount, " +
                "ei.warranty_days, ei.warranty_km, ei.warranty_terms " +
                "FROM repair_order_parts rop " +
                "JOIN estimate_items ei ON rop.estimate_item_id = ei.id " +
                "WHERE rop.repair_order_id = ? AND rop.quantity > 0",
                request.getRepairOrderId());

        for (Map<String, Object> op : orderParts) {
            BigDecimal qty = (BigDecimal) op.get("quantity");
            BigDecimal price = (BigDecimal) op.get("unit_price");
            BigDecimal cost = (BigDecimal) op.get("unit_cost");
            BigDecimal approvedQuantity = (BigDecimal) op.get("approved_quantity");
            BigDecimal discount = ((BigDecimal) op.get("discount_amount")).multiply(qty).divide(approvedQuantity, 2, RoundingMode.HALF_UP);
            BigDecimal tax = ((BigDecimal) op.get("tax_amount")).multiply(qty).divide(approvedQuantity, 2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = qty.multiply(price).setScale(2, RoundingMode.HALF_UP).subtract(discount).add(tax);
            subtotal = subtotal.add(lineTotal);

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .lineNo(lineNo++)
                    .itemType(InvoiceItemType.PART)
                    .repairOrderPartId(((Number) op.get("rop_id")).longValue())
                    .itemName((String) op.get("part_name"))
                    .unit((String) op.get("unit"))
                    .quantity(qty)
                    .discountAmount(discount)
                    .taxAmount(tax)
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
            throw new ConflictException(ResponseCode.INVOICE_BLOCKED,
                    "Phiếu không có hạng mục dịch vụ hoặc phụ tùng nào để tính tiền.");
        }

        if (invoice.getDiscountAmount().compareTo(subtotal) > 0) {
            throw new BusinessRuleException("Số tiền giảm giá không được lớn hơn tạm tính của hóa đơn.");
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

    /**
     * Rules shared by issuing an invoice and its read-only readiness check. Returns the order row;
     * {@code lockOrder} takes a row lock and is only wanted when the invoice is actually issued.
     */
    private Map<String, Object> requireIssueReady(long actorUserId, Long repairOrderId, boolean lockOrder) {
        if (invoiceRepository.existsByRepairOrderIdAndStatusNot(repairOrderId, InvoiceStatus.VOID)) {
            log.warn("Invoice already issued for repair order {}", repairOrderId);
            throw new ConflictException("Phiếu đã có hóa đơn còn hiệu lực.");
        }

        // Check readiness via I03 contract if available, and also verify DB state directly
        if (repairWorkflow.isPresent()) {
            repairWorkflow.get().requireInvoiceReady(actorUserId, repairOrderId);
        }

        // Direct DB verification of order readiness
        List<Map<String, Object>> orderRows = jdbcTemplate.queryForList(
                "SELECT ro.id, ro.customer_id, ro.vehicle_id, ro.latest_qc_check_id, ro.status " +
                "FROM repair_orders ro WHERE ro.id = ?" + (lockOrder ? " FOR UPDATE" : ""),
                repairOrderId);
        if (orderRows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + repairOrderId);
        }
        Map<String, Object> order = orderRows.get(0);

        Object latestQcObj = order.get("latest_qc_check_id");
        if (latestQcObj == null) {
            log.warn("Cannot issue invoice: repair order {} has not passed QC", repairOrderId);
            throw new ConflictException(ResponseCode.INVOICE_BLOCKED,
                    "Phiếu chưa có kết quả QC. Hãy hoàn tất kiểm tra chất lượng trước khi lập hóa đơn.");
        }
        Long latestQcId = ((Number) latestQcObj).longValue();
        List<Map<String, Object>> qcRows = jdbcTemplate.queryForList(
                "SELECT check_type, result FROM vehicle_checks WHERE id = ?", latestQcId);
        if (qcRows.isEmpty() || !"QC".equals(qcRows.get(0).get("check_type")) || !"PASSED".equals(qcRows.get(0).get("result"))) {
            log.warn("Cannot issue invoice: QC check {} is not PASSED", latestQcId);
            throw new ConflictException(ResponseCode.INVOICE_BLOCKED,
                    "Biên bản QC gần nhất chưa đạt. Hãy kiểm tra chất lượng lại trước khi lập hóa đơn.");
        }

        // Verify no active labor timers
        Integer openTimers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM labor_sessions ls " +
                "JOIN task_assignments ta ON ls.assignment_id = ta.id " +
                "JOIN repair_tasks rt ON ta.task_id = rt.id " +
                "WHERE rt.repair_order_id = ? AND ls.ends_at IS NULL",
                Integer.class, repairOrderId);
        if (openTimers != null && openTimers > 0) {
            log.warn("Cannot issue invoice: repair order {} has {} active labor timers", repairOrderId, openTimers);
            throw new ConflictException(ResponseCode.INVOICE_BLOCKED,
                    "Còn kỹ thuật viên đang bấm giờ làm việc. Hãy dừng bấm giờ trước khi lập hóa đơn.");
        }
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceReadinessResponse getInvoiceReadiness(Long repairOrderId) {
        try {
            requireIssueReady(currentUser.userId(), repairOrderId, false);
            Integer billable = jdbcTemplate.queryForObject(
                    "SELECT (SELECT COUNT(*) FROM repair_tasks rt JOIN estimate_items ei ON rt.estimate_item_id = ei.id " +
                    "WHERE rt.repair_order_id = ? AND rt.status = 'COMPLETED' AND rt.task_type = 'REPAIR' " +
                    "AND ei.approval_status = 'APPROVED') + " +
                    "(SELECT COUNT(*) FROM repair_order_parts WHERE repair_order_id = ? AND quantity > 0)",
                    Integer.class, repairOrderId, repairOrderId);
            if (billable == null || billable == 0) {
                throw new ConflictException(ResponseCode.INVOICE_BLOCKED,
                        "Phiếu không có hạng mục dịch vụ hoặc phụ tùng nào để tính tiền.");
            }
            return new InvoiceReadinessResponse(true, null);
        } catch (BusinessException blocked) {
            return new InvoiceReadinessResponse(false, blocked.getMessage());
        }
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        requireOwnInvoiceForCustomer(invoice);
        return enrichInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByCode(String code) {
        Invoice invoice = invoiceRepository.findByInvoiceCode(code.trim())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        requireOwnInvoiceForCustomer(invoice);
        return enrichInvoiceResponse(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceByOrderId(Long repairOrderId) {
        Invoice invoice = invoiceRepository.findActiveByRepairOrderId(repairOrderId)
                .or(() -> invoiceRepository.findFirstByRepairOrderIdOrderByIdDesc(repairOrderId))
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        requireOwnInvoiceForCustomer(invoice);
        return enrichInvoiceResponse(invoice);
    }

    private void requireOwnInvoiceForCustomer(Invoice invoice) {
        if (currentUser.role() == Role.CUSTOMER) {
            customerScope.requireResource(AccessResource.INVOICE, invoice.getId());
        }
    }

    @Override
    public Page<InvoiceResponse> searchInvoices(
            InvoiceStatus status,
            Long customerId,
            Long orderId,
            String query,
            Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        Long scopedCustomerId = currentUser.role() == Role.CUSTOMER
                ? customerScope.customerIdForQuery(customerId)
                : customerId;
        return invoiceRepository.searchInvoices(status, scopedCustomerId, orderId, cleanQuery, pageable)
                .map(this::enrichInvoiceResponse);
    }

    @Override
    @Transactional
    @com.Group13_65CNTT.AutoGarage.audit.annotation.Audited(
            action = com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants.ACTION_INVOICE_WARRANTY_SET,
            entity = com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants.ENTITY_INVOICE,
            idArgument = 1)
    public InvoiceResponse updateInvoiceItemWarranty(
            long actorUserId, Long invoiceId, Long itemId,
            com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceItemWarrantyRequest request) {
        Invoice invoice = invoiceRepository.findByIdWithLock(invoiceId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new ConflictException(ResponseCode.WARRANTY_BLOCKED,
                    "Hóa đơn đã bị hủy, không thể thêm bảo hành.");
        }
        List<String> orderStatus = jdbcTemplate.queryForList(
                "SELECT status FROM repair_orders WHERE id = ?", String.class, invoice.getRepairOrderId());
        if (orderStatus.isEmpty()) {
            throw new ConflictException(ResponseCode.WARRANTY_BLOCKED,
                    "Không tìm thấy phiếu sửa chữa của hóa đơn, không thể thêm bảo hành.");
        }
        if ("CLOSED".equals(orderStatus.get(0))) {
            throw new ConflictException(ResponseCode.WARRANTY_BLOCKED,
                    "Phiếu đã bàn giao, không thể thêm bảo hành. Bảo hành chỉ thêm được trước khi bàn giao xe.");
        }
        InvoiceItem item = invoiceItemRepository.findById(itemId)
                .filter(found -> found.getInvoice() != null && Objects.equals(found.getInvoice().getId(), invoiceId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dòng hóa đơn với ID: " + itemId));
        Integer books = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM warranties WHERE invoice_item_id = ?", Integer.class, itemId);
        if (books != null && books > 0) {
            throw new ConflictException(ResponseCode.WARRANTY_BLOCKED,
                    "Dòng \"" + item.getItemName() + "\" đã có sổ bảo hành, không thể sửa chính sách. "
                            + "Hãy điều chỉnh trong màn Bảo hành.");
        }
        String terms = request.getWarrantyTerms() == null ? null : request.getWarrantyTerms().trim();
        item.setWarrantyDays(request.getWarrantyDays());
        item.setWarrantyKm(request.getWarrantyKm());
        item.setWarrantyTerms(terms == null || terms.isEmpty() ? null : terms);
        invoiceItemRepository.save(item);
        log.info("Invoice {} item {} warranty set to {} days / {} km by {}", invoiceId, itemId,
                request.getWarrantyDays(), request.getWarrantyKm(), actorUserId);
        return enrichInvoiceResponse(invoice);
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
            throw new BusinessException(ResponseCode.BAD_REQUEST, "Hóa đơn đã thu tiền, không thể hủy");
        }

        // The order goes back to "completed" so it can be invoiced again; once the car is
        // handed over the invoice backs the warranty books and must stay.
        List<Map<String, Object>> orderRows = jdbcTemplate.queryForList(
                "SELECT status FROM repair_orders WHERE id = ? FOR UPDATE", invoice.getRepairOrderId());
        String orderStatus = orderRows.isEmpty() ? null : (String) orderRows.get(0).get("status");
        if (!"INVOICED".equals(orderStatus)) {
            throw new BusinessException(ResponseCode.BAD_REQUEST,
                    "Chỉ hủy được hóa đơn khi xe chưa bàn giao (phiếu đang ở trạng thái đã lập hóa đơn)");
        }

        invoice.setStatus(InvoiceStatus.VOID);
        invoice.setVoidReason(voidReason != null ? voidReason.trim() : "Cancelled");
        invoice.setVoidedBy(actorUserId);
        invoice.setVoidedAt(Instant.now());
        invoiceRepository.save(invoice);
        jdbcTemplate.update("UPDATE repair_orders SET status = 'COMPLETED' WHERE id = ? AND status = 'INVOICED'",
                invoice.getRepairOrderId());
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
        String requestHash = RequestFingerprint.of("RECEIVE_PAYMENT", actorUserId, request.getInvoiceId(), request.getAmount(), request.getPaymentMethod(), request.getProviderReference(), request.getNotes());
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for receiving payment key: {}", requestKey);
                Payment existing = paymentRepository.findByTransactionCode(RequestFingerprint.resultField(completed, "transactionCode"))
                        .orElseThrow(() -> new BusinessException(ResponseCode.DUPLICATE_RESOURCE));
                return paymentMapper.toResponse(existing);
            }
        }

        Invoice invoice = invoiceRepository.findByIdWithLock(request.getInvoiceId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED, "Hóa đơn đã bị hủy, không thể thu tiền.");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED, "Hóa đơn đã được thu đủ.");
        }

        BigDecimal currentPaid = paymentRepository.calculatePaidAmountForInvoice(invoice.getId());
        BigDecimal balance = invoice.getTotalAmount().subtract(currentPaid);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Số tiền thu phải lớn hơn 0.");
        }
        if (request.getAmount().compareTo(balance) > 0) {
            log.warn("Payment amount {} exceeds invoice balance {}", request.getAmount(), balance);
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED,
                    "Số tiền thu vượt số còn nợ (" + formatMoney(balance) + " đ).");
        }

        requireFreeProviderReference(request.getProviderReference());
        String txCode = resolveTransactionCode(request.getTransactionCode(), PaymentDirection.IN);
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
    public long recordPayrollDisbursement(long actorUserId, long payrollPeriodId, BigDecimal amount, String notes) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }
        Payment payment = Payment.builder()
                .transactionCode("TX-PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .direction(PaymentDirection.OUT)
                .payrollPeriodId(payrollPeriodId)
                .amount(amount)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paidAt(Instant.now())
                .recordedBy(actorUserId)
                .notes(notes)
                .build();
        return paymentRepository.save(payment).getId();
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
        String requestHash = RequestFingerprint.of("PAY_SUPPLIER", actorUserId, request.getInventoryDocumentId(), request.getAmount(), request.getPaymentMethod(), request.getProviderReference(), request.getNotes());
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for paying supplier key: {}", requestKey);
                Payment existing = paymentRepository.findByTransactionCode(RequestFingerprint.resultField(completed, "transactionCode"))
                        .orElseThrow(() -> new BusinessException(ResponseCode.DUPLICATE_RESOURCE));
                return paymentMapper.toResponse(existing);
            }
        }

        InventoryDocument doc = documentRepository.findByIdWithLock(request.getInventoryDocumentId())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (doc.getDocumentType() != DocumentType.IN || doc.getStatus() != DocumentStatus.POSTED || doc.getSupplier() == null) {
            log.warn("Cannot pay document {}: type={}, status={}, supplier={}",
                    doc.getId(), doc.getDocumentType(), doc.getStatus(), doc.getSupplier());
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED,
                    "Chỉ chi trả được cho phiếu nhập kho của nhà cung cấp đã ghi sổ.");
        }

        BigDecimal currentPaid = paymentRepository.calculatePaidAmountForDocument(doc.getId());
        BigDecimal balance = doc.getTotalAmount().subtract(currentPaid);

        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Số tiền chi phải lớn hơn 0.");
        }
        if (request.getAmount().compareTo(balance) > 0) {
            log.warn("Payment amount {} exceeds document balance {}", request.getAmount(), balance);
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED,
                    "Số tiền chi vượt số còn nợ nhà cung cấp (" + formatMoney(balance) + " đ).");
        }

        requireFreeProviderReference(request.getProviderReference());
        String txCode = resolveTransactionCode(request.getTransactionCode(), PaymentDirection.OUT);
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
    @Transactional(readOnly = true)
    public String suggestTransactionCode(PaymentDirection direction) {
        return generateTransactionCode(direction);
    }

    /** Uses the typed code when given (normalised and checked), otherwise generates the next one of the day. */
    private String resolveTransactionCode(String requested, PaymentDirection direction) {
        lockPaymentCodes();
        String code = requested == null ? "" : requested.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            return generateTransactionCode(direction);
        }
        if (code.length() > PaymentCodePolicy.MAX_LENGTH || !code.matches(PaymentCodePolicy.ALLOWED_PATTERN)) {
            throw new BusinessRuleException("Mã giao dịch chỉ gồm chữ, số và các ký tự - _ . (tối đa "
                    + PaymentCodePolicy.MAX_LENGTH + " ký tự).");
        }
        if (paymentRepository.existsByTransactionCode(code)) {
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED,
                    "Mã giao dịch " + code + " đã tồn tại. Hãy nhập mã khác hoặc bấm Sinh lại.");
        }
        return code;
    }

    /**
     * Serialises code allocation: the row lock is held until the surrounding transaction commits or rolls back,
     * so two concurrent payments never pick or check against the same not-yet-committed code.
     */
    private void lockPaymentCodes() {
        jdbcTemplate.queryForObject("SELECT id FROM payment_code_lock WHERE id = 1 FOR UPDATE", Integer.class);
    }

    private String generateTransactionCode(PaymentDirection direction) {
        String dayPrefix = PaymentCodePolicy.dayPrefix(direction, PaymentCodePolicy.today());
        long next = paymentRepository
                .findCodesStartingWith(dayPrefix, PageRequest.of(0, PaymentCodePolicy.NUMBERING_LOOKBACK))
                .stream()
                .map(code -> code.substring(dayPrefix.length()))
                .filter(tail -> !tail.isEmpty() && tail.length() <= 9 && tail.chars().allMatch(Character::isDigit))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(0L) + 1;
        for (int attempt = 0; attempt < PaymentCodePolicy.GENERATION_ATTEMPTS; attempt++) {
            String candidate = PaymentCodePolicy.format(dayPrefix, next + attempt);
            if (!paymentRepository.existsByTransactionCode(candidate)) {
                return candidate;
            }
        }
        throw new ConflictException(ResponseCode.PAYMENT_BLOCKED,
                "Không sinh được mã giao dịch mới, hãy thử lại sau ít giây.");
    }

    private void requireFreeProviderReference(String providerReference) {
        if (providerReference == null || providerReference.isBlank()) {
            return;
        }
        String reference = providerReference.trim();
        if (paymentRepository.existsByProviderReference(reference)) {
            log.warn("Provider reference already exists: {}", reference);
            throw new ConflictException(ResponseCode.PAYMENT_BLOCKED,
                    "Mã tham chiếu " + reference + " đã được dùng cho giao dịch khác.");
        }
    }

    private static String formatMoney(BigDecimal amount) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f", amount);
    }

    @Override
    @Transactional
    public void requireHandoverAllowed(long actorUserId, long repairOrderId) {
        HandoverReadinessResponse result = evaluateHandover(repairOrderId, true);
        if (!result.allowed()) {
            log.warn("Handover of order {} blocked: {}", repairOrderId, result.reasons());
            throw new ConflictException(ResponseCode.HANDOVER_BLOCKED, String.join(" ", result.reasons()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public HandoverReadinessResponse getHandoverReadiness(Long repairOrderId) {
        return evaluateHandover(repairOrderId, false);
    }

    /**
     * Credit rule: a customer with no credit limit must pay the invoice in full; with a limit the debt
     * left after this invoice may not exceed it, so at least debt − limit must be collected. Customers with an
     * overdue invoice are always blocked.
     */
    private HandoverReadinessResponse evaluateHandover(long repairOrderId, boolean lock) {
        Optional<Invoice> found = lock
                ? invoiceRepository.findActiveByRepairOrderIdWithLock(repairOrderId)
                : invoiceRepository.findActiveByRepairOrderId(repairOrderId);
        if (found.isEmpty()) {
            return blockedHandover(null, "Phiếu chưa có hóa đơn còn hiệu lực. Hãy lập hóa đơn trước khi bàn giao.");
        }
        Invoice invoice = found.get();
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            return blockedHandover(invoice.getId(),
                    "Hóa đơn của phiếu đã bị hủy. Hãy lập lại hóa đơn trước khi bàn giao.");
        }

        BigDecimal paid = invoice.getStatus() == InvoiceStatus.PAID
                ? invoice.getTotalAmount()
                : paymentRepository.calculatePaidAmountForInvoice(invoice.getId());
        BigDecimal balance = invoice.getTotalAmount().subtract(paid).max(BigDecimal.ZERO);
        if (balance.signum() == 0) {
            // Fully paid: nothing owed on this invoice, so the credit limit is irrelevant.
            return new HandoverReadinessResponse(true, List.of(), invoice.getId(), balance, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        }

        List<Map<String, Object>> custRows = jdbcTemplate.queryForList(
                "SELECT credit_limit FROM customers WHERE id = ?" + (lock ? " FOR UPDATE" : ""),
                invoice.getCustomerId());
        if (custRows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy khách hàng của hóa đơn");
        }
        BigDecimal creditLimit = (BigDecimal) custRows.get(0).get("credit_limit");
        if (creditLimit == null) {
            creditLimit = BigDecimal.ZERO;
        }
        BigDecimal debt = invoiceBalanceViewRepository.calculateTotalOutstandingBalanceForCustomer(invoice.getCustomerId());
        if (debt == null) {
            debt = BigDecimal.ZERO;
        }

        List<String> reasons = new ArrayList<>();
        List<String> overdue = new ArrayList<>();
        // balance > 0 here: the fully-paid case returned above.
        BigDecimal minimumPayment = debt.subtract(creditLimit).max(BigDecimal.ZERO).min(balance);
        if (debt.compareTo(creditLimit) > 0) {
            reasons.add(creditLimit.signum() == 0
                    ? "Khách chưa có hạn mức nợ nên phải thanh toán đủ. Công nợ hiện tại " + formatMoney(debt)
                            + " đ, cần thu tối thiểu " + formatMoney(minimumPayment) + " đ trước khi bàn giao."
                    : "Công nợ của khách là " + formatMoney(debt) + " đ, vượt hạn mức " + formatMoney(creditLimit)
                            + " đ. Cần thu tối thiểu " + formatMoney(minimumPayment) + " đ trước khi bàn giao.");
        }
        LocalDate today = LocalDate.now();
        for (InvoiceBalanceView iv : invoiceBalanceViewRepository.findByCustomerId(invoice.getCustomerId())) {
            if (iv.getDueDate() != null && iv.getDueDate().isBefore(today)
                    && iv.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
                overdue.add(Objects.toString(iv.getInvoiceCode(), "#" + iv.getInvoiceId()));
            }
        }
        if (!overdue.isEmpty()) {
            reasons.add("Khách có hóa đơn quá hạn chưa thanh toán (" + String.join(", ", overdue)
                    + "). Hãy thu nợ quá hạn trước khi bàn giao.");
        }
        return new HandoverReadinessResponse(reasons.isEmpty(), List.copyOf(reasons), invoice.getId(), balance,
                debt, creditLimit, minimumPayment, List.copyOf(overdue));
    }

    private static HandoverReadinessResponse blockedHandover(Long invoiceId, String reason) {
        return new HandoverReadinessResponse(false, List.of(reason), invoiceId, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, List.of());
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
        String encoded = tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(val.toString());
        return encoded.substring(1, encoded.length() - 1);
    }
}

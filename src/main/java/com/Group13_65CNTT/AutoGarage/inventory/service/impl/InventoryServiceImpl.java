package com.Group13_65CNTT.AutoGarage.inventory.service.impl;

import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentItemRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentItemResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryTransactionResponse;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.StockReconciliationResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocument;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryDocumentItem;
import com.Group13_65CNTT.AutoGarage.inventory.entity.InventoryTransaction;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Part;
import com.Group13_65CNTT.AutoGarage.inventory.entity.RepairOrderPart;
import com.Group13_65CNTT.AutoGarage.inventory.entity.Supplier;
import com.Group13_65CNTT.AutoGarage.inventory.entity.TransactionType;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.InventoryDocumentMapper;
import com.Group13_65CNTT.AutoGarage.inventory.mapper.InventoryTransactionMapper;
import com.Group13_65CNTT.AutoGarage.inventory.repository.InventoryDocumentItemRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.InventoryDocumentRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.InventoryTransactionRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.PartRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.RepairOrderPartRepository;
import com.Group13_65CNTT.AutoGarage.inventory.repository.SupplierRepository;
import com.Group13_65CNTT.AutoGarage.inventory.service.InventoryService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {
    private final InventoryDocumentRepository documentRepository;
    private final InventoryDocumentItemRepository itemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final PartRepository partRepository;
    private final SupplierRepository supplierRepository;
    private final RepairOrderPartRepository repairOrderPartRepository;
    private final InventoryDocumentMapper documentMapper;
    private final InventoryTransactionMapper transactionMapper;
    private final JdbcTemplate jdbcTemplate;
    private final Optional<NotificationAudit> notificationAudit;

    @Override
    @Transactional
    public InventoryDocumentResponse createDocument(long actorUserId, InventoryDocumentCreateRequest request) {
        validateDocumentTypeConstraints(request);

        String code = generateDocumentCode(request.getDocumentType());
        InventoryDocument document = InventoryDocument.builder()
                .documentCode(code)
                .documentType(request.getDocumentType())
                .status(DocumentStatus.DRAFT)
                .repairOrderId(request.getRepairOrderId())
                .supplierInvoiceNumber(request.getSupplierInvoiceNumber())
                .supplierInvoiceDate(request.getSupplierInvoiceDate())
                .dueDate(request.getDueDate())
                .reason(request.getReason())
                .taxAmount(request.getTaxAmount() != null ? request.getTaxAmount() : BigDecimal.ZERO)
                .createdBy(actorUserId)
                .build();

        if (request.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
            if (request.getSupplierInvoiceNumber() != null && !request.getSupplierInvoiceNumber().isBlank()) {
                if (documentRepository.existsBySupplierIdAndSupplierInvoiceNumber(supplier.getId(), request.getSupplierInvoiceNumber().trim())) {
                    log.warn("Supplier invoice number already exists for supplier: {}", request.getSupplierInvoiceNumber());
                    throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
                }
            }
            document.setSupplier(supplier);
        }

        if (request.getSourceDocumentId() != null) {
            InventoryDocument source = documentRepository.findById(request.getSourceDocumentId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
            document.setSourceDocument(source);
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<InventoryDocumentItem> items = new ArrayList<>();
        int lineNo = 1;

        for (InventoryDocumentItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST);
            }
            if (request.getDocumentType() != DocumentType.ADJUSTMENT && itemReq.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(ResponseCode.BAD_REQUEST);
            }

            Part part = partRepository.findById(itemReq.getPartId())
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

            BigDecimal lineTotal = itemReq.getQuantity().abs()
                    .multiply(itemReq.getUnitCost())
                    .setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineTotal);

            InventoryDocumentItem item = InventoryDocumentItem.builder()
                    .document(document)
                    .lineNo(lineNo++)
                    .part(part)
                    .estimateItemId(itemReq.getEstimateItemId())
                    .quantity(itemReq.getQuantity())
                    .unitCost(itemReq.getUnitCost())
                    .lineTotal(lineTotal)
                    .notes(itemReq.getNotes())
                    .build();

            if (itemReq.getRepairOrderPartId() != null) {
                RepairOrderPart rop = repairOrderPartRepository.findById(itemReq.getRepairOrderPartId())
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
                item.setRepairOrderPart(rop);
            }

            if (itemReq.getSourceItemId() != null) {
                InventoryDocumentItem sourceItem = itemRepository.findById(itemReq.getSourceItemId())
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
                item.setSourceItem(sourceItem);
            }

            items.add(item);
        }

        document.setSubtotal(subtotal);
        document.setTotalAmount(subtotal.add(document.getTaxAmount()));
        document.setItems(items);

        InventoryDocument saved = documentRepository.save(document);
        log.info("Created inventory document id: {}, code: {}, type: {}", saved.getId(), saved.getDocumentCode(), saved.getDocumentType());
        return documentMapper.toResponse(saved);
    }

    @Override
    public InventoryDocumentResponse getDocumentById(Long id) {
        InventoryDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return documentMapper.toResponse(document);
    }

    @Override
    public InventoryDocumentResponse getDocumentByCode(String code) {
        InventoryDocument document = documentRepository.findByDocumentCode(code.trim())
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        return documentMapper.toResponse(document);
    }

    @Override
    public Page<InventoryDocumentResponse> searchDocuments(
            DocumentType type,
            DocumentStatus status,
            Long supplierId,
            Long orderId,
            String query,
            Pageable pageable) {
        String cleanQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return documentRepository.searchDocuments(type, status, supplierId, orderId, cleanQuery, pageable)
                .map(documentMapper::toResponse);
    }

    @Override
    @Transactional
    public void cancelDocument(long actorUserId, Long documentId) {
        InventoryDocument document = documentRepository.findByIdWithLock(documentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            log.warn("Cannot cancel document {} with status {}", documentId, document.getStatus());
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        document.setStatus(DocumentStatus.CANCELLED);
        documentRepository.save(document);
        log.info("Document {} cancelled by actor {}", documentId, actorUserId);
    }

    @Override
    @Transactional
    public void post(long actorUserId, long documentId, String requestKey) {
        String requestHash = "POST_DOCUMENT:" + documentId;
        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            String completed = notificationAudit.get().findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                log.info("Idempotent replay for posting document key: {}", requestKey);
                return;
            }
        }

        InventoryDocument document = documentRepository.findByIdWithLock(documentId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        if (document.getStatus() == DocumentStatus.POSTED) {
            log.warn("Document {} already posted", documentId);
            throw new BusinessException(ResponseCode.DUPLICATE_RESOURCE);
        }
        if (document.getStatus() == DocumentStatus.CANCELLED) {
            log.warn("Cannot post cancelled document {}", documentId);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        List<InventoryDocumentItem> items = itemRepository.findByDocumentIdOrderByLineNoAsc(documentId);
        if (items.isEmpty()) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        // Lock all parts in ascending ID order to prevent deadlocks
        List<Long> partIds = items.stream()
                .map(i -> i.getPart().getId())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Part> lockedParts = partRepository.findAllByIdInOrderByIdAscWithLock(partIds).stream()
                .collect(Collectors.toMap(Part::getId, Function.identity()));

        Instant now = Instant.now();

        for (InventoryDocumentItem item : items) {
            Part part = lockedParts.get(item.getPart().getId());
            if (part == null) {
                throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
            }

            BigDecimal qty = item.getQuantity();
            BigDecimal cost = item.getUnitCost();

            switch (document.getDocumentType()) {
                case IN -> {
                    BigDecimal oldStock = part.getStockQuantity();
                    BigDecimal newStock = oldStock.add(qty);
                    // Weighted average cost update
                    BigDecimal currentVal = oldStock.multiply(part.getPurchasePrice());
                    BigDecimal addedVal = qty.multiply(cost);
                    BigDecimal newCost = currentVal.add(addedVal).divide(newStock, 2, RoundingMode.HALF_UP);
                    part.setStockQuantity(newStock);
                    part.setPurchasePrice(newCost);

                    recordTransaction(item, part, TransactionType.IN, qty, newStock, cost, actorUserId, now);
                }
                case OPENING -> {
                    BigDecimal newStock = part.getStockQuantity().add(qty);
                    part.setStockQuantity(newStock);
                    if (part.getPurchasePrice().compareTo(BigDecimal.ZERO) == 0) {
                        part.setPurchasePrice(cost);
                    }
                    recordTransaction(item, part, TransactionType.OPENING, qty, newStock, cost, actorUserId, now);
                }
                case OUT -> {
                    if (part.getStockQuantity().compareTo(qty) < 0) {
                        log.warn("Insufficient stock for part {}: stock={}, requested={}", part.getId(), part.getStockQuantity(), qty);
                        throw new BusinessException(ResponseCode.BAD_REQUEST);
                    }
                    BigDecimal newStock = part.getStockQuantity().subtract(qty);
                    // Deduct reserved quantity up to qty
                    BigDecimal deductReserved = part.getReservedQuantity().min(qty);
                    part.setStockQuantity(newStock);
                    part.setReservedQuantity(part.getReservedQuantity().subtract(deductReserved));

                    recordTransaction(item, part, TransactionType.OUT, qty.negate(), newStock, cost, actorUserId, now);

                    // Update repair_order_parts
                    updateRepairOrderPartOnIssue(document.getRepairOrderId(), item, part, qty, cost);
                }
                case RETURN -> {
                    if (item.getSourceItem() == null) {
                        throw new BusinessException(ResponseCode.BAD_REQUEST);
                    }
                    BigDecimal originalQty = item.getSourceItem().getQuantity();
                    BigDecimal alreadyReturned = itemRepository.getTotalReturnedQuantityForSourceItem(item.getSourceItem().getId());
                    if (alreadyReturned.add(qty).compareTo(originalQty) > 0) {
                        log.warn("Return quantity {} exceeds available returnable quantity {}", qty, originalQty.subtract(alreadyReturned));
                        throw new BusinessException(ResponseCode.BAD_REQUEST);
                    }

                    BigDecimal newStock = part.getStockQuantity().add(qty);
                    part.setStockQuantity(newStock);

                    recordTransaction(item, part, TransactionType.RETURN, qty, newStock, cost, actorUserId, now);

                    // Deduct net quantity in repair_order_parts, keeping row with 0 if fully returned
                    updateRepairOrderPartOnReturn(document.getRepairOrderId(), item, part, qty);
                }
                case ADJUSTMENT -> {
                    BigDecimal newStock = part.getStockQuantity().add(qty); // qty can be negative or positive
                    if (newStock.compareTo(BigDecimal.ZERO) < 0 || newStock.compareTo(part.getReservedQuantity()) < 0) {
                        log.warn("Adjustment result violates stock/reserved invariant for part {}: newStock={}, reserved={}",
                                part.getId(), newStock, part.getReservedQuantity());
                        throw new BusinessException(ResponseCode.BAD_REQUEST);
                    }
                    part.setStockQuantity(newStock);

                    recordTransaction(item, part, TransactionType.ADJUSTMENT, qty, newStock, cost, actorUserId, now);
                }
            }
        }

        partRepository.saveAll(lockedParts.values());

        document.setStatus(DocumentStatus.POSTED);
        document.setPostedBy(actorUserId);
        document.setPostedAt(now);
        documentRepository.save(document);

        if (requestKey != null && !requestKey.isBlank() && notificationAudit.isPresent()) {
            notificationAudit.get().recordCompletedRequest(actorUserId, "POST_DOCUMENT", "INVENTORY_DOCUMENT", document.getId(), requestKey, requestHash, "{\"status\":\"POSTED\"}");
        }

        log.info("Posted inventory document id: {}, code: {} by actor: {}", document.getId(), document.getDocumentCode(), actorUserId);
    }

    private void updateRepairOrderPartOnIssue(Long repairOrderId, InventoryDocumentItem item, Part part, BigDecimal qty, BigDecimal cost) {
        if (repairOrderId == null) return;
        Optional<RepairOrderPart> existing = repairOrderPartRepository.findByRepairOrderIdAndPartId(repairOrderId, part.getId());
        if (existing.isPresent()) {
            RepairOrderPart rop = existing.get();
            rop.setQuantity(rop.getQuantity().add(qty));
            rop.setUnitCost(cost);
            repairOrderPartRepository.save(rop);
        } else {
            RepairOrderPart rop = RepairOrderPart.builder()
                    .repairOrderId(repairOrderId)
                    .estimateItemId(item.getEstimateItemId() != null ? item.getEstimateItemId() : 0L)
                    .part(part)
                    .quantity(qty)
                    .unitPrice(part.getSellingPrice())
                    .unitCost(cost)
                    .build();
            repairOrderPartRepository.save(rop);
        }
    }

    private void updateRepairOrderPartOnReturn(Long repairOrderId, InventoryDocumentItem item, Part part, BigDecimal returnQty) {
        if (repairOrderId == null) return;
        Optional<RepairOrderPart> existing = repairOrderPartRepository.findByRepairOrderIdAndPartId(repairOrderId, part.getId());
        if (existing.isPresent()) {
            RepairOrderPart rop = existing.get();
            BigDecimal newQty = rop.getQuantity().subtract(returnQty);
            if (newQty.compareTo(BigDecimal.ZERO) < 0) newQty = BigDecimal.ZERO;
            rop.setQuantity(newQty); // preserve row with quantity = 0
            repairOrderPartRepository.save(rop);
        }
    }

    private void recordTransaction(
            InventoryDocumentItem item,
            Part part,
            TransactionType type,
            BigDecimal change,
            BigDecimal stockAfter,
            BigDecimal unitCost,
            long actorUserId,
            Instant now) {
        InventoryTransaction tx = InventoryTransaction.builder()
                .documentItem(item)
                .part(part)
                .transactionType(type)
                .quantityChange(change)
                .stockAfter(stockAfter)
                .unitCost(unitCost)
                .createdBy(actorUserId)
                .createdAt(now)
                .build();
        transactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void reserve(long actorUserId, long estimateItemId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, part_id, approval_status, item_type, reserved_quantity FROM estimate_items WHERE id = ?",
                estimateItemId);

        if (rows.isEmpty()) {
            throw new BusinessException(ResponseCode.RESOURCE_NOT_FOUND);
        }

        Map<String, Object> row = rows.get(0);
        String status = (String) row.get("approval_status");
        String itemType = (String) row.get("item_type");
        Long partId = ((Number) row.get("part_id")).longValue();

        if (!"APPROVED".equals(status) || !"PART".equals(itemType)) {
            log.warn("Cannot reserve item {}: status={}, itemType={}", estimateItemId, status, itemType);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        Part part = partRepository.findByIdWithLock(partId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        BigDecimal available = part.getAvailableQuantity();
        if (available.compareTo(quantity) < 0) {
            log.warn("Insufficient available stock to reserve for part {}: available={}, requested={}",
                    partId, available, quantity);
            throw new BusinessException(ResponseCode.BAD_REQUEST);
        }

        part.setReservedQuantity(part.getReservedQuantity().add(quantity));
        partRepository.save(part);

        jdbcTemplate.update(
                "UPDATE estimate_items SET reserved_quantity = reserved_quantity + ? WHERE id = ?",
                quantity, estimateItemId);

        log.info("Reserved quantity {} for part {} on estimate item {}", quantity, partId, estimateItemId);
    }

    @Override
    @Transactional
    public void releaseForOrder(long actorUserId, long repairOrderId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ei.id, ei.part_id, ei.reserved_quantity " +
                "FROM estimate_items ei " +
                "JOIN estimates e ON ei.estimate_id = e.id " +
                "WHERE e.repair_order_id = ? AND ei.item_type = 'PART' AND ei.reserved_quantity > 0 " +
                "ORDER BY ei.part_id ASC",
                repairOrderId);

        if (rows.isEmpty()) return;

        List<Long> partIds = rows.stream()
                .map(r -> ((Number) r.get("part_id")).longValue())
                .distinct()
                .sorted()
                .toList();

        Map<Long, Part> lockedParts = partRepository.findAllByIdInOrderByIdAscWithLock(partIds).stream()
                .collect(Collectors.toMap(Part::getId, Function.identity()));

        for (Map<String, Object> row : rows) {
            Long estimateItemId = ((Number) row.get("id")).longValue();
            Long partId = ((Number) row.get("part_id")).longValue();
            BigDecimal reserved = (BigDecimal) row.get("reserved_quantity");

            Part part = lockedParts.get(partId);
            if (part != null) {
                BigDecimal newReserved = part.getReservedQuantity().subtract(reserved);
                if (newReserved.compareTo(BigDecimal.ZERO) < 0) newReserved = BigDecimal.ZERO;
                part.setReservedQuantity(newReserved);
            }

            jdbcTemplate.update(
                    "UPDATE estimate_items SET reserved_quantity = 0 WHERE id = ?",
                    estimateItemId);
        }

        partRepository.saveAll(lockedParts.values());
        log.info("Released all reserved parts for repair order {}", repairOrderId);
    }

    @Override
    public Page<InventoryTransactionResponse> getTransactionsByPartId(Long partId, Pageable pageable) {
        return transactionRepository.findByPartIdOrderByCreatedAtDesc(partId, pageable)
                .map(transactionMapper::toResponse);
    }

    @Override
    public StockReconciliationResponse reconcilePartStock(Long partId) {
        Part part = partRepository.findById(partId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));

        BigDecimal ledgerTotal = transactionRepository.calculateLedgerQuantityForPart(partId);
        BigDecimal discrepancy = part.getStockQuantity().subtract(ledgerTotal);
        boolean matched = discrepancy.compareTo(BigDecimal.ZERO) == 0;

        return StockReconciliationResponse.builder()
                .partId(part.getId())
                .sku(part.getSku())
                .partName(part.getPartName())
                .stockQuantity(part.getStockQuantity())
                .ledgerQuantity(ledgerTotal)
                .reservedQuantity(part.getReservedQuantity())
                .availableQuantity(part.getAvailableQuantity())
                .discrepancy(discrepancy)
                .matched(matched)
                .build();
    }

    @Override
    public List<StockReconciliationResponse> reconcileAllParts() {
        return partRepository.findAll().stream()
                .map(part -> reconcilePartStock(part.getId()))
                .toList();
    }

    private void validateDocumentTypeConstraints(InventoryDocumentCreateRequest req) {
        switch (req.getDocumentType()) {
            case IN -> {
                if (req.getSupplierId() == null || req.getRepairOrderId() != null || req.getSourceDocumentId() != null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST);
                }
            }
            case OPENING -> {
                if (req.getSupplierId() != null || req.getRepairOrderId() != null || req.getSourceDocumentId() != null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST);
                }
            }
            case OUT -> {
                if (req.getRepairOrderId() == null || req.getSupplierId() != null || req.getSourceDocumentId() != null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST);
                }
            }
            case RETURN -> {
                if (req.getRepairOrderId() == null || req.getSourceDocumentId() == null || req.getSupplierId() != null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST);
                }
            }
            case ADJUSTMENT -> {
                if (req.getReason() == null || req.getReason().isBlank() || req.getSupplierId() != null || req.getRepairOrderId() != null || req.getSourceDocumentId() != null) {
                    throw new BusinessException(ResponseCode.BAD_REQUEST);
                }
            }
        }
    }

    private String generateDocumentCode(DocumentType type) {
        String prefix = switch (type) {
            case IN -> "DOC-IN";
            case OPENING -> "DOC-OPN";
            case OUT -> "DOC-OUT";
            case RETURN -> "DOC-RET";
            case ADJUSTMENT -> "DOC-ADJ";
        };
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

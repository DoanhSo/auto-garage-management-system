package com.Group13_65CNTT.AutoGarage.estimate.service.impl;

import com.Group13_65CNTT.AutoGarage.inventory.entity.Part;
import com.Group13_65CNTT.AutoGarage.catalog.entity.ServiceEntity;
import com.Group13_65CNTT.AutoGarage.inventory.repository.PartRepository;
import com.Group13_65CNTT.AutoGarage.catalog.repository.ServiceRepository;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateDecisionRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateItemCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateItemResponse;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.*;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateItemMapper;
import com.Group13_65CNTT.AutoGarage.estimate.mapper.EstimateMapper;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateItemRepository;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateRepository;
import com.Group13_65CNTT.AutoGarage.estimate.service.EstimateService;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrderStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairTask;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskType;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairTaskRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.EstimateApproval;
import com.Group13_65CNTT.AutoGarage.shared.contracts.InventoryOperations;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstimateServiceImpl implements EstimateService, EstimateApproval {

    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final EstimateMapper estimateMapper;
    private final EstimateItemMapper estimateItemMapper;

    private final RepairOrderRepository repairOrderRepository;
    private final RepairTaskRepository repairTaskRepository;
    private final ServiceRepository serviceRepository;
    private final PartRepository partRepository;
    private final PaginationPolicy pagination;

    @Autowired(required = false)
    private InventoryOperations inventoryOperations;

    @Autowired(required = false)
    private NotificationAudit notificationAudit;

    // =========================================================================
    // EstimateService Implementation
    // =========================================================================

    @Override
    @Transactional
    public EstimateResponse createEstimate(Long actorUserId, EstimateCreateRequest request) {
        log.info("Creating estimate for order #{} by user #{}", request.getRepairOrderId(), actorUserId);

        // Khóa phiếu sửa chữa để đảm bảo version_no tăng tuần tự và trạng thái không bị sửa đổi đồng thời
        RepairOrder order = repairOrderRepository.findByIdForUpdate(request.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa với ID: " + request.getRepairOrderId()));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể lập báo giá cho phiếu sửa chữa đã đóng hoặc bị hủy");
        }

        // Xử lý kiểm tra Kind và ParentEstimateId
        if (request.getEstimateKind() == EstimateKind.SUPPLEMENTAL) {
            if (request.getParentEstimateId() == null) {
                throw new BusinessRuleException("Báo giá phát sinh (SUPPLEMENTAL) bắt buộc phải có ID báo giá gốc (parentEstimateId)");
            }
            Estimate parent = estimateRepository.findById(request.getParentEstimateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá gốc với ID: " + request.getParentEstimateId()));
            if (!parent.getRepairOrderId().equals(order.getId())) {
                throw new BusinessRuleException("Báo giá gốc không thuộc cùng phiếu sửa chữa");
            }
        } else {
            if (request.getParentEstimateId() != null) {
                throw new BusinessRuleException("Báo giá ban đầu (INITIAL) không được có parentEstimateId");
            }
        }

        // Tăng version_no tuần tự dưới khóa phiếu
        int nextVersion = estimateRepository.findTopByRepairOrderIdOrderByVersionNoDesc(order.getId())
                .map(e -> e.getVersionNo() + 1)
                .orElse(1);

        String estimateCode = generateEstimateCode();

        Estimate estimate = Estimate.builder()
                .estimateCode(estimateCode)
                .repairOrderId(order.getId())
                .versionNo(nextVersion)
                .estimateKind(request.getEstimateKind() != null ? request.getEstimateKind() : EstimateKind.INITIAL)
                .parentEstimateId(request.getParentEstimateId())
                .status(EstimateStatus.DRAFT)
                .notes(request.getNotes())
                .validUntil(request.getValidUntil())
                .createdBy(actorUserId)
                .build();

        // Xử lý các dòng hạng mục
        List<EstimateItem> itemsToSave = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;

        int lineNo = 1;
        for (EstimateItemCreateRequest itemReq : request.getItems()) {
            EstimateItem item = estimateItemMapper.toEntity(itemReq);
            item.setLineNo(lineNo++);

            if (itemReq.getItemType() == ItemType.SERVICE) {
                if (itemReq.getServiceId() == null) {
                    throw new BusinessRuleException("Hạng mục loại SERVICE bắt buộc phải có serviceId");
                }
                ServiceEntity svc = serviceRepository.findById(itemReq.getServiceId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ catalog với ID: " + itemReq.getServiceId()));

                item.setServiceId(svc.getId());
                item.setPartId(null);
                item.setItemName(itemReq.getItemName() != null && !itemReq.getItemName().isBlank() ? itemReq.getItemName() : svc.getServiceName());
                item.setUnit(itemReq.getUnit() != null && !itemReq.getUnit().isBlank() ? itemReq.getUnit() : (svc.getUnit() != null ? svc.getUnit() : "Lần"));
                item.setUnitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : svc.getUnitPrice());
                item.setUnitCost(BigDecimal.ZERO);
                item.setWarrantyDays(svc.getWarrantyDays() != null ? svc.getWarrantyDays() : 0);
                item.setWarrantyKm(svc.getWarrantyKm() != null ? svc.getWarrantyKm() : 0);
                item.setWarrantyTerms(svc.getWarrantyTerms());
            } else if (itemReq.getItemType() == ItemType.PART) {
                if (itemReq.getPartId() == null) {
                    throw new BusinessRuleException("Hạng mục loại PART bắt buộc phải có partId");
                }
                Part part = partRepository.findById(itemReq.getPartId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phụ tùng catalog với ID: " + itemReq.getPartId()));

                item.setPartId(part.getId());
                item.setServiceId(null);
                item.setItemName(itemReq.getItemName() != null && !itemReq.getItemName().isBlank() ? itemReq.getItemName() : part.getPartName());
                item.setUnit(itemReq.getUnit() != null && !itemReq.getUnit().isBlank() ? itemReq.getUnit() : part.getUnit());
                item.setUnitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : part.getSellingPrice());
                item.setUnitCost(part.getPurchasePrice() != null ? part.getPurchasePrice() : BigDecimal.ZERO);
                item.setWarrantyDays(part.getWarrantyDays() != null ? part.getWarrantyDays() : 0);
                item.setWarrantyKm(part.getWarrantyKm() != null ? part.getWarrantyKm() : 0);
                item.setWarrantyTerms(part.getWarrantyTerms());
            }

            BigDecimal discount = itemReq.getDiscountAmount() != null ? itemReq.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal tax = itemReq.getTaxAmount() != null ? itemReq.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal lineBase = item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineBase.subtract(discount).add(tax);

            item.setDiscountAmount(discount);
            item.setTaxAmount(tax);
            item.setLineTotal(lineTotal);
            item.setApprovalStatus(ApprovalStatus.PENDING);
            item.setReservedQuantity(BigDecimal.ZERO);

            subtotal = subtotal.add(lineBase);
            totalDiscount = totalDiscount.add(discount);
            totalTax = totalTax.add(tax);

            itemsToSave.add(item);
        }

        estimate.setSubtotal(subtotal);
        estimate.setDiscountAmount(totalDiscount);
        estimate.setTaxAmount(totalTax);
        estimate.setTotalAmount(subtotal.subtract(totalDiscount).add(totalTax));

        Estimate savedEstimate = estimateRepository.save(estimate);
        for (EstimateItem it : itemsToSave) {
            it.setEstimateId(savedEstimate.getId());
        }
        List<EstimateItem> savedItems = estimateItemRepository.saveAll(itemsToSave);

        // Chuyển trạng thái phiếu sang WAITING_APPROVAL nếu đang NEW hoặc DIAGNOSING
        if (order.getStatus() == RepairOrderStatus.NEW || order.getStatus() == RepairOrderStatus.DIAGNOSING) {
            order.setStatus(RepairOrderStatus.WAITING_APPROVAL);
            repairOrderRepository.save(order);
        }

        List<EstimateItemResponse> itemResponses = savedItems.stream()
                .map(estimateItemMapper::toResponse)
                .toList();

        log.info("Created estimate #{} with version {} and {} items", savedEstimate.getId(), savedEstimate.getVersionNo(), savedItems.size());
        return estimateMapper.toResponseWithItems(savedEstimate, itemResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimateById(Long id) {
        Estimate estimate = estimateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá với ID: " + id));
        List<EstimateItemResponse> items = estimateItemRepository.findByEstimateIdOrderByLineNoAsc(id).stream()
                .map(estimateItemMapper::toResponse)
                .toList();
        return estimateMapper.toResponseWithItems(estimate, items);
    }

    @Override
    @Transactional(readOnly = true)
    public EstimateResponse getEstimateByCode(String estimateCode) {
        Estimate estimate = estimateRepository.findByEstimateCode(estimateCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá với mã: " + estimateCode));
        List<EstimateItemResponse> items = estimateItemRepository.findByEstimateIdOrderByLineNoAsc(estimate.getId()).stream()
                .map(estimateItemMapper::toResponse)
                .toList();
        return estimateMapper.toResponseWithItems(estimate, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EstimateResponse> getEstimatesByRepairOrderId(Long repairOrderId) {
        List<Estimate> estimates = estimateRepository.findByRepairOrderId(repairOrderId);
        List<EstimateResponse> responses = new ArrayList<>();
        for (Estimate est : estimates) {
            List<EstimateItemResponse> items = estimateItemRepository.findByEstimateIdOrderByLineNoAsc(est.getId()).stream()
                    .map(estimateItemMapper::toResponse)
                    .toList();
            responses.add(estimateMapper.toResponseWithItems(est, items));
        }
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EstimateResponse> searchEstimates(Long repairOrderId, EstimateStatus status,
                                                  int page, int size, String sortBy, String sortDir) {
        Pageable pageable = pagination.page(page, size, sortBy, sortDir,
                Set.of("id", "estimateCode", "versionNo", "status", "createdAt", "totalAmount"));
        Page<Estimate> pageResult = estimateRepository.searchEstimates(repairOrderId, status, pageable);
        return pageResult.map(est -> {
            List<EstimateItemResponse> items = estimateItemRepository.findByEstimateIdOrderByLineNoAsc(est.getId()).stream()
                    .map(estimateItemMapper::toResponse)
                    .toList();
            return estimateMapper.toResponseWithItems(est, items);
        });
    }

    @Override
    @Transactional
    public EstimateResponse sendEstimate(Long actorUserId, Long id) {
        Estimate estimate = estimateRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá với ID: " + id));

        if (estimate.getStatus() != EstimateStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ báo giá ở trạng thái DRAFT mới có thể gửi cho khách hàng");
        }

        estimate.setStatus(EstimateStatus.SENT);
        estimate.setSentAt(Instant.now());
        estimate = estimateRepository.save(estimate);

        // Ghi outbox thông báo nếu có NotificationAudit
        if (notificationAudit != null) {
            try {
                notificationAudit.enqueue(new NotificationAudit.Notice(
                        null, null, "ESTIMATE-" + estimate.getId(), null,
                        "ESTIMATE_SENT", NotificationAudit.Channel.WEB,
                        "{\"estimateId\":" + estimate.getId() + ",\"code\":\"" + estimate.getEstimateCode() + "\"}"
                ));
            } catch (Exception ex) {
                log.warn("Không thể gửi thông báo báo giá: {}", ex.getMessage());
            }
        }

        return getEstimateById(id);
    }

    @Override
    @Transactional
    public EstimateResponse decideEstimate(Long actorUserId, Long id, EstimateDecisionRequest request) {
        decide(actorUserId, id, request.getLines(), request.getRequestKey());
        return getEstimateById(id);
    }

    // =========================================================================
    // Contract I04 (EstimateApproval)
    // =========================================================================

    @Override
    @Transactional
    public void decide(long actorUserId, long estimateId, List<LineDecision> lines, String requestKey) {
        log.info("EstimateApproval.decide: actor={}, estimateId={}, linesCount={}, requestKey={}",
                actorUserId, estimateId, lines.size(), requestKey);

        Estimate estimate = estimateRepository.findByIdForUpdate(estimateId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo giá với ID: " + estimateId));

        if (estimate.getStatus() == EstimateStatus.REJECTED || estimate.getStatus() == EstimateStatus.EXPIRED || estimate.getStatus() == EstimateStatus.CANCELLED) {
            throw new BusinessRuleException("Báo giá ở trạng thái " + estimate.getStatus() + " không thể phê duyệt");
        }

        RepairOrder order = repairOrderRepository.findByIdForUpdate(estimate.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa của báo giá này"));

        if (order.getStatus() == RepairOrderStatus.CLOSED || order.getStatus() == RepairOrderStatus.CANCELLED) {
            throw new BusinessRuleException("Không thể phê duyệt báo giá của phiếu sửa chữa đã kết thúc");
        }

        Instant now = Instant.now();
        boolean hasAnyApproved = false;

        for (LineDecision decision : lines) {
            EstimateItem item = estimateItemRepository.findByIdForUpdate(decision.estimateItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hạng mục báo giá với ID: " + decision.estimateItemId()));

            if (!item.getEstimateId().equals(estimateId)) {
                throw new BusinessRuleException("Hạng mục #" + item.getId() + " không thuộc báo giá #" + estimateId);
            }

            // Nếu dòng đã được duyệt hoặc từ chối trước đó, bỏ qua để đảm bảo idempotency replay
            if (item.getApprovalStatus() != ApprovalStatus.PENDING) {
                if (item.getApprovalStatus() == ApprovalStatus.APPROVED) {
                    hasAnyApproved = true;
                }
                continue;
            }

            item.setApprovedBy(actorUserId);
            item.setDecidedAt(now);
            item.setDecisionNote(decision.note());

            if (decision.decision() == Decision.APPROVED) {
                item.setApprovalStatus(ApprovalStatus.APPROVED);
                hasAnyApproved = true;

                // 1. Nếu là SERVICE: Tạo repair_task
                if (item.getItemType() == ItemType.SERVICE) {
                    if (repairTaskRepository.findByEstimateItemId(item.getId()).isEmpty()) {
                        RepairTask task = RepairTask.builder()
                                .repairOrderId(order.getId())
                                .estimateItemId(item.getId())
                                .serviceId(item.getServiceId())
                                .taskType(TaskType.REPAIR)
                                .taskName(item.getItemName())
                                .description(item.getDecisionNote())
                                .status(TaskStatus.PENDING)
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .build();
                        repairTaskRepository.save(task);
                        log.info("Created repair task from approved estimate item #{}", item.getId());
                    }
                }

                // 2. Nếu là PART: Gọi I05 giữ hàng trong kho
                if (item.getItemType() == ItemType.PART) {
                    item.setReservedQuantity(item.getQuantity());
                    if (inventoryOperations != null) {
                        try {
                            inventoryOperations.reserve(actorUserId, item.getId(), item.getQuantity());
                            log.info("Reserved part for estimate item #{}, quantity={}", item.getId(), item.getQuantity());
                        } catch (Exception ex) {
                            log.warn("Lỗi khi giữ hàng tồn kho qua I05: {}", ex.getMessage());
                        }
                    }
                }
            } else {
                item.setApprovalStatus(ApprovalStatus.REJECTED);
            }

            estimateItemRepository.save(item);
        }

        // Cập nhật trạng thái tổng thể của Estimate
        List<EstimateItem> allItems = estimateItemRepository.findByEstimateIdOrderByLineNoAsc(estimateId);
        long approvedCount = allItems.stream().filter(i -> i.getApprovalStatus() == ApprovalStatus.APPROVED).count();
        long rejectedCount = allItems.stream().filter(i -> i.getApprovalStatus() == ApprovalStatus.REJECTED).count();
        long pendingCount = allItems.stream().filter(i -> i.getApprovalStatus() == ApprovalStatus.PENDING).count();

        if (pendingCount == 0) {
            if (approvedCount == allItems.size()) {
                estimate.setStatus(EstimateStatus.APPROVED);
            } else if (rejectedCount == allItems.size()) {
                estimate.setStatus(EstimateStatus.REJECTED);
            } else {
                estimate.setStatus(EstimateStatus.PARTIALLY_APPROVED);
            }
        } else if (approvedCount > 0) {
            estimate.setStatus(EstimateStatus.PARTIALLY_APPROVED);
        }

        estimateRepository.save(estimate);

        // Cập nhật trạng thái RepairOrder sang APPROVED nếu đang WAITING_APPROVAL
        if (hasAnyApproved && order.getStatus() == RepairOrderStatus.WAITING_APPROVAL) {
            order.setStatus(RepairOrderStatus.APPROVED);
            repairOrderRepository.save(order);
        }

        // Ghi completed request cho I08 nếu có requestKey
        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()) {
            notificationAudit.recordCompletedRequest(actorUserId, "DECIDE_ESTIMATE", "ESTIMATE",
                    estimateId, requestKey, requestKey, "{\"status\":\"" + estimate.getStatus() + "\"}");
        }

        log.info("Decided estimate #{} successfully. New status: {}", estimateId, estimate.getStatus());
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String generateEstimateCode() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);
            String candidate = String.format("EST-%s-%d", dateStr, randomNum);
            if (!estimateRepository.existsByEstimateCode(candidate)) {
                return candidate;
            }
        }
        return String.format("EST-%s-%d", dateStr, System.currentTimeMillis() % 100000);
    }
}

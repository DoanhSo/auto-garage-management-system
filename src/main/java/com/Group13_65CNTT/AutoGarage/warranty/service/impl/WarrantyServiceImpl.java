package com.Group13_65CNTT.AutoGarage.warranty.service.impl;

import com.Group13_65CNTT.AutoGarage.common.util.RequestFingerprint;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;

import com.Group13_65CNTT.AutoGarage.billing.entity.Invoice;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceItem;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceItemRepository;
import com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceRepository;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.repair.entity.RepairOrder;
import com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository;
import com.Group13_65CNTT.AutoGarage.shared.contracts.NotificationAudit;
import com.Group13_65CNTT.AutoGarage.shared.contracts.RepairWorkflow;
import com.Group13_65CNTT.AutoGarage.shared.contracts.WarrantyOperations;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimDecideRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimOpenRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyClaimResponse;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyResponse;
import com.Group13_65CNTT.AutoGarage.warranty.entity.ClaimStatus;
import com.Group13_65CNTT.AutoGarage.warranty.entity.Warranty;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyClaim;
import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import com.Group13_65CNTT.AutoGarage.warranty.mapper.WarrantyClaimMapper;
import com.Group13_65CNTT.AutoGarage.warranty.mapper.WarrantyMapper;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyClaimRepository;
import com.Group13_65CNTT.AutoGarage.warranty.repository.WarrantyRepository;
import com.Group13_65CNTT.AutoGarage.warranty.service.WarrantyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarrantyServiceImpl implements WarrantyService, WarrantyOperations {
    private final com.Group13_65CNTT.AutoGarage.common.security.CustomerScope customerScope;
    private final com.Group13_65CNTT.AutoGarage.identity.service.ObjectAccessService objectAccess;

    private final WarrantyRepository warrantyRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final WarrantyMapper warrantyMapper;
    private final WarrantyClaimMapper warrantyClaimMapper;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final RepairOrderRepository repairOrderRepository;
    private final RepairWorkflow repairWorkflow;
    private final PaginationPolicy pagination;

    @Autowired(required = false)
    private NotificationAudit notificationAudit;

    // =========================================================================
    // Contract I07 (WarrantyOperations)
    // =========================================================================

    @Override
    @Transactional
    public void createFromInvoice(long actorUserId, long invoiceId, int odometerOut) {
        log.info("WarrantyOperations.createFromInvoice: actor={}, invoiceId={}, odoOut={}",
                actorUserId, invoiceId, odometerOut);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn với ID: " + invoiceId));

        RepairOrder order = repairOrderRepository.findById(invoice.getRepairOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu sửa chữa tương ứng với hóa đơn"));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceId(invoiceId);

        Instant now = Instant.now();
        for (InvoiceItem item : items) {
            int days = item.getWarrantyDays() != null ? item.getWarrantyDays() : 0;
            int km = item.getWarrantyKm() != null ? item.getWarrantyKm() : 0;

            // Chỉ tạo warranty nếu dòng có chính sách bảo hành (days > 0 hoặc km > 0)
            if (days > 0 || km > 0) {
                if (warrantyRepository.findByInvoiceItemId(item.getId()).isEmpty()) {
                    String warrantyCode = generateCode("WAR");
                    Instant expiresAt = days > 0 ? now.plus(days, ChronoUnit.DAYS) : null;
                    Integer endOdometer = km > 0 ? odometerOut + km : null;

                    Warranty warranty = Warranty.builder()
                            .warrantyCode(warrantyCode)
                            .invoiceItemId(item.getId())
                            .vehicleId(order.getVehicleId())
                            .customerId(order.getCustomerId())
                            .startsAt(now)
                            .expiresAt(expiresAt)
                            .startOdometer(odometerOut)
                            .endOdometer(endOdometer)
                            .warrantyDays(days)
                            .warrantyKm(km)
                            .termsSnapshot(item.getWarrantyTerms())
                            .status(WarrantyStatus.ACTIVE)
                            .build();

                    warrantyRepository.save(warranty);
                    log.info("Tạo bảo hành thành công: code={}, item={}", warrantyCode, item.getItemName());
                }
            }
        }
    }

    @Override
    @Transactional
    public long openClaim(long actorUserId, long warrantyId, int currentOdometer, String description, String requestKey) {
        objectAccess.requireResourceAccess(actorUserId, com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.WARRANTY, warrantyId);
        log.info("WarrantyOperations.openClaim: actor={}, warrantyId={}, odo={}, requestKey={}",
                actorUserId, warrantyId, currentOdometer, requestKey);

        String requestHash = RequestFingerprint.of("OPEN_CLAIM", actorUserId, warrantyId, currentOdometer, description);
        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()) {
            String completed = notificationAudit.findCompletedRequest(requestKey, requestHash);
            if (completed != null) {
                return warrantyClaimRepository.findByClaimCode(RequestFingerprint.resultField(completed, "claimCode"))
                        .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND)).getId();
            }
        }
        if (currentOdometer < 0) {
            throw new BusinessRuleException("Số km hiện tại phải >= 0");
        }
        if (description == null || description.isBlank()) {
            throw new BusinessRuleException("Mô tả hiện tượng hư hỏng không được để trống");
        }

        Warranty warranty = warrantyRepository.findById(warrantyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bảo hành với ID: " + warrantyId));

        if (warranty.getStatus() != WarrantyStatus.ACTIVE) {
            throw new BusinessRuleException("Chính sách bảo hành không ở trạng thái ACTIVE (hiện tại: " + warranty.getStatus() + ")");
        }
        if (warranty.getStartOdometer() != null && currentOdometer < warranty.getStartOdometer()) {
            throw new BusinessRuleException("Số km yêu cầu bảo hành không được nhỏ hơn số km đã bàn giao");
        }

        // Kiểm tra thời gian: mốc bằng hạn được coi là hết hạn
        if (warranty.getExpiresAt() != null && !Instant.now().isBefore(warranty.getExpiresAt())) {
            warranty.setStatus(WarrantyStatus.EXPIRED);
            warrantyRepository.save(warranty);
            throw new BusinessRuleException("Chính sách bảo hành đã hết hạn về thời gian");
        }

        // Kiểm tra số km: mốc bằng hạn được coi là hết hạn
        if (warranty.getEndOdometer() != null && currentOdometer >= warranty.getEndOdometer()) {
            throw new BusinessRuleException(String.format(
                    "Chính sách bảo hành đã hết hạn về số km (hiện tại: %d, hạn mức: %d)",
                    currentOdometer, warranty.getEndOdometer()));
        }

        String claimCode = generateCode("CLM");

        WarrantyClaim claim = WarrantyClaim.builder()
                .claimCode(claimCode)
                .warrantyId(warrantyId)
                .requestedBy(actorUserId)
                .requestedAt(Instant.now())
                .currentOdometer(currentOdometer)
                .description(description)
                .status(ClaimStatus.OPEN)
                .build();

        WarrantyClaim savedClaim = warrantyClaimRepository.save(claim);

        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()) {
            notificationAudit.recordCompletedRequest(actorUserId, "OPEN_CLAIM", "WARRANTY_CLAIM",
                    savedClaim.getId(), requestKey, requestHash, "{\"claimCode\":\"" + claimCode + "\"}");
        }

        log.info("Yêu cầu bảo hành được tạo thành công: id={}, code={}", savedClaim.getId(), savedClaim.getClaimCode());
        return savedClaim.getId();
    }

    @Override
    @Transactional
    public void decideClaim(long actorUserId, long claimId, boolean approved, String reason, String requestKey) {
        objectAccess.requireRole(actorUserId, com.Group13_65CNTT.AutoGarage.common.enums.Role.ADMIN.name());
        log.info("WarrantyOperations.decideClaim: actor={}, claimId={}, approved={}, reason={}",
                actorUserId, claimId, approved, reason);

        String requestHash = RequestFingerprint.of("DECIDE_CLAIM", actorUserId, claimId, approved, reason);
        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()
                && notificationAudit.findCompletedRequest(requestKey, requestHash) != null) {
            return;
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Lý do phê duyệt hoặc từ chối không được để trống");
        }

        WarrantyClaim claim = warrantyClaimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo hành với ID: " + claimId));

        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new BusinessRuleException("Yêu cầu bảo hành không ở trạng thái chờ duyệt (OPEN), hiện tại: " + claim.getStatus());
        }

        claim.setDecidedBy(actorUserId);
        claim.setDecidedAt(Instant.now());
        claim.setDecisionReason(reason);

        if (approved) {
            claim.setStatus(ClaimStatus.APPROVED);
            warrantyClaimRepository.save(claim);

            // Khi duyệt bảo hành: gọi I03 RepairWorkflow để tạo phiếu sửa chữa bảo hành mới
            long orderId = repairWorkflow.createWarrantyOrder(actorUserId, claimId);
            log.info("Đã tạo phiếu sửa chữa bảo hành #{} cho claim #{}", orderId, claimId);
        } else {
            claim.setStatus(ClaimStatus.REJECTED);
            claim.setClosedAt(Instant.now());
            warrantyClaimRepository.save(claim);
            log.info("Yêu cầu bảo hành #{} bị từ chối với lý do: {}", claimId, reason);
        }

        if (notificationAudit != null && requestKey != null && !requestKey.isBlank()) {
            notificationAudit.recordCompletedRequest(actorUserId, "DECIDE_CLAIM", "WARRANTY_CLAIM",
                    claimId, requestKey, requestHash, "{\"status\":\"" + claim.getStatus() + "\"}");
        }
    }

    // =========================================================================
    // WarrantyService REST API Implementations
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public WarrantyResponse getWarrantyById(Long id) {
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.WARRANTY, id);
        Warranty warranty = warrantyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin bảo hành với ID: " + id));
        return warrantyMapper.toResponse(warranty);
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyResponse getWarrantyByCode(String warrantyCode) {
        Warranty warranty = warrantyRepository.findByWarrantyCode(warrantyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin bảo hành với mã: " + warrantyCode));
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.WARRANTY, warranty.getId());
        return warrantyMapper.toResponse(warranty);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarrantyResponse> getWarrantiesByVehicleId(Long vehicleId, int page, int size) {
        customerScope.requireFrontDesk();
        Pageable pageable = PageRequest.of(page, size);
        return warrantyRepository.findByVehicleId(vehicleId, pageable).map(warrantyMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarrantyResponse> getWarrantiesByCustomerId(Long customerId, int page, int size) {
        customerScope.requireFrontDesk();
        Pageable pageable = PageRequest.of(page, size);
        return warrantyRepository.findByCustomerId(customerId, pageable).map(warrantyMapper::toResponse);
    }

    @Override
    @Transactional
    public WarrantyClaimResponse openClaim(Long actorUserId, WarrantyClaimOpenRequest request) {
        long claimId = openClaim(actorUserId, request.getWarrantyId(), request.getCurrentOdometer(),
                request.getDescription(), request.getRequestKey());
        return getClaimById(claimId);
    }

    @Override
    @Transactional
    public WarrantyClaimResponse decideClaim(Long actorUserId, Long claimId, WarrantyClaimDecideRequest request) {
        decideClaim(actorUserId, claimId, request.getApproved(), request.getReason(), request.getRequestKey());
        return getClaimById(claimId);
    }

    @Override
    @Transactional(readOnly = true)
    public WarrantyClaimResponse getClaimById(Long claimId) {
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.WARRANTY_CLAIM, claimId);
        WarrantyClaim claim = warrantyClaimRepository.findById(claimId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu bảo hành với ID: " + claimId));
        return warrantyClaimMapper.toResponse(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WarrantyClaimResponse> getClaimsByWarrantyId(Long warrantyId, int page, int size) {
        customerScope.requireResource(com.Group13_65CNTT.AutoGarage.common.enums.AccessResource.WARRANTY, warrantyId);
        Pageable pageable = PageRequest.of(page, size);
        return warrantyClaimRepository.findByWarrantyId(warrantyId, pageable).map(warrantyClaimMapper::toResponse);
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String generateCode(String prefix) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 10; i++) {
            int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);
            String candidate = String.format("%s-%s-%d", prefix, dateStr, randomNum);
            if (prefix.equals("WAR") && warrantyRepository.findByWarrantyCode(candidate).isEmpty()) {
                return candidate;
            }
            if (prefix.equals("CLM") && warrantyClaimRepository.findByClaimCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return String.format("%s-%s-%d", prefix, dateStr, System.currentTimeMillis() % 100000);
    }
}

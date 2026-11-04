package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.audit.annotation.Audited;
import com.Group13_65CNTT.AutoGarage.common.constant.AuditConstants;

import com.Group13_65CNTT.AutoGarage.common.config.WorkshopProperties;
import com.Group13_65CNTT.AutoGarage.common.enums.AccessResource;
import com.Group13_65CNTT.AutoGarage.common.security.CustomerScope;
import com.Group13_65CNTT.AutoGarage.reception.repository.VehicleCheckRepository;
import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.mapper.VehicleCheckMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentFormat;
import com.Group13_65CNTT.AutoGarage.reception.service.impl.AttachmentStorage;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.enums.Role;
import com.Group13_65CNTT.AutoGarage.reception.dto.response.AttachmentDownloadResponse;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentServiceImpl implements AttachmentService {
    private final WorkshopProperties workshop;

    private final AttachmentRepository attachmentRepository;
    private final VehicleCheckMapper vehicleCheckMapper;
    private final VehicleCheckRepository vehicleCheckRepository;
    private final CustomerScope customerScope;

    private final AttachmentStorage storage;
    private final CurrentUserProvider currentUser;
    private final com.Group13_65CNTT.AutoGarage.repair.repository.RepairOrderRepository repairOrderRepository;

    @Override
    @Audited(action = AuditConstants.ACTION_ATTACHMENT_UPLOADED, entity = AuditConstants.ENTITY_ATTACHMENT)
    public AttachmentResponse uploadAttachment(Long repairOrderId, Long vehicleCheckId, Long warrantyClaimId,
                                               AttachmentCategory category, Long uploadedBy, MultipartFile file) {
        if (uploadedBy == null || uploadedBy <= 0) {
            throw new BusinessException(ResponseCode.UNAUTHORIZED);
        }
        if (currentUser.userId() != uploadedBy) throw new BusinessException(ResponseCode.FORBIDDEN);
        if (currentUser.role() != Role.ADMIN && currentUser.role() != Role.RECEPTIONIST
                && currentUser.role() != Role.TECHNICIAN) throw new BusinessException(ResponseCode.FORBIDDEN);
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Tệp tải lên không được để trống");
        }

        if (file.getSize() > workshop.getMaxAttachmentBytes()) {
            throw new BusinessException(ResponseCode.UPLOAD_TOO_LARGE);
        }

        AttachmentFormat format = AttachmentFormat.forContentType(file.getContentType());
        String contentType = format.getContentType();

        if (category == null) {
            throw new BusinessRuleException("Danh mục tệp đính kèm (category) không được để trống");
        }

        // Validate constraint ck_attachments_target
        if (repairOrderId != null && warrantyClaimId != null) {
            throw new BusinessRuleException("Tệp đính kèm không thể liên kết đồng thời cả phiếu sửa chữa và yêu cầu bảo hành");
        }
        if (repairOrderId == null && warrantyClaimId == null) {
            throw new BusinessRuleException("Tệp đính kèm phải gắn với phiếu sửa chữa hoặc yêu cầu bảo hành");
        }
        if (repairOrderId == null && category != AttachmentCategory.WARRANTY) {
            throw new BusinessRuleException("Tệp đính kèm bảo hành phải có category là WARRANTY");
        }

        // Validate constraint ck_attachments_check_target
        if (vehicleCheckId != null && repairOrderId == null) {
            throw new BusinessRuleException("Tệp gắn với biên bản kiểm tra phải thuộc về một phiếu sửa chữa");
        }

        customerScope.requireResource(repairOrderId != null ? AccessResource.REPAIR_ORDER : AccessResource.WARRANTY_CLAIM,
                repairOrderId != null ? repairOrderId : warrantyClaimId);
        if (repairOrderId != null) {
            var orderStatus = repairOrderRepository.findById(repairOrderId)
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND))
                    .getStatus();
            if (!com.Group13_65CNTT.AutoGarage.repair.service.RepairStageRules.attachmentWindow(category).contains(orderStatus)) {
                throw new BusinessRuleException("Ảnh " + com.Group13_65CNTT.AutoGarage.repair.service.RepairStageRules.attachmentCategoryLabel(category)
                        + " chỉ tải lên ở giai đoạn " + com.Group13_65CNTT.AutoGarage.repair.service.RepairStageRules.attachmentStageLabel(category));
            }
        }
        if (vehicleCheckId != null) {
            var check = vehicleCheckRepository.findById(vehicleCheckId)
                    .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
            if (!java.util.Objects.equals(repairOrderId, check.getRepairOrderId())
                    || !category.name().equals(check.getCheckType().name())) {
                throw new BusinessException(ResponseCode.BAD_REQUEST);
            }
        }
        byte[] content = readContent(file);
        if (!format.matches(content)) throw new BusinessException(ResponseCode.UNSUPPORTED_MEDIA_TYPE);
        String sha256 = calculateSha256(content);
        String storageKey = "attachments/" + category.name().toLowerCase(java.util.Locale.ROOT)
                + "/" + UUID.randomUUID() + format.getExtension();

        Attachment attachment = new Attachment();
        attachment.setRepairOrderId(repairOrderId);
        attachment.setVehicleCheckId(vehicleCheckId);
        attachment.setWarrantyClaimId(warrantyClaimId);
        attachment.setStorageKey(storageKey);
        attachment.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file");
        attachment.setContentType(contentType);
        attachment.setSizeBytes((long) content.length);
        attachment.setSha256(sha256);
        attachment.setCategory(category);
        attachment.setUploadedBy(uploadedBy);

        // Register rollback cleanup before the row/audit commit; metadata is never committed without bytes.
        storage.store(storageKey, content);
        try {
            Attachment saved = attachmentRepository.saveAndFlush(attachment);
            return vehicleCheckMapper.toAttachmentResponse(saved);
        } catch (org.springframework.dao.DataAccessException failure) {
            storage.remove(storageKey);
            throw failure;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentContent loadAttachmentContent(Long id) {
        try {
            var download = downloadAttachment(id);
            return new com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentContent(
                    download.content(), download.contentType(), download.filename());
        } catch (BusinessException failure) {
            if (failure.getResponseCode() != ResponseCode.RESOURCE_NOT_FOUND) throw failure;
            // Retain develop's clearly labelled preview for seed records without an original file.
            customerScope.requireResource(AccessResource.ATTACHMENT, id);
            var attachment = attachmentRepository.findById(id).orElseThrow(() -> failure);
            return new com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentContent(
                    placeholderSvg(attachment).getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "image/svg+xml", attachment.getOriginalFilename());
        }
    }

    private static String placeholderSvg(Attachment attachment) {
        String category = attachment.getCategory() != null ? attachment.getCategory().name() : "";
        String sha = attachment.getSha256() != null ? attachment.getSha256() : "";
        String shortSha = sha.length() > 16 ? sha.substring(0, 16) + "…" : sha;
        return """
                <svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">\
                <rect width="800" height="600" fill="#f1f5f9"/>\
                <rect x="40" y="40" width="720" height="520" rx="16" fill="#ffffff" stroke="#cbd5e1"/>\
                <text x="400" y="250" font-family="sans-serif" font-size="28" font-weight="700" fill="#245bdb" text-anchor="middle">%s</text>\
                <text x="400" y="300" font-family="sans-serif" font-size="22" fill="#172338" text-anchor="middle">%s</text>\
                <text x="400" y="345" font-family="monospace" font-size="16" fill="#64748b" text-anchor="middle">SHA-256: %s</text>\
                <text x="400" y="395" font-family="sans-serif" font-size="15" fill="#94a3b8" text-anchor="middle">Ảnh mẫu — chưa có tệp gốc trên máy chủ</text>\
                </svg>"""
                .formatted(xml(category), xml(attachment.getOriginalFilename()), xml(shortSha));
    }

    private static String xml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse getAttachmentById(Long id) {
        customerScope.requireResource(AccessResource.ATTACHMENT, id);
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tệp đính kèm với ID: " + id));
        return vehicleCheckMapper.toAttachmentResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByRepairOrder(Long repairOrderId) {
        customerScope.requireEvidenceAccess(repairOrderId);
        return attachmentRepository.findAllByRepairOrderId(repairOrderId)
                .stream()
                .map(vehicleCheckMapper::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByVehicleCheck(Long vehicleCheckId) {
        var check = vehicleCheckRepository.findById(vehicleCheckId)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        customerScope.requireEvidenceAccess(check.getRepairOrderId());
        return attachmentRepository.findAllByVehicleCheckId(vehicleCheckId)
                .stream()
                .map(vehicleCheckMapper::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentDownloadResponse downloadAttachment(Long id) {
        customerScope.requireResource(AccessResource.ATTACHMENT, id);
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCode.RESOURCE_NOT_FOUND));
        byte[] content = storage.read(attachment.getStorageKey());
        if (!calculateSha256(content).equals(attachment.getSha256())) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR);
        }
        return new AttachmentDownloadResponse(attachment.getOriginalFilename(), attachment.getContentType(), content);
    }

    private byte[] readContent(MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            byte[] content = stream.readNBytes(Math.toIntExact(workshop.getMaxAttachmentBytes() + 1));
            if (content.length > workshop.getMaxAttachmentBytes()) throw new BusinessException(ResponseCode.UPLOAD_TOO_LARGE);
            return content;
        } catch (IOException failure) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, failure);
        }
    }

    private String calculateSha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException failure) {
            throw new BusinessException(ResponseCode.INTERNAL_SERVER_ERROR, failure);
        }
    }
}

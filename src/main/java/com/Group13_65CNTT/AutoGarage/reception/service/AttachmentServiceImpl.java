package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.mapper.VehicleCheckMapper;
import com.Group13_65CNTT.AutoGarage.reception.repository.AttachmentRepository;
import com.Group13_65CNTT.AutoGarage.shared.exception.BusinessRuleException;
import com.Group13_65CNTT.AutoGarage.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AttachmentServiceImpl implements AttachmentService {

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "application/pdf"
    );

    private final AttachmentRepository attachmentRepository;
    private final VehicleCheckMapper vehicleCheckMapper;

    public AttachmentServiceImpl(AttachmentRepository attachmentRepository,
                                 VehicleCheckMapper vehicleCheckMapper) {
        this.attachmentRepository = attachmentRepository;
        this.vehicleCheckMapper = vehicleCheckMapper;
    }

    @Override
    public AttachmentResponse uploadAttachment(Long repairOrderId, Long vehicleCheckId, Long warrantyClaimId,
                                               AttachmentCategory category, Long uploadedBy, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Tệp tải lên không được để trống");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("Kích thước tệp vượt quá giới hạn cho phép (tối đa 15MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException("Định dạng tệp (" + contentType + ") không được hỗ trợ. Chỉ chấp nhận JPG, PNG, WEBP, HEIC, PDF");
        }

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

        String sha256 = calculateSha256(file);
        String storageKey = generateSafeStorageKey(category, file.getOriginalFilename());

        Attachment attachment = new Attachment();
        attachment.setRepairOrderId(repairOrderId);
        attachment.setVehicleCheckId(vehicleCheckId);
        attachment.setWarrantyClaimId(warrantyClaimId);
        attachment.setStorageKey(storageKey);
        attachment.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file");
        attachment.setContentType(contentType);
        attachment.setSizeBytes(file.getSize());
        attachment.setSha256(sha256);
        attachment.setCategory(category);
        attachment.setUploadedBy(uploadedBy != null ? uploadedBy : 1L);

        Attachment saved = attachmentRepository.save(attachment);
        return vehicleCheckMapper.toAttachmentResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttachmentResponse getAttachmentById(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tệp đính kèm với ID: " + id));
        return vehicleCheckMapper.toAttachmentResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByRepairOrder(Long repairOrderId) {
        return attachmentRepository.findAllByRepairOrderId(repairOrderId)
                .stream()
                .map(vehicleCheckMapper::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsByVehicleCheck(Long vehicleCheckId) {
        return attachmentRepository.findAllByVehicleCheckId(vehicleCheckId)
                .stream()
                .map(vehicleCheckMapper::toAttachmentResponse)
                .toList();
    }

    private String calculateSha256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new BusinessRuleException("Không thể tính mã băm SHA-256 của tệp đính kèm: " + e.getMessage());
        }
    }

    private String generateSafeStorageKey(AttachmentCategory category, String originalFilename) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        return "attachments/" + category.name().toLowerCase() + "/" + datePrefix + "/" + UUID.randomUUID() + extension;
    }
}

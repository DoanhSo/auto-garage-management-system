package com.Group13_65CNTT.AutoGarage.reception.dto;

import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;

import java.time.Instant;

public class AttachmentResponse {

    private Long id;
    private Long repairOrderId;
    private Long vehicleCheckId;
    private Long warrantyClaimId;
    private String storageKey;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private String sha256;
    private AttachmentCategory category;
    private Long uploadedBy;
    private Instant uploadedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRepairOrderId() {
        return repairOrderId;
    }

    public void setRepairOrderId(Long repairOrderId) {
        this.repairOrderId = repairOrderId;
    }

    public Long getVehicleCheckId() {
        return vehicleCheckId;
    }

    public void setVehicleCheckId(Long vehicleCheckId) {
        this.vehicleCheckId = vehicleCheckId;
    }

    public Long getWarrantyClaimId() {
        return warrantyClaimId;
    }

    public void setWarrantyClaimId(Long warrantyClaimId) {
        this.warrantyClaimId = warrantyClaimId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public AttachmentCategory getCategory() {
        return category;
    }

    public void setCategory(AttachmentCategory category) {
        this.category = category;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Instant uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

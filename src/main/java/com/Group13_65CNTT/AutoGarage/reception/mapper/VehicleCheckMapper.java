package com.Group13_65CNTT.AutoGarage.reception.mapper;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.dto.CreateVehicleCheckRequest;
import com.Group13_65CNTT.AutoGarage.reception.dto.VehicleCheckResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.Attachment;
import com.Group13_65CNTT.AutoGarage.reception.entity.VehicleCheck;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class VehicleCheckMapper {

    public VehicleCheck toEntity(CreateVehicleCheckRequest request, int attemptNo) {
        if (request == null) {
            return null;
        }
        VehicleCheck entity = new VehicleCheck();
        entity.setRepairOrderId(request.getRepairOrderId());
        entity.setCheckType(request.getCheckType());
        entity.setAttemptNo(attemptNo);
        entity.setResult(request.getResult());
        entity.setChecklistJson(request.getChecklistJson());
        if (request.getSchemaVersion() != null) {
            entity.setSchemaVersion(request.getSchemaVersion());
        }
        entity.setOdometer(request.getOdometer());
        entity.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        entity.setCheckedBy(request.getCheckedBy());
        return entity;
    }

    public VehicleCheckResponse toResponse(VehicleCheck check, List<AttachmentResponse> attachments) {
        if (check == null) {
            return null;
        }
        VehicleCheckResponse response = new VehicleCheckResponse();
        response.setId(check.getId());
        response.setRepairOrderId(check.getRepairOrderId());
        response.setCheckType(check.getCheckType());
        response.setAttemptNo(check.getAttemptNo());
        response.setResult(check.getResult());
        response.setChecklistJson(check.getChecklistJson());
        response.setSchemaVersion(check.getSchemaVersion());
        response.setOdometer(check.getOdometer());
        response.setNotes(check.getNotes());
        response.setCheckedBy(check.getCheckedBy());
        response.setCheckedAt(check.getCheckedAt());
        response.setAttachments(attachments != null ? attachments : Collections.emptyList());
        return response;
    }

    public AttachmentResponse toAttachmentResponse(Attachment attachment) {
        if (attachment == null) {
            return null;
        }
        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setRepairOrderId(attachment.getRepairOrderId());
        response.setVehicleCheckId(attachment.getVehicleCheckId());
        response.setWarrantyClaimId(attachment.getWarrantyClaimId());
        response.setStorageKey(attachment.getStorageKey());
        response.setOriginalFilename(attachment.getOriginalFilename());
        response.setContentType(attachment.getContentType());
        response.setSizeBytes(attachment.getSizeBytes());
        response.setSha256(attachment.getSha256());
        response.setCategory(attachment.getCategory());
        response.setUploadedBy(attachment.getUploadedBy());
        response.setUploadedAt(attachment.getUploadedAt());
        return response;
    }
}

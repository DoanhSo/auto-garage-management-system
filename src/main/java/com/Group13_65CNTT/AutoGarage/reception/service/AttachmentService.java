package com.Group13_65CNTT.AutoGarage.reception.service;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AttachmentService {

    AttachmentResponse uploadAttachment(Long repairOrderId, Long vehicleCheckId, Long warrantyClaimId,
                                       AttachmentCategory category, Long uploadedBy, MultipartFile file);

    AttachmentResponse getAttachmentById(Long id);

    List<AttachmentResponse> getAttachmentsByRepairOrder(Long repairOrderId);

    List<AttachmentResponse> getAttachmentsByVehicleCheck(Long vehicleCheckId);
}

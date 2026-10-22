package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.service.AttachmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @RequestParam(required = false) Long repairOrderId,
            @RequestParam(required = false) Long vehicleCheckId,
            @RequestParam(required = false) Long warrantyClaimId,
            @RequestParam AttachmentCategory category,
            @RequestParam(required = false, defaultValue = "1") Long uploadedBy,
            @RequestParam("file") MultipartFile file) {

        AttachmentResponse response = attachmentService.uploadAttachment(
                repairOrderId, vehicleCheckId, warrantyClaimId, category, uploadedBy, file
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttachmentResponse> getAttachmentById(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachmentById(id));
    }

    @GetMapping("/order/{repairOrderId}")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByRepairOrder(@PathVariable Long repairOrderId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByRepairOrder(repairOrderId));
    }

    @GetMapping("/check/{vehicleCheckId}")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByVehicleCheck(@PathVariable Long vehicleCheckId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByVehicleCheck(vehicleCheckId));
    }
}

package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.service.AttachmentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ATTACHMENTS_BASE)
@RequiredArgsConstructor
@Tag(name = "Attachments")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class AttachmentController {

    private final AttachmentService attachmentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/upload")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @RequestParam(required = false) Long repairOrderId,
            @RequestParam(required = false) Long vehicleCheckId,
            @RequestParam(required = false) Long warrantyClaimId,
            @RequestParam AttachmentCategory category,
            @RequestParam(required = false) Long uploadedBy,
            @RequestParam("file") MultipartFile file) {
        Long resolvedUploader =
                currentUserProvider.userIdOrNull() != null
                        ? currentUserProvider.userIdOrNull()
                        : (uploadedBy != null ? uploadedBy : 1L);
        AttachmentResponse response =
                attachmentService.uploadAttachment(
                        repairOrderId,
                        vehicleCheckId,
                        warrantyClaimId,
                        category,
                        resolvedUploader,
                        file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<AttachmentResponse> getAttachmentById(@PathVariable Long id) {
        return ResponseEntity.ok(attachmentService.getAttachmentById(id));
    }

    @GetMapping("/order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByRepairOrder(
            @PathVariable Long repairOrderId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByRepairOrder(repairOrderId));
    }

    @GetMapping("/check/{vehicleCheckId}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsByVehicleCheck(
            @PathVariable Long vehicleCheckId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsByVehicleCheck(vehicleCheckId));
    }
}

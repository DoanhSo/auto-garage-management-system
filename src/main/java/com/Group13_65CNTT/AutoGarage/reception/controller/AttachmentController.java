package com.Group13_65CNTT.AutoGarage.reception.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.reception.dto.AttachmentResponse;
import com.Group13_65CNTT.AutoGarage.reception.entity.AttachmentCategory;
import com.Group13_65CNTT.AutoGarage.reception.service.AttachmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.ATTACHMENTS_BASE)
@RequiredArgsConstructor
@Tag(name = "Attachment Management", description = "Vehicle condition photos, check sheet scans and repair documents")
@SecurityRequirement(name = "bearerAuth")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTIONIST_OR_TECHNICIAN_OR_ADMIN)
    @Operation(summary = "Upload repair or vehicle attachment")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @RequestParam(required = false) Long repairOrderId,
            @RequestParam(required = false) Long vehicleCheckId,
            @RequestParam(required = false) Long warrantyClaimId,
            @RequestParam AttachmentCategory category,
            @RequestParam(required = false, defaultValue = "1") Long uploadedBy,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {

        AttachmentResponse response = attachmentService.uploadAttachment(
                repairOrderId, vehicleCheckId, warrantyClaimId, category, uploadedBy, file
        );
        return ResponseEntity.created(URI.create(ApiPaths.ATTACHMENTS_BASE + "/" + response.getId()))
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch attachment metadata by ID")
    public ResponseEntity<ApiResponse<AttachmentResponse>> getAttachmentById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        AttachmentResponse response = attachmentService.getAttachmentById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch attachments by repair order ID")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachmentsByRepairOrder(
            @PathVariable Long repairOrderId,
            HttpServletRequest httpRequest) {
        List<AttachmentResponse> list = attachmentService.getAttachmentsByRepairOrder(repairOrderId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, list, httpRequest.getRequestURI()));
    }

    @GetMapping("/check/{vehicleCheckId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Fetch attachments by vehicle check ID")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getAttachmentsByVehicleCheck(
            @PathVariable Long vehicleCheckId,
            HttpServletRequest httpRequest) {
        List<AttachmentResponse> list = attachmentService.getAttachmentsByVehicleCheck(vehicleCheckId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, list, httpRequest.getRequestURI()));
    }
}

package com.Group13_65CNTT.AutoGarage.quality.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.quality.dto.request.QualityCheckCreateRequest;
import com.Group13_65CNTT.AutoGarage.quality.dto.response.QualityCheckResponse;
import com.Group13_65CNTT.AutoGarage.quality.service.QualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.QUALITY_CHECKS_BASE)
@RequiredArgsConstructor
@Tag(name = "Quality Checks", description = "Kiểm tra chất lượng (QC) và yêu cầu làm lại (TV3)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class QualityCheckController {

    private final QualityService qualityService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Thực hiện kiểm tra chất lượng (QC) cho phiếu sửa chữa")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> performQualityCheck(
            @Valid @RequestBody QualityCheckCreateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        QualityCheckResponse response = qualityService.performQualityCheck(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy thông tin chi tiết biên bản QC theo ID")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> getQualityCheckById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        QualityCheckResponse response = qualityService.getQualityCheckById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy tất cả các biên bản QC của phiếu sửa chữa theo thứ tự attempt")
    public ResponseEntity<ApiResponse<List<QualityCheckResponse>>> getQualityChecksByRepairOrderId(
            @PathVariable Long repairOrderId,
            HttpServletRequest httpRequest) {
        List<QualityCheckResponse> response = qualityService.getQualityChecksByRepairOrderId(repairOrderId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/order/{repairOrderId}/latest")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy biên bản QC mới nhất của phiếu sửa chữa")
    public ResponseEntity<ApiResponse<QualityCheckResponse>> getLatestQualityCheck(
            @PathVariable Long repairOrderId,
            HttpServletRequest httpRequest) {
        QualityCheckResponse response = qualityService.getLatestQualityCheck(repairOrderId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }
}

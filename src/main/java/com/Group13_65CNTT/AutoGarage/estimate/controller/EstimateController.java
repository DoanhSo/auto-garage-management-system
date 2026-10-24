package com.Group13_65CNTT.AutoGarage.estimate.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateCreateRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.request.EstimateDecisionRequest;
import com.Group13_65CNTT.AutoGarage.estimate.dto.response.EstimateResponse;
import com.Group13_65CNTT.AutoGarage.estimate.entity.EstimateStatus;
import com.Group13_65CNTT.AutoGarage.estimate.service.EstimateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ESTIMATES_BASE)
@RequiredArgsConstructor
@Tag(name = "Estimates", description = "Quản lý lập báo giá và phê duyệt từng dòng (TV3)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class EstimateController {

    private final EstimateService estimateService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Lập báo giá mới (INITIAL hoặc SUPPLEMENTAL)")
    public ResponseEntity<ApiResponse<EstimateResponse>> createEstimate(
            @Valid @RequestBody EstimateCreateRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        EstimateResponse response = estimateService.createEstimate(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Lấy thông tin chi tiết báo giá theo ID")
    public ResponseEntity<ApiResponse<EstimateResponse>> getEstimateById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        EstimateResponse response = estimateService.getEstimateById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/code/{estimateCode}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Lấy thông tin chi tiết báo giá theo mã báo giá")
    public ResponseEntity<ApiResponse<EstimateResponse>> getEstimateByCode(
            @PathVariable String estimateCode,
            HttpServletRequest httpRequest) {
        EstimateResponse response = estimateService.getEstimateByCode(estimateCode);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Tìm kiếm và phân trang danh sách báo giá")
    public ResponseEntity<ApiResponse<Page<EstimateResponse>>> searchEstimates(
            @RequestParam(required = false) Long repairOrderId,
            @RequestParam(required = false) EstimateStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<EstimateResponse> response = estimateService.searchEstimates(
                repairOrderId, status, page, size, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Lấy danh sách các bản báo giá của phiếu sửa chữa")
    public ResponseEntity<ApiResponse<List<EstimateResponse>>> getEstimatesByRepairOrderId(
            @PathVariable Long repairOrderId,
            HttpServletRequest httpRequest) {
        List<EstimateResponse> response = estimateService.getEstimatesByRepairOrderId(repairOrderId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/send")
    @PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)
    @Operation(summary = "Gửi báo giá cho khách hàng phê duyệt")
    public ResponseEntity<ApiResponse<EstimateResponse>> sendEstimate(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        EstimateResponse response = estimateService.sendEstimate(actorId, id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/{id}/decide")
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Khách hàng hoặc nhân viên phê duyệt từng dòng báo giá (I04)")
    public ResponseEntity<ApiResponse<EstimateResponse>> decideEstimate(
            @PathVariable Long id,
            @Valid @RequestBody EstimateDecisionRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userId();
        EstimateResponse response = estimateService.decideEstimate(actorId, id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }
}

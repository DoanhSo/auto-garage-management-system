package com.Group13_65CNTT.AutoGarage.warranty.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimDecideRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.request.WarrantyClaimOpenRequest;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyClaimResponse;
import com.Group13_65CNTT.AutoGarage.warranty.dto.response.WarrantyResponse;
import com.Group13_65CNTT.AutoGarage.warranty.service.WarrantyService;
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

@RestController
@RequestMapping(ApiPaths.WARRANTIES_BASE)
@RequiredArgsConstructor
@Tag(name = "Warranties", description = "Quản lý chính sách bảo hành và yêu cầu bảo hành (TV3)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class WarrantyController {

    private final WarrantyService warrantyService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy thông tin bảo hành theo ID")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarrantyById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        WarrantyResponse response = warrantyService.getWarrantyById(id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/code/{warrantyCode}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy thông tin bảo hành theo mã bảo hành")
    public ResponseEntity<ApiResponse<WarrantyResponse>> getWarrantyByCode(
            @PathVariable String warrantyCode,
            HttpServletRequest httpRequest) {
        WarrantyResponse response = warrantyService.getWarrantyByCode(warrantyCode);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/vehicle/{vehicleId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy danh sách chính sách bảo hành của xe")
    public ResponseEntity<ApiResponse<Page<WarrantyResponse>>> getWarrantiesByVehicleId(
            @PathVariable Long vehicleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Page<WarrantyResponse> response = warrantyService.getWarrantiesByVehicleId(vehicleId, page, size);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy danh sách chính sách bảo hành của khách hàng")
    public ResponseEntity<ApiResponse<Page<WarrantyResponse>>> getWarrantiesByCustomerId(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Page<WarrantyResponse> response = warrantyService.getWarrantiesByCustomerId(customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/claims")
    @PreAuthorize(SecurityConstants.HAS_FRONT_DESK)
    @Operation(summary = "Tiếp nhận và mở yêu cầu bảo hành")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> openClaim(
            @Valid @RequestBody WarrantyClaimOpenRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        WarrantyClaimResponse response = warrantyService.openClaim(actorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping("/claims/{claimId}/decide")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Phê duyệt hoặc từ chối yêu cầu bảo hành (Chỉ Quản trị viên)")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> decideClaim(
            @PathVariable Long claimId,
            @Valid @RequestBody WarrantyClaimDecideRequest request,
            HttpServletRequest httpRequest) {
        Long actorId = currentUserProvider.userIdOrNull();
        WarrantyClaimResponse response = warrantyService.decideClaim(actorId, claimId, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/claims/{claimId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy chi tiết yêu cầu bảo hành theo ID")
    public ResponseEntity<ApiResponse<WarrantyClaimResponse>> getClaimById(
            @PathVariable Long claimId,
            HttpServletRequest httpRequest) {
        WarrantyClaimResponse response = warrantyService.getClaimById(claimId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/{warrantyId}/claims")
    @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)
    @Operation(summary = "Lấy danh sách các lần claim của một chính sách bảo hành")
    public ResponseEntity<ApiResponse<Page<WarrantyClaimResponse>>> getClaimsByWarrantyId(
            @PathVariable Long warrantyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        Page<WarrantyClaimResponse> response = warrantyService.getClaimsByWarrantyId(warrantyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }
}

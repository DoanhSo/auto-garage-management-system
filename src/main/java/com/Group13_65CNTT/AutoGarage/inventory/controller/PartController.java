package com.Group13_65CNTT.AutoGarage.inventory.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.PartUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.PartResponse;
import com.Group13_65CNTT.AutoGarage.inventory.service.PartService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.PARTS_BASE)
@Tag(name = "Parts", description = "Quản lý danh mục phụ tùng vật tư")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class PartController {
    private final PartService partService;
    private final PaginationPolicy pagination;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Khai báo phụ tùng mới")
    public ApiResponse<PartResponse> create(
            @Valid @RequestBody PartCreateRequest request,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.CREATED,
                partService.createPart(request),
                http.getRequestURI());
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Tìm kiếm và phân trang danh mục phụ tùng")
    public ApiResponse<Page<PartResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "partName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                Set.of("id", "partName", "sku", "purchasePrice", "sellingPrice", "stockQuantity", "createdAt"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                partService.searchParts(query, supplierId, active, pageable),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.PARTS_LOW_STOCK)
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Cảnh báo phụ tùng dưới ngưỡng tồn kho an toàn")
    public ApiResponse<List<PartResponse>> getLowStock(HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                partService.getLowStockParts(),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
    @Operation(summary = "Xem chi tiết phụ tùng theo ID")
    public ApiResponse<PartResponse> getById(
            @PathVariable Long id,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                partService.getPartById(id),
                http.getRequestURI());
    }

    @PutMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Cập nhật thông tin phụ tùng (không đổi trực tiếp tồn)")
    public ApiResponse<PartResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PartUpdateRequest request,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                partService.updatePart(id, request),
                http.getRequestURI());
    }

    @DeleteMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Vô hiệu hóa phụ tùng")
    public ApiResponse<Void> deactivate(
            @PathVariable Long id,
            HttpServletRequest http) {
        partService.deactivatePart(id);
        return ApiResponse.success(ResponseCode.SUCCESS, null, http.getRequestURI());
    }
}

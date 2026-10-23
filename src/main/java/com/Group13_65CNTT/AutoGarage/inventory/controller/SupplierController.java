package com.Group13_65CNTT.AutoGarage.inventory.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.SupplierUpdateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.SupplierResponse;
import com.Group13_65CNTT.AutoGarage.inventory.service.SupplierService;

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

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.SUPPLIERS_BASE)
@Tag(name = "Suppliers", description = "Quản lý nhà cung cấp phụ tùng (UC20)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class SupplierController {
    private final SupplierService supplierService;
    private final PaginationPolicy pagination;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Khai báo nhà cung cấp mới")
    public ApiResponse<SupplierResponse> create(
            @Valid @RequestBody SupplierCreateRequest request,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.CREATED,
                supplierService.createSupplier(request),
                http.getRequestURI());
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Tìm kiếm và phân trang danh sách nhà cung cấp")
    public ApiResponse<Page<SupplierResponse>> search(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "supplierName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                Set.of("id", "supplierName", "supplierCode", "createdAt"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                supplierService.searchSuppliers(query, active, pageable),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Xem chi tiết nhà cung cấp theo ID")
    public ApiResponse<SupplierResponse> getById(
            @PathVariable Long id,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                supplierService.getSupplierById(id),
                http.getRequestURI());
    }

    @PutMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Cập nhật thông tin nhà cung cấp")
    public ApiResponse<SupplierResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierUpdateRequest request,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                supplierService.updateSupplier(id, request),
                http.getRequestURI());
    }

    @DeleteMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Vô hiệu hóa nhà cung cấp (xóa mềm bảo toàn lịch sử)")
    public ApiResponse<Void> deactivate(
            @PathVariable Long id,
            HttpServletRequest http) {
        supplierService.deactivateSupplier(id);
        return ApiResponse.success(ResponseCode.SUCCESS, null, http.getRequestURI());
    }
}

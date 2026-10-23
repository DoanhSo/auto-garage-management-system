package com.Group13_65CNTT.AutoGarage.inventory.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentCreateRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.request.InventoryDocumentPostRequest;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.InventoryDocumentResponse;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentStatus;
import com.Group13_65CNTT.AutoGarage.inventory.entity.DocumentType;
import com.Group13_65CNTT.AutoGarage.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.INVENTORY_DOCUMENTS_BASE)
@Tag(name = "Inventory Documents", description = "Quản lý chứng từ nhập/xuất/trả/điều chỉnh kho (UC22, UC24, UC25, UC26)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class InventoryDocumentController {
    private final InventoryService inventoryService;
    private final CurrentUserProvider currentUser;
    private final PaginationPolicy pagination;

    @PostMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Lập chứng từ kho mới (trạng thái DRAFT)")
    public ApiResponse<InventoryDocumentResponse> create(
            @Valid @RequestBody InventoryDocumentCreateRequest request,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        return ApiResponse.success(
                ResponseCode.CREATED,
                inventoryService.createDocument(actorId, request),
                http.getRequestURI());
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Tìm kiếm và phân trang chứng từ kho")
    public ApiResponse<Page<InventoryDocumentResponse>> search(
            @RequestParam(required = false) DocumentType type,
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                java.util.Set.of("createdAt", "documentCode", "totalAmount"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                inventoryService.searchDocuments(type, status, supplierId, orderId, query, pageable),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Xem chi tiết chứng từ kho theo ID")
    public ApiResponse<InventoryDocumentResponse> getById(
            @PathVariable Long id,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                inventoryService.getDocumentById(id),
                http.getRequestURI());
    }

    @PostMapping(ApiPaths.INVENTORY_DOCUMENT_POST)
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Ghi sổ chứng từ kho (POSTED: cập nhật tồn, giá vốn, sổ kho nguyên tử)")
    public ApiResponse<Void> postDocument(
            @PathVariable Long id,
            @RequestBody(required = false) InventoryDocumentPostRequest postRequest,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        String requestKey = postRequest != null ? postRequest.getRequestKey() : null;
        inventoryService.post(actorId, id, requestKey);
        return ApiResponse.success(ResponseCode.SUCCESS, null, http.getRequestURI());
    }

    @PostMapping(ApiPaths.INVENTORY_DOCUMENT_CANCEL)
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Hủy chứng từ kho đang ở trạng thái DRAFT")
    public ApiResponse<Void> cancelDocument(
            @PathVariable Long id,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        inventoryService.cancelDocument(actorId, id);
        return ApiResponse.success(ResponseCode.SUCCESS, null, http.getRequestURI());
    }
}

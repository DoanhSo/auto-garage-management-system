package com.Group13_65CNTT.AutoGarage.inventory.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.inventory.dto.response.StockReconciliationResponse;
import com.Group13_65CNTT.AutoGarage.inventory.service.InventoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.INVENTORY_RECONCILIATION_BASE)
@Tag(name = "Stock Reconciliation", description = "Đối soát số tồn thực tế và sổ kho (TV4-09 / UC38)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class StockReconciliationController {
    private final InventoryService inventoryService;

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Đối soát toàn bộ danh mục phụ tùng giữa số tồn và sổ kho")
    public ApiResponse<List<StockReconciliationResponse>> reconcileAll(HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                inventoryService.reconcileAllParts(),
                http.getRequestURI());
    }

    @GetMapping({ApiPaths.BY_ID, "/parts/{id}"})
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Đối soát số tồn và sổ kho cho một phụ tùng")
    public ApiResponse<StockReconciliationResponse> reconcilePart(
            @PathVariable Long id,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                inventoryService.reconcilePartStock(id),
                http.getRequestURI());
    }
}

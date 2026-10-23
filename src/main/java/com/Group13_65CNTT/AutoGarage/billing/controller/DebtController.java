package com.Group13_65CNTT.AutoGarage.billing.controller;

import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.SupplierBalanceResponse;
import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.DEBTS_BASE)
@Tag(name = "Debts", description = "Sổ theo dõi công nợ khách hàng và nhà cung cấp (v_invoice_balances, v_supplier_balances)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class DebtController {
    private final BillingService billingService;
    private final PaginationPolicy pagination;

    @GetMapping(ApiPaths.DEBTS_INVOICE_BALANCES)
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTION_OR_ADMIN)
    @Operation(summary = "Sổ theo dõi công nợ khách hàng theo hóa đơn (v_invoice_balances)")
    public ApiResponse<Page<InvoiceBalanceResponse>> getCustomerDebts(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Boolean hasBalance,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "issuedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                Set.of("issuedAt", "invoiceCode", "totalAmount", "balanceAmount"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.getCustomerDebtLedger(customerId, hasBalance, query, pageable),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.DEBTS_SUPPLIER_BALANCES)
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Sổ theo dõi công nợ nhà cung cấp (v_supplier_balances)")
    public ApiResponse<Page<SupplierBalanceResponse>> getSupplierDebts(
            @RequestParam(required = false) Boolean hasBalance,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "supplierName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                Set.of("supplierName", "supplierCode", "purchaseAmount", "balanceAmount"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.getSupplierDebtLedger(hasBalance, query, pageable),
                http.getRequestURI());
    }
}

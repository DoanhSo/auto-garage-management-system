package com.Group13_65CNTT.AutoGarage.billing.controller;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceIssueRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.InvoiceVoidRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.InvoiceResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus;
import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.INVOICES_BASE)
@Tag(name = "Invoices", description = "Quản lý hóa đơn dịch vụ và phụ tùng (UC28)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class InvoiceController {
    private final BillingService billingService;
    private final CurrentUserProvider currentUser;
    private final PaginationPolicy pagination;

    @PostMapping(ApiPaths.INVOICES_ISSUE)
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTION_OR_ADMIN)
    @Operation(summary = "Lập và phát hành hóa đơn cho phiếu sửa chữa (chụp snapshot bất biến)")
    public ApiResponse<InvoiceResponse> issueInvoice(
            @Valid @RequestBody InvoiceIssueRequest request,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        return ApiResponse.success(
                ResponseCode.CREATED,
                billingService.issueInvoice(actorId, request),
                http.getRequestURI());
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Tìm kiếm và phân trang hóa đơn")
    public ApiResponse<Page<InvoiceResponse>> search(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "issuedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                Set.of("id", "issuedAt", "invoiceCode", "totalAmount"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.searchInvoices(status, customerId, orderId, query, pageable),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.BY_ID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Xem chi tiết hóa đơn theo ID")
    public ApiResponse<InvoiceResponse> getById(
            @PathVariable Long id,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.getInvoiceById(id),
                http.getRequestURI());
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Xem hóa đơn theo ID phiếu sửa chữa")
    public ApiResponse<InvoiceResponse> getByOrderId(
            @PathVariable Long orderId,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.getInvoiceByOrderId(orderId),
                http.getRequestURI());
    }

    @PostMapping(ApiPaths.INVOICES_VOID)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Hủy hóa đơn chưa thanh toán (chỉ ADMIN)")
    public ApiResponse<Void> voidInvoice(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceVoidRequest request,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        billingService.voidInvoice(actorId, id, request.getVoidReason());
        return ApiResponse.success(ResponseCode.SUCCESS, null, http.getRequestURI());
    }
}

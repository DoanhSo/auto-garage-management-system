package com.Group13_65CNTT.AutoGarage.billing.controller;

import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.SupplierPaymentRequest;
import com.Group13_65CNTT.AutoGarage.billing.dto.response.PaymentResponse;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
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

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.PAYMENTS_BASE)
@Tag(name = "Payments", description = "Quản lý thu tiền khách và chi trả NCC (UC29, UC30)")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class PaymentController {
    private final BillingService billingService;
    private final CurrentUserProvider currentUser;
    private final PaginationPolicy pagination;

    @PostMapping(ApiPaths.PAYMENTS_RECEIVE)
    @PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTION_OR_ADMIN)
    @Operation(summary = "Thu tiền khách hàng theo hóa đơn (direction = IN, UC29)")
    public ApiResponse<PaymentResponse> receivePayment(
            @Valid @RequestBody PaymentReceiveRequest request,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        return ApiResponse.success(
                ResponseCode.CREATED,
                billingService.receivePayment(actorId, request),
                http.getRequestURI());
    }

    @PostMapping(ApiPaths.PAYMENTS_PAY_SUPPLIER)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Chi trả tiền cho nhà cung cấp theo chứng từ nhập kho (direction = OUT, UC30, chỉ ADMIN)")
    public ApiResponse<PaymentResponse> paySupplier(
            @Valid @RequestBody SupplierPaymentRequest request,
            HttpServletRequest http) {
        long actorId = currentUser.userId();
        return ApiResponse.success(
                ResponseCode.CREATED,
                billingService.paySupplier(actorId, request),
                http.getRequestURI());
    }

    @GetMapping
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Tìm kiếm và phân trang lịch sử giao dịch thu/chi")
    public ApiResponse<Page<PaymentResponse>> search(
            @RequestParam(required = false) PaymentDirection direction,
            @RequestParam(required = false) Long invoiceId,
            @RequestParam(required = false) Long documentId,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1) int size,
            @RequestParam(defaultValue = "paidAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest http) {
        Pageable pageable = pagination.page(
                page, size, sortBy, sortDir,
                Set.of("id", "paidAt", "amount", "transactionCode"));
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.searchPayments(direction, invoiceId, documentId, query, pageable),
                http.getRequestURI());
    }

    @GetMapping("/invoice/{invoiceId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_STAFF)
    @Operation(summary = "Xem danh sách các lần thanh toán của hóa đơn")
    public ApiResponse<List<PaymentResponse>> getForInvoice(
            @PathVariable Long invoiceId,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.getPaymentsForInvoice(invoiceId),
                http.getRequestURI());
    }

    @GetMapping("/document/{documentId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)
    @Operation(summary = "Xem danh sách các lần chi trả của chứng từ nhập mua")
    public ApiResponse<List<PaymentResponse>> getForDocument(
            @PathVariable Long documentId,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                billingService.getPaymentsForDocument(documentId),
                http.getRequestURI());
    }
}

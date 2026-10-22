package com.Group13_65CNTT.AutoGarage.reporting.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.ReportConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.constant.UserQueryConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.reporting.dto.response.DashboardResponse;
import com.Group13_65CNTT.AutoGarage.reporting.dto.response.InvoiceBalanceResponse;
import com.Group13_65CNTT.AutoGarage.reporting.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.REPORTS_BASE)
@Tag(name = "Reports")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class ReportController {
    private final PaginationPolicy pagination;
    private final ReportService reportService;

    @GetMapping(ApiPaths.REPORTS_DASHBOARD)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Revenue/cash flow in [from,until); current debt and inventory snapshot")
    public ApiResponse<DashboardResponse> dashboard(
            @RequestParam Instant from, @RequestParam Instant until, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, reportService.dashboard(from, until), http.getRequestURI());
    }

    @GetMapping(ApiPaths.REPORTS_OUTSTANDING_INVOICES)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Paginate outstanding invoices")
    public ApiResponse<Page<InvoiceBalanceResponse>> outstanding(
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = UserQueryConstants.DEFAULT_SIZE_EXPRESSION) @Min(1)
                    int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                reportService.outstandingInvoices(pagination.page(page, size, Sort.unsorted())),
                http.getRequestURI());
    }

    @GetMapping(ApiPaths.REPORTS_DASHBOARD_EXPORT)
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Export dashboard as UTF-8 CSV")
    public ResponseEntity<byte[]> export(@RequestParam Instant from, @RequestParam Instant until) {
        return ResponseEntity.ok()
                .header(ReportConstants.HEADER_CONTENT_TYPE, ReportConstants.CSV_CONTENT_TYPE)
                .header(
                        ReportConstants.HEADER_CONTENT_DISPOSITION,
                        ReportConstants.DASHBOARD_CSV_DISPOSITION)
                .body(reportService.exportDashboard(from, until));
    }
}

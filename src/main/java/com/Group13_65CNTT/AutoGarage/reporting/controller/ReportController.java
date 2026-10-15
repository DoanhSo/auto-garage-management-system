package com.Group13_65CNTT.AutoGarage.reporting.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.reporting.dto.response.*;
import com.Group13_65CNTT.AutoGarage.reporting.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.*;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPaths.REPORTS_BASE)
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {
    private final PaginationPolicy pagination;
    private final ReportService reportService;

    @GetMapping("/dashboard")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Revenue/cash flow in [from,until); current debt and inventory snapshot")
    public ApiResponse<DashboardResponse> dashboard(
            @RequestParam Instant from, @RequestParam Instant until, HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS, reportService.dashboard(from, until), http.getRequestURI());
    }

    @GetMapping("/outstanding-invoices")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Paginate outstanding invoices")
    public ApiResponse<Page<InvoiceBalanceResponse>> outstanding(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "${garage.pagination.default-size:20}") @Min(1) int size,
            HttpServletRequest http) {
        return ApiResponse.success(
                ResponseCode.SUCCESS,
                reportService.outstandingInvoices(pagination.page(page, size, Sort.unsorted())),
                http.getRequestURI());
    }

    @GetMapping("/dashboard/export")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Export dashboard as UTF-8 CSV")
    public ResponseEntity<byte[]> export(@RequestParam Instant from, @RequestParam Instant until) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv;charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=garage-dashboard.csv")
                .body(reportService.exportDashboard(from, until));
    }
}

package com.Group13_65CNTT.AutoGarage.payroll.controller;

import com.Group13_65CNTT.AutoGarage.common.constant.ApiPaths;
import com.Group13_65CNTT.AutoGarage.common.constant.SecurityConstants;
import com.Group13_65CNTT.AutoGarage.common.dto.ApiResponse;
import com.Group13_65CNTT.AutoGarage.common.enums.ResponseCode;
import com.Group13_65CNTT.AutoGarage.common.security.CurrentUserProvider;
import com.Group13_65CNTT.AutoGarage.common.util.PaginationPolicy;
import com.Group13_65CNTT.AutoGarage.payroll.constant.PayrollConstants;
import com.Group13_65CNTT.AutoGarage.repair.dto.response.EmployeeWorkSummaryResponse;
import com.Group13_65CNTT.AutoGarage.repair.service.WorkHistoryService;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.CommissionStatusUpdateRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.PayrollItemAdjustRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.PayrollPeriodCreateRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.OrderCommissionResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollItemResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodDetailResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayslipResponse;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import com.Group13_65CNTT.AutoGarage.payroll.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.PAYROLL_BASE)
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Kỳ tính lương, phiếu lương và hoa hồng theo đơn hoàn thành")
@SecurityRequirement(name = SecurityConstants.OPENAPI_SECURITY_SCHEME_NAME)
public class PayrollController {

    private final PayrollService payrollService;
    private final PaginationPolicy pagination;
    private final CurrentUserProvider currentUserProvider;
    private final WorkHistoryService workHistoryService;

    // ----- Commissions -----

    @GetMapping(ApiPaths.PAYROLL_COMMISSIONS)
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Tìm kiếm hoa hồng và thưởng đơn hàng")
    public ResponseEntity<ApiResponse<Page<OrderCommissionResponse>>> searchCommissions(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long repairOrderId,
            @RequestParam(required = false) CommissionStatus status,
            @RequestParam(required = false) Instant fromEarnedAt,
            @RequestParam(required = false) Instant toEarnedAt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "earnedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<OrderCommissionResponse> response = payrollService.searchCommissions(
                employeeId, repairOrderId, status, fromEarnedAt, toEarnedAt,
                pagination.page(page, size != null ? size : pagination.defaultSize(), sortBy, sortDir,
                        PayrollConstants.COMMISSION_SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PostMapping(ApiPaths.PAYROLL_COMMISSIONS + "/calculate-order/{repairOrderId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Tính thưởng cho một lệnh sửa chữa đã hoàn thành")
    public ResponseEntity<ApiResponse<List<OrderCommissionResponse>>> calculateOrderCommissions(
            @PathVariable Long repairOrderId, HttpServletRequest httpRequest) {
        List<OrderCommissionResponse> response =
                payrollService.calculateOrderCommissions(currentUserProvider.userId(), repairOrderId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @PutMapping(ApiPaths.PAYROLL_COMMISSIONS + "/{id}/status")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Duyệt hoặc hủy một khoản hoa hồng (Chỉ Quản trị viên)")
    public ResponseEntity<ApiResponse<OrderCommissionResponse>> updateCommissionStatus(
            @PathVariable Long id,
            @Valid @RequestBody CommissionStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        OrderCommissionResponse response =
                payrollService.updateCommissionStatus(currentUserProvider.userId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    // ----- Periods -----

    @PostMapping(ApiPaths.PAYROLL_PERIODS)
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Khởi tạo kỳ tính lương")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> createPeriod(
            @Valid @RequestBody PayrollPeriodCreateRequest request, HttpServletRequest httpRequest) {
        PayrollPeriodResponse response = payrollService.createPeriod(currentUserProvider.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(ResponseCode.CREATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping(ApiPaths.PAYROLL_PERIODS)
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Danh sách kỳ tính lương")
    public ResponseEntity<ApiResponse<Page<PayrollPeriodResponse>>> listPeriods(
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            @RequestParam(defaultValue = "periodMonth") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest httpRequest) {
        Page<PayrollPeriodResponse> response = payrollService.listPeriods(
                year,
                pagination.page(page, size != null ? size : pagination.defaultSize(), sortBy, sortDir,
                        PayrollConstants.PERIOD_SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }

    @GetMapping(ApiPaths.PAYROLL_PERIODS + "/{id}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Chi tiết kỳ tính lương kèm phiếu lương từng nhân viên")
    public ResponseEntity<ApiResponse<PayrollPeriodDetailResponse>> getPeriod(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SUCCESS, payrollService.getPeriod(id), httpRequest.getRequestURI()));
    }

    @PostMapping(ApiPaths.PAYROLL_PERIODS + "/{id}/calculate")
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Tính toán tự động giờ công và thưởng đơn cho kỳ lương")
    public ResponseEntity<ApiResponse<PayrollPeriodDetailResponse>> calculatePeriod(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        PayrollPeriodDetailResponse response = payrollService.calculatePeriod(currentUserProvider.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PutMapping(ApiPaths.PAYROLL_PERIODS + "/{id}/items/{itemId}")
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Chỉnh phụ cấp, giảm trừ và ghi chú của một phiếu lương")
    public ResponseEntity<ApiResponse<PayrollItemResponse>> adjustItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody PayrollItemAdjustRequest request,
            HttpServletRequest httpRequest) {
        PayrollItemResponse response = payrollService.adjustItem(currentUserProvider.userId(), id, itemId, request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping(ApiPaths.PAYROLL_PERIODS + "/{id}/approve")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Phê duyệt bảng lương (Chỉ Quản trị viên)")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> approvePeriod(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        PayrollPeriodResponse response = payrollService.approvePeriod(currentUserProvider.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @PostMapping(ApiPaths.PAYROLL_PERIODS + "/{id}/disburse")
    @PreAuthorize(SecurityConstants.HAS_ROLE_ADMIN)
    @Operation(summary = "Chi trả lương và hạch toán phiếu chi vào sổ quỹ (Chỉ Quản trị viên)")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> disbursePeriod(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        PayrollPeriodResponse response = payrollService.disbursePeriod(currentUserProvider.userId(), id);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.UPDATED, response, httpRequest.getRequestURI()));
    }

    @GetMapping("/work-summary")
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_MANAGER)
    @Operation(summary = "Tổng hợp xe và ca làm việc của một kỹ thuật viên trong tháng để đối soát lương")
    public ResponseEntity<ApiResponse<EmployeeWorkSummaryResponse>> getWorkSummary(
            @RequestParam Long employeeId,
            @RequestParam @jakarta.validation.constraints.Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                ResponseCode.SUCCESS,
                workHistoryService.getEmployeeWorkSummary(employeeId, month),
                httpRequest.getRequestURI()));
    }

    // ----- Payslips -----

    @GetMapping(ApiPaths.PAYROLL_MY_PAYSLIPS)
    @PreAuthorize(SecurityConstants.HAS_ROLE_PAYROLL_VIEW)
    @Operation(summary = "Phiếu lương của chính người dùng đăng nhập")
    public ResponseEntity<ApiResponse<Page<PayslipResponse>>> getMyPayslips(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) Integer size,
            HttpServletRequest httpRequest) {
        Page<PayslipResponse> response = payrollService.getMyPayslips(
                currentUserProvider.userId(),
                pagination.page(page, size != null ? size : pagination.defaultSize(), "id", "desc",
                        PayrollConstants.PAYSLIP_SORT_FIELDS));
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response, httpRequest.getRequestURI()));
    }
}

package com.Group13_65CNTT.AutoGarage.payroll.service;

import com.Group13_65CNTT.AutoGarage.payroll.dto.request.CommissionStatusUpdateRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.PayrollItemAdjustRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.request.PayrollPeriodCreateRequest;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.OrderCommissionResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollItemResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodDetailResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayslipResponse;
import com.Group13_65CNTT.AutoGarage.payroll.entity.CommissionStatus;
import com.Group13_65CNTT.AutoGarage.shared.contracts.PayrollOperations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

public interface PayrollService extends PayrollOperations {

    PayrollPeriodResponse createPeriod(long actorUserId, PayrollPeriodCreateRequest request);

    Page<PayrollPeriodResponse> listPeriods(Integer year, Pageable pageable);

    PayrollPeriodDetailResponse getPeriod(Long periodId);

    /** Period of the given YYYY-MM month with its payslips, or null when it has not been created yet. */
    PayrollPeriodDetailResponse getPeriodByMonth(String periodMonth);

    PayrollPeriodDetailResponse calculatePeriod(long actorUserId, Long periodId);

    PayrollItemResponse adjustItem(long actorUserId, Long periodId, Long itemId, PayrollItemAdjustRequest request);

    PayrollPeriodResponse approvePeriod(long actorUserId, Long periodId);

    PayrollPeriodResponse disbursePeriod(long actorUserId, Long periodId);

    Page<PayslipResponse> getMyPayslips(long actorUserId, Pageable pageable);

    Page<OrderCommissionResponse> searchCommissions(
            Long employeeId,
            Long repairOrderId,
            CommissionStatus status,
            Instant fromEarnedAt,
            Instant toEarnedAt,
            Pageable pageable);

    List<OrderCommissionResponse> calculateOrderCommissions(long actorUserId, Long repairOrderId);

    OrderCommissionResponse updateCommissionStatus(long actorUserId, Long commissionId, CommissionStatusUpdateRequest request);
}

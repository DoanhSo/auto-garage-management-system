package com.Group13_65CNTT.AutoGarage.payroll.mapper;

import com.Group13_65CNTT.AutoGarage.payroll.dto.response.OrderCommissionResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollItemResponse;
import com.Group13_65CNTT.AutoGarage.payroll.dto.response.PayrollPeriodResponse;
import com.Group13_65CNTT.AutoGarage.payroll.entity.OrderCommission;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollItem;
import com.Group13_65CNTT.AutoGarage.payroll.entity.PayrollPeriod;
import org.springframework.stereotype.Component;

@Component
public class PayrollMapper {

    public PayrollPeriodResponse toResponse(PayrollPeriod period, long completedOrderCount) {
        return new PayrollPeriodResponse(
                period.getId(),
                period.getPeriodCode(),
                period.getPeriodMonth(),
                period.getFromDate(),
                period.getToDate(),
                period.getStatus(),
                period.getTotalEmployees(),
                period.getTotalRegularHours(),
                period.getTotalOvertimeHours(),
                period.getTotalBasePay(),
                period.getTotalCommissions(),
                period.getTotalAllowances(),
                period.getTotalDeductions(),
                period.getTotalNetPay(),
                (int) completedOrderCount,
                period.getApprovedBy(),
                period.getApprovedAt(),
                period.getPaymentId(),
                period.getNotes(),
                period.getCreatedAt(),
                period.getVersion());
    }

    public PayrollItemResponse toResponse(PayrollItem item, String employeeCode, String employeeName) {
        return new PayrollItemResponse(
                item.getId(),
                item.getPayrollPeriodId(),
                item.getEmployeeId(),
                employeeCode,
                employeeName,
                item.getBaseSalarySnapshot(),
                item.getHourlyRateSnapshot(),
                item.getRegularHours(),
                item.getOvertimeHours(),
                item.getShiftCount(),
                item.getCompletedOrderCount(),
                item.getHourlyPayAmount(),
                item.getCommissionAmount(),
                item.getAllowanceAmount(),
                item.getDeductionAmount(),
                item.getGrossPay(),
                item.getNetPay(),
                item.getStatus(),
                item.getPaidAt(),
                item.getNotes());
    }

    public OrderCommissionResponse toResponse(OrderCommission commission) {
        return new OrderCommissionResponse(
                commission.getId(),
                commission.getRepairOrderId(),
                commission.getTaskId(),
                commission.getEmployeeId(),
                commission.getCommissionType(),
                commission.getBaseAmount(),
                commission.getRatePercent(),
                commission.getCommissionAmount(),
                commission.getStatus(),
                commission.getEarnedAt(),
                commission.getPayrollItemId(),
                commission.getNotes());
    }
}

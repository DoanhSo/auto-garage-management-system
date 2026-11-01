import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { payrollApi } from '../api/payrollApi';
import type { PayrollItemAdjustInput, PayrollPeriodCreateInput } from '../types/payroll';

export function usePayrollPeriodsQuery(params?: { year?: number; page?: number; size?: number }) {
  return useQuery({
    queryKey: ['payroll-periods', params],
    queryFn: () => payrollApi.listPeriods(params),
  });
}

export function usePayrollPeriodQuery(id: string | number | null | undefined) {
  return useQuery({
    queryKey: ['payroll-period', id],
    queryFn: () => payrollApi.getPeriod(id as string | number),
    enabled: !!id,
  });
}

export function usePayrollPeriodByMonthQuery(month: string) {
  return useQuery({
    queryKey: ['payroll-period', 'by-month', month],
    queryFn: () => payrollApi.getPeriodByMonth(month),
  });
}

function useInvalidatePayroll() {
  const queryClient = useQueryClient();
  return () => {
    queryClient.invalidateQueries({ queryKey: ['payroll-periods'] });
    queryClient.invalidateQueries({ queryKey: ['payroll-period'] });
    queryClient.invalidateQueries({ queryKey: ['payments'] });
  };
}

export function useCreatePayrollPeriodMutation() {
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: (data: PayrollPeriodCreateInput) => payrollApi.createPeriod(data),
    onSuccess: invalidate,
  });
}

export function useCalculatePayrollMutation() {
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: (id: string | number) => payrollApi.calculatePeriod(id),
    onSuccess: invalidate,
  });
}

export function useAdjustPayrollItemMutation() {
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: ({
      periodId,
      itemId,
      data,
    }: {
      periodId: string | number;
      itemId: string | number;
      data: PayrollItemAdjustInput;
    }) => payrollApi.adjustItem(periodId, itemId, data),
    onSuccess: invalidate,
  });
}

export function useApprovePayrollMutation() {
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: (id: string | number) => payrollApi.approvePeriod(id),
    onSuccess: invalidate,
  });
}

export function useDisbursePayrollMutation() {
  const invalidate = useInvalidatePayroll();
  return useMutation({
    mutationFn: (id: string | number) => payrollApi.disbursePeriod(id),
    onSuccess: invalidate,
  });
}

export function useEmployeeWorkSummaryQuery(employeeId: string | null, month: string) {
  return useQuery({
    queryKey: ['payroll-work-summary', employeeId, month],
    queryFn: () => payrollApi.getWorkSummary(employeeId as string, month),
    enabled: !!employeeId,
  });
}

export function useMyPayslipsQuery(enabled = true) {
  return useQuery({
    queryKey: ['payroll-my-payslips'],
    queryFn: () => payrollApi.getMyPayslips(),
    enabled,
  });
}

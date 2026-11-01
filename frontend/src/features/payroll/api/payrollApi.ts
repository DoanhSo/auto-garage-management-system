import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  CommissionStatus,
  EmployeeWorkSummary,
  OrderCommission,
  Payslip,
  PayrollItem,
  PayrollItemAdjustInput,
  PayrollPeriod,
  PayrollPeriodCreateInput,
  PayrollPeriodDetail,
} from '../types/payroll';

export const payrollApi = {
  listPeriods: (params?: { year?: number; page?: number; size?: number }) =>
    request<Page<PayrollPeriod>>(
      '/payroll/periods' +
        queryString({
          year: params?.year,
          page: params?.page ?? 0,
          size: params?.size ?? 20,
          sortBy: 'periodMonth',
          sortDir: 'desc',
        }),
    ),

  getPeriod: (id: string | number) => request<PayrollPeriodDetail>(`/payroll/periods/${id}`),

  createPeriod: (data: PayrollPeriodCreateInput) =>
    request<PayrollPeriod>('/payroll/periods', { method: 'POST', body: data }),

  calculatePeriod: (id: string | number) =>
    request<PayrollPeriodDetail>(`/payroll/periods/${id}/calculate`, { method: 'POST' }),

  adjustItem: (periodId: string | number, itemId: string | number, data: PayrollItemAdjustInput) =>
    request<PayrollItem>(`/payroll/periods/${periodId}/items/${itemId}`, {
      method: 'PUT',
      body: data,
    }),

  approvePeriod: (id: string | number) =>
    request<PayrollPeriod>(`/payroll/periods/${id}/approve`, { method: 'POST' }),

  disbursePeriod: (id: string | number) =>
    request<PayrollPeriod>(`/payroll/periods/${id}/disburse`, { method: 'POST' }),

  getWorkSummary: (employeeId: string | number, month: string) =>
    request<EmployeeWorkSummary>('/payroll/work-summary' + queryString({ employeeId, month })),

  getMyPayslips: (params?: { page?: number; size?: number }) =>
    request<Page<Payslip>>(
      '/payroll/my-payslips' + queryString({ page: params?.page ?? 0, size: params?.size ?? 12 }),
    ),

  listCommissions: (params: {
    employeeId?: string | number;
    repairOrderId?: string | number;
    status?: CommissionStatus;
    page?: number;
    size?: number;
  }) =>
    request<Page<OrderCommission>>(
      '/payroll/commissions' +
        queryString({
          employeeId: params.employeeId,
          repairOrderId: params.repairOrderId,
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
        }),
    ),

  updateCommissionStatus: (id: string | number, status: CommissionStatus) =>
    request<OrderCommission>(`/payroll/commissions/${id}/status`, {
      method: 'PUT',
      body: { status },
    }),
};

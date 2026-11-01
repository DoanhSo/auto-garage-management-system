import type { VehicleWorkHistoryEntry } from '../../vehicle/types/workHistory';

export type PayrollStatus = 'DRAFT' | 'CALCULATED' | 'APPROVED' | 'PAID';
export type PayrollItemStatus = 'CALCULATED' | 'CONFIRMED' | 'PAID';
export type CommissionType =
  'ORDER_COMPLETION' | 'LABOR_SHARE' | 'FAST_TURNAROUND' | 'QC_EXCELLENCE';
export type CommissionStatus = 'PENDING' | 'APPROVED' | 'PAID_OUT' | 'CANCELLED';

export const payrollStatusLabels: Record<PayrollStatus, string> = {
  DRAFT: 'Nháp',
  CALCULATED: 'Đã tính',
  APPROVED: 'Đã duyệt',
  PAID: 'Đã chi trả',
};

export const payrollItemStatusLabels: Record<PayrollItemStatus, string> = {
  CALCULATED: 'Đã tính',
  CONFIRMED: 'Đã xác nhận',
  PAID: 'Đã chi trả',
};

export const commissionTypeLabels: Record<CommissionType, string> = {
  ORDER_COMPLETION: 'Thưởng hoàn thành lệnh',
  LABOR_SHARE: 'Hoa hồng tiền công',
  FAST_TURNAROUND: 'Thưởng vượt tiến độ',
  QC_EXCELLENCE: 'Thưởng kiểm định QC',
};

export const commissionStatusLabels: Record<CommissionStatus, string> = {
  PENDING: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  PAID_OUT: 'Đã chi',
  CANCELLED: 'Đã hủy',
};

export interface PayrollPeriod {
  id: string;
  periodCode: string;
  periodMonth: string;
  fromDate: string;
  toDate: string;
  status: PayrollStatus;
  totalEmployees: number;
  totalRegularHours: string | number;
  totalOvertimeHours: string | number;
  totalBasePay: string | number;
  totalCommissions: string | number;
  totalAllowances: string | number;
  totalDeductions: string | number;
  totalNetPay: string | number;
  completedOrderCount: number;
  approvedBy?: string | null;
  approvedAt?: string | null;
  paymentId?: string | null;
  notes?: string | null;
  createdAt: string;
  version?: string | number;
}

export interface PayrollItem {
  id: string;
  payrollPeriodId: string;
  employeeId: string;
  employeeCode?: string | null;
  employeeName?: string | null;
  baseSalarySnapshot: string | number;
  hourlyRateSnapshot: string | number;
  regularHours: string | number;
  overtimeHours: string | number;
  shiftCount: number;
  completedOrderCount: number;
  hourlyPayAmount: string | number;
  commissionAmount: string | number;
  allowanceAmount: string | number;
  deductionAmount: string | number;
  grossPay: string | number;
  netPay: string | number;
  status: PayrollItemStatus;
  paidAt?: string | null;
  notes?: string | null;
}

export interface PayrollPeriodDetail {
  period: PayrollPeriod;
  items: PayrollItem[];
}

export interface OrderCommission {
  id: string;
  repairOrderId: string;
  taskId?: string | null;
  employeeId: string;
  commissionType: CommissionType;
  baseAmount: string | number;
  ratePercent: string | number;
  commissionAmount: string | number;
  status: CommissionStatus;
  earnedAt: string;
  payrollItemId?: string | null;
  notes?: string | null;
}

export interface Payslip {
  item: PayrollItem;
  periodCode: string;
  periodMonth: string;
  fromDate: string;
  toDate: string;
  periodStatus: PayrollStatus;
  laborDays: { date: string; regularHours: string | number; overtimeHours: string | number }[];
  commissions: OrderCommission[];
}

export interface EmployeeWorkSummary {
  employeeId: string;
  month: string;
  totalMinutes: number;
  orderCount: number;
  passedOrderCount: number;
  entries: VehicleWorkHistoryEntry[];
}

export interface PayrollPeriodCreateInput {
  periodMonth: string;
  fromDate?: string;
  toDate?: string;
  notes?: string;
}

export interface PayrollItemAdjustInput {
  allowanceAmount?: number;
  deductionAmount?: number;
  notes?: string;
}

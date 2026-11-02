import type { VehicleWorkHistoryEntry } from '../../vehicle/types/workHistory';

export type PayrollStatus = 'DRAFT' | 'CALCULATED' | 'APPROVED' | 'PAID';
export type PayrollItemStatus = 'CALCULATED' | 'CONFIRMED' | 'PAID';
export type CommissionType =
  | 'ORDER_COMPLETION'
  | 'LABOR_SHARE'
  | 'FAST_TURNAROUND'
  | 'QC_EXCELLENCE'
  | 'TIME_SAVED_EFFICIENCY'
  | 'WARRANTY_DEDUCTION';
export type CommissionStatus = 'PENDING' | 'APPROVED' | 'PAID_OUT' | 'CANCELLED';

export const periodStatusVariant: Record<
  PayrollStatus,
  'secondary' | 'warning' | 'primary' | 'success'
> = {
  DRAFT: 'secondary',
  CALCULATED: 'warning',
  APPROVED: 'primary',
  PAID: 'success',
};

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
  TIME_SAVED_EFFICIENCY: 'Thưởng tiết kiệm giờ',
  WARRANTY_DEDUCTION: 'Khấu trừ lỗi bảo hành',
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
  orderBonusAmount: string | number;
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
  shiftId?: string | null;
  shiftType?: string | null;
  serviceBayId?: string | null;
  bayCode?: string | null;
  licensePlate?: string | null;
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

/** Sub-screens of /payroll, kept in `?tab=` so the sidebar tree and the page tabs agree. */
export const PAYROLL_TABS = ['monthly', 'employees', 'coefficients', 'bonus'] as const;
export type PayrollTab = (typeof PAYROLL_TABS)[number];
export const payrollTabLabels: Record<PayrollTab, string> = {
  monthly: 'Bảng lương tháng',
  employees: 'Quản lý nhân viên',
  coefficients: 'Hệ số & Đơn giá lương',
  bonus: 'Chính sách thưởng ca',
};

/** Mirrors PayrollConstants on the backend: per-order bonus by the order's rank within one shift. */
export const SHIFT_BONUS_TIERS = [
  { label: 'Bậc 1', range: '1 – 2 đơn/ca', amount: 50_000 },
  { label: 'Bậc 2', range: '3 – 4 đơn/ca', amount: 80_000 },
  { label: 'Bậc 3', range: 'Từ 5 đơn/ca', amount: 120_000 },
] as const;
export const QC_PASS_BONUS = 30_000;

/** Bounds enforced by the `salary_coefficient` check constraint. */
export const SALARY_COEFFICIENT_MIN = 0.5;
export const SALARY_COEFFICIENT_MAX = 3;
export const SALARY_COEFFICIENT_PRESETS = [
  { value: 1, label: 'Cơ bản' },
  { value: 1.15, label: 'Thợ khá' },
  { value: 1.3, label: 'Thợ giỏi' },
  { value: 1.5, label: 'Thợ chính / QC' },
  { value: 2, label: 'Chuyên gia' },
] as const;
export const STANDARD_SHIFT_HOURS = 8;
export const STANDARD_WORKDAYS_PER_MONTH = 26;

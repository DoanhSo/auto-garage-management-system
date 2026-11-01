export * from './schedulingTypes';

export type ShiftStatus =
  'ACTIVE' | 'CANCELLED' | 'PENDING' | 'REJECTED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED';

export type ShiftType = 'MORNING' | 'AFTERNOON' | 'OVERTIME' | 'FULL_DAY' | 'NIGHT';

export const shiftTypeLabels: Record<ShiftType, string> = {
  MORNING: 'Ca sáng',
  AFTERNOON: 'Ca chiều',
  OVERTIME: 'Tăng ca',
  FULL_DAY: 'Cả ngày',
  NIGHT: 'Ca đêm',
};

/** Khung giờ gợi ý theo loại ca (giờ địa phương, HH:mm). */
export const shiftTypeWindows: Record<ShiftType, { start: string; end: string }> = {
  MORNING: { start: '07:30', end: '11:30' },
  AFTERNOON: { start: '13:00', end: '17:00' },
  OVERTIME: { start: '17:30', end: '20:30' },
  FULL_DAY: { start: '08:00', end: '17:00' },
  NIGHT: { start: '20:00', end: '23:59' },
};

export const shiftStatusLabels: Record<ShiftStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  CANCELLED: 'Đã hủy',
  PENDING: 'Chờ duyệt',
  REJECTED: 'Bị từ chối',
  SCHEDULED: 'Đã lên lịch',
  IN_PROGRESS: 'Đang trong ca',
  COMPLETED: 'Đã hoàn thành',
};

export const bayTypeLabels: Record<string, string> = {
  TWO_POST: 'Cầu nâng 2 trụ',
  FOUR_POST: 'Cầu nâng 4 trụ',
  SCISSOR: 'Cầu nâng cắt kéo',
  DIAGNOSTIC: 'Khoang chẩn đoán điện',
  INSPECTION: 'Khoang kiểm định QC',
  PAINT: 'Khoang đồng sơn',
  WASH: 'Khoang rửa xe & chăm sóc',
  GENERAL: 'Khoang sửa chữa chung',
  QUICK: 'Bảo dưỡng nhanh',
  HEAVY: 'Đại tu gầm máy',
  ALIGNMENT: 'Cân chỉnh góc lái',
};

export interface ServiceBayCreateInput {
  bayCode: string;
  bayName: string;
  bayType: string;
  description?: string | null;
  isActive?: boolean;
}

export interface Employee {
  id: string | number;
  userId: string | number;
  employeeCode: string;
  fullName?: string | null;
  username?: string | null;
  email?: string | null;
  joinedAt?: string | null;
  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;
  skillsJson?: string | null;
  canQc: boolean;
  /** Ẩn (null) với nhân viên không thuộc lễ tân/quản trị. */
  hourlyCost: number | null;
  baseSalary?: number | string;
  salaryCoefficient?: number | string;
  overtimeMultiplier?: number | string;
  defaultCommissionRate?: number | string;
  isActive: boolean;
  createdAt: string;
  version?: string | number;
}

export interface Shift {
  id: string | number;
  employeeId: string | number;
  startsAt: string;
  endsAt: string;
  shiftType?: ShiftType;
  serviceBayId?: string | number | null;
  attendanceStatus?: AttendanceStatus;
  actualStartsAt?: string | null;
  actualEndsAt?: string | null;
  status: ShiftStatus;
  notes?: string | null;
  registeredAt?: string | null;
  approvedBy?: string | number | null;
  rejectionReason?: string | null;
  createdBy: string | number;
  createdAt: string;
  version?: string | number;
}

export interface ShiftRegisterInput {
  startsAt: string;
  endsAt: string;
  shiftType?: ShiftType;
  serviceBayId?: number | null;
  notes?: string | null;
}

export interface ShiftCreateInput {
  employeeId: number;
  startsAt: string;
  endsAt: string;
  shiftType?: ShiftType;
  serviceBayId?: number | null;
  notes?: string | null;
}

/** Khớp RepairServiceImpl.RECEPTION_BAY_HOLD: khoang được giữ từ lúc tiếp nhận trong khoảng này. */
export const RECEPTION_BAY_HOLD_HOURS = 12;

export type AttendanceStatus = 'SCHEDULED' | 'CHECKED_IN' | 'LATE' | 'CHECKED_OUT' | 'ABSENT';

export const attendanceLabels: Record<AttendanceStatus, string> = {
  SCHEDULED: 'Chưa vào ca',
  CHECKED_IN: 'Đã vào ca',
  LATE: 'Vào ca muộn',
  CHECKED_OUT: 'Đã ra ca',
  ABSENT: 'Vắng mặt',
};

export interface BayShiftTechnician {
  employeeId: string;
  employeeCode: string;
  fullName: string;
  shiftId: string;
  shiftType?: ShiftType | null;
  attendanceStatus?: AttendanceStatus | null;
  shiftStartsAt: string;
  shiftEndsAt: string;
  skills: string[];
  canQc: boolean;
  onShiftNow: boolean;
}

export interface ShiftReconciliation {
  shiftId: string;
  employeeId: string;
  employeeCode?: string | null;
  employeeName?: string | null;
  shiftType?: ShiftType | null;
  startsAt: string;
  endsAt: string;
  attendanceStatus: AttendanceStatus;
  actualStartsAt?: string | null;
  actualEndsAt?: string | null;
  scheduledHours: number | string;
  laborHours: number | string;
  idleHours: number | string;
  overShiftHours: number | string;
  utilizationPercent: number;
}

export type TechnicianAvailability = 'AVAILABLE' | 'BUSY' | 'OVERLOADED';

export const availabilityLabels: Record<TechnicianAvailability, string> = {
  AVAILABLE: 'Đang rảnh',
  BUSY: 'Đang thi công',
  OVERLOADED: 'Quá tải',
};

export interface OnShiftTechnician {
  employeeId: string;
  employeeCode: string;
  fullName: string;
  shiftId: string;
  shiftType?: ShiftType | null;
  attendanceStatus?: AttendanceStatus | null;
  shiftStartsAt: string;
  shiftEndsAt: string;
  serviceBayId?: string | null;
  bayCode?: string | null;
  bayName?: string | null;
  skills: string[];
  canQc: boolean;
  activeOrdersCount: number;
  completedOrdersInShift: number;
  currentShiftBonus: number | string;
  hasActiveTimer: boolean;
  activeTaskId?: string | null;
  availabilityStatus: TechnicianAvailability;
}

export interface MaintenanceInput {
  startsAt: string;
  endsAt: string;
  notes?: string | null;
}

export interface EmployeeCreateInput {
  userId: number;
  employeeCode: string;
  joinedAt?: string | null;
  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;
  skillsJson?: string | null;
  canQc: boolean;
  hourlyCost: number;
  baseSalary?: number;
  salaryCoefficient?: number;
  overtimeMultiplier?: number;
  defaultCommissionRate?: number;
}

export interface EmployeeUpdateInput {
  joinedAt?: string | null;
  bankName?: string | null;
  bankAccountNumber?: string | null;
  bankAccountHolder?: string | null;
  skillsJson?: string | null;
  canQc?: boolean;
  hourlyCost?: number;
  baseSalary?: number;
  salaryCoefficient?: number;
  overtimeMultiplier?: number;
  defaultCommissionRate?: number;
  isActive?: boolean;
}

export const reservationTypeLabels: Record<string, string> = {
  BOOKING: 'Giữ chỗ lịch hẹn',
  REPAIR: 'Đang sửa xe',
  MAINTENANCE: 'Bảo trì cầu nâng',
};

export const reservationStatusLabels: Record<string, string> = {
  HELD: 'Đang giữ chỗ',
  IN_USE: 'Đang sử dụng',
  RELEASED: 'Đã trả khoang',
  EXPIRED: 'Hết hạn giữ',
  CANCELLED: 'Đã hủy',
};

export const SKILL_LABELS: Record<string, string> = {
  ENGINE: 'Máy',
  CHASSIS: 'Gầm',
  ELECTRIC: 'Điện',
  BODY: 'Đồng sơn',
  AC: 'Điều hòa',
  GENERAL: 'Bảo dưỡng chung',
};

/** skillsJson do backend lưu dạng chuỗi JSON (mảng mã kỹ năng). */
export function parseSkills(skillsJson?: string | null): string[] {
  if (!skillsJson) return [];
  try {
    const parsed: unknown = JSON.parse(skillsJson);
    return Array.isArray(parsed) ? parsed.filter((s): s is string => typeof s === 'string') : [];
  } catch {
    return [];
  }
}

/** Số giờ công chuẩn một tháng: 26 ngày × 8 giờ. */
export const STANDARD_MONTHLY_HOURS = 208;

export interface SalaryRoleTemplate {
  label: string;
  frameSalary: number;
  coefficient: number;
  commissionRate: number;
}

export const salaryRoleTemplates: Record<string, SalaryRoleTemplate> = {
  FOREMAN: {
    label: 'Quản đốc xưởng',
    frameSalary: 10_000_000,
    coefficient: 1.5,
    commissionRate: 15,
  },
  LEAD: { label: 'KTV Trưởng', frameSalary: 10_000_000, coefficient: 1.3, commissionRate: 12 },
  SENIOR: { label: 'KTV Chính', frameSalary: 8_000_000, coefficient: 1.2, commissionRate: 10 },
  STANDARD: { label: 'KTV Tiêu chuẩn', frameSalary: 8_000_000, coefficient: 1, commissionRate: 10 },
  QC: { label: 'KTV Kiểm định QC', frameSalary: 8_000_000, coefficient: 1.15, commissionRate: 10 },
  APPRENTICE: {
    label: 'Thợ phụ / Học việc',
    frameSalary: 5_000_000,
    coefficient: 0.8,
    commissionRate: 5,
  },
};

export const hourlyCostFromSalary = (baseSalary: number): number =>
  Math.round(baseSalary / STANDARD_MONTHLY_HOURS / 500) * 500;

export const BANK_NAMES = [
  'Vietcombank',
  'VietinBank',
  'BIDV',
  'Agribank',
  'Techcombank',
  'MB Bank',
  'ACB',
  'VPBank',
  'Sacombank',
  'TPBank',
];

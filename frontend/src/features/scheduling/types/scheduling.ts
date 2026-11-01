export * from './schedulingTypes';

export type ShiftStatus = 'ACTIVE' | 'CANCELLED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED';

export const shiftStatusLabels: Record<ShiftStatus, string> = {
  ACTIVE: 'Đang hoạt động',
  CANCELLED: 'Đã hủy',
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
  skillsJson?: string | null;
  canQc: boolean;
  hourlyCost: number;
  isActive: boolean;
  createdAt: string;
  version?: string | number;
}

export interface Shift {
  id: string | number;
  employeeId: string | number;
  startsAt: string;
  endsAt: string;
  status: ShiftStatus;
  notes?: string | null;
  createdBy: string | number;
  createdAt: string;
  version?: string | number;
}

export interface ShiftCreateInput {
  employeeId: number;
  startsAt: string;
  endsAt: string;
  notes?: string | null;
}

export interface MaintenanceInput {
  startsAt: string;
  endsAt: string;
  notes?: string | null;
}

export interface EmployeeCreateInput {
  userId: number;
  employeeCode: string;
  skillsJson?: string | null;
  canQc: boolean;
  hourlyCost: number;
}

export interface EmployeeUpdateInput {
  skillsJson?: string | null;
  canQc?: boolean;
  hourlyCost?: number;
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

export * from './schedulingTypes';

export type ShiftStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

export const shiftStatusLabels: Record<ShiftStatus, string> = {
  SCHEDULED: 'Đã lên lịch',
  IN_PROGRESS: 'Đang trong ca',
  COMPLETED: 'Đã hoàn thành',
  CANCELLED: 'Đã hủy',
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

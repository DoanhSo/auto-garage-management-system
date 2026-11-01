import type { ShiftType } from '../../scheduling/types/scheduling';

export type AssignmentStage = 'DIAGNOSIS' | 'REPAIR_MAIN' | 'REPAIR_ASSISTANT' | 'QC_INSPECTION';
export type WorkResultStatus = 'IN_PROGRESS' | 'PASSED' | 'REWORK_FAILED' | 'COMPLETED';

export const assignmentStageLabels: Record<AssignmentStage, string> = {
  DIAGNOSIS: 'Chẩn đoán',
  REPAIR_MAIN: 'Sửa chữa (thợ chính)',
  REPAIR_ASSISTANT: 'Sửa chữa (thợ phụ)',
  QC_INSPECTION: 'Kiểm thử QC',
};

export const workResultLabels: Record<WorkResultStatus, string> = {
  IN_PROGRESS: 'Đang thực hiện',
  PASSED: 'Đạt',
  COMPLETED: 'Hoàn thành',
  REWORK_FAILED: 'Phải làm lại',
};

export interface VehicleWorkHistoryEntry {
  id: string;
  repairOrderId: string;
  orderCode?: string | null;
  licensePlate?: string | null;
  stage: AssignmentStage;
  taskName: string;
  employeeId: string;
  employeeName?: string | null;
  shiftType?: ShiftType | null;
  startedAt: string;
  completedAt?: string | null;
  actualMinutes: number;
  resultStatus: WorkResultStatus;
  notes?: string | null;
}

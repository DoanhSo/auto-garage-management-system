export type OrderType = 'WALK_IN' | 'BOOKING' | 'WARRANTY';

export type RepairOrderStatus =
  | 'NEW'
  | 'DIAGNOSING'
  | 'WAITING_APPROVAL'
  | 'APPROVED'
  | 'IN_PROGRESS'
  | 'WAITING_PARTS'
  | 'WAITING_QC'
  | 'QC_FAILED'
  | 'COMPLETED'
  | 'INVOICED'
  | 'CLOSED'
  | 'CANCELLED';

export interface RepairOrder {
  id: string;
  orderCode: string;
  bookingId?: string | null;
  customerId: string;
  vehicleId: string;
  orderType: OrderType;
  status: RepairOrderStatus;
  odometerIn: number;
  odometerOut?: number | null;
  fuelLevelPercent?: number | null;
  customerComplaint?: string | null;
  diagnosis?: string | null;
  receivedBy: string;
  leadTechnicianId?: string | null;
  diagnosticTechnicianId?: string | null;
  serviceBayId?: string | null;
  qcTechnicianId?: string | null;
  receivedAt: string;
  latestQcCheckId?: string | null;
  handedOverBy?: string | null;
  handedOverAt?: string | null;
  closedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export type TaskType = 'REPAIR' | 'DIAGNOSIS' | 'MAINTENANCE';
export type TaskStatus = 'PENDING' | 'ASSIGNED' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'REWORK' | 'CANCELLED';

export interface RepairTask {
  id: string;
  repairOrderId: string;
  estimateItemId?: string | null;
  serviceId?: string | null;
  taskType: TaskType;
  taskName: string;
  description?: string | null;
  status: TaskStatus;
  quantity: number;
  unitPrice: number;
  startedAt?: string | null;
  completedAt?: string | null;
  completionNote?: string | null;
  createdAt: string;
  version: number;
}

export type AssignmentRole = 'MAIN' | 'ASSISTANT' | 'QC';

export interface TaskAssignment {
  id: string;
  taskId: string;
  employeeId: string;
  employeeName?: string;
  assignmentRole: AssignmentRole;
  isActive: boolean;
  startsAt: string;
  endsAt: string;
  assignedAt: string;
  releasedAt?: string | null;
}

export type QcIneligibleReason = 'MAIN_REPAIRER';

export const QC_INELIGIBLE_LABELS: Record<QcIneligibleReason, string> = {
  MAIN_REPAIRER: 'Thợ chính của phiếu này',
};

/** Kỹ thuật viên có quyền QC đang trong ca; `eligible=false` thì không chọn được cho phiếu này. */
export interface QcCandidate {
  employeeId: string;
  employeeCode: string;
  fullName: string;
  shiftEndsAt: string;
  serviceBayId?: string | null;
  bayCode?: string | null;
  bayName?: string | null;
  skills: string[];
  eligible: boolean;
  reason?: QcIneligibleReason | null;
}

export type StopReason = 'PAUSE' | 'COMPLETE' | 'CANCEL';

export interface LaborSession {
  id: string;
  assignmentId: string;
  employeeId: string;
  startsAt: string;
  endsAt?: string | null;
  stopReason?: StopReason | null;
  hourlyCostSnapshot?: number | null;
  durationSeconds?: number | null;
}

export interface TaskWorkSummary {
  taskId: string;
  repairOrderId: string;
  taskName: string;
  status: TaskStatus;
  completionNote?: string | null;
  totalActualSeconds: number;
  totalLaborCost: number;
  assignments: TaskAssignment[];
  sessions: LaborSession[];
}

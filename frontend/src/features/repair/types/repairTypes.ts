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
export type TaskStatus = 'PENDING' | 'IN_PROGRESS' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';

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
  role: AssignmentRole;
  assignedAt: string;
  releasedAt?: string | null;
}

export type StopReason = 'PAUSE' | 'COMPLETE' | 'CANCEL';

export interface LaborSession {
  id: string;
  assignmentId: string;
  startedAt: string;
  endedAt?: string | null;
  stopReason?: StopReason | null;
  durationSeconds?: number | null;
  hourlyCost?: number | null;
}

export interface TaskWorkSummary {
  taskId: string;
  taskName: string;
  status: TaskStatus;
  totalDurationSeconds: number;
  totalCost: number;
  sessionCount: number;
}

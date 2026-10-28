import { request } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  RepairOrder,
  RepairOrderStatus,
  RepairTask,
  TaskAssignment,
  LaborSession,
  TaskWorkSummary,
  AssignmentRole,
  StopReason,
} from '../types/repairTypes';

export const repairApi = {
  async searchRepairOrders(params?: {
    keyword?: string;
    status?: RepairOrderStatus;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<Page<RepairOrder>> {
    const q = new URLSearchParams();
    if (params?.keyword) q.set('keyword', params.keyword);
    if (params?.status) q.set('status', params.status);
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.size !== undefined) q.set('size', String(params.size));
    if (params?.sortBy) q.set('sortBy', params.sortBy);
    if (params?.sortDir) q.set('sortDir', params.sortDir);

    const query = q.toString() ? `?${q.toString()}` : '';
    return request<Page<RepairOrder>>(`/repair-orders${query}`);
  },

  async getRepairOrderById(id: string | number): Promise<RepairOrder> {
    return request<RepairOrder>(`/repair-orders/${id}`);
  },

  async updateDiagnosis(id: string | number, diagnosis: string): Promise<RepairOrder> {
    return request<RepairOrder>(`/repair-orders/${id}/diagnosis`, {
      method: 'PATCH',
      body: { diagnosis },
    });
  },

  async transitionStatus(
    id: string | number,
    newStatus: RepairOrderStatus,
    note?: string,
  ): Promise<RepairOrder> {
    return request<RepairOrder>(`/repair-orders/${id}/status`, {
      method: 'PATCH',
      body: { newStatus, note },
    });
  },

  async handOverOrder(
    id: string | number,
    data: { odometerOut: number; notes?: string; requestKey?: string },
  ): Promise<RepairOrder> {
    return request<RepairOrder>(`/repair-orders/${id}/handover`, {
      method: 'POST',
      body: data,
    });
  },

  async getTasksByRepairOrderId(orderId: string | number): Promise<RepairTask[]> {
    return request<RepairTask[]>(`/repair-orders/${orderId}/tasks`);
  },

  async createTask(
    orderId: string | number,
    data: {
      taskName: string;
      description?: string;
      taskType?: string;
      quantity?: number;
      unitPrice?: number;
    },
  ): Promise<RepairTask> {
    return request<RepairTask>(`/repair-orders/${orderId}/tasks`, {
      method: 'POST',
      body: data,
    });
  },

  async assignTask(
    taskId: string | number,
    data: { employeeId: string | number; role: AssignmentRole },
  ): Promise<TaskAssignment> {
    return request<TaskAssignment>(`/repair-tasks/${taskId}/assignments`, {
      method: 'POST',
      body: data,
    });
  },

  async releaseAssignment(assignmentId: string | number): Promise<void> {
    await request<void>(`/repair-tasks/assignments/${assignmentId}`, {
      method: 'DELETE',
    });
  },

  async getAssignmentsByTaskId(taskId: string | number): Promise<TaskAssignment[]> {
    return request<TaskAssignment[]>(`/repair-tasks/${taskId}/assignments`);
  },

  async startTimer(assignmentId: string | number): Promise<LaborSession> {
    return request<LaborSession>(
      `/repair-tasks/assignments/${assignmentId}/timer/start`,
      { method: 'POST' },
    );
  },

  async stopTimer(assignmentId: string | number, stopReason: StopReason): Promise<LaborSession> {
    return request<LaborSession>(
      `/repair-tasks/assignments/${assignmentId}/timer/stop`,
      {
        method: 'POST',
        body: { stopReason },
      },
    );
  },

  async completeTask(taskId: string | number, note?: string): Promise<TaskWorkSummary> {
    return request<TaskWorkSummary>(`/repair-tasks/${taskId}/complete`, {
      method: 'POST',
      body: { note },
    });
  },

  async getTaskWorkSummary(taskId: string | number): Promise<TaskWorkSummary> {
    return request<TaskWorkSummary>(`/repair-tasks/${taskId}/summary`);
  },
};

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { repairApi } from '../api/repairApi';
import type { RepairOrderStatus, AssignmentRole, StopReason } from '../types/repairTypes';

export function useRepairOrdersQuery(params?: {
  keyword?: string;
  status?: RepairOrderStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['repair-orders', params],
    queryFn: () => repairApi.searchRepairOrders(params),
  });
}

export function useRepairOrderQuery(id: string | number | null | undefined) {
  return useQuery({
    queryKey: ['repair-order', id],
    queryFn: () => (id ? repairApi.getRepairOrderById(id) : null),
    enabled: !!id,
  });
}

export function useRepairTasksQuery(repairOrderId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['repair-tasks', repairOrderId],
    queryFn: () => (repairOrderId ? repairApi.getTasksByRepairOrderId(repairOrderId) : []),
    enabled: !!repairOrderId,
  });
}

export function useUpdateDiagnosisMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, diagnosis }: { id: string | number; diagnosis: string }) =>
      repairApi.updateDiagnosis(id, diagnosis),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: ['repair-order', id] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

export function useTransitionStatusMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      newStatus,
      note,
    }: {
      id: string | number;
      newStatus: RepairOrderStatus;
      note?: string;
    }) => repairApi.transitionStatus(id, newStatus, note),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: ['repair-order', id] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

export function useHandoverMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      odometerOut,
      notes,
    }: {
      id: string | number;
      odometerOut: number;
      notes?: string;
    }) => repairApi.handOverOrder(id, { odometerOut, notes }),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: ['repair-order', id] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

export function useCreateTaskMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      data,
    }: {
      orderId: string | number;
      data: {
        taskName: string;
        description?: string;
        taskType?: string;
        quantity?: number;
        unitPrice?: number;
      };
    }) => repairApi.createTask(orderId, data),
    onSuccess: (_, { orderId }) => {
      void qc.invalidateQueries({ queryKey: ['repair-tasks', orderId] });
      void qc.invalidateQueries({ queryKey: ['repair-order', orderId] });
    },
  });
}

export function useAssignTaskMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      taskId,
      employeeId,
      role,
    }: {
      taskId: string | number;
      employeeId: string | number;
      role: AssignmentRole;
    }) => repairApi.assignTask(taskId, { employeeId, role }),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['repair-tasks'] });
    },
  });
}

export function useStartTimerMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (assignmentId: string | number) => repairApi.startTimer(assignmentId),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['repair-tasks'] });
    },
  });
}

export function useStopTimerMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      assignmentId,
      stopReason,
    }: {
      assignmentId: string | number;
      stopReason: StopReason;
    }) => repairApi.stopTimer(assignmentId, stopReason),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['repair-tasks'] });
    },
  });
}

export function useCompleteTaskMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, note }: { taskId: string | number; note?: string }) =>
      repairApi.completeTask(taskId, note),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['repair-tasks'] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

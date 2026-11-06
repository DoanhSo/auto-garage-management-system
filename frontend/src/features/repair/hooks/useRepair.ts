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

export function useUpdateRepairOrderMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      data,
    }: {
      id: string | number;
      data: { leadTechnicianId?: number; customerComplaint?: string; fuelLevelPercent?: number };
    }) => repairApi.updateOrder(id, data),
    onSuccess: (_, { id }) => {
      void qc.invalidateQueries({ queryKey: ['repair-order', id] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
    },
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

function useRefreshTaskWork() {
  const qc = useQueryClient();
  return () => {
    void qc.invalidateQueries({ queryKey: ['repair-tasks'] });
    void qc.invalidateQueries({ queryKey: ['task-summary'] });
    void qc.invalidateQueries({ queryKey: ['on-shift-technicians'] });
    void qc.invalidateQueries({ queryKey: ['repair-orders'] });
  };
}

export function useTaskSummaryQuery(taskId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['task-summary', taskId],
    queryFn: () => repairApi.getTaskWorkSummary(taskId as string | number),
    enabled: !!taskId,
    refetchInterval: 15_000,
  });
}

export function useAssignTaskMutation() {
  const refresh = useRefreshTaskWork();
  return useMutation({
    mutationFn: ({
      taskId,
      ...data
    }: {
      taskId: string | number;
      employeeId: string | number;
      assignmentRole: AssignmentRole;
      startsAt: string;
      endsAt: string;
    }) => repairApi.assignTask(taskId, data),
    onSuccess: refresh,
  });
}

export function useQcAssignmentQuery(orderId: string | number | null | undefined, enabled = true) {
  return useQuery({
    queryKey: ['qc-assignment', orderId],
    queryFn: () => repairApi.getQcAssignment(orderId as string | number),
    enabled: !!orderId && enabled,
  });
}

export function useQcCandidatesQuery(orderId: string | number | null | undefined, enabled = true) {
  return useQuery({
    queryKey: ['qc-candidates', orderId],
    queryFn: () => repairApi.getQcCandidates(orderId as string | number),
    enabled: !!orderId && enabled,
    refetchInterval: 60_000,
  });
}

export function useAssignQcInspectorMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      orderId,
      ...data
    }: {
      orderId: string | number;
      employeeId: string | number;
      startsAt: string;
      endsAt: string;
    }) => repairApi.assignQcInspector(orderId, data),
    onSuccess: (_, { orderId }) => {
      void qc.invalidateQueries({ queryKey: ['qc-assignment', orderId] });
      void qc.invalidateQueries({ queryKey: ['qc-candidates', orderId] });
      void qc.invalidateQueries({ queryKey: ['on-shift-technicians'] });
      void qc.invalidateQueries({ queryKey: ['repair-order', orderId] });
    },
  });
}

export function useReleaseAssignmentMutation() {
  const refresh = useRefreshTaskWork();
  return useMutation({
    mutationFn: (assignmentId: string | number) => repairApi.releaseAssignment(assignmentId),
    onSuccess: refresh,
  });
}

export function useStartTimerMutation() {
  const refresh = useRefreshTaskWork();
  return useMutation({
    mutationFn: (assignmentId: string | number) => repairApi.startTimer(assignmentId),
    onSuccess: refresh,
  });
}

export function useStopTimerMutation() {
  const refresh = useRefreshTaskWork();
  return useMutation({
    mutationFn: ({
      assignmentId,
      stopReason,
    }: {
      assignmentId: string | number;
      stopReason: StopReason;
    }) => repairApi.stopTimer(assignmentId, stopReason),
    onSuccess: refresh,
  });
}

export function useCompleteTaskMutation() {
  const refresh = useRefreshTaskWork();
  return useMutation({
    mutationFn: ({ taskId, note }: { taskId: string | number; note?: string }) =>
      repairApi.completeTask(taskId, note),
    onSuccess: refresh,
  });
}

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { schedulingApi } from '../api/schedulingApi';
import type {
  ServiceBayCreateInput,
  ShiftCreateInput,
  ShiftStatus,
  MaintenanceInput,
  EmployeeCreateInput,
  EmployeeUpdateInput,
} from '../types/scheduling';

export function useServiceBaysQuery(params?: {
  isActive?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['service-bays', params],
    queryFn: () => schedulingApi.getServiceBays(params),
  });
}

export function useActiveReservationsQuery() {
  return useQuery({
    queryKey: ['bay-reservations', 'active'],
    queryFn: () => schedulingApi.getActiveReservations(),
    refetchInterval: 15000,
  });
}

export function useBayReservationsQuery(bayId: number | string | null | undefined) {
  return useQuery({
    queryKey: ['bay-reservations', bayId],
    queryFn: () => (bayId ? schedulingApi.getBayReservations(bayId) : null),
    enabled: !!bayId,
  });
}

export function useCreateServiceBayMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ServiceBayCreateInput) => schedulingApi.createBay(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-bays'] });
    },
  });
}

export function useUpdateServiceBayMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: Partial<ServiceBayCreateInput> }) =>
      schedulingApi.updateBay(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-bays'] });
    },
  });
}

export function useDeleteServiceBayMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string | number) => schedulingApi.deleteBay(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['service-bays'] });
    },
  });
}

export function useOnShiftTechniciansQuery(enabled = true) {
  return useQuery({
    enabled,
    queryKey: ['on-shift-technicians'],
    queryFn: () => schedulingApi.getOnShiftTechnicians(),
    refetchInterval: 60_000,
  });
}

export function useSchedulingEmployeesQuery(params?: { active?: boolean }, enabled = true) {
  return useQuery({
    enabled,
    queryKey: ['scheduling-employees', params],
    queryFn: async () => {
      const page = await schedulingApi.listEmployees(params);
      return page.content ?? [];
    },
  });
}

export function useShiftsQuery(params?: {
  employeeId?: number;
  status?: ShiftStatus;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ['employee-shifts', params],
    queryFn: () => schedulingApi.listShifts(params),
  });
}

export function useCreateShiftMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: ShiftCreateInput) => schedulingApi.createShift(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employee-shifts'] });
    },
  });
}

export function useCancelShiftMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }: { id: string | number; reason?: string }) =>
      schedulingApi.cancelShift(id, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['employee-shifts'] });
    },
  });
}

export function useScheduleMaintenanceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ bayId, data }: { bayId: string | number; data: MaintenanceInput }) =>
      schedulingApi.scheduleMaintenance(bayId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['bay-reservations'] });
    },
  });
}

export function useCreateEmployeeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EmployeeCreateInput) => schedulingApi.createEmployee(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduling-employees'] });
    },
  });
}

export function useUpdateEmployeeMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: EmployeeUpdateInput }) =>
      schedulingApi.updateEmployee(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['scheduling-employees'] });
    },
  });
}

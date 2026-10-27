import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { vehicleApi, type VehicleQueryParams, type VehicleTimelineParams } from '../api/vehicleApi';
import type {
  VehicleCreate,
  VehicleUpdate,
  TransferOwnership,
} from '../../../shared/types/contracts';

export const vehicleKeys = {
  all: ['vehicles'] as const,
  lists: () => [...vehicleKeys.all, 'list'] as const,
  list: (params?: VehicleQueryParams) => [...vehicleKeys.lists(), params] as const,
  details: () => [...vehicleKeys.all, 'detail'] as const,
  detail: (id: string) => [...vehicleKeys.details(), id] as const,
  timelines: () => [...vehicleKeys.all, 'timeline'] as const,
  timeline: (id: string, params?: VehicleTimelineParams) =>
    [...vehicleKeys.timelines(), id, params] as const,
};

export function useVehiclesQuery(params?: VehicleQueryParams) {
  return useQuery({
    queryKey: vehicleKeys.list(params),
    queryFn: ({ signal }) => vehicleApi.list(params, signal),
  });
}

export function useVehicleQuery(id: string | null) {
  return useQuery({
    queryKey: id ? vehicleKeys.detail(id) : ['vehicles', 'empty'],
    queryFn: ({ signal }) => (id ? vehicleApi.get(id, signal) : Promise.reject('No ID')),
    enabled: Boolean(id),
  });
}

export function useVehicleTimelineQuery(id: string | null, params?: VehicleTimelineParams) {
  return useQuery({
    queryKey: id ? vehicleKeys.timeline(id, params) : ['vehicles', 'timeline', 'empty'],
    queryFn: ({ signal }) =>
      id ? vehicleApi.timeline(id, params, signal) : Promise.reject('No ID'),
    enabled: Boolean(id),
  });
}

export function useCreateVehicleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: VehicleCreate) => vehicleApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}

export function useUpdateVehicleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: VehicleUpdate }) =>
      vehicleApi.update(id, values),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.timelines() });
    },
  });
}

export function useTransferOwnershipMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: TransferOwnership }) =>
      vehicleApi.transfer(id, values),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.detail(variables.id) });
      queryClient.invalidateQueries({ queryKey: vehicleKeys.timelines() });
    },
  });
}

export function useDeleteVehicleMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => vehicleApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vehicleKeys.all });
    },
  });
}

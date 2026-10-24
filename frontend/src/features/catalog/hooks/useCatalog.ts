import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { catalogApi, type ServiceQueryParams } from '../api/catalogApi';
import type { ServiceCreate, ServiceUpdate } from '../../../shared/types/contracts';

export const serviceKeys = {
  all: ['services'] as const,
  lists: () => [...serviceKeys.all, 'list'] as const,
  list: (params?: ServiceQueryParams) => [...serviceKeys.lists(), params] as const,
  details: () => [...serviceKeys.all, 'detail'] as const,
  detail: (id: string) => [...serviceKeys.details(), id] as const,
};

export function useServicesQuery(params?: ServiceQueryParams) {
  return useQuery({
    queryKey: serviceKeys.list(params),
    queryFn: ({ signal }) => catalogApi.list(params, signal),
  });
}

export function useServiceQuery(id: string | null) {
  return useQuery({
    queryKey: id ? serviceKeys.detail(id) : ['services', 'empty'],
    queryFn: ({ signal }) => (id ? catalogApi.get(id, signal) : Promise.reject('No ID')),
    enabled: Boolean(id),
  });
}

export function useCreateServiceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: ServiceCreate) => catalogApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.all });
    },
  });
}

export function useUpdateServiceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: ServiceUpdate }) =>
      catalogApi.update(id, values),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.all });
      queryClient.invalidateQueries({ queryKey: serviceKeys.detail(variables.id) });
    },
  });
}

export function useDeleteServiceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => catalogApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: serviceKeys.all });
    },
  });
}

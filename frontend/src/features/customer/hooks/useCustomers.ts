import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { customerApi, type CustomerQueryParams } from '../api/customerApi';
import type { CustomerCreate, CustomerUpdate } from '../../../shared/types/contracts';

export const customerKeys = {
  all: ['customers'] as const,
  lists: () => [...customerKeys.all, 'list'] as const,
  list: (params?: CustomerQueryParams) => [...customerKeys.lists(), params] as const,
  details: () => [...customerKeys.all, 'detail'] as const,
  detail: (id: string) => [...customerKeys.details(), id] as const,
};

export function useCustomersQuery(params?: CustomerQueryParams) {
  return useQuery({
    queryKey: customerKeys.list(params),
    queryFn: ({ signal }) => customerApi.list(params, signal),
  });
}

export function useCustomerQuery(id: string | null) {
  return useQuery({
    queryKey: id ? customerKeys.detail(id) : ['customers', 'empty'],
    queryFn: ({ signal }) => (id ? customerApi.get(id, signal) : Promise.reject('No ID')),
    enabled: Boolean(id),
  });
}

export function useCreateCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: CustomerCreate) => customerApi.create(values),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

export function useUpdateCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: CustomerUpdate }) =>
      customerApi.update(id, values),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables.id) });
    },
  });
}

export function useDeleteCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => customerApi.remove(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

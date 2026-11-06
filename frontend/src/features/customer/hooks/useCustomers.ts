import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { customerApi, type CustomerQueryParams } from '../api/customerApi';
import type { Customer, CustomerCreate, CustomerUpdate } from '../../../shared/types/contracts';

export const customerKeys = {
  all: ['customers'] as const,
  lists: () => [...customerKeys.all, 'list'] as const,
  list: (params?: CustomerQueryParams) => [...customerKeys.lists(), params] as const,
  details: () => [...customerKeys.all, 'detail'] as const,
  detail: (id: string) => [...customerKeys.details(), id] as const,
};

export function useCustomersQuery(params?: CustomerQueryParams, enabled = true) {
  return useQuery({
    queryKey: customerKeys.list(params),
    queryFn: ({ signal }) => customerApi.list(params, signal),
    enabled,
  });
}

export function useMyCustomerQuery(enabled = true) {
  return useQuery({
    queryKey: [...customerKeys.all, 'me'],
    queryFn: ({ signal }) => customerApi.me(signal),
    enabled,
  });
}

export function useCustomerQuery(id: string | null, enabled = true) {
  return useQuery({
    queryKey: id ? customerKeys.detail(id) : ['customers', 'empty'],
    queryFn: ({ signal }) => (id ? customerApi.get(id, signal) : Promise.reject('No ID')),
    enabled: Boolean(id) && enabled,
    refetchOnMount: 'always',
  });
}

export function useCreateCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (values: CustomerCreate) => customerApi.create(values),
    onSuccess: (data) => {
      queryClient.setQueryData(customerKeys.detail(data.id), data);
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

export function useUpdateCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: CustomerUpdate }) =>
      customerApi.update(id, values),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(customerKeys.detail(variables.id), data);
      queryClient.setQueriesData({ queryKey: customerKeys.lists() }, (old: Customer[] | undefined) => {
        if (!old) return old;
        return old.map((c) => (c.id === variables.id ? data : c));
      });
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

export function useActivateCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => customerApi.activate(id),
    onSuccess: (data, variables) => {
      queryClient.setQueryData(customerKeys.detail(variables), data);
      queryClient.setQueriesData({ queryKey: customerKeys.lists() }, (old: Customer[] | undefined) => {
        if (!old) return old;
        return old.map((c) => (c.id === variables ? data : c));
      });
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
    },
  });
}

export function useDeleteCustomerMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => customerApi.remove(id),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: customerKeys.all });
      queryClient.invalidateQueries({ queryKey: customerKeys.detail(variables) });
    },
  });
}

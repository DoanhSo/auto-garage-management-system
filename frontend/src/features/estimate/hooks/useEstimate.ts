import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { estimateApi } from '../api/estimateApi';
import type {
  EstimateStatus,
  EstimateCreateInput,
  EstimateDecisionInput,
} from '../types/estimate';

export function useEstimatesQuery(params: {
  repairOrderId?: number;
  status?: EstimateStatus;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['estimates', params],
    queryFn: () => estimateApi.listEstimates(params),
  });
}

export function useEstimateQuery(id: string | number | null | undefined) {
  return useQuery({
    queryKey: ['estimate', id],
    queryFn: () => (id ? estimateApi.getEstimate(id) : null),
    enabled: !!id,
  });
}

export function useCreateEstimateMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: EstimateCreateInput) => estimateApi.createEstimate(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['estimates'] });
    },
  });
}

export function useSendEstimateMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string | number) => estimateApi.sendEstimate(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['estimates'] });
      queryClient.invalidateQueries({ queryKey: ['estimate', id] });
    },
  });
}

export function useDecideEstimateMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: EstimateDecisionInput }) =>
      estimateApi.decideEstimate(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['estimates'] });
      queryClient.invalidateQueries({ queryKey: ['estimate', id] });
      queryClient.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

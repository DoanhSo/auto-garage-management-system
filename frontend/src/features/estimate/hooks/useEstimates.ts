import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { estimateApi } from '../api/estimateApi';
import type { EstimateCreateRequest, EstimateDecisionRequest } from '../types/estimateTypes';

export function useEstimatesQuery(repairOrderId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['estimates', repairOrderId],
    queryFn: () => (repairOrderId ? estimateApi.getEstimatesByRepairOrderId(repairOrderId) : []),
    enabled: !!repairOrderId,
  });
}

export function useEstimateQuery(id: string | number | null | undefined) {
  return useQuery({
    queryKey: ['estimate', id],
    queryFn: () => (id ? estimateApi.getEstimateById(id) : null),
    enabled: !!id,
  });
}

export function useCreateEstimateMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: EstimateCreateRequest) => estimateApi.createEstimate(data),
    onSuccess: (_, variables) => {
      void qc.invalidateQueries({ queryKey: ['estimates', variables.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
      void qc.invalidateQueries({ queryKey: ['repair-order', variables.repairOrderId] });
    },
  });
}

export function useSendEstimateMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string | number) => estimateApi.sendEstimate(id),
    onSuccess: (data) => {
      void qc.invalidateQueries({ queryKey: ['estimates', data.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['estimate', data.id] });
      void qc.invalidateQueries({ queryKey: ['repair-order', data.repairOrderId] });
    },
  });
}

export function useDecideEstimateMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: EstimateDecisionRequest }) =>
      estimateApi.decideEstimate(id, data),
    onSuccess: (data) => {
      void qc.invalidateQueries({ queryKey: ['estimates', data.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['estimate', data.id] });
      void qc.invalidateQueries({ queryKey: ['repair-tasks', data.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['repair-order', data.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['parts'] });
    },
  });
}

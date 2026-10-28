import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { qualityApi } from '../api/qualityApi';
import type { QualityCheckCreateRequest } from '../types/qualityTypes';

export function useQualityChecksQuery(repairOrderId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['quality-checks', repairOrderId],
    queryFn: () => (repairOrderId ? qualityApi.getQualityChecksByRepairOrderId(repairOrderId) : []),
    enabled: !!repairOrderId,
  });
}

export function usePerformQualityCheckMutation() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: QualityCheckCreateRequest) => qualityApi.performQualityCheck(data),
    onSuccess: (_, variables) => {
      void qc.invalidateQueries({ queryKey: ['quality-checks', variables.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['repair-order', variables.repairOrderId] });
      void qc.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

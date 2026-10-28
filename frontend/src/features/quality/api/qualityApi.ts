import { request } from '../../../shared/api/client';
import type { QualityCheck, QualityCheckCreateRequest } from '../types/qualityTypes';

export const qualityApi = {
  async getQualityChecksByRepairOrderId(repairOrderId: string | number): Promise<QualityCheck[]> {
    return request<QualityCheck[]>(`/quality-checks/order/${repairOrderId}`);
  },

  async performQualityCheck(data: QualityCheckCreateRequest): Promise<QualityCheck> {
    return request<QualityCheck>('/quality-checks', {
      method: 'POST',
      body: data,
    });
  },
};

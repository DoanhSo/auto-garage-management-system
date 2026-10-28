import { request } from '../../../shared/api/client';
import type {
  Estimate,
  EstimateCreateRequest,
  EstimateDecisionRequest,
} from '../types/estimateTypes';

export const estimateApi = {
  async getEstimatesByRepairOrderId(repairOrderId: string | number): Promise<Estimate[]> {
    return request<Estimate[]>(`/estimates/order/${repairOrderId}`);
  },

  async getEstimateById(id: string | number): Promise<Estimate> {
    return request<Estimate>(`/estimates/${id}`);
  },

  async createEstimate(data: EstimateCreateRequest): Promise<Estimate> {
    return request<Estimate>('/estimates', {
      method: 'POST',
      body: data,
    });
  },

  async sendEstimate(id: string | number): Promise<Estimate> {
    return request<Estimate>(`/estimates/${id}/send`, {
      method: 'POST',
    });
  },

  async decideEstimate(id: string | number, data: EstimateDecisionRequest): Promise<Estimate> {
    return request<Estimate>(`/estimates/${id}/decide`, {
      method: 'POST',
      body: data,
    });
  },
};

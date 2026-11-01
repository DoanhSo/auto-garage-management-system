import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  Estimate,
  EstimateCreateRequest,
  EstimateDecisionRequest,
} from '../types/estimateTypes';
import type {
  EstimateCreateInput,
  EstimateDecisionInput,
  EstimateStatus,
} from '../types/estimate';

export const estimateApi = {
  async getEstimatesByRepairOrderId(repairOrderId: string | number): Promise<Estimate[]> {
    return request<Estimate[]>(`/estimates/order/${repairOrderId}`);
  },

  async getEstimateById(id: string | number): Promise<Estimate> {
    return request<Estimate>(`/estimates/${id}`);
  },

  getEstimate(id: string | number): Promise<Estimate> {
    return request<Estimate>(`/estimates/${id}`);
  },

  listEstimates(params: {
    repairOrderId?: number;
    status?: EstimateStatus;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<Page<Estimate>> {
    return request<Page<Estimate>>(
      '/estimates' +
        queryString({
          repairOrderId: params.repairOrderId,
          status: params.status,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'createdAt',
          sortDir: params.sortDir ?? 'desc',
        }),
    );
  },

  async createEstimate(data: EstimateCreateRequest | EstimateCreateInput): Promise<Estimate> {
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

  async decideEstimate(id: string | number, data: EstimateDecisionRequest | EstimateDecisionInput): Promise<Estimate> {
    return request<Estimate>(`/estimates/${id}/decide`, {
      method: 'POST',
      body: data,
    });
  },
};

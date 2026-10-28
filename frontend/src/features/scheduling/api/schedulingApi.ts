import { request } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type { BayReservation, ServiceBay } from '../types/schedulingTypes';

export const schedulingApi = {
  async getServiceBays(params?: {
    isActive?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<Page<ServiceBay>> {
    const q = new URLSearchParams();
    if (params?.isActive !== undefined) q.set('isActive', String(params.isActive));
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.size !== undefined) q.set('size', String(params.size));
    if (params?.sortBy) q.set('sortBy', params.sortBy);
    if (params?.sortDir) q.set('sortDir', params.sortDir);
    const query = q.toString() ? `?${q.toString()}` : '';
    return request<Page<ServiceBay>>(`/service-bays${query}`);
  },

  async getActiveReservations(): Promise<BayReservation[]> {
    return request<BayReservation[]>('/bay-reservations/active');
  },

  async getBayReservations(
    bayId: number | string,
    params?: { page?: number; size?: number },
  ): Promise<Page<BayReservation>> {
    const q = new URLSearchParams();
    if (params?.page !== undefined) q.set('page', String(params.page));
    if (params?.size !== undefined) q.set('size', String(params.size));
    const query = q.toString() ? `?${q.toString()}` : '';
    return request<Page<BayReservation>>(`/service-bays/${bayId}/reservations${query}`);
  },
};

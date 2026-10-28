import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type { BayReservation } from '../types/schedulingTypes';
import type {
  ServiceBay,
  ServiceBayCreateInput,
  Employee,
  Shift,
  ShiftCreateInput,
  ShiftStatus,
} from '../types/scheduling';

export const schedulingApi = {
  // Service Bays query (for both WorkshopBayGrid and SchedulingPage)
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

  listBays: (params?: {
    keyword?: string;
    active?: boolean;
    page?: number;
    size?: number;
  }) =>
    request<Page<ServiceBay>>(
      '/service-bays' +
        queryString({
          keyword: params?.keyword,
          active: params?.active,
          page: params?.page ?? 0,
          size: params?.size ?? 50,
          sortBy: 'bayCode',
          sortDir: 'asc',
        }),
    ),

  getBay: (id: string | number) => request<ServiceBay>(`/service-bays/${id}`),

  createBay: (data: ServiceBayCreateInput) =>
    request<ServiceBay>('/service-bays', {
      method: 'POST',
      body: data,
    }),

  updateBay: (id: string | number, data: Partial<ServiceBayCreateInput>) =>
    request<ServiceBay>(`/service-bays/${id}`, {
      method: 'PUT',
      body: data,
    }),

  deleteBay: (id: string | number) =>
    request<void>(`/service-bays/${id}`, {
      method: 'DELETE',
    }),

  // Reservations
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

  // Employees
  listEmployees: (params?: { active?: boolean; page?: number; size?: number }) =>
    request<Page<Employee>>(
      '/employees' +
        queryString({
          active: params?.active,
          page: params?.page ?? 0,
          size: params?.size ?? 50,
        }),
    ),

  // Shifts
  listShifts: (params?: {
    employeeId?: number;
    status?: ShiftStatus;
    page?: number;
    size?: number;
  }) =>
    request<Page<Shift>>(
      '/employee-shifts' +
        queryString({
          employeeId: params?.employeeId,
          status: params?.status,
          page: params?.page ?? 0,
          size: params?.size ?? 50,
          sortBy: 'startsAt',
          sortDir: 'desc',
        }),
    ),

  createShift: (data: ShiftCreateInput) =>
    request<Shift>('/employee-shifts', {
      method: 'POST',
      body: data,
    }),

  cancelShift: (id: string | number, reason?: string) =>
    request<Shift>(`/employee-shifts/${id}/cancel`, {
      method: 'PUT',
      body: { reason },
    }),
};

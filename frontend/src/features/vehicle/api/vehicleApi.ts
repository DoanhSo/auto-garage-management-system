import { request, queryString } from '../../../shared/api/client';
import { vehicleSchema, vehicleTimelineSchema } from '../../../shared/types/schemas';
import type {
  Vehicle,
  VehicleCreate,
  VehicleUpdate,
  TransferOwnership,
  VehicleTimeline,
} from '../../../shared/types/contracts';
import { z } from 'zod';

export interface VehicleQueryParams {
  customerId?: string;
  search?: string;
  isActive?: boolean;
}

export interface VehicleTimelineParams {
  requestingCustomerId?: string;
  role?: string;
}

export const vehicleApi = {
  list: async (params?: VehicleQueryParams, signal?: AbortSignal): Promise<Vehicle[]> => {
    const raw = await request<unknown>(
      '/vehicles' +
        queryString({
          customerId: params?.customerId,
          search: params?.search,
          isActive: params?.isActive,
        }),
      { signal },
    );
    return z.array(vehicleSchema).parse(raw);
  },

  get: async (id: string, signal?: AbortSignal): Promise<Vehicle> => {
    const raw = await request<unknown>(`/vehicles/${id}`, { signal });
    return vehicleSchema.parse(raw);
  },

  getByPlate: async (plate: string, signal?: AbortSignal): Promise<Vehicle> => {
    const raw = await request<unknown>(`/vehicles/plate/${encodeURIComponent(plate)}`, { signal });
    return vehicleSchema.parse(raw);
  },

  create: async (values: VehicleCreate): Promise<Vehicle> => {
    const raw = await request<unknown>('/vehicles', {
      method: 'POST',
      body: values,
    });
    return vehicleSchema.parse(raw);
  },

  update: async (id: string, values: VehicleUpdate): Promise<Vehicle> => {
    const raw = await request<unknown>(`/vehicles/${id}`, {
      method: 'PUT',
      body: values,
    });
    return vehicleSchema.parse(raw);
  },

  transfer: async (id: string, values: TransferOwnership): Promise<Vehicle> => {
    const raw = await request<unknown>(`/vehicles/${id}/transfer`, {
      method: 'POST',
      body: values,
    });
    return vehicleSchema.parse(raw);
  },

  remove: (id: string): Promise<null> => {
    return request<null>(`/vehicles/${id}`, {
      method: 'DELETE',
    });
  },

  timeline: async (
    id: string,
    params?: VehicleTimelineParams,
    signal?: AbortSignal,
  ): Promise<VehicleTimeline> => {
    const raw = await request<unknown>(
      `/vehicles/${id}/timeline` +
        queryString({
          requestingCustomerId: params?.requestingCustomerId,
          role: params?.role,
        }),
      { signal },
    );
    return vehicleTimelineSchema.parse(raw);
  },
};

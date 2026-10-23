import { request, queryString } from '../../../shared/api/client';
import { serviceSchema } from '../../../shared/types/schemas';
import type {
  Service,
  ServiceCreate,
  ServiceUpdate,
  ServiceType,
} from '../../../shared/types/contracts';
import { z } from 'zod';

export interface ServiceQueryParams {
  search?: string;
  type?: ServiceType;
  isActive?: boolean;
}

export const catalogApi = {
  list: async (params?: ServiceQueryParams, signal?: AbortSignal): Promise<Service[]> => {
    const raw = await request<unknown>(
      '/services' +
        queryString({
          search: params?.search,
          type: params?.type,
          isActive: params?.isActive,
        }),
      { signal },
    );
    return z.array(serviceSchema).parse(raw);
  },

  get: async (id: string, signal?: AbortSignal): Promise<Service> => {
    const raw = await request<unknown>(`/services/${id}`, { signal });
    return serviceSchema.parse(raw);
  },

  getByCode: async (code: string, signal?: AbortSignal): Promise<Service> => {
    const raw = await request<unknown>(`/services/code/${encodeURIComponent(code)}`, { signal });
    return serviceSchema.parse(raw);
  },

  create: async (values: ServiceCreate): Promise<Service> => {
    const raw = await request<unknown>('/services', {
      method: 'POST',
      body: values,
    });
    return serviceSchema.parse(raw);
  },

  update: async (id: string, values: ServiceUpdate): Promise<Service> => {
    const raw = await request<unknown>(`/services/${id}`, {
      method: 'PUT',
      body: values,
    });
    return serviceSchema.parse(raw);
  },

  remove: (id: string): Promise<null> => {
    return request<null>(`/services/${id}`, {
      method: 'DELETE',
    });
  },
};

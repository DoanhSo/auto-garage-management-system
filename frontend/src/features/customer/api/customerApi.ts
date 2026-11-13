import { request, queryString } from '../../../shared/api/client';
import { customerSchema } from '../../../shared/types/schemas';
import type { Customer, CustomerCreate, CustomerUpdate } from '../../../shared/types/contracts';
import { z } from 'zod';

export interface CustomerQueryParams {
  search?: string;
  isActive?: boolean;
  hasAccount?: boolean;
}

export const customerApi = {
  me: async (signal?: AbortSignal): Promise<Customer> =>
    customerSchema.parse(await request<unknown>('/customers/me', { signal })),
  list: async (params?: CustomerQueryParams, signal?: AbortSignal): Promise<Customer[]> => {
    const raw = await request<unknown>(
      '/customers' +
        queryString({
          search: params?.search,
          isActive: params?.isActive,
          hasAccount: params?.hasAccount,
        }),
      { signal },
    );
    return z.array(customerSchema).parse(raw);
  },

  get: async (id: string, signal?: AbortSignal): Promise<Customer> => {
    const raw = await request<unknown>(`/customers/${id}`, { signal });
    return customerSchema.parse(raw);
  },

  getByCode: async (code: string, signal?: AbortSignal): Promise<Customer> => {
    const raw = await request<unknown>(`/customers/code/${code}`, { signal });
    return customerSchema.parse(raw);
  },

  create: async (values: CustomerCreate): Promise<Customer> => {
    const raw = await request<unknown>('/customers', {
      method: 'POST',
      body: values,
    });
    return customerSchema.parse(raw);
  },

  update: async (id: string, values: CustomerUpdate): Promise<Customer> => {
    const raw = await request<unknown>(`/customers/${id}`, {
      method: 'PUT',
      body: values,
    });
    return customerSchema.parse(raw);
  },

  createAccount: async (
    id: string,
    values: { username: string; email: string | null; password: string },
  ): Promise<Customer> =>
    customerSchema.parse(
      await request<unknown>(`/customers/${id}/account`, { method: 'POST', body: values }),
    ),

  linkAccount: async (id: string, sourceCustomerId: string): Promise<Customer> =>
    customerSchema.parse(
      await request<unknown>(`/customers/${id}/account/link`, {
        method: 'POST',
        body: { sourceCustomerId },
      }),
    ),

  activate: async (id: string): Promise<Customer> => {
    const raw = await request<unknown>(`/customers/${id}/activate`, { method: 'POST' });
    return customerSchema.parse(raw);
  },

  remove: (id: string): Promise<null> => {
    return request<null>(`/customers/${id}`, {
      method: 'DELETE',
    });
  },
};

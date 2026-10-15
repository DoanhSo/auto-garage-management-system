import { useQuery } from '@tanstack/react-query';
import { z } from 'zod';
import { request } from '../api/client';
import type { UiPolicy } from '../types/contracts';
export const policySchema = z
  .object({
    passwordMinLength: z.number().int().min(1),
    passwordMaxUtf8Bytes: z.number().int().positive(),
    paginationDefaultSize: z.number().int().positive(),
    paginationMaxSize: z.number().int().positive(),
    paginationMaxPage: z.number().int().nonnegative(),
    reportMaxPeriodSeconds: z.number().nonnegative(),
    garageTimeZone: z.string().refine((value) => {
      try {
        new Intl.DateTimeFormat('vi-VN', { timeZone: value });
        return true;
      } catch {
        return false;
      }
    }),
  })
  .refine((value) => value.paginationDefaultSize <= value.paginationMaxSize);
export function useUiPolicy() {
  return useQuery({
    queryKey: ['ui-policy'],
    queryFn: async ({ signal }) =>
      policySchema.parse(
        await request<unknown>('/public/ui-policy', {
          signal,
          authenticated: false,
        }),
      ),
    staleTime: 60000,
  });
}
export function pageSizes(policy: UiPolicy): number[] {
  return [
    ...new Set(
      [10, 20, 50, 100, policy.paginationDefaultSize, policy.paginationMaxSize].filter(
        (value) => value <= policy.paginationMaxSize,
      ),
    ),
  ].sort((a, b) => a - b);
}

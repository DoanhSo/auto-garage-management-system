import { request, queryString } from '../../../shared/api/client';
import { dashboardSchema, invoiceSchema, pageSchema } from '../../../shared/types/schemas';
import type { ReportRange } from '../../../shared/utils/dates';
export const reportsApi = {
  dashboard: async (range: ReportRange, signal?: AbortSignal) =>
    dashboardSchema.parse(
      await request<unknown>(`/reports/dashboard${queryString({ ...range })}`, {
        signal,
      }),
    ),
  debts: async (page: number, size: number, signal?: AbortSignal) =>
    pageSchema(invoiceSchema).parse(
      await request<unknown>(`/reports/outstanding-invoices${queryString({ page, size })}`, {
        signal,
      }),
    ),
  exportCsv: (range: ReportRange, signal?: AbortSignal) =>
    request<Blob>(`/reports/dashboard/export${queryString({ ...range })}`, {
      signal,
      binary: true,
    }),
};

import { request, queryString } from '../../../shared/api/client';
import { auditSchema, pageSchema } from '../../../shared/types/schemas';
export interface AuditParams {
  actorId?: string;
  action?: string;
  page: number;
  size: number;
}
export const auditApi = {
  list: async (params: AuditParams, signal?: AbortSignal) =>
    pageSchema(auditSchema).parse(
      await request<unknown>('/admin/audit-events' + queryString({ ...params }), { signal }),
    ),
};

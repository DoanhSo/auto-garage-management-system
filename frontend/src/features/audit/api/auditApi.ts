import { request, queryString } from '../../../shared/api/client';
import { auditSchema, pageSchema } from '../../../shared/types/schemas';
export interface AuditParams {
  actorId?: string;
  action?: string;
  /** Từ khóa tìm tự do: hành động, đối tượng, người thực hiện. */
  query?: string;
  /** Mã hành động suy ra từ nhãn tiếng Việt của từ khóa. */
  actions?: string[];
  page: number;
  size: number;
}
function auditQuery({ actions, ...scalars }: AuditParams): string {
  const values = new URLSearchParams(queryString({ ...scalars }).slice(1));
  actions?.forEach((code) => values.append('actions', code));
  return `?${values.toString()}`;
}
export const auditApi = {
  list: async (params: AuditParams, signal?: AbortSignal) =>
    pageSchema(auditSchema).parse(
      await request<unknown>(`/admin/audit-events${auditQuery(params)}`, { signal }),
    ),
};

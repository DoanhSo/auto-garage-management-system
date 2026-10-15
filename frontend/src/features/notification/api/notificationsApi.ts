import { request, queryString } from '../../../shared/api/client';
import { noticeSchema, pageSchema } from '../../../shared/types/schemas';
export const notificationsApi = {
  inbox: async (params: { unread: boolean; page: number; size: number }, signal?: AbortSignal) =>
    pageSchema(noticeSchema).parse(
      await request<unknown>('/notifications' + queryString(params), {
        signal,
      }),
    ),
  get: async (id: string, signal?: AbortSignal) =>
    noticeSchema.parse(await request<unknown>(`/notifications/${id}`, { signal })),
  markRead: async (id: string) =>
    noticeSchema.parse(await request<unknown>(`/notifications/${id}/read`, { method: 'POST' })),
};

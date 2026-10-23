import { request, queryString } from '../../../shared/api/client';
import type { NoticeBroadcastRequest } from '../../../shared/types/contracts';
import {
  noticeBroadcastResultSchema,
  noticeSchema,
  pageSchema,
} from '../../../shared/types/schemas';
export const notificationsApi = {
  inbox: async (params: { unread: boolean; page: number; size: number }, signal?: AbortSignal) =>
    pageSchema(noticeSchema).parse(
      await request<unknown>('/notifications' + queryString(params), {
        signal,
      }),
    ),
  adminList: async (
    params: { status?: string; channel?: string; page: number; size: number },
    signal?: AbortSignal,
  ) =>
    pageSchema(noticeSchema).parse(
      await request<unknown>('/notifications/admin' + queryString(params), {
        signal,
      }),
    ),
  get: async (id: string, signal?: AbortSignal) =>
    noticeSchema.parse(await request<unknown>(`/notifications/${id}`, { signal })),
  markRead: async (id: string) =>
    noticeSchema.parse(await request<unknown>(`/notifications/${id}/read`, { method: 'POST' })),
  broadcast: async (values: NoticeBroadcastRequest) =>
    noticeBroadcastResultSchema.parse(
      await request<unknown>('/notifications/broadcast', {
        method: 'POST',
        body: values,
      }),
    ),
  remove: async (id: string) =>
    request<null>(`/notifications/${id}`, {
      method: 'DELETE',
    }),
};

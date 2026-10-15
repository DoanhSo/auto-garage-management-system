import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../../app/providers/AuthProvider';
import { env } from '../../../shared/config/env';
import { notificationsApi } from '../api/notificationsApi';
const polling = {
  refetchInterval: (query: { state: { status: string } }) =>
    query.state.status === 'error'
      ? Math.min(env.notificationPollMs * 2, 600000)
      : env.notificationPollMs,
  refetchIntervalInBackground: false,
  refetchOnWindowFocus: true,
};
export function useUnreadCountQuery() {
  const { user, sessionKey } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'notifications', 'unread-count', user?.id],
    queryFn: async ({ signal }) =>
      (await notificationsApi.inbox({ unread: true, page: 0, size: 1 }, signal)).totalElements,
    enabled: !!user,
    ...polling,
  });
}
export function useInboxQuery(params: { unread: boolean; page: number; size: number }) {
  const { user, sessionKey } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'notifications', 'inbox', user?.id, params],
    queryFn: ({ signal }) => notificationsApi.inbox(params, signal),
    enabled: !!user,
    ...polling,
  });
}
export function useNotificationQuery(id: string) {
  const { user, sessionKey } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'notifications', 'detail', user?.id, id],
    queryFn: ({ signal }) => notificationsApi.get(id, signal),
    enabled: !!user,
    refetchOnWindowFocus: false,
  });
}
export function useMarkReadMutation() {
  const { user, sessionKey } = useAuth();
  const client = useQueryClient();
  return useMutation({
    mutationFn: notificationsApi.markRead,
    retry: 0,
    onSuccess: async (notice) => {
      client.setQueryData(
        ['private', sessionKey, 'notifications', 'detail', user?.id, notice.id],
        notice,
      );
      await client.invalidateQueries({
        predicate: (query) =>
          query.queryKey[1] === sessionKey &&
          query.queryKey[2] === 'notifications' &&
          query.queryKey[3] !== 'detail',
      });
    },
  });
}

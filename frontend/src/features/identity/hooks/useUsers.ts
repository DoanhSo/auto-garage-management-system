import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuth } from '../../../app/providers/AuthProvider';
import { usersApi, type UsersParams } from '../api/usersApi';
import type { UserCreate, UserUpdate } from '../../../shared/types/contracts';
export function useUsersQuery(params: UsersParams) {
  const { sessionKey, user } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'users', 'list', params],
    queryFn: ({ signal }) => usersApi.list(params, signal),
    enabled: user?.role === 'ADMIN',
  });
}
export function useUserQuery(id: string | null) {
  const { sessionKey, user } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'users', 'detail', id],
    queryFn: ({ signal }) => usersApi.get(id!, signal),
    enabled: !!id && user?.role === 'ADMIN',
    staleTime: 0,
    gcTime: 0,
    refetchOnMount: 'always',
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}
export function useCreateUserMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (values: UserCreate) => usersApi.create(values),
    retry: 0,
    gcTime: 0,
    onSuccess: () =>
      client.invalidateQueries({
        predicate: (query) => query.queryKey[2] === 'users',
      }),
  });
}
export function useUpdateUserMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, values }: { id: string; values: UserUpdate }) => usersApi.update(id, values),
    retry: 0,
    onSuccess: () => {
      void client.invalidateQueries({
        predicate: (query) => query.queryKey[2] === 'users' && query.queryKey[3] === 'list',
      });
    },
  });
}
export function useDeleteUserMutation() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ id, version }: { id: string; version: string }) => usersApi.remove(id, version),
    retry: 0,
    onSuccess: () => {
      void client.invalidateQueries({
        predicate: (query) => query.queryKey[2] === 'users' && query.queryKey[3] === 'list',
      });
    },
  });
}

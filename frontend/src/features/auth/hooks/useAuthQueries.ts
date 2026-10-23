import { useMutation, useQuery } from '@tanstack/react-query';
import { authApi } from '../api/authApi';
import { useAuth } from '../../../app/providers/AuthProvider';
export function useLoginMutation() {
  const { login } = useAuth();
  return useMutation({ mutationFn: login, retry: 0, gcTime: 0 });
}
export function usePkceCallbackMutation() {
  const { completePkceLogin } = useAuth();
  return useMutation({ mutationFn: completePkceLogin, retry: 0, gcTime: 0 });
}
export function useLogoutMutation() {
  const { logout } = useAuth();
  return useMutation({ mutationFn: logout, retry: 0, gcTime: 0 });
}
export function useMeQuery() {
  const { user, sessionKey } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'me'],
    queryFn: ({ signal }) => authApi.me(signal),
    enabled: !!user,
    staleTime: 30000,
    refetchOnWindowFocus: 'always',
  });
}

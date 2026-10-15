import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../../../app/providers/AuthProvider';
import { auditApi, type AuditParams } from '../api/auditApi';
export function useAuditEventsQuery(params: AuditParams, valid = true) {
  const { sessionKey, user } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'audit', params],
    queryFn: ({ signal }) => auditApi.list(params, signal),
    enabled: user?.role === 'ADMIN' && valid,
  });
}

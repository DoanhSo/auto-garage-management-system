import { useMutation, useQuery } from '@tanstack/react-query';
import { useAuth } from '../../../app/providers/AuthProvider';
import type { ReportRange } from '../../../shared/utils/dates';
import { reportsApi } from '../api/reportsApi';
export function useDashboardQuery(range: ReportRange | null) {
  const { user, sessionKey } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'reports', 'dashboard', range],
    queryFn: ({ signal }) => reportsApi.dashboard(range!, signal),
    enabled: user?.role === 'ADMIN' && !!range,
  });
}
export function useDebtsQuery(page: number, size: number) {
  const { user, sessionKey } = useAuth();
  return useQuery({
    queryKey: ['private', sessionKey, 'reports', 'debts', page, size],
    queryFn: ({ signal }) => reportsApi.debts(page, size, signal),
    enabled: user?.role === 'ADMIN',
  });
}
export function useExportMutation() {
  return useMutation({
    mutationFn: (range: ReportRange) => reportsApi.exportCsv(range),
    retry: 0,
    gcTime: 0,
  });
}

import { useQuery } from '@tanstack/react-query';
import { schedulingApi } from '../api/schedulingApi';

export function useServiceBaysQuery(isActive = true) {
  return useQuery({
    queryKey: ['service-bays', { isActive }],
    queryFn: () => schedulingApi.getServiceBays({ isActive, size: 50, sortBy: 'bayCode', sortDir: 'asc' }),
  });
}

export function useActiveReservationsQuery() {
  return useQuery({
    queryKey: ['bay-reservations', 'active'],
    queryFn: () => schedulingApi.getActiveReservations(),
    refetchInterval: 15000,
  });
}

export function useBayReservationsQuery(bayId: number | string | null | undefined) {
  return useQuery({
    queryKey: ['bay-reservations', bayId],
    queryFn: () => (bayId ? schedulingApi.getBayReservations(bayId) : null),
    enabled: !!bayId,
  });
}

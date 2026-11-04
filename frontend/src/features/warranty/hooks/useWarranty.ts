import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { warrantyApi, type WarrantySearchParams } from '../api/warrantyApi';
import type {
  WarrantyAdjustInput,
  WarrantyClaimOpenInput,
  WarrantyClaimDecideInput,
  WarrantyVoidInput,
} from '../types/warranty';

export function useWarrantySearchQuery(params: WarrantySearchParams) {
  return useQuery({
    queryKey: ['warranties', 'search', params],
    queryFn: () => warrantyApi.searchWarranties(params),
  });
}

export function useClaimSearchQuery(params: WarrantySearchParams, enabled = true) {
  return useQuery({
    queryKey: ['warranty-claims', 'search', params],
    queryFn: () => warrantyApi.searchClaims(params),
    enabled,
  });
}

export function useVehicleWarrantiesQuery(vehicleId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['warranties', 'vehicle', vehicleId],
    queryFn: () => (vehicleId ? warrantyApi.getWarrantiesByVehicle(vehicleId) : null),
    enabled: !!vehicleId,
  });
}

export function useCustomerWarrantiesQuery(customerId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['warranties', 'customer', customerId],
    queryFn: () => (customerId ? warrantyApi.getWarrantiesByCustomer(customerId) : null),
    enabled: !!customerId,
  });
}

export function useWarrantyClaimsQuery(warrantyId: string | number | null | undefined) {
  return useQuery({
    queryKey: ['warranty-claims', warrantyId],
    queryFn: () => (warrantyId ? warrantyApi.getClaimsByWarranty(warrantyId) : null),
    enabled: !!warrantyId,
  });
}

export function useOpenClaimMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: WarrantyClaimOpenInput) => warrantyApi.openClaim(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['warranty-claims'] });
      queryClient.invalidateQueries({ queryKey: ['warranties'] });
    },
  });
}

export function useDecideClaimMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ claimId, data }: { claimId: string | number; data: WarrantyClaimDecideInput }) =>
      warrantyApi.decideClaim(claimId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['warranty-claims'] });
      queryClient.invalidateQueries({ queryKey: ['warranties'] });
      queryClient.invalidateQueries({ queryKey: ['repair-orders'] });
    },
  });
}

export function useVoidWarrantyMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: WarrantyVoidInput }) =>
      warrantyApi.voidWarranty(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['warranties'] });
    },
  });
}

export function useAdjustWarrantyMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: WarrantyAdjustInput }) =>
      warrantyApi.adjustWarranty(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['warranties'] });
    },
  });
}

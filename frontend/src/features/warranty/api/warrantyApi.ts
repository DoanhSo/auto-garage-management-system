import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  Warranty,
  WarrantyClaim,
  WarrantyClaimOpenInput,
  WarrantyClaimDecideInput,
  WarrantyAdjustInput,
  WarrantyVoidInput,
} from '../types/warranty';

export interface WarrantySearchParams {
  vehicleId?: string;
  customerId?: string;
}

/** The book and claim lists load one bounded page; the API caps page size at 100. */
export const WARRANTY_PAGE_SIZE = 100;

export const warrantyApi = {
  searchWarranties: (params: WarrantySearchParams) =>
    request<Page<Warranty>>(
      '/warranties' +
        queryString({
          ...params,
          page: 0,
          size: WARRANTY_PAGE_SIZE,
          sortBy: 'startsAt',
          sortDir: 'desc',
        }),
    ),

  searchClaims: (params: WarrantySearchParams) =>
    request<Page<WarrantyClaim>>(
      '/warranties/claims' +
        queryString({
          ...params,
          page: 0,
          size: WARRANTY_PAGE_SIZE,
          sortBy: 'requestedAt',
          sortDir: 'desc',
        }),
    ),

  getWarranty: (id: string | number) => request<Warranty>(`/warranties/${id}`),

  voidWarranty: (id: string | number, data: WarrantyVoidInput) =>
    request<Warranty>(`/warranties/${id}/void`, { method: 'POST', body: data }),

  adjustWarranty: (id: string | number, data: WarrantyAdjustInput) =>
    request<Warranty>(`/warranties/${id}/adjust`, { method: 'POST', body: data }),

  getWarrantiesByVehicle: (vehicleId: string | number, page = 0, size = 50) =>
    request<Page<Warranty>>(`/warranties/vehicle/${vehicleId}` + queryString({ page, size })),

  getWarrantiesByCustomer: (customerId: string | number, page = 0, size = 50) =>
    request<Page<Warranty>>(`/warranties/customer/${customerId}` + queryString({ page, size })),

  openClaim: (data: WarrantyClaimOpenInput) =>
    request<WarrantyClaim>('/warranties/claims', {
      method: 'POST',
      body: data,
    }),

  getClaim: (id: string | number) => request<WarrantyClaim>(`/warranties/claims/${id}`),

  getClaimsByWarranty: (warrantyId: string | number, page = 0, size = 50) =>
    request<Page<WarrantyClaim>>(`/warranties/${warrantyId}/claims` + queryString({ page, size })),

  decideClaim: (claimId: string | number, data: WarrantyClaimDecideInput) =>
    request<WarrantyClaim>(`/warranties/claims/${claimId}/decide`, {
      method: 'POST',
      body: data,
    }),
};

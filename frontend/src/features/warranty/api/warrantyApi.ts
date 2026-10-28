import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  Warranty,
  WarrantyClaim,
  WarrantyClaimOpenInput,
  WarrantyClaimDecideInput,
} from '../types/warranty';

export const warrantyApi = {
  getWarranty: (id: string | number) => request<Warranty>(`/warranties/${id}`),

  getWarrantiesByVehicle: (vehicleId: string | number, page = 0, size = 50) =>
    request<Page<Warranty>>(
      `/warranties/vehicle/${vehicleId}` + queryString({ page, size }),
    ),

  getWarrantiesByCustomer: (customerId: string | number, page = 0, size = 50) =>
    request<Page<Warranty>>(
      `/warranties/customer/${customerId}` + queryString({ page, size }),
    ),

  openClaim: (data: WarrantyClaimOpenInput) =>
    request<WarrantyClaim>('/warranties/claims', {
      method: 'POST',
      body: data,
    }),

  getClaim: (id: string | number) => request<WarrantyClaim>(`/warranties/claims/${id}`),

  getClaimsByWarranty: (warrantyId: string | number, page = 0, size = 50) =>
    request<Page<WarrantyClaim>>(
      `/warranties/${warrantyId}/claims` + queryString({ page, size }),
    ),

  decideClaim: (claimId: string | number, data: WarrantyClaimDecideInput) =>
    request<WarrantyClaim>(`/warranties/claims/${claimId}/decide`, {
      method: 'POST',
      body: data,
    }),
};

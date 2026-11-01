export type WarrantyStatus = 'ACTIVE' | 'EXPIRED' | 'VOID';
export type ClaimStatus = 'OPEN' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';

export const warrantyStatusLabels: Record<WarrantyStatus, string> = {
  ACTIVE: 'Còn hiệu lực',
  EXPIRED: 'Hết hạn bảo hành',
  VOID: 'Đã hủy / Vô hiệu',
};

export const claimStatusLabels: Record<ClaimStatus, string> = {
  OPEN: 'Mới tiếp nhận',
  IN_REVIEW: 'Đang xem xét',
  APPROVED: 'Chấp thuận bảo hành',
  REJECTED: 'Từ chối bảo hành',
};

export interface Warranty {
  id: string;
  warrantyCode: string;
  invoiceItemId: string;
  vehicleId: string;
  customerId: string;
  startsAt: string;
  expiresAt: string;
  startOdometer: number;
  endOdometer: number;
  warrantyDays: number;
  warrantyKm: number;
  termsSnapshot?: string | null;
  status: WarrantyStatus;
  createdAt: string;
}

export interface WarrantyClaim {
  id: string;
  claimCode: string;
  warrantyId: string;
  requestedBy: string;
  requestedAt: string;
  currentOdometer: number;
  description: string;
  status: ClaimStatus;
  decidedBy?: string | null;
  decidedAt?: string | null;
  decisionReason?: string | null;
  handlingOrderId?: string | null;
  closedAt?: string | null;
  version: string;
}

export interface WarrantyClaimOpenInput {
  warrantyId: number;
  currentOdometer: number;
  description: string;
}

export interface WarrantyClaimDecideInput {
  status: 'APPROVED' | 'REJECTED';
  decisionReason: string;
}

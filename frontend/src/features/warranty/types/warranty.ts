export type WarrantyStatus = 'ACTIVE' | 'EXPIRED' | 'VOID';
export type ClaimStatus = 'OPEN' | 'IN_REVIEW' | 'APPROVED' | 'IN_PROGRESS' | 'REJECTED' | 'CLOSED';

export const warrantyStatusLabels: Record<WarrantyStatus, string> = {
  ACTIVE: 'Còn hiệu lực',
  EXPIRED: 'Hết hạn bảo hành',
  VOID: 'Đã hủy / Vô hiệu',
};

export const claimStatusLabels: Record<ClaimStatus, string> = {
  OPEN: 'Mới tiếp nhận',
  IN_REVIEW: 'Đang xem xét',
  APPROVED: 'Chấp thuận bảo hành',
  IN_PROGRESS: 'Đang sửa chữa bảo hành',
  REJECTED: 'Từ chối bảo hành',
  CLOSED: 'Đã hoàn tất',
};

export interface Warranty {
  id: string;
  warrantyCode: string;
  invoiceItemId: string;
  vehicleId: string;
  customerId: string;
  startsAt: string;
  expiresAt?: string | null;
  startOdometer: number;
  endOdometer?: number | null;
  warrantyDays: number;
  warrantyKm: number;
  termsSnapshot?: string | null;
  status: WarrantyStatus;
  voidReason?: string | null;
  voidedAt?: string | null;
  adjustmentNote?: string | null;
  adjustedAt?: string | null;
  createdAt: string;
}

export interface WarrantyVoidInput {
  reason: string;
}

export interface WarrantyAdjustInput {
  warrantyDays: number;
  warrantyKm: number;
  termsSnapshot: string;
  reason: string;
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

/** Khớp WarrantyClaimDecideRequest của backend. */
export interface WarrantyClaimDecideInput {
  approved: boolean;
  reason: string;
  requestKey?: string;
}

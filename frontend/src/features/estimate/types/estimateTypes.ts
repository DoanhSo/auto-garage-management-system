export type EstimateKind = 'INITIAL' | 'SUPPLEMENTAL';
export type EstimateStatus = 'DRAFT' | 'SENT' | 'PARTIALLY_APPROVED' | 'APPROVED' | 'REJECTED' | 'EXPIRED';
export type ItemType = 'SERVICE' | 'PART';
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface EstimateItem {
  id: string;
  estimateId: string;
  lineNo: number;
  itemType: ItemType;
  serviceId?: string | null;
  partId?: string | null;
  itemName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  unitCost?: number | null;
  discountAmount?: number | null;
  taxAmount?: number | null;
  lineTotal: number;
  approvalStatus: ApprovalStatus;
  approvedBy?: string | null;
  decidedAt?: string | null;
  decisionNote?: string | null;
  reservedQuantity?: number | null;
  warrantyDays?: number | null;
  warrantyKm?: number | null;
  warrantyTerms?: string | null;
}

export interface Estimate {
  id: string;
  estimateCode: string;
  repairOrderId: string;
  versionNo: number;
  estimateKind: EstimateKind;
  parentEstimateId?: string | null;
  status: EstimateStatus;
  subtotal: number;
  discountAmount: number;
  taxAmount: number;
  totalAmount: number;
  notes?: string | null;
  createdBy: string;
  sentAt?: string | null;
  validUntil?: string | null;
  createdAt: string;
  version: number;
  items: EstimateItem[];
}

export interface LineDecision {
  estimateItemId: string | number;
  decision: 'APPROVED' | 'REJECTED';
  note?: string;
}

export interface EstimateDecisionRequest {
  lines: LineDecision[];
  requestKey?: string;
}

export interface EstimateItemCreate {
  itemType: ItemType;
  serviceId?: string | number | null;
  partId?: string | number | null;
  itemName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  taxAmount?: number;
  warrantyDays?: number;
  warrantyKm?: number;
  warrantyTerms?: string;
}

export interface EstimateCreateRequest {
  repairOrderId: string | number;
  estimateKind?: EstimateKind;
  parentEstimateId?: string | number | null;
  validUntil?: string | null;
  notes?: string;
  items: EstimateItemCreate[];
}

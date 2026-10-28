export * from './estimateTypes';

export const estimateStatusLabels: Record<string, string> = {
  DRAFT: 'Bản nháp',
  SENT: 'Đã gửi khách',
  APPROVED: 'Khách duyệt toàn bộ',
  PARTIALLY_APPROVED: 'Khách duyệt một phần',
  REJECTED: 'Khách từ chối',
  EXPIRED: 'Hết hiệu lực',
  SUPERSEDED: 'Bị thay thế bởi bản mới',
  CANCELLED: 'Đã hủy',
};

export interface EstimateItemCreateInput {
  itemType: 'SERVICE' | 'PART';
  serviceId?: number | null;
  partId?: number | null;
  itemName: string;
  unit: string;
  quantity: number;
  unitPrice: number;
  discountAmount?: number;
  warrantyDays?: number;
  warrantyKm?: number;
}

export interface EstimateCreateInput {
  repairOrderId: number;
  estimateKind: 'INITIAL' | 'SUPPLEMENTAL';
  parentEstimateId?: number | null;
  discountAmount?: number;
  taxAmount?: number;
  notes?: string | null;
  validDays?: number;
  items: EstimateItemCreateInput[];
}

export interface LineDecisionInput {
  itemId: number;
  status: 'APPROVED' | 'REJECTED';
  note?: string;
}

export interface EstimateDecisionInput {
  decisions: LineDecisionInput[];
  decisionNote?: string;
}

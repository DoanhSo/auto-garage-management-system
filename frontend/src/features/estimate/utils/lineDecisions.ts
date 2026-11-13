import type { EstimateItem, EstimateStatus, LineDecisionInput } from '../types/estimate';

export type Decision = LineDecisionInput['decision'];

/** Trạng thái báo giá còn duyệt được (khớp EstimateServiceImpl.decide; bản nháp được gửi rồi mới duyệt). */
export const DECIDABLE_STATUSES: readonly EstimateStatus[] = [
  'DRAFT',
  'SENT',
  'PARTIALLY_APPROVED',
  'APPROVED',
];

export function canDecideEstimate(status: string): boolean {
  return (DECIDABLE_STATUSES as readonly string[]).includes(status);
}

/** Các dòng còn chờ quyết định. */
export function pendingItems(items: readonly EstimateItem[]): EstimateItem[] {
  return items.filter((item) => item.approvalStatus === 'PENDING');
}

/**
 * Lập danh sách quyết định gửi lên máy chủ: chỉ các dòng còn chờ.
 * `id` của dòng trong dữ liệu thật là số dù khai báo là chuỗi, nên luôn so sánh qua `String(...)`.
 */
export function buildLineDecisions(
  items: readonly EstimateItem[],
  decisions: Readonly<Record<string, Decision>>,
  note?: string,
): LineDecisionInput[] {
  return pendingItems(items).map((item) => ({
    estimateItemId: Number(item.id),
    decision: decisions[String(item.id)] ?? 'APPROVED',
    note,
  }));
}

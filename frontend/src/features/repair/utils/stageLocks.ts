import type { Estimate } from '../../estimate/types/estimateTypes';
import type { AttachmentCategory } from '../../../shared/types/contracts';
import type { RepairOrderStatus } from '../types/repairTypes';

/**
 * Mirrors backend RepairStageRules: data of a finished stage is a read-only record.
 * The backend is what enforces it; these helpers only keep the UI from offering dead ends.
 * See docs/REPAIR_ORDER_LIFECYCLE.md.
 */

const DIAGNOSIS_EDITABLE: RepairOrderStatus[] = ['NEW', 'DIAGNOSING'];
const INITIAL_ESTIMATE_ALLOWED: RepairOrderStatus[] = ['NEW', 'DIAGNOSING', 'WAITING_APPROVAL'];
const SUPPLEMENTAL_ALLOWED: RepairOrderStatus[] = [
  'APPROVED',
  'IN_PROGRESS',
  'WAITING_PARTS',
  'QC_FAILED',
];

const ATTACHMENT_WINDOWS: Record<AttachmentCategory, RepairOrderStatus[]> = {
  CHECK_IN: ['NEW'],
  DIAGNOSIS: DIAGNOSIS_EDITABLE,
  QC: ['WAITING_QC', 'QC_FAILED', 'COMPLETED'],
  HANDOVER: ['COMPLETED', 'INVOICED', 'CLOSED'],
  WARRANTY: [],
};

export const isDiagnosisLocked = (status: RepairOrderStatus) =>
  !DIAGNOSIS_EDITABLE.includes(status);

/** Why the diagnosis can no longer change, or null while it is still open. */
export function diagnosisLockReason(status: RepairOrderStatus): string | null {
  if (!isDiagnosisLocked(status)) return null;
  if (status === 'CANCELLED') return 'Phiếu đã hủy.';
  if (status === 'WAITING_APPROVAL') {
    return 'Đã lập báo giá nên chẩn đoán được giữ nguyên làm căn cứ. Chỉ mở lại được khi khách từ chối toàn bộ báo giá.';
  }
  return 'Khách đã duyệt báo giá. Thay đổi phạm vi sửa chữa hãy lập báo giá phát sinh.';
}

/** An estimate still waiting on the customer; an expired one counts as closed. */
const awaitsCustomer = (estimate: Estimate) =>
  estimate.status === 'SENT' || estimate.status === 'PARTIALLY_APPROVED';

/** Reopening is the one way back: nothing approved and nothing still awaiting the customer. */
export function canReopenDiagnosis(status: RepairOrderStatus, estimates: Estimate[]): boolean {
  if (status !== 'WAITING_APPROVAL') return false;
  const anyApproved = estimates.some((estimate) =>
    estimate.items?.some((item) => item.approvalStatus === 'APPROVED'),
  );
  return !anyApproved && !estimates.some(awaitsCustomer);
}

export type NewEstimateMode =
  { kind: 'INITIAL' } | { kind: 'SUPPLEMENTAL'; parentEstimateId: string | number } | null;

/** Which estimate a technician may draft now, or null when none. */
export function newEstimateMode(status: RepairOrderStatus, estimates: Estimate[]): NewEstimateMode {
  const approved = estimates
    .filter(
      (estimate) => estimate.status === 'APPROVED' || estimate.status === 'PARTIALLY_APPROVED',
    )
    .sort((a, b) => b.versionNo - a.versionNo);
  if (approved.length > 0) {
    if (!SUPPLEMENTAL_ALLOWED.includes(status)) return null;
    const parent = approved.find((estimate) => estimate.estimateKind === 'INITIAL') ?? approved[0];
    return { kind: 'SUPPLEMENTAL', parentEstimateId: parent.id };
  }
  return INITIAL_ESTIMATE_ALLOWED.includes(status) ? { kind: 'INITIAL' } : null;
}

/** Line decisions are possible only on an estimate sent to the customer and still valid. */
export const canDecideOn = (estimate: Estimate) => awaitsCustomer(estimate);

export const allowedAttachmentCategories = (
  status: RepairOrderStatus,
  candidates: AttachmentCategory[],
): AttachmentCategory[] =>
  candidates.filter((category) => ATTACHMENT_WINDOWS[category].includes(status));

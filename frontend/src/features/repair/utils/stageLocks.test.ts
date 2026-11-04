import { describe, expect, it } from 'vitest';
import type { Estimate, EstimateItem } from '../../estimate/types/estimateTypes';
import {
  allowedAttachmentCategories,
  canDecideOn,
  canReopenDiagnosis,
  diagnosisLockReason,
  isDiagnosisLocked,
  newEstimateMode,
} from './stageLocks';

const item = (approvalStatus: EstimateItem['approvalStatus']) =>
  ({ id: 'i', approvalStatus }) as EstimateItem;

const estimate = (patch: Partial<Estimate>): Estimate =>
  ({
    id: '1',
    versionNo: 1,
    estimateKind: 'INITIAL',
    status: 'DRAFT',
    items: [],
    ...patch,
  }) as Estimate;

describe('stageLocks', () => {
  it('keeps the diagnosis open only before an estimate exists', () => {
    expect(isDiagnosisLocked('NEW')).toBe(false);
    expect(isDiagnosisLocked('DIAGNOSING')).toBe(false);
    expect(diagnosisLockReason('DIAGNOSING')).toBeNull();
    expect(diagnosisLockReason('WAITING_APPROVAL')).toMatch(/từ chối toàn bộ/);
    expect(diagnosisLockReason('IN_PROGRESS')).toMatch(/báo giá phát sinh/);
    expect(diagnosisLockReason('CLOSED')).not.toBeNull();
  });

  it('reopens the diagnosis only after the customer turned everything down', () => {
    const rejected = estimate({ status: 'REJECTED', items: [item('REJECTED')] });
    const draft = estimate({ id: '2', versionNo: 2, status: 'DRAFT' });
    expect(canReopenDiagnosis('WAITING_APPROVAL', [rejected, draft])).toBe(true);
    expect(canReopenDiagnosis('WAITING_APPROVAL', [estimate({ status: 'SENT' })])).toBe(false);
    expect(
      canReopenDiagnosis('WAITING_APPROVAL', [
        estimate({ status: 'EXPIRED', items: [item('APPROVED')] }),
      ]),
    ).toBe(false);
    expect(canReopenDiagnosis('APPROVED', [rejected])).toBe(false);
  });

  it('switches to supplemental estimates once anything is approved', () => {
    expect(newEstimateMode('DIAGNOSING', [])).toEqual({ kind: 'INITIAL' });
    expect(newEstimateMode('WAITING_APPROVAL', [estimate({ status: 'REJECTED' })])).toEqual({
      kind: 'INITIAL',
    });
    const approved = estimate({ id: '7', status: 'PARTIALLY_APPROVED' });
    expect(newEstimateMode('IN_PROGRESS', [approved])).toEqual({
      kind: 'SUPPLEMENTAL',
      parentEstimateId: '7',
    });
    expect(newEstimateMode('WAITING_QC', [approved])).toBeNull();
    expect(newEstimateMode('COMPLETED', [])).toBeNull();
  });

  it('lets lines be decided only on estimates awaiting the customer', () => {
    expect(canDecideOn(estimate({ status: 'SENT' }))).toBe(true);
    expect(canDecideOn(estimate({ status: 'PARTIALLY_APPROVED' }))).toBe(true);
    expect(canDecideOn(estimate({ status: 'DRAFT' }))).toBe(false);
    expect(canDecideOn(estimate({ status: 'EXPIRED' }))).toBe(false);
    expect(canDecideOn(estimate({ status: 'CANCELLED' }))).toBe(false);
  });

  it('offers only photo categories of the current stage', () => {
    const both = ['DIAGNOSIS', 'CHECK_IN'] as const;
    expect(allowedAttachmentCategories('NEW', [...both])).toEqual(['DIAGNOSIS', 'CHECK_IN']);
    expect(allowedAttachmentCategories('DIAGNOSING', [...both])).toEqual(['DIAGNOSIS']);
    expect(allowedAttachmentCategories('IN_PROGRESS', [...both])).toEqual([]);
    expect(allowedAttachmentCategories('WAITING_QC', ['QC'])).toEqual(['QC']);
  });
});

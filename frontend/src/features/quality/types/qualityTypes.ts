export type CheckResult = 'PASSED' | 'FAILED';
export type CheckType = 'INITIAL' | 'QC' | 'HANDOVER';

export interface QualityCheck {
  id: string;
  repairOrderId: string;
  checkType: CheckType;
  attemptNo: number;
  result: CheckResult;
  checklistJson: string;
  schemaVersion?: number;
  odometer?: number | null;
  notes?: string | null;
  checkedBy: string;
  checkedAt: string;
}

export interface QualityCheckCreateRequest {
  repairOrderId: string | number;
  result: CheckResult;
  checklistJson?: string;
  odometer?: number | null;
  notes?: string | null;
  reworkTaskIds?: Array<string | number>;
}

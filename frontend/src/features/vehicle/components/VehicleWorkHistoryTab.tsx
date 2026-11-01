import { Search, ShieldCheck, Wrench, type LucideIcon } from 'lucide-react';
import { Badge, Empty, Loading, QueryError } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import { shiftTypeLabels } from '../../scheduling/types/scheduling';
import { useVehicleWorkHistoryQuery } from '../hooks/useVehicles';
import {
  assignmentStageLabels,
  workResultLabels,
  type AssignmentStage,
  type WorkResultStatus,
} from '../types/workHistory';

const stageIcon: Record<AssignmentStage, { icon: LucideIcon; className: string }> = {
  DIAGNOSIS: { icon: Search, className: 'stage-diagnosis' },
  REPAIR_MAIN: { icon: Wrench, className: 'stage-repair' },
  REPAIR_ASSISTANT: { icon: Wrench, className: 'stage-repair' },
  QC_INSPECTION: { icon: ShieldCheck, className: 'stage-qc' },
};

const resultVariant: Record<WorkResultStatus, 'secondary' | 'success' | 'danger' | 'primary'> = {
  IN_PROGRESS: 'secondary',
  PASSED: 'success',
  COMPLETED: 'primary',
  REWORK_FAILED: 'danger',
};

export function VehicleWorkHistoryTab({ vehicleId }: { vehicleId: string | null }) {
  const query = useVehicleWorkHistoryQuery(vehicleId);

  if (query.isLoading) return <Loading label="Đang tải lịch sử thi công…" />;
  if (query.isError) return <QueryError error={query.error} retry={() => void query.refetch()} />;
  const rows = query.data ?? [];
  if (rows.length === 0)
    return (
      <Empty
        title="Chưa có lịch sử thi công"
        description="Các công đoạn chẩn đoán, sửa chữa và kiểm thử sẽ được lưu vết tại đây."
      />
    );

  return (
    <ol className="timeline" aria-label="Lịch sử thi công và nhân sự">
      {rows.map((row) => {
        const { icon: Icon, className } = stageIcon[row.stage];
        return (
          <li key={row.id} className="timeline-item">
            <span className={`timeline-dot ${className}`}>
              <Icon size={16} aria-hidden />
            </span>
            <div className="timeline-body">
              <p>
                <strong>{assignmentStageLabels[row.stage]}</strong> · {row.taskName}{' '}
                <Badge variant={resultVariant[row.resultStatus]}>
                  {workResultLabels[row.resultStatus]}
                </Badge>
              </p>
              <p className="muted">
                {row.employeeName ?? '—'}
                {row.shiftType ? ` · ${shiftTypeLabels[row.shiftType]}` : ''} · Lệnh #
                {row.orderCode ?? row.repairOrderId}
              </p>
              <p className="muted tabular">
                {formatDateTime(row.startedAt)}
                {row.completedAt
                  ? ` → ${formatDateTime(row.completedAt)}`
                  : ' · đang thực hiện'} · {row.actualMinutes} phút
              </p>
              {row.notes && <p>{row.notes}</p>}
            </div>
          </li>
        );
      })}
    </ol>
  );
}

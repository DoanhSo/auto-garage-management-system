import { useEffect, useState } from 'react';
import { Pause, Play, UserMinus, UserPlus } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Loading, QueryError } from '../../../shared/components/ui';
import {
  useReleaseAssignmentMutation,
  useStartTimerMutation,
  useStopTimerMutation,
  useTaskSummaryQuery,
} from '../hooks/useRepair';
import type { AssignmentRole, RepairTask, TaskAssignment } from '../types/repairTypes';

const roleLabels: Record<AssignmentRole, string> = {
  MAIN: 'Thợ chính',
  ASSISTANT: 'Thợ phụ',
  QC: 'QC',
};

function clock(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(Math.floor(safe / 3600))}:${pad(Math.floor((safe % 3600) / 60))}:${pad(safe % 60)}`;
}

function useNow(active: boolean): number {
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    if (!active) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [active]);
  return now;
}

export function TaskWorkPanel({
  task,
  canAssign,
  ownEmployeeId,
  canOperate,
  onAssign,
}: {
  task: RepairTask;
  canAssign: boolean;
  ownEmployeeId?: string | number | null;
  canOperate: boolean;
  onAssign: (task: RepairTask) => void;
}) {
  const notify = useToast();
  const summary = useTaskSummaryQuery(task.id);
  const start = useStartTimerMutation();
  const stop = useStopTimerMutation();
  const release = useReleaseAssignmentMutation();

  const data = summary.data;
  const openSessions = new Map(
    (data?.sessions ?? [])
      .filter((s) => !s.endsAt)
      .map((s) => [String(s.assignmentId), s] as const),
  );
  const now = useNow(openSessions.size > 0);
  const finished = task.status === 'COMPLETED' || task.status === 'CANCELLED';

  const run = async (action: () => Promise<unknown>, success?: string) => {
    try {
      await action();
      if (success) notify(success);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const secondsOf = (assignment: TaskAssignment): number =>
    (data?.sessions ?? [])
      .filter((s) => String(s.assignmentId) === String(assignment.id))
      .reduce((sum, s) => {
        const end = s.endsAt ? new Date(s.endsAt).getTime() : now;
        return sum + Math.max(0, (end - new Date(s.startsAt).getTime()) / 1000);
      }, 0);

  const canTouch = (assignment: TaskAssignment) =>
    canOperate && (canAssign || String(assignment.employeeId) === String(ownEmployeeId));

  return (
    <div className="task-work-panel">
      <div className="row-actions">
        <strong className="text-sm">Phân công & bấm giờ</strong>
        <span className="grow" />
        {data && (
          <span className="muted tabular">Tổng {clock(data.totalActualSeconds)}</span>
        )}
        {canAssign && !finished && (
          <Button variant="secondary" onClick={() => onAssign(task)}>
            <UserPlus size={14} /> Phân công
          </Button>
        )}
      </div>

      {summary.isLoading && <Loading label="Đang tải phân công…" />}
      {summary.isError && <QueryError error={summary.error} retry={() => void summary.refetch()} />}
      {data && data.assignments.length === 0 && (
        <p className="muted">
          Chưa có ai được phân công. Bấm "Phân công" để chọn thợ đang trực ca.
        </p>
      )}
      {data && data.assignments.length > 0 && (
        <ul className="task-work-list">
          {data.assignments.map((assignment) => {
            const running = openSessions.has(String(assignment.id));
            return (
              <li key={assignment.id} className={assignment.isActive ? '' : 'released'}>
                <div>
                  <strong>{assignment.employeeName ?? `NV #${assignment.employeeId}`}</strong>{' '}
                  <Badge variant={assignment.assignmentRole === 'MAIN' ? 'primary' : 'secondary'}>
                    {roleLabels[assignment.assignmentRole]}
                  </Badge>{' '}
                  {!assignment.isActive && <Badge variant="outline">Đã kết thúc</Badge>}
                  {running && <Badge variant="warning">Đang bấm giờ</Badge>}
                </div>
                {assignment.assignmentRole !== 'QC' && (
                  <span className="tabular muted">{clock(secondsOf(assignment))}</span>
                )}
                {assignment.isActive && !finished && (
                  <div className="row-actions">
                    {assignment.assignmentRole !== 'QC' && canTouch(assignment) && !running && (
                      <Button
                        variant="primary"
                        pending={start.isPending}
                        onClick={() =>
                          void run(() => start.mutateAsync(assignment.id), 'Đã bắt đầu bấm giờ')
                        }
                      >
                        <Play size={14} /> Bắt đầu
                      </Button>
                    )}
                    {running && canTouch(assignment) && (
                      <Button
                        variant="secondary"
                        pending={stop.isPending}
                        onClick={() =>
                          void run(
                            () =>
                              stop.mutateAsync({
                                assignmentId: assignment.id,
                                stopReason: 'PAUSE',
                              }),
                            'Đã tạm dừng bấm giờ',
                          )
                        }
                      >
                        <Pause size={14} /> Tạm dừng
                      </Button>
                    )}
                    {canAssign && (
                      <Button
                        variant="ghost"
                        pending={release.isPending}
                        onClick={() =>
                          void run(
                            () => release.mutateAsync(assignment.id),
                            'Đã giải phóng phân công',
                          )
                        }
                      >
                        <UserMinus size={14} /> Gỡ
                      </Button>
                    )}
                  </div>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}

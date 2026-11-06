import { useEffect, useMemo, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Modal } from '../../../shared/components/ui';
import { useOnShiftTechniciansQuery } from '../../scheduling/hooks/useScheduling';
import { SKILL_LABELS, availabilityLabels } from '../../scheduling/types/scheduling';
import { useAssignTaskMutation } from '../hooks/useRepair';
import type { AssignmentRole, RepairTask } from '../types/repairTypes';
import { DEFAULT_ASSIGNMENT_HOURS, toLocalInput } from '../utils/assignmentWindow';

const roleLabels: Record<AssignmentRole, string> = {
  MAIN: 'Thợ chính',
  ASSISTANT: 'Thợ phụ',
  QC: 'Kiểm định chất lượng (QC)',
};

export function AssignTaskModal({
  task,
  onClose,
}: {
  task: RepairTask | null;
  onClose: () => void;
}) {
  const notify = useToast();
  const assign = useAssignTaskMutation();
  const onShift = useOnShiftTechniciansQuery(!!task);
  const technicians = useMemo(() => onShift.data ?? [], [onShift.data]);

  const [employeeId, setEmployeeId] = useState('');
  const [role, setRole] = useState<AssignmentRole>('MAIN');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');

  const selected = technicians.find((tech) => String(tech.employeeId) === employeeId);

  useEffect(() => {
    if (!task) return;
    setEmployeeId('');
    setRole('MAIN');
    const now = new Date();
    setStartsAt(toLocalInput(now));
    setEndsAt(toLocalInput(new Date(now.getTime() + DEFAULT_ASSIGNMENT_HOURS * 3_600_000)));
  }, [task]);

  const pickEmployee = (id: string) => {
    setEmployeeId(id);
    const tech = technicians.find((t) => String(t.employeeId) === id);
    if (!tech) return;
    const shiftEnd = new Date(tech.shiftEndsAt).getTime();
    const wanted = Date.now() + DEFAULT_ASSIGNMENT_HOURS * 3_600_000;
    setEndsAt(toLocalInput(new Date(Math.min(shiftEnd, wanted))));
    if (role === 'QC' && !tech.canQc) setRole('MAIN');
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!task) return;
    if (!employeeId) {
      notify('Chọn kỹ thuật viên đang trong ca trực');
      return;
    }
    const start = new Date(startsAt);
    const end = new Date(endsAt);
    if (!(end > start)) {
      notify('Thời gian kết thúc phải sau thời gian bắt đầu');
      return;
    }
    try {
      await assign.mutateAsync({
        taskId: task.id,
        employeeId,
        assignmentRole: role,
        startsAt: start.toISOString(),
        endsAt: end.toISOString(),
      });
      notify('Đã phân công công việc');
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  return (
    <Modal
      open={!!task}
      title="Phân công công việc"
      description={task ? task.taskName : ''}
      onOpenChange={(open) => !open && !assign.isPending && onClose()}
    >
      <form onSubmit={submit} noValidate className="editor-form account-editor-form">
        <FormError message={assign.error ? errorMessage(assign.error) : undefined} />

        <div className="full-width-field">
          <Field
            label="Kỹ thuật viên đang trực ca"
            required
            hint={
              technicians.length === 0
                ? 'Chưa có ai trong ca trực. Hãy xếp ca ở màn Khoang & lịch ca.'
                : 'Theo ca đang trực lúc này, không phải ca của ngày nhập xe'
            }
          >
            <select
              className="select"
              value={employeeId}
              disabled={assign.isPending}
              onChange={(e) => pickEmployee(e.target.value)}
            >
              <option value="">-- Chọn kỹ thuật viên --</option>
              {technicians.map((tech) => (
                <option key={tech.employeeId} value={tech.employeeId}>
                  {tech.fullName} ·{' '}
                  {tech.skills.map((s) => SKILL_LABELS[s] ?? s).join(', ') || 'Đa năng'}
                  {tech.canQc ? ' · QC' : ''} · {availabilityLabels[tech.availabilityStatus]}
                </option>
              ))}
            </select>
          </Field>
        </div>

        <Field label="Vai trò" required>
          <select
            className="select"
            value={role}
            disabled={assign.isPending}
            onChange={(e) => setRole(e.target.value as AssignmentRole)}
          >
            {(Object.keys(roleLabels) as AssignmentRole[]).map((key) => (
              <option
                key={key}
                value={key}
                disabled={key === 'QC' && selected ? !selected.canQc : false}
              >
                {roleLabels[key]}
              </option>
            ))}
          </select>
        </Field>
        <div />

        <Field label="Bắt đầu" required>
          <input
            type="datetime-local"
            className="input"
            value={startsAt}
            disabled={assign.isPending}
            onChange={(e) => setStartsAt(e.target.value)}
          />
        </Field>
        <Field label="Kết thúc" required>
          <input
            type="datetime-local"
            className="input"
            value={endsAt}
            disabled={assign.isPending}
            onChange={(e) => setEndsAt(e.target.value)}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={assign.isPending}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={assign.isPending}>
            Phân công
          </Button>
        </div>
      </form>
    </Modal>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Modal } from '../../../shared/components/ui';
import { useOnShiftTechniciansQuery } from '../../scheduling/hooks/useScheduling';
import { SKILL_LABELS, availabilityLabels } from '../../scheduling/types/scheduling';
import { useAssignQcInspectorMutation } from '../hooks/useRepair';
import { DEFAULT_ASSIGNMENT_HOURS, toLocalInput } from '../utils/assignmentWindow';

export function AssignQcModal({
  orderId,
  open,
  onClose,
}: {
  orderId: string | number;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const assign = useAssignQcInspectorMutation();
  const onShift = useOnShiftTechniciansQuery(open);
  const inspectors = useMemo(
    () => (onShift.data ?? []).filter((tech) => tech.canQc),
    [onShift.data],
  );

  const [employeeId, setEmployeeId] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');

  useEffect(() => {
    if (!open) return;
    setEmployeeId('');
    const now = new Date();
    setStartsAt(toLocalInput(now));
    setEndsAt(toLocalInput(new Date(now.getTime() + DEFAULT_ASSIGNMENT_HOURS * 3_600_000)));
  }, [open]);

  const pickEmployee = (id: string) => {
    setEmployeeId(id);
    const tech = inspectors.find((t) => String(t.employeeId) === id);
    if (!tech) return;
    const shiftEnd = new Date(tech.shiftEndsAt).getTime();
    const wanted = Date.now() + DEFAULT_ASSIGNMENT_HOURS * 3_600_000;
    setEndsAt(toLocalInput(new Date(Math.min(shiftEnd, wanted))));
  };

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!employeeId) {
      notify('Chọn kỹ thuật viên QC đang trong ca trực');
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
        orderId,
        employeeId,
        startsAt: start.toISOString(),
        endsAt: end.toISOString(),
      });
      notify('Đã phân công kỹ thuật viên QC');
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  return (
    <Modal
      open={open}
      title="Phân công kỹ thuật viên QC"
      description="Người được chọn sẽ kiểm định xe. Thợ chính đã sửa phiếu này không được chọn."
      onOpenChange={(next) => !next && !assign.isPending && onClose()}
    >
      <form onSubmit={submit} noValidate className="editor-form account-editor-form">
        <FormError message={assign.error ? errorMessage(assign.error) : undefined} />

        <div className="full-width-field">
          <Field
            label="Kỹ thuật viên QC đang trực ca"
            required
            hint={
              inspectors.length === 0
                ? 'Chưa có kỹ thuật viên QC nào trong ca trực. Hãy xếp ca ở màn Khoang & lịch ca.'
                : 'Chỉ hiển thị người có quyền QC và có ca trực bao phủ thời điểm hiện tại'
            }
          >
            <select
              className="select"
              value={employeeId}
              disabled={assign.isPending}
              onChange={(e) => pickEmployee(e.target.value)}
            >
              <option value="">-- Chọn kỹ thuật viên --</option>
              {inspectors.map((tech) => (
                <option key={tech.employeeId} value={tech.employeeId}>
                  {tech.fullName} ·{' '}
                  {tech.skills.map((s) => SKILL_LABELS[s] ?? s).join(', ') || 'Đa năng'} ·{' '}
                  {availabilityLabels[tech.availabilityStatus]}
                </option>
              ))}
            </select>
          </Field>
        </div>

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

import { useState, useEffect } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import {
  Button,
  Combobox,
  ConfirmDialog,
  Field,
  FormError,
  Sheet,
} from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { ApiError, errorMessage } from '../../../shared/api/errors';
import {
  useCreateShiftMutation,
  useSchedulingEmployeesQuery,
  useServiceBaysQuery,
} from '../hooks/useScheduling';
import {
  SKILL_LABELS,
  bayTypeLabels,
  parseSkills,
  shiftTypeLabels,
  shiftTypeWindows,
  type ShiftType,
} from '../types/scheduling';

function expectedHours(startsAt: string, endsAt: string): string {
  const diff = (new Date(endsAt).getTime() - new Date(startsAt).getTime()) / 3_600_000;
  return Number.isFinite(diff) && diff > 0 ? `${diff.toFixed(1).replace('.', ',')} giờ` : '—';
}

export function ShiftEditor({
  open,
  initialEmployeeId,
  initialServiceBayId,
  onClose,
  onSaved,
}: {
  open: boolean;
  initialEmployeeId?: string | number | null;
  initialServiceBayId?: string | number | null;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const createMutation = useCreateShiftMutation();
  const [discard, setDiscard] = useState(false);

  const [employeeId, setEmployeeId] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [notes, setNotes] = useState('');
  const [shiftType, setShiftType] = useState<ShiftType>('FULL_DAY');
  const [serviceBayId, setServiceBayId] = useState('');

  const bays = useServiceBaysQuery({ isActive: true, page: 0, size: 100 });
  const { data: employees = [] } = useSchedulingEmployeesQuery({ active: true });

  const selectedEmployee = employees.find((emp) => String(emp.id) === employeeId);
  const selectedBay = (bays.data?.content ?? []).find((bay) => String(bay.id) === serviceBayId);
  const employeeLabel = (emp: { fullName?: string | null; employeeCode: string }) =>
    `${emp.fullName ?? emp.employeeCode} · ${emp.employeeCode}`;

  const applyPreset = (type: ShiftType) => {
    const day = startsAt.slice(0, 10);
    setShiftType(type);
    setStartsAt(`${day}T${shiftTypeWindows[type].start}`);
    setEndsAt(`${day}T${shiftTypeWindows[type].end}`);
  };

  useEffect(() => {
    if (open) {
      setEmployeeId(initialEmployeeId ? String(initialEmployeeId) : '');
      // Mặc định ca hôm nay theo giờ địa phương 08:00 đến 17:00
      const now = new Date();
      const pad = (n: number) => String(n).padStart(2, '0');
      const localToday = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
      setStartsAt(`${localToday}T08:00`);
      setEndsAt(`${localToday}T17:00`);
      setNotes('');
      setShiftType('FULL_DAY');
      setServiceBayId(initialServiceBayId ? String(initialServiceBayId) : '');
      setDiscard(false);
    } else {
      setEmployeeId('');
      setNotes('');
      setServiceBayId('');
      setDiscard(false);
    }
  }, [open, initialEmployeeId, initialServiceBayId]);

  const isDirty = initialEmployeeId
    ? employeeId !== String(initialEmployeeId) ||
      serviceBayId !== (initialServiceBayId ? String(initialServiceBayId) : '') ||
      notes.trim() !== '' ||
      shiftType !== 'FULL_DAY'
    : !!employeeId || !!notes.trim() || !!serviceBayId || shiftType !== 'FULL_DAY';
  const busy = createMutation.isPending;
  const blocker = useUnsavedChangesGuard(open && isDirty && !busy);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!employeeId || !startsAt || !endsAt) {
      notify('Vui lòng điền đầy đủ kỹ thuật viên và thời gian ca làm');
      return;
    }

    const startDate = new Date(startsAt);
    const endDate = new Date(endsAt);

    if (endDate <= startDate) {
      notify('Thời gian kết thúc ca làm phải sau thời gian bắt đầu');
      return;
    }

    try {
      await createMutation.mutateAsync({
        employeeId: Number(employeeId),
        startsAt: startDate.toISOString(),
        endsAt: endDate.toISOString(),
        shiftType,
        serviceBayId: serviceBayId ? Number(serviceBayId) : undefined,
        notes: notes.trim() || undefined,
      });
      notify('Phân công ca làm việc thành công');
      onSaved();
      onClose();
    } catch (err: unknown) {
      if (err instanceof ApiError && !err.detail && (err.code === 2003 || err.status === 409)) {
        notify(
          'Kỹ thuật viên đã có ca làm việc khác trong khoảng thời gian đã chọn (trùng ca làm)',
        );
      } else {
        notify(errorMessage(err));
      }
    }
  };

  const handleClose = () => {
    if (busy) return;
    if (isDirty) setDiscard(true);
    else onClose();
  };

  return (
    <>
      <Sheet
        open={open}
        placement="center"
        title="Phân ca làm việc kỹ thuật viên"
        description="Lập lịch làm việc cho kỹ thuật viên. Hệ thống tự động kiểm tra trùng lặp thời gian ca làm."
        onClose={handleClose}
      >
        <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
          <FormError message={createMutation.error?.message} />

          <div className="full-width-field">
            <Field label="Kỹ thuật viên" required>
              <Combobox
                value={employeeId}
                onChange={setEmployeeId}
                disabled={busy}
                placeholder="Gõ tên hoặc mã để tìm kỹ thuật viên…"
                options={employees.map((emp) => ({
                  value: String(emp.id),
                  label: employeeLabel(emp),
                  description:
                    [
                      parseSkills(emp.skillsJson)
                        .map((s) => SKILL_LABELS[s] ?? s)
                        .join(', '),
                      emp.canQc ? 'QC' : '',
                    ]
                      .filter(Boolean)
                      .join(' · ') || undefined,
                }))}
              />
            </Field>
          </div>

          <Field
            label="Khoang trực"
            hint={
              selectedBay
                ? `${bayTypeLabels[selectedBay.bayType] ?? selectedBay.bayType} · tối đa 2 thợ (1 chính, 1 phụ) cùng trực`
                : 'Nếu không chọn khoang, thợ sẽ vào danh sách thợ cơ động để điều động linh hoạt khi tiếp nhận xe.'
            }
          >
            <Combobox
              value={serviceBayId}
              onChange={setServiceBayId}
              disabled={busy}
              placeholder="Không gán khoang (Thợ cơ động / Hỗ trợ toàn xưởng / QC)"
              options={(bays.data?.content ?? []).map((bay) => ({
                value: String(bay.id),
                label: `${bay.bayCode} · ${bayTypeLabels[bay.bayType] ?? bay.bayName}`,
              }))}
            />
          </Field>

          <Field label="Loại ca">
            <select
              className="select"
              value={shiftType}
              onChange={(e) => applyPreset(e.target.value as ShiftType)}
              disabled={busy}
            >
              {Object.entries(shiftTypeLabels).map(([key, label]) => (
                <option key={key} value={key}>
                  {label} {shiftTypeWindows[key as ShiftType].start}–
                  {shiftTypeWindows[key as ShiftType].end}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Thời gian bắt đầu ca" required>
            <input
              type="datetime-local"
              className="input"
              value={startsAt}
              onChange={(e) => setStartsAt(e.target.value)}
              disabled={busy}
              required
            />
          </Field>

          <Field label="Thời gian kết thúc ca" required>
            <input
              type="datetime-local"
              className="input"
              value={endsAt}
              onChange={(e) => setEndsAt(e.target.value)}
              disabled={busy}
              required
            />
          </Field>

          <div className="full-width-field info-strip" aria-live="polite">
            <span>
              <strong>
                {selectedEmployee ? employeeLabel(selectedEmployee) : 'Chưa chọn thợ'}
              </strong>
              {' → '}
              <strong>{selectedBay ? selectedBay.bayCode : 'thợ cơ động'}</strong>
              {' · '}
              {shiftTypeLabels[shiftType]} · {expectedHours(startsAt, endsAt)}
            </span>
          </div>
          <div className="full-width-field">
            <p className="description">
              Giới hạn: tối đa 12 giờ/ngày, 60 giờ/tuần và nghỉ ít nhất 8 giờ giữa hai ca ở hai ngày
              liên tiếp. Hệ thống sẽ báo ngay nếu ca vi phạm.
            </p>
          </div>

          <div className="full-width-field">
            <Field label="Ghi chú ca làm">
              <input
                type="text"
                placeholder="Ca chính xưởng sửa chữa, trực hỗ trợ..."
                className="input"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                disabled={busy}
              />
            </Field>
          </div>

          <div className="form-actions account-editor-actions">
            <Button disabled={busy} onClick={handleClose}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={busy}>
              {busy ? 'Đang xếp ca…' : 'Xác nhận xếp ca'}
            </Button>
          </div>
        </form>
      </Sheet>

      <ConfirmDialog
        open={open && (discard || blocker.state === 'blocked')}
        title="Bỏ thay đổi chưa lưu?"
        description="Thông tin ca làm việc đang nhập sẽ bị hủy."
        onCancel={() => {
          setDiscard(false);
          blocker.reset?.();
        }}
        onConfirm={() => {
          setDiscard(false);
          blocker.proceed?.();
          onClose();
        }}
      />
    </>
  );
}

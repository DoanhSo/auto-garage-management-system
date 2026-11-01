import { useState, useEffect } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { ApiError, errorMessage } from '../../../shared/api/errors';
import { useUsersQuery } from '../../identity/hooks/useUsers';
import {
  useCreateShiftMutation,
  useSchedulingEmployeesQuery,
  useServiceBaysQuery,
} from '../hooks/useScheduling';
import {
  SKILL_LABELS,
  parseSkills,
  shiftTypeLabels,
  shiftTypeWindows,
  type ShiftType,
} from '../types/scheduling';

const SHIFT_PRESETS: ShiftType[] = ['MORNING', 'AFTERNOON', 'FULL_DAY', 'OVERTIME'];

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
  const users = useUsersQuery({ page: 0, size: 100, sortBy: 'fullName', sortDir: 'asc' });
  const userNames = new Map(
    (users.data?.content ?? []).map((u) => [String(u.id), u.fullName] as const),
  );

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
    }
  }, [open, initialEmployeeId, initialServiceBayId]);

  const isDirty = !!employeeId;
  const busy = createMutation.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

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
      if (err instanceof ApiError && (err.code === 2003 || err.status === 409)) {
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
              <select
                className="select"
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
                disabled={busy}
                required
              >
                <option value="">-- Chọn kỹ thuật viên --</option>
                {employees.map((emp) => (
                  <option key={emp.id} value={emp.id}>
                    {userNames.get(String(emp.userId)) ?? emp.employeeCode} · {emp.employeeCode}
                    {parseSkills(emp.skillsJson).length > 0 &&
                      ` · ${parseSkills(emp.skillsJson)
                        .map((s) => SKILL_LABELS[s] ?? s)
                        .join(', ')}`}
                    {emp.canQc ? ' · QC' : ''}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          <div className="full-width-field row-actions" aria-label="Chọn nhanh ca chuẩn">
            {SHIFT_PRESETS.map((type) => (
              <Button
                key={type}
                type="button"
                variant={shiftType === type ? 'primary' : 'secondary'}
                disabled={busy}
                onClick={() => applyPreset(type)}
              >
                {shiftTypeLabels[type]} {shiftTypeWindows[type].start}–{shiftTypeWindows[type].end}
              </Button>
            ))}
          </div>

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

          <Field label="Loại ca">
            <select
              className="select"
              value={shiftType}
              onChange={(e) => setShiftType(e.target.value as ShiftType)}
              disabled={busy}
            >
              {Object.entries(shiftTypeLabels).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </select>
          </Field>

          <Field label="Khoang trực cố định">
            <select
              className="select"
              value={serviceBayId}
              onChange={(e) => setServiceBayId(e.target.value)}
              disabled={busy}
            >
              <option value="">-- Không gán khoang --</option>
              {(bays.data?.content ?? []).map((bay) => (
                <option key={bay.id} value={bay.id}>
                  {bay.bayCode} · {bay.bayName}
                </option>
              ))}
            </select>
          </Field>

          <div className="full-width-field">
            <p className="description">
              Tổng giờ dự kiến của ca: {expectedHours(startsAt, endsAt)}
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
        open={discard || blocker.state === 'blocked'}
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

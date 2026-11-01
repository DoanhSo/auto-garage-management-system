import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { ApiError, errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Combobox, Field, FormError, Modal } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import {
  useMyShiftsQuery,
  useRegisterShiftMutation,
  useServiceBaysQuery,
} from '../hooks/useScheduling';
import {
  bayTypeLabels,
  shiftStatusLabels,
  shiftTypeLabels,
  shiftTypeWindows,
  type ShiftType,
} from '../types/scheduling';

const pad = (n: number) => String(n).padStart(2, '0');
const localDate = (date: Date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

const statusVariant: Record<string, 'warning' | 'success' | 'danger' | 'secondary'> = {
  PENDING: 'warning',
  ACTIVE: 'success',
  REJECTED: 'danger',
};

export function ShiftRegistrationModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const notify = useToast();
  const register = useRegisterShiftMutation();
  const mine = useMyShiftsQuery(open);
  const bays = useServiceBaysQuery({ isActive: true, page: 0, size: 100 });

  const [day, setDay] = useState('');
  const [shiftType, setShiftType] = useState<ShiftType>('FULL_DAY');
  const [bayId, setBayId] = useState('');
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (!open) return;
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    setDay(localDate(tomorrow));
    setShiftType('FULL_DAY');
    setBayId('');
    setNotes('');
    register.reset();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!day) {
      notify('Chọn ngày làm việc dự kiến');
      return;
    }
    const window = shiftTypeWindows[shiftType];
    const startsAt = new Date(`${day}T${window.start}`);
    const endsAt = new Date(`${day}T${window.end}`);
    try {
      await register.mutateAsync({
        startsAt: startsAt.toISOString(),
        endsAt: endsAt.toISOString(),
        shiftType,
        serviceBayId: bayId ? Number(bayId) : undefined,
        notes: notes.trim() || undefined,
      });
      notify('Đã gửi đăng ký ca, chờ quản đốc phê duyệt');
      setNotes('');
    } catch (err: unknown) {
      notify(err instanceof ApiError && err.detail ? err.detail : errorMessage(err));
    }
  };

  return (
    <Modal
      open={open}
      size="lg"
      title="Đăng ký ca làm việc"
      description="Chọn ngày và khung ca mong muốn. Ca chỉ vào lịch chính thức sau khi quản đốc phê duyệt."
      onOpenChange={(next) => !next && !register.isPending && onClose()}
    >
      <form onSubmit={submit} noValidate className="editor-form account-editor-form">
        <FormError message={register.error ? errorMessage(register.error) : undefined} />
        <Field label="Ngày làm việc" required>
          <input
            type="date"
            className="input"
            value={day}
            min={localDate(new Date())}
            disabled={register.isPending}
            onChange={(e) => setDay(e.target.value)}
          />
        </Field>
        <Field
          label="Khung ca mong muốn"
          hint={`${shiftTypeWindows[shiftType].start}–${shiftTypeWindows[shiftType].end}`}
        >
          <select
            className="select"
            value={shiftType}
            disabled={register.isPending}
            onChange={(e) => setShiftType(e.target.value as ShiftType)}
          >
            {Object.entries(shiftTypeLabels).map(([key, label]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </Field>
        <div className="full-width-field">
          <Field label="Khoang mong muốn" hint="Để trống nếu bạn làm thợ cơ động">
            <Combobox
              value={bayId}
              onChange={setBayId}
              disabled={register.isPending}
              placeholder="Không chọn khoang (thợ cơ động)"
              options={(bays.data?.content ?? []).map((bay) => ({
                value: String(bay.id),
                label: `${bay.bayCode} · ${bayTypeLabels[bay.bayType] ?? bay.bayName}`,
              }))}
            />
          </Field>
        </div>
        <div className="full-width-field">
          <Field label="Lý do / ghi chú">
            <input
              className="input"
              maxLength={500}
              value={notes}
              disabled={register.isPending}
              onChange={(e) => setNotes(e.target.value)}
            />
          </Field>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={register.isPending}>
            Đóng
          </Button>
          <Button variant="primary" type="submit" pending={register.isPending}>
            Gửi đăng ký ca
          </Button>
        </div>
      </form>

      <section aria-label="Ca đã đăng ký" className="stack">
        <h3 className="section-title">Ca của tôi gần đây</h3>
        {(mine.data?.content ?? []).length === 0 ? (
          <p className="description">Bạn chưa có ca nào.</p>
        ) : (
          <table className="data-table">
            <caption className="sr-only">Danh sách ca và trạng thái đăng ký</caption>
            <thead>
              <tr>
                <th scope="col">Thời gian</th>
                <th scope="col">Loại ca</th>
                <th scope="col">Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {(mine.data?.content ?? []).map((shift) => (
                <tr key={shift.id}>
                  <td>{formatDateTime(shift.startsAt)}</td>
                  <td>{shift.shiftType ? shiftTypeLabels[shift.shiftType] : '—'}</td>
                  <td>
                    <Badge variant={statusVariant[shift.status] ?? 'secondary'}>
                      {shiftStatusLabels[shift.status] ?? shift.status}
                    </Badge>
                    {shift.status === 'REJECTED' && shift.rejectionReason && (
                      <div className="description">Lý do: {shift.rejectionReason}</div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </Modal>
  );
}

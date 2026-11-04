import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { ApiError, errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Combobox, Field, FormError, Modal } from '../../../shared/components/ui';
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
  type Shift,
  type ShiftType,
} from '../types/scheduling';

const pad = (n: number) => String(n).padStart(2, '0');
const localDate = (date: Date) =>
  `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;

/** Thứ tự hiển thị thẻ chọn nhanh khung ca. */
const shiftTypeOrder: ShiftType[] = ['MORNING', 'AFTERNOON', 'FULL_DAY', 'OVERTIME', 'NIGHT'];

/** Tổng số giờ công của một khung ca, định dạng "9,0 giờ". */
function shiftDurationLabel(type: ShiftType): string {
  const { start, end } = shiftTypeWindows[type];
  const [sh, sm] = start.split(':').map(Number);
  const [eh, em] = end.split(':').map(Number);
  const hours = (eh * 60 + em - (sh * 60 + sm)) / 60;
  return `${hours.toFixed(1).replace('.', ',')} giờ`;
}

/** Nhãn ngày làm việc dạng "Thứ Năm, 05/11/2026". */
function formatDayLabel(day: string): string {
  if (!day) return '—';
  const date = new Date(`${day}T00:00:00`);
  return Number.isNaN(date.valueOf())
    ? '—'
    : date.toLocaleDateString('vi-VN', {
        weekday: 'long',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      });
}

/** Khung giờ bắt đầu–kết thúc dạng "dd/MM/yyyy · HH:mm–HH:mm". */
function formatShiftRange(shift: Shift): string {
  const start = new Date(shift.startsAt);
  const end = new Date(shift.endsAt);
  if (Number.isNaN(start.valueOf()) || Number.isNaN(end.valueOf())) return '—';
  const datePart = start.toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  });
  const time = (value: Date) =>
    value.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
  return `${datePart} · ${time(start)}–${time(end)}`;
}

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

  const dayOffset = (offset: number) => {
    const date = new Date();
    date.setDate(date.getDate() + offset);
    return localDate(date);
  };

  const bayList = bays.data?.content ?? [];
  const bayById = new Map(bayList.map((bay) => [String(bay.id), bay] as const));
  const selectedBay = bayId ? bayById.get(bayId) : undefined;
  const bayLabel = bayId ? (selectedBay?.bayCode ?? `Khoang #${bayId}`) : 'Thợ cơ động';

  const recentShifts = (mine.data?.content ?? []).slice(0, 5);
  const window = shiftTypeWindows[shiftType];

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!day) {
      notify('Chọn ngày làm việc dự kiến');
      return;
    }
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
      size="md"
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
        <Field label="Khoang mong muốn" hint="Để trống nếu bạn làm thợ cơ động">
          <Combobox
            value={bayId}
            onChange={setBayId}
            disabled={register.isPending}
            placeholder="Không chọn khoang (thợ cơ động)"
            options={bayList.map((bay) => ({
              value: String(bay.id),
              label: `${bay.bayCode} · ${bayTypeLabels[bay.bayType] ?? bay.bayName}`,
            }))}
          />
        </Field>

        <div className="full-width-field">
          <div className="shift-quick-dates" role="group" aria-label="Chọn nhanh ngày làm việc">
            <span>Chọn nhanh:</span>
            <button
              type="button"
              className="shift-quick-chip"
              aria-pressed={day === dayOffset(0)}
              disabled={register.isPending}
              onClick={() => setDay(dayOffset(0))}
            >
              Hôm nay
            </button>
            <button
              type="button"
              className="shift-quick-chip"
              aria-pressed={day === dayOffset(1)}
              disabled={register.isPending}
              onClick={() => setDay(dayOffset(1))}
            >
              Ngày mai
            </button>
            <button
              type="button"
              className="shift-quick-chip"
              aria-pressed={day === dayOffset(2)}
              disabled={register.isPending}
              onClick={() => setDay(dayOffset(2))}
            >
              Ngày kia
            </button>
          </div>
        </div>

        <div className="full-width-field">
          <div className="field">
            <span className="field-label">
              Khung ca mong muốn<span className="required"> *</span>
            </span>
            <div className="shift-type-grid" role="radiogroup" aria-label="Khung ca mong muốn">
              {shiftTypeOrder.map((type) => {
                const typeWindow = shiftTypeWindows[type];
                return (
                  <label
                    key={type}
                    className={`shift-type-pill${shiftType === type ? ' is-selected' : ''}`}
                  >
                    <input
                      type="radio"
                      name="shiftType"
                      value={type}
                      checked={shiftType === type}
                      disabled={register.isPending}
                      onChange={() => setShiftType(type)}
                    />
                    <strong>{shiftTypeLabels[type]}</strong>
                    <span>
                      {typeWindow.start}–{typeWindow.end}
                    </span>
                    <small>{shiftDurationLabel(type)}</small>
                  </label>
                );
              })}
            </div>
          </div>
        </div>

        <div className="full-width-field info-strip" aria-live="polite">
          <span>
            <strong>{formatDayLabel(day)}</strong>
            {' · '}
            <strong>{shiftTypeLabels[shiftType]}</strong> ({window.start}–{window.end} ·{' '}
            {shiftDurationLabel(shiftType)}){' → Vị trí: '}
            <strong>{bayLabel}</strong>
            {bayId ? '' : ' (hỗ trợ toàn xưởng)'}
          </span>
        </div>

        <div className="full-width-field">
          <p className="description">
            Giới hạn: tối đa 12 giờ/ngày, 60 giờ/tuần và nghỉ tối thiểu 8 giờ giữa hai ca liền kề.
            Hệ thống sẽ báo ngay nếu ca vi phạm.
          </p>
        </div>
        <div className="full-width-field">
          <Field label="Lý do / ghi chú">
            <input
              className="input"
              maxLength={500}
              placeholder="Nhập ghi chú hoặc đề xuất ca trực (không bắt buộc)…"
              value={notes}
              disabled={register.isPending}
              onChange={(e) => setNotes(e.target.value)}
            />
          </Field>
        </div>

        <section className="full-width-field shift-recent-section" aria-label="Ca đã đăng ký">
          <h3 className="section-title">
            Ca của tôi gần đây <small>Tối đa 5 ca</small>
          </h3>
          {recentShifts.length === 0 ? (
            <div className="shift-recent-empty">
              Bạn chưa đăng ký ca nào. Ca đã gửi sẽ hiển thị tại đây để theo dõi trạng thái duyệt.
            </div>
          ) : (
            <table className="data-table">
              <caption className="sr-only">Danh sách ca và trạng thái đăng ký</caption>
              <thead>
                <tr>
                  <th scope="col">Ngày &amp; giờ</th>
                  <th scope="col">Loại ca</th>
                  <th scope="col">Khoang trực</th>
                  <th scope="col">Trạng thái</th>
                </tr>
              </thead>
              <tbody>
                {recentShifts.map((shift) => (
                  <tr key={shift.id}>
                    <td className="tabular">{formatShiftRange(shift)}</td>
                    <td>{shift.shiftType ? shiftTypeLabels[shift.shiftType] : '—'}</td>
                    <td>
                      {shift.serviceBayId
                        ? (bayById.get(String(shift.serviceBayId))?.bayCode ??
                          `Khoang #${shift.serviceBayId}`)
                        : 'Thợ cơ động'}
                    </td>
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

        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={register.isPending}>
            Đóng
          </Button>
          <Button variant="primary" type="submit" pending={register.isPending}>
            Gửi đăng ký ca
          </Button>
        </div>
      </form>
    </Modal>
  );
}

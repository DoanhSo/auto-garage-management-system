import { useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useScheduleMaintenanceMutation } from '../hooks/useScheduling';
import type { ServiceBay } from '../types/scheduling';

const pad = (n: number) => String(n).padStart(2, '0');
const localInput = (d: Date) =>
  `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;

function defaultRange() {
  const start = new Date();
  start.setMinutes(0, 0, 0);
  start.setHours(start.getHours() + 1);
  const end = new Date(start.getTime() + 2 * 3_600_000);
  return { start: localInput(start), end: localInput(end) };
}

export function BayMaintenanceModal({
  bay,
  open,
  onClose,
  onSaved,
}: {
  bay: ServiceBay | null;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const mutation = useScheduleMaintenanceMutation();
  const [range, setRange] = useState(defaultRange);
  const [notes, setNotes] = useState('');

  if (!bay) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const startsAt = new Date(range.start);
    const endsAt = new Date(range.end);
    if (!range.start || !range.end || endsAt <= startsAt) {
      notify('Thời gian kết thúc bảo trì phải sau thời gian bắt đầu');
      return;
    }
    try {
      await mutation.mutateAsync({
        bayId: bay.id,
        data: {
          startsAt: startsAt.toISOString(),
          endsAt: endsAt.toISOString(),
          notes: notes.trim() || undefined,
        },
      });
      notify('Đã khóa khoang để bảo trì');
      setNotes('');
      onSaved();
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  return (
    <Sheet
      open={open}
      placement="center"
      title={`Bảo trì khoang ${bay.bayCode}`}
      description="Khoang sẽ không nhận xe hoặc lịch hẹn trong khung giờ bảo trì."
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
        <FormError message={mutation.error ? errorMessage(mutation.error) : undefined} />
        <Field label="Bắt đầu bảo trì" required>
          <input
            type="datetime-local"
            className="input"
            value={range.start}
            onChange={(e) => setRange((r) => ({ ...r, start: e.target.value }))}
          />
        </Field>
        <Field label="Kết thúc bảo trì" required>
          <input
            type="datetime-local"
            className="input"
            value={range.end}
            onChange={(e) => setRange((r) => ({ ...r, end: e.target.value }))}
          />
        </Field>
        <div className="full-width-field">
          <Field label="Nội dung bảo trì">
            <input
              className="input"
              placeholder="VD: Thay dầu thủy lực, kiểm tra an toàn cầu nâng"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </Field>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose}>Hủy</Button>
          <Button variant="primary" type="submit" pending={mutation.isPending}>
            Khóa khoang bảo trì
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

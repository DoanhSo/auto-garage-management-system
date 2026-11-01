import { useState } from 'react';
import { CalendarClock } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { ApiError, errorMessage } from '../../../shared/api/errors';
import { Badge, Button, Field, Modal } from '../../../shared/components/ui';
import { formatDateTime } from '../../../shared/utils/format';
import {
  usePendingRegistrationsQuery,
  useReviewRegistrationMutation,
  useSchedulingEmployeesQuery,
  useServiceBaysQuery,
} from '../hooks/useScheduling';
import { shiftTypeLabels, type Shift } from '../types/scheduling';

export function PendingShiftRegistrations({ canReview }: { canReview: boolean }) {
  const notify = useToast();
  const pending = usePendingRegistrationsQuery(true);
  const review = useReviewRegistrationMutation();
  const employees = useSchedulingEmployeesQuery({ active: true });
  const bays = useServiceBaysQuery({ isActive: true, page: 0, size: 100 });
  const [rejecting, setRejecting] = useState<Shift | null>(null);
  const [reason, setReason] = useState('');

  const rows = pending.data?.content ?? [];
  if (rows.length === 0) return null;

  const employeeName = (employeeId: string | number) => {
    const emp = (employees.data ?? []).find((e) => String(e.id) === String(employeeId));
    return emp ? (emp.fullName ?? emp.employeeCode) : `#${employeeId}`;
  };
  const bayCode = (id?: string | number | null) =>
    (bays.data?.content ?? []).find((b) => String(b.id) === String(id))?.bayCode;

  const run = async (id: string | number, rejectReason?: string) => {
    try {
      await review.mutateAsync({ id, reason: rejectReason });
      notify(rejectReason === undefined ? 'Đã phê duyệt ca làm' : 'Đã từ chối đăng ký ca');
      setRejecting(null);
      setReason('');
    } catch (err: unknown) {
      notify(err instanceof ApiError && err.detail ? err.detail : errorMessage(err));
    }
  };

  return (
    <section aria-label="Đăng ký ca chờ duyệt">
      <h2 className="section-title">
        <CalendarClock size={16} aria-hidden /> Đăng ký ca chờ duyệt ({rows.length})
      </h2>
      <div className="table-scroll">
        <table className="data-table">
          <caption className="sr-only">Danh sách đăng ký ca đang chờ duyệt</caption>
          <thead>
            <tr>
              <th scope="col">Kỹ thuật viên</th>
              <th scope="col">Ca</th>
              <th scope="col">Thời gian</th>
              <th scope="col">Khoang</th>
              <th scope="col">Ghi chú</th>
              <th scope="col" />
            </tr>
          </thead>
          <tbody>
            {rows.map((shift) => (
              <tr key={shift.id}>
                <td>
                  <strong>{employeeName(shift.employeeId)}</strong>
                </td>
                <td>
                  <Badge variant="warning">
                    {shift.shiftType ? shiftTypeLabels[shift.shiftType] : 'Ca'}
                  </Badge>
                </td>
                <td>
                  {formatDateTime(shift.startsAt)} – {formatDateTime(shift.endsAt)}
                </td>
                <td>{bayCode(shift.serviceBayId) ?? 'Thợ cơ động'}</td>
                <td>{shift.notes ?? '—'}</td>
                <td>
                  {canReview ? (
                    <div className="row-actions">
                      <Button
                        variant="primary"
                        pending={review.isPending}
                        onClick={() => void run(shift.id)}
                      >
                        Duyệt
                      </Button>
                      <Button variant="secondary" onClick={() => setRejecting(shift)}>
                        Từ chối
                      </Button>
                    </div>
                  ) : (
                    <span className="muted">Chờ quản trị viên</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <Modal
        open={!!rejecting}
        title="Từ chối đăng ký ca"
        description={rejecting ? employeeName(rejecting.employeeId) : ''}
        onOpenChange={(open) => !open && !review.isPending && setRejecting(null)}
      >
        <form
          className="editor-form account-editor-form"
          noValidate
          onSubmit={(e) => {
            e.preventDefault();
            if (!reason.trim()) {
              notify('Nhập lý do từ chối');
              return;
            }
            if (rejecting) void run(rejecting.id, reason);
          }}
        >
          <div className="full-width-field">
            <Field label="Lý do từ chối" required>
              <input
                className="input"
                maxLength={500}
                value={reason}
                onChange={(e) => setReason(e.target.value)}
              />
            </Field>
          </div>
          <div className="form-actions account-editor-actions">
            <Button onClick={() => setRejecting(null)} disabled={review.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={review.isPending}>
              Từ chối đăng ký
            </Button>
          </div>
        </form>
      </Modal>
    </section>
  );
}

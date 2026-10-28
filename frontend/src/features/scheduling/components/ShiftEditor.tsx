import { useState, useEffect } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useCreateShiftMutation, useSchedulingEmployeesQuery } from '../hooks/useScheduling';

export function ShiftEditor({
  open,
  onClose,
  onSaved,
}: {
  open: boolean;
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

  const { data: employees = [] } = useSchedulingEmployeesQuery({ active: true });

  useEffect(() => {
    if (open) {
      setEmployeeId('');
      // Mặc định ca hôm nay 08:00 đến 17:00
      const today = new Date().toISOString().slice(0, 10);
      setStartsAt(`${today}T08:00`);
      setEndsAt(`${today}T17:00`);
      setNotes('');
      setDiscard(false);
    }
  }, [open]);

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
        notes: notes.trim() || undefined,
      });
      notify('Phân công ca làm việc thành công');
      onSaved();
      onClose();
    } catch {
      notify('Không thể xếp ca làm (Kiểm tra lại xem có bị trùng lặp ca làm việc hay không)');
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
        title="Xếp ca làm việc kỹ thuật viên (M06)"
        description="Lập lịch làm việc cho thợ kỹ thuật, hệ thống tự động kiểm tra chống trùng lặp ca làm (UC37)"
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
                    {emp.employeeCode} {emp.canQc ? '(Có quyền QC)' : ''}
                  </option>
                ))}
              </select>
            </Field>
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

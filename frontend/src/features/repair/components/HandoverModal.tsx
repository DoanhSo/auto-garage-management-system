import { useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useHandoverMutation } from '../hooks/useRepair';
import type { RepairOrder } from '../types/repairTypes';

export function HandoverModal({
  order,
  open,
  onClose,
}: {
  order: RepairOrder;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const handover = useHandoverMutation();

  const [odometerOut, setOdometerOut] = useState<number>(order.odometerIn);
  const [notes, setNotes] = useState('');

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (odometerOut < order.odometerIn) {
      notify(`Số km khi bàn giao không được nhỏ hơn số km lúc vào (${order.odometerIn.toLocaleString('vi-VN')} km)`);
      return;
    }

    try {
      await handover.mutateAsync({
        id: order.id,
        odometerOut,
        notes: notes.trim() || undefined,
      });
      notify(`Đã bàn giao xe lệnh #${order.orderCode} thành công! Lệnh sửa chữa đã được đóng.`);
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="md"
      title={`Bàn giao xe cho khách hàng • #${order.orderCode}`}
      description="Xác nhận hoàn thành toàn bộ quy trình dịch vụ, ghi nhận ODO xuất xưởng và đóng lệnh sửa chữa."
    >
      <form onSubmit={handleSubmit} noValidate className="modal-form">
        <FormError message={handover.error ? errorMessage(handover.error) : undefined} />

        <Field label="Số ODO khi vào (km)">
          <input
            className="input"
            disabled
            value={order.odometerIn.toLocaleString('vi-VN')}
          />
        </Field>

        <Field label="Số ODO khi bàn giao (km)" required>
          <input
            type="number"
            min={order.odometerIn}
            className="input"
            value={odometerOut}
            onChange={(e) => setOdometerOut(Number(e.target.value))}
            required
          />
        </Field>

        <div className="full-width">
          <Field label="Ghi chú bàn giao xe">
            <textarea
              className="input"
              rows={3}
              placeholder="Ghi nhận đồ dùng, tình trạng xe và dặn dò khách hàng khi nhận xe..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </Field>
        </div>

        <div className="modal-form-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={handover.isPending}>
            {handover.isPending ? 'Đang xử lý…' : 'Xác nhận bàn giao xe'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

import { useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { usePerformQualityCheckMutation } from '../../quality/hooks/useQuality';
import type { CheckResult } from '../../quality/types/qualityTypes';

export function PerformQCModal({
  orderId,
  open,
  onClose,
}: {
  orderId: string | number;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const performQC = usePerformQualityCheckMutation();

  const [result, setResult] = useState<CheckResult>('PASSED');
  const [odometer, setOdometer] = useState<number | ''>('');
  const [notes, setNotes] = useState('');
  const [brakeCheck, setBrakeCheck] = useState(true);
  const [lightsCheck, setLightsCheck] = useState(true);
  const [engineCheck, setEngineCheck] = useState(true);
  const [dashboardCheck, setDashboardCheck] = useState(true);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    try {
      const checklistJson = JSON.stringify({
        brakes: brakeCheck ? 'OK' : 'FAIL',
        lights: lightsCheck ? 'OK' : 'FAIL',
        engine: engineCheck ? 'OK' : 'FAIL',
        dashboardWarnings: dashboardCheck ? 'CLEAR' : 'WARNING',
      });

      await performQC.mutateAsync({
        repairOrderId: orderId,
        result,
        odometer: odometer !== '' ? Number(odometer) : null,
        notes: notes.trim() || undefined,
        checklistJson,
      });

      notify(
        result === 'PASSED'
          ? 'Kiểm tra QC ĐẠT! Xe đã sẵn sàng lập hóa đơn và bàn giao.'
          : 'Ghi nhận QC KHÔNG ĐẠT. Lệnh sửa chữa đã chuyển trạng thái yêu cầu làm lại (Rework).',
      );
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
      title="Kiểm định chất lượng sau sửa chữa (QC)"
      description="Kiểm tra toàn bộ các hạng mục đã hoàn thành, lắp ráp phụ tùng và chạy thử trước khi bàn giao xe."
    >
      <form onSubmit={handleSubmit} noValidate className="modal-form">
        <FormError message={performQC.error ? errorMessage(performQC.error) : undefined} />

        <Field label="Kết quả kiểm định" required>
          <select
            className="select"
            value={result}
            onChange={(e) => setResult(e.target.value as CheckResult)}
          >
            <option value="PASSED">✓ Đạt tiêu chuẩn xuất xưởng</option>
            <option value="FAILED">✗ Không đạt - Cần sửa lại</option>
          </select>
        </Field>

        <Field label="Số ODO kiểm tra (km)">
          <input
            type="number"
            min="0"
            className="input"
            placeholder="VD: 50120"
            value={odometer}
            onChange={(e) => setOdometer(e.target.value ? Number(e.target.value) : '')}
          />
        </Field>

        <div className="full-width p-3 border rounded-lg bg-muted/15 space-y-2">
          <span className="text-xs font-semibold text-muted-foreground uppercase tracking-wide">
            Checklist kiểm tra tiêu chuẩn
          </span>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={brakeCheck}
                onChange={(e) => setBrakeCheck(e.target.checked)}
              />
              <span>Hệ thống phanh & lốp an toàn</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={lightsCheck}
                onChange={(e) => setLightsCheck(e.target.checked)}
              />
              <span>Hệ thống đèn chiếu sáng & tín hiệu</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={engineCheck}
                onChange={(e) => setEngineCheck(e.target.checked)}
              />
              <span>Khoang máy, dầu nhớt, nước mát</span>
            </label>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={dashboardCheck}
                onChange={(e) => setDashboardCheck(e.target.checked)}
              />
              <span>Bảng đồng hồ không báo lỗi lạ</span>
            </label>
          </div>
        </div>

        <div className="full-width">
          <Field label="Nhận xét / Ghi chú kiểm định">
            <textarea
              className="input"
              rows={3}
              placeholder="Ghi nhận tình trạng xe khi kiểm tra chạy thử..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </Field>
        </div>

        <div className="modal-form-actions">
          <Button type="button" variant="secondary" onClick={onClose}>
            Hủy
          </Button>
          <Button
            type="submit"
            variant={result === 'PASSED' ? 'primary' : 'danger'}
            pending={performQC.isPending}
          >
            {performQC.isPending ? 'Đang lưu…' : 'Xác nhận kết quả QC'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

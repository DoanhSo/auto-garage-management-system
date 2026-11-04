import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { formatDate } from '../../../shared/utils/format';
import { useAdjustWarrantyMutation } from '../hooks/useWarranty';
import type { Warranty } from '../types/warranty';

const DAY_MS = 86_400_000;

export function AdjustWarrantyModal({
  warranty,
  onClose,
}: {
  warranty: Warranty | null;
  onClose: () => void;
}) {
  const notify = useToast();
  const adjust = useAdjustWarrantyMutation();
  const [days, setDays] = useState('0');
  const [km, setKm] = useState('0');
  const [terms, setTerms] = useState('');
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string>();

  useEffect(() => {
    if (!warranty) return;
    setDays(String(warranty.warrantyDays));
    setKm(String(warranty.warrantyKm));
    setTerms(warranty.termsSnapshot ?? '');
    setReason('');
    setError(undefined);
    adjust.reset();
    // Reset only when a different book is opened.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [warranty?.id]);

  if (!warranty) return null;

  const dayCount = Number(days);
  const kmCount = Number(km);
  const previewEnd =
    Number.isInteger(dayCount) && dayCount > 0
      ? formatDate(
          new Date(new Date(warranty.startsAt).getTime() + dayCount * DAY_MS).toISOString(),
        )
      : 'không giới hạn';
  const previewOdo =
    Number.isInteger(kmCount) && kmCount > 0
      ? `${(warranty.startOdometer + kmCount).toLocaleString('vi-VN')} km`
      : 'không giới hạn';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!Number.isInteger(dayCount) || dayCount < 0 || !Number.isInteger(kmCount) || kmCount < 0) {
      setError('Số ngày và số km phải là số nguyên không âm.');
      return;
    }
    if (dayCount === 0 && kmCount === 0) {
      setError('Sổ bảo hành phải giới hạn theo số ngày hoặc số km.');
      return;
    }
    if (!reason.trim()) {
      setError('Nhập lý do điều chỉnh.');
      return;
    }
    setError(undefined);
    try {
      await adjust.mutateAsync({
        id: warranty.id,
        data: {
          warrantyDays: dayCount,
          warrantyKm: kmCount,
          termsSnapshot: terms,
          reason: reason.trim(),
        },
      });
      notify(`Đã điều chỉnh sổ bảo hành #${warranty.warrantyCode}`);
      onClose();
    } catch (err: unknown) {
      setError(errorMessage(err));
    }
  };

  return (
    <Sheet
      open
      placement="center"
      size="md"
      title={`Điều chỉnh sổ #${warranty.warrantyCode}`}
      description={`Hạn mới tính từ ngày bàn giao ${formatDate(warranty.startsAt)} và ODO ${warranty.startOdometer.toLocaleString('vi-VN')} km.`}
      onClose={onClose}
      pending={adjust.isPending}
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
        <FormError message={error} />
        <Field label="Số ngày bảo hành" hint={`Hết hạn: ${previewEnd}`}>
          <input
            type="number"
            min={0}
            className="input"
            value={days}
            disabled={adjust.isPending}
            onChange={(e) => setDays(e.target.value)}
          />
        </Field>
        <Field label="Số km bảo hành" hint={`Tối đa: ${previewOdo}`}>
          <input
            type="number"
            min={0}
            step={500}
            className="input"
            value={km}
            disabled={adjust.isPending}
            onChange={(e) => setKm(e.target.value)}
          />
        </Field>
        <div className="full-width-field">
          <Field label="Điều khoản bảo hành">
            <input
              className="input"
              maxLength={2000}
              value={terms}
              disabled={adjust.isPending}
              onChange={(e) => setTerms(e.target.value)}
            />
          </Field>
        </div>
        <div className="full-width-field">
          <Field label="Lý do điều chỉnh" required>
            <textarea
              className="input"
              rows={3}
              maxLength={500}
              placeholder="VD: Gia hạn chăm sóc khách thân thiết, sửa sai thông tin hóa đơn…"
              value={reason}
              disabled={adjust.isPending}
              onChange={(e) => setReason(e.target.value)}
            />
          </Field>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={adjust.isPending}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={adjust.isPending}>
            Lưu điều chỉnh
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

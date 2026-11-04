import { useEffect, useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useVoidWarrantyMutation } from '../hooks/useWarranty';
import type { Warranty } from '../types/warranty';

export function VoidWarrantyModal({
  warranty,
  onClose,
}: {
  warranty: Warranty | null;
  onClose: () => void;
}) {
  const notify = useToast();
  const voidMutation = useVoidWarrantyMutation();
  const [reason, setReason] = useState('');
  const [error, setError] = useState<string>();

  useEffect(() => {
    setReason('');
    setError(undefined);
    voidMutation.reset();
    // Reset only when a different book is opened.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [warranty?.id]);

  if (!warranty) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) {
      setError('Nhập lý do hủy sổ.');
      return;
    }
    setError(undefined);
    try {
      await voidMutation.mutateAsync({ id: warranty.id, data: { reason: reason.trim() } });
      notify(`Đã hủy sổ bảo hành #${warranty.warrantyCode}`);
      onClose();
    } catch (err: unknown) {
      setError(errorMessage(err));
    }
  };

  return (
    <Sheet
      open
      placement="center"
      size="sm"
      title={`Hủy sổ bảo hành #${warranty.warrantyCode}?`}
      description="Sổ được giữ lại để tra cứu nhưng không thể mở khiếu nại mới. Không hủy được khi còn khiếu nại đang xử lý."
      onClose={onClose}
      pending={voidMutation.isPending}
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
        <FormError message={error} />
        <div className="full-width-field">
          <Field label="Lý do hủy sổ" required>
            <textarea
              className="input"
              rows={3}
              maxLength={500}
              placeholder="VD: Khách tự tháo lắp, hóa đơn bị hủy, xe gặp tai nạn…"
              value={reason}
              disabled={voidMutation.isPending}
              onChange={(e) => setReason(e.target.value)}
            />
          </Field>
        </div>
        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={voidMutation.isPending}>
            Không hủy
          </Button>
          <Button variant="danger" type="submit" pending={voidMutation.isPending}>
            Hủy sổ bảo hành
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

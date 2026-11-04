import { useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, ReadOnlyField, Sheet } from '../../../shared/components/ui';
import { useDecideClaimMutation } from '../hooks/useWarranty';
import type { WarrantyClaim } from '../types/warranty';

export function DecideClaimModal({
  claim,
  open,
  onClose,
  onSaved,
}: {
  claim: WarrantyClaim | null;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const decideMutation = useDecideClaimMutation();

  const [status, setStatus] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [decisionReason, setDecisionReason] = useState('');

  if (!claim) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!decisionReason.trim()) {
      notify('Vui lòng nêu rõ lý do phê duyệt hoặc từ chối');
      return;
    }

    try {
      await decideMutation.mutateAsync({
        claimId: claim.id,
        data: {
          approved: status === 'APPROVED',
          reason: decisionReason.trim(),
        },
      });
      notify(
        status === 'APPROVED'
          ? 'Đã duyệt bảo hành và tự động mở Lệnh sửa chữa bảo hành mới'
          : 'Đã từ chối yêu cầu bảo hành',
      );
      onSaved();
      onClose();
    } catch (err: unknown) {
      notify(errorMessage(err) ?? 'Không thể duyệt yêu cầu bảo hành');
    }
  };

  return (
    <Sheet
      open={open}
      placement="center"
      title={`Xử lý khiếu nại bảo hành #${claim.claimCode}`}
      description="Xem xét tính hợp lệ của khiếu nại (ODO, ngày hết hạn) và đưa ra quyết định xử lý"
      onClose={onClose}
    >
      <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
        <FormError
          message={decideMutation.error ? errorMessage(decideMutation.error) : undefined}
        />

        <dl className="full-width-field detail-list">
          <ReadOnlyField label="Nội dung khách khiếu nại">{claim.description}</ReadOnlyField>
          <ReadOnlyField label="Số ODO ghi nhận">
            {claim.currentOdometer.toLocaleString('vi-VN')} km
          </ReadOnlyField>
        </dl>

        <div className="full-width-field">
          <Field label="Quyết định xử lý" required>
            <select
              className="select"
              value={status}
              onChange={(e) => setStatus(e.target.value as 'APPROVED' | 'REJECTED')}
            >
              <option value="APPROVED">
                Chấp thuận — tự động mở phiếu sửa chữa bảo hành miễn phí
              </option>
              <option value="REJECTED">
                Từ chối — lỗi không thuộc phạm vi bảo hành hoặc quá hạn
              </option>
            </select>
          </Field>
        </div>

        <div className="full-width-field">
          <Field label="Lý do & Căn cứ kỹ thuật" required>
            <textarea
              className="input"
              rows={4}
              placeholder="Nêu rõ kết quả giám định kỹ thuật và lý do chấp thuận / từ chối..."
              value={decisionReason}
              onChange={(e) => setDecisionReason(e.target.value)}
              required
            />
          </Field>
        </div>

        <div className="form-actions account-editor-actions">
          <Button onClick={onClose} disabled={decideMutation.isPending}>
            Hủy
          </Button>
          <Button
            variant={status === 'APPROVED' ? 'primary' : 'secondary'}
            className={status === 'APPROVED' ? undefined : 'delete-action'}
            type="submit"
            pending={decideMutation.isPending}
          >
            {status === 'APPROVED' ? 'Chấp thuận bảo hành' : 'Từ chối bảo hành'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

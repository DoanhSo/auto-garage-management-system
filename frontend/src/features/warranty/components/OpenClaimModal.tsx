import { useState, useEffect } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useOpenClaimMutation } from '../hooks/useWarranty';
import type { Warranty } from '../types/warranty';

export function OpenClaimModal({
  warranty,
  open,
  onClose,
  onSaved,
}: {
  warranty: Warranty | null;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const openClaimMutation = useOpenClaimMutation();
  const [discard, setDiscard] = useState(false);

  const [currentOdometer, setCurrentOdometer] = useState<number>(warranty?.startOdometer ?? 0);
  const [description, setDescription] = useState('');

  useEffect(() => {
    if (open && warranty) {
      setCurrentOdometer(warranty.startOdometer);
      setDescription('');
      setDiscard(false);
    }
  }, [open, warranty]);

  const isDirty = !!description || (!!warranty && currentOdometer !== warranty.startOdometer);
  const busy = openClaimMutation.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  if (!warranty) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!description.trim()) {
      notify('Vui lòng nhập mô tả hiện tượng hư hỏng cần bảo hành');
      return;
    }

    try {
      await openClaimMutation.mutateAsync({
        warrantyId: Number(warranty.id),
        currentOdometer: Number(currentOdometer),
        description: description.trim(),
      });
      notify('Đã mở hồ sơ khiếu nại bảo hành thành công');
      onSaved();
      onClose();
    } catch {
      notify('Không thể mở khiếu nại (Kiểm tra lại xem đã quá hạn ngày hoặc km bảo hành hay chưa)');
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
        title={`Mở yêu cầu bảo hành #${warranty.warrantyCode}`}
        description={`Hạn bảo hành đến: ${warranty.expiresAt.slice(0, 10)} hoặc ${warranty.endOdometer?.toLocaleString('vi-VN')} km`}
        onClose={handleClose}
      >
        <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
          <FormError message={openClaimMutation.error?.message} />

          <Field label="Số ODO hiện tại khi khiếu nại (km)" required hint={`Mức tối đa được bảo hành: ${warranty.endOdometer?.toLocaleString('vi-VN')} km`}>
            <input
              type="number"
              className="input"
              value={currentOdometer}
              onChange={(e) => setCurrentOdometer(Number(e.target.value))}
              disabled={busy}
              required
            />
          </Field>

          <Field label="Thời hạn bảo hành">
            <input
              type="text"
              className="input"
              value={`${warranty.warrantyDays} ngày / ${warranty.warrantyKm.toLocaleString('vi-VN')} km`}
              disabled
            />
          </Field>

          <div className="full-width-field">
            <Field label="Mô tả chi tiết lỗi phát sinh cần bảo hành" required>
              <textarea
                className="input min-h-[85px] resize-none"
                placeholder="Ví dụ: Phụ tùng thay thế phát ra tiếng kêu lạ, dầu nhớt bị rò rỉ sau khi nhận xe..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={busy}
                required
              />
            </Field>
          </div>

          <div className="form-actions account-editor-actions">
            <Button disabled={busy} onClick={handleClose}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={busy}>
              {busy ? 'Đang gửi…' : 'Gửi yêu cầu bảo hành'}
            </Button>
          </div>
        </form>
      </Sheet>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung khiếu nại đang nhập sẽ bị hủy."
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

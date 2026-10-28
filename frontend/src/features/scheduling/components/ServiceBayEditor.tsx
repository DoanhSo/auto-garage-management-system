import { useState, useEffect } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useCreateServiceBayMutation, useUpdateServiceBayMutation } from '../hooks/useScheduling';
import type { ServiceBay } from '../types/scheduling';

export function ServiceBayEditor({
  bay,
  open,
  onClose,
  onSaved,
}: {
  bay?: ServiceBay | null;
  open: boolean;
  onClose: () => void;
  onSaved: () => void;
}) {
  const notify = useToast();
  const createMutation = useCreateServiceBayMutation();
  const updateMutation = useUpdateServiceBayMutation();
  const [discard, setDiscard] = useState(false);

  const [bayCode, setBayCode] = useState('');
  const [bayName, setBayName] = useState('');
  const [bayType, setBayType] = useState('GENERAL');
  const [description, setDescription] = useState('');
  const [isActive, setIsActive] = useState(true);

  useEffect(() => {
    if (open) {
      setBayCode(bay?.bayCode ?? '');
      setBayName(bay?.bayName ?? '');
      setBayType(bay?.bayType ?? 'GENERAL');
      setDescription(bay?.description ?? '');
      setIsActive(bay?.isActive ?? true);
      setDiscard(false);
    }
  }, [bay, open]);

  const isDirty = bay
    ? bayCode !== bay.bayCode || bayName !== bay.bayName || description !== (bay.description ?? '')
    : !!bayCode || !!bayName;

  const busy = createMutation.isPending || updateMutation.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!bayCode.trim() || !bayName.trim()) {
      notify('Vui lòng nhập đầy đủ mã và tên khoang');
      return;
    }

    try {
      if (bay) {
        await updateMutation.mutateAsync({
          id: bay.id,
          data: {
            bayName: bayName.trim(),
            bayType,
            description: description.trim() || undefined,
            isActive,
          },
        });
        notify('Cập nhật khoang sửa chữa thành công');
      } else {
        await createMutation.mutateAsync({
          bayCode: bayCode.trim().toUpperCase(),
          bayName: bayName.trim(),
          bayType,
          description: description.trim() || undefined,
          isActive,
        });
        notify('Thêm khoang sửa chữa thành công');
      }
      onSaved();
      onClose();
    } catch {
      notify('Không thể lưu khoang sửa chữa');
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
        title={bay ? `Sửa Khoang #${bay.bayCode}` : 'Thêm Khoang sửa chữa mới'}
        description="Quản lý vị trí cầu nâng, loại dịch vụ hỗ trợ và trạng thái hoạt động"
        onClose={handleClose}
      >
        <form onSubmit={handleSubmit} noValidate className="editor-form account-editor-form">
          <FormError message={createMutation.error?.message || updateMutation.error?.message} />

          <Field label="Mã khoang sửa chữa" required hint="Ví dụ: BAY-01, BAY-02">
            <input
              type="text"
              className="input uppercase"
              placeholder="BAY-01"
              value={bayCode}
              onChange={(e) => setBayCode(e.target.value)}
              disabled={!!bay || busy}
              required
            />
          </Field>

          <Field label="Tên gọi khoang" required hint="Ví dụ: Cầu nâng 2 trụ số 1">
            <input
              type="text"
              className="input"
              placeholder="Khoang bảo dưỡng nhanh số 1"
              value={bayName}
              onChange={(e) => setBayName(e.target.value)}
              disabled={busy}
              required
            />
          </Field>

          <Field label="Loại khoang dịch vụ" required>
            <select
              className="select"
              value={bayType}
              onChange={(e) => setBayType(e.target.value)}
              disabled={busy}
            >
              <option value="TWO_POST">Cầu nâng 2 trụ</option>
              <option value="FOUR_POST">Cầu nâng 4 trụ</option>
              <option value="SCISSOR">Cầu nâng cắt kéo</option>
              <option value="DIAGNOSTIC">Khoang chẩn đoán điện</option>
              <option value="INSPECTION">Khoang kiểm định QC & xuất xưởng</option>
              <option value="PAINT">Khoang đồng sơn</option>
              <option value="WASH">Khoang rửa xe & chăm sóc</option>
              <option value="GENERAL">Khoang sửa chữa chung</option>
            </select>
          </Field>

          <Field label="Trạng thái khả dụng">
            <select
              className="select"
              value={isActive ? 'true' : 'false'}
              onChange={(e) => setIsActive(e.target.value === 'true')}
              disabled={busy}
            >
              <option value="true">Đang hoạt động</option>
              <option value="false">Tạm ngưng / Bảo trì</option>
            </select>
          </Field>

          <div className="full-width-field">
            <Field label="Mô tả / Thiết bị trang bị tại khoang">
              <textarea
                className="input min-h-[70px] resize-none"
                placeholder="Trọng tải nâng 4 tấn, có máy hút dầu nhớt chuyên dụng..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={busy}
              />
            </Field>
          </div>

          <div className="form-actions account-editor-actions">
            <Button disabled={busy} onClick={handleClose}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={busy}>
              {busy ? 'Đang lưu…' : bay ? 'Lưu thay đổi' : 'Thêm Khoang'}
            </Button>
          </div>
        </form>
      </Sheet>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Thông tin khoang đang nhập sẽ bị hủy."
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

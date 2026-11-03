import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import {
  useRecordVehicleCheckMutation,
  useUploadOrderAttachmentMutation,
} from '../hooks/useReception';
import { AttachmentFilePicker } from './AttachmentFilePicker';
import { vehicleCheckFormSchema, type VehicleCheckFormValues } from '../schemas/receptionSchema';
import { ClipboardCheck, Upload } from 'lucide-react';

export function VehicleCheckModal({
  repairOrderId,
  orderCode,
  open,
  onClose,
}: {
  repairOrderId: string;
  orderCode?: string;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const recordCheck = useRecordVehicleCheckMutation();
  const uploadAttachment = useUploadOrderAttachmentMutation();

  const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
  const [isUploading, setIsUploading] = useState(false);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<VehicleCheckFormValues>({
    resolver: zodResolver(vehicleCheckFormSchema),
    defaultValues: {
      repairOrderId,
      checkType: 'CHECK_IN',
      odometer: 0,
      notes: '',
      exteriorCondition: '',
      interiorCondition: '',
      warningLights: '',
    },
  });

  async function onSubmit(values: VehicleCheckFormValues) {
    try {
      setIsUploading(true);

      const checklistObj = {
        exterior: values.exteriorCondition?.trim() || 'Bình thường',
        interior: values.interiorCondition?.trim() || 'Bình thường',
        warningLights: values.warningLights?.trim() || 'Không có đèn báo lỗi',
      };

      const checkRecord = await recordCheck.mutateAsync({
        repairOrderId: values.repairOrderId,
        checkType: values.checkType,
        odometer: Number(values.odometer) || null,
        checklistJson: JSON.stringify(checklistObj),
        notes: values.notes?.trim() || null,
      });

      if (selectedFiles.length > 0) {
        for (const file of selectedFiles) {
          await uploadAttachment.mutateAsync({
            repairOrderId: values.repairOrderId,
            vehicleCheckId: checkRecord.id,
            category: values.checkType,
            file,
          });
        }
      }

      notify('Lập biên bản kiểm tra tình trạng xe thành công');
      reset();
      setSelectedFiles([]);
      onClose();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    } finally {
      setIsUploading(false);
    }
  }

  const busy = recordCheck.isPending || isUploading;
  const generalError = recordCheck.error || uploadAttachment.error;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="lg"
      title={`Biên bản kiểm tra xe ${orderCode ? `#${orderCode}` : ''}`}
      description="Ghi nhận hiện trạng ngoại thất, nội thất, đèn cảnh báo và ảnh chụp tình trạng phương tiện."
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form grid-3col"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THÔNG SỐ & LOẠI KIỂM TRA</div>

        <Field label="Loại kiểm tra" required error={errors.checkType?.message}>
          <select className="select" disabled={busy} {...register('checkType')}>
            <option value="CHECK_IN">Kiểm tra tiếp nhận</option>
          </select>
        </Field>

        <Field
          label="Số ODO kiểm tra (km)"
          error={errors.odometer?.message}
        >
          <input
            type="number"
            className="input"
            min={0}
            placeholder="15000"
            disabled={busy}
            {...register('odometer')}
          />
        </Field>

        <Field
          label="Đèn cảnh báo trên táp-lô"
          hint="Check Engine, ABS, Túi khí..."
        >
          <input
            type="text"
            className="input"
            placeholder="Không có đèn báo lỗi..."
            disabled={busy}
            {...register('warningLights')}
          />
        </Field>

        <div className="form-section-label">TÌNH TRẠNG HIỆN TRƯỜNG PHƯƠNG TIỆN</div>

        <Field
          label="Ngoại thất (Xước, móp)"
        >
          <input
            type="text"
            className="input"
            placeholder="Mô tả vết xước ngoại thất..."
            disabled={busy}
            {...register('exteriorCondition')}
          />
        </Field>

        <Field
          label="Nội thất & Đồ đạc trên xe"
        >
          <input
            type="text"
            className="input"
            placeholder="Ghế da sạch, đồ dùng..."
            disabled={busy}
            {...register('interiorCondition')}
          />
        </Field>

        <Field
          label="Ghi chú tổng kết"
          error={errors.notes?.message}
        >
          <input
            type="text"
            className="input"
            placeholder="Ghi chú kết luận..."
            disabled={busy}
            {...register('notes')}
          />
        </Field>

        <div className="full-width-field">
          <div className="info-strip" style={{ marginBottom: 8 }}>
            <Upload size={18} />
            <span>
              <strong>Tải lên ảnh chụp hiện trạng / Biên bản giấy:</strong> Hình ảnh được băm
              SHA-256 chống giả mạo và lưu trữ an toàn cùng lệnh sửa chữa.
            </span>
          </div>

          <AttachmentFilePicker files={selectedFiles} onChange={setSelectedFiles} disabled={busy} />
        </div>

        <div className="form-actions account-editor-actions">
          <Button disabled={busy} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={busy}>
            <ClipboardCheck size={16} />
            {busy ? 'Đang lưu biên bản…' : 'Lưu biên bản kiểm tra'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

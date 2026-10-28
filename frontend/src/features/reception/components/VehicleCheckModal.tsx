import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useRecordVehicleCheckMutation, useUploadAttachmentMutation } from '../hooks/useReception';
import { vehicleCheckFormSchema, type VehicleCheckFormValues } from '../schemas/receptionSchema';
import { ClipboardCheck, Upload, Image } from 'lucide-react';

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
  const uploadAttachment = useUploadAttachmentMutation();

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

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      setSelectedFiles(Array.from(e.target.files));
    }
  };

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
          const formData = new FormData();
          formData.append('file', file);
          formData.append('vehicleCheckId', checkRecord.id);
          formData.append('attachmentType', 'CHECK_PHOTO');
          await uploadAttachment.mutateAsync(formData);
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
        className="editor-form account-editor-form"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THÔNG SỐ & LOẠI KIỂM TRA</div>

        <Field label="Loại kiểm tra" required error={errors.checkType?.message}>
          <select className="select" disabled={busy} {...register('checkType')}>
            <option value="CHECK_IN">Kiểm tra tiếp nhận (Check-in)</option>
            <option value="CHECK_OUT">Kiểm tra xuất xưởng (Check-out)</option>
            <option value="PERIODIC">Kiểm định định kỳ</option>
          </select>
        </Field>

        <Field label="Số ODO kiểm tra (km)" error={errors.odometer?.message} hint="Số km tại thời điểm bàn giao kiểm tra">
          <input
            type="number"
            className="input"
            min={0}
            placeholder="15000"
            disabled={busy}
            {...register('odometer')}
          />
        </Field>

        <div className="form-section-label">TÌNH TRẠNG HIỆN TRƯỜNG PHƯƠNG TIỆN</div>

        <Field
          label="Tình trạng ngoại thất (Vết trầy xước, móp méo)"
          hint="Ví dụ: Xước nhẹ cản sau bên phụ, móp nhẹ vè cua lốp..."
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
          label="Tình trạng nội thất & Đồ đạc trên xe"
          hint="Ghi nhận đồ dùng cá nhân có giá trị hoặc tình trạng khoang lái"
        >
          <input
            type="text"
            className="input"
            placeholder="Ghế da sạch, không để đồ quý giá..."
            disabled={busy}
            {...register('interiorCondition')}
          />
        </Field>

        <Field
          label="Đèn cảnh báo trên bảng táp-lô"
          hint="Kiểm tra các đèn Check Engine, ABS, Túi khí, Áp suất lốp..."
        >
          <input
            type="text"
            className="input"
            placeholder="Không có đèn báo lỗi taplo..."
            disabled={busy}
            {...register('warningLights')}
          />
        </Field>

        <Field label="Ghi chú tổng kết của người kiểm tra" error={errors.notes?.message} hint="Ghi chú thêm về tiếng ồn bất thường, rò rỉ dung dịch...">
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
              <strong>Tải lên ảnh chụp hiện trạng / Biên bản giấy:</strong> Hình ảnh được băm SHA-256 chống giả mạo và lưu trữ an toàn cùng lệnh sửa chữa.
            </span>
          </div>

          <label className="button button-secondary" style={{ display: 'inline-flex', alignItems: 'center', gap: 6, cursor: 'pointer' }}>
            <Upload size={16} />
            <span>Chọn tệp ảnh hoặc PDF</span>
            <input
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp,application/pdf"
              className="sr-only"
              onChange={handleFileChange}
              disabled={busy}
            />
          </label>

          {selectedFiles.length > 0 && (
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
              {selectedFiles.map((f, idx) => (
                <span
                  key={idx}
                  className="badge badge-neutral"
                  style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}
                >
                  <Image size={13} />
                  <span>{f.name}</span>
                </span>
              ))}
            </div>
          )}
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

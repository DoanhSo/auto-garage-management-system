import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError } from '../../../shared/api/errors';
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

      const checkResponse = await recordCheck.mutateAsync({
        repairOrderId: values.repairOrderId,
        checkType: values.checkType,
        checklistJson: JSON.stringify(checklistObj),
        odometer: Number.isFinite(values.odometer) ? Number(values.odometer) : null,
        notes: values.notes?.trim() || null,
      });

      // Nếu có tệp đính kèm, upload từng tệp (BR16)
      if (selectedFiles.length > 0) {
        for (const file of selectedFiles) {
          const formData = new FormData();
          formData.append('file', file);
          formData.append('repairOrderId', values.repairOrderId);
          formData.append('vehicleCheckId', checkResponse.id);
          formData.append('category', values.checkType);
          await uploadAttachment.mutateAsync(formData);
        }
      }

      notify('Ghi nhận biên bản kiểm tra xe thành công');
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

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      title={`Biên bản kiểm tra xe ${orderCode ? '#' + orderCode : ''}`}
      description="Kiểm tra hiện trạng ngoại thất, nội thất, đèn báo và chụp ảnh bằng chứng ban đầu."
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form"
      >
        <FormError message={errors.root?.server?.message} />

        <div className="form-row-2">
          <Field label="Loại biên bản kiểm tra" required error={errors.checkType?.message}>
            <select className="select" disabled={busy} {...register('checkType')}>
              <option value="CHECK_IN">Kiểm tra tiếp nhận ban đầu (Check-in)</option>
              <option value="HANDOVER">Kiểm tra bàn giao xe khách (Handover)</option>
            </select>
          </Field>

          <Field label="Số ODO kiểm tra" error={errors.odometer?.message}>
            <input
              type="number"
              className="input"
              min={0}
              placeholder="15000"
              disabled={busy}
              {...register('odometer')}
            />
          </Field>
        </div>

        <Field
          label="Tình trạng ngoại thất (Vết trầy xước, móp méo)"
          hint="Ví dụ: Xước nhẹ cản sau bên phụ, móp nhẹ vè cua lốp trước..."
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
          label="Đèn cảnh báo trên bảng taplo"
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

        <Field label="Ghi chú tổng kết của người kiểm tra" error={errors.notes?.message}>
          <textarea
            className="input min-h-[70px] resize-none"
            placeholder="Ghi chú thêm về tiếng ồn bất thường, rò rỉ dung dịch dưới gầm..."
            disabled={busy}
            {...register('notes')}
          />
        </Field>

        <div className="p-3 border border-dashed border-border rounded-lg bg-muted/30 space-y-2">
          <label className="text-xs font-semibold text-foreground flex items-center gap-1.5 cursor-pointer">
            <Upload size={14} className="text-primary" />
            <span>Tải lên ảnh chụp hiện trạng / Biên bản giấy (BR16)</span>
            <input
              type="file"
              multiple
              accept="image/jpeg,image/png,image/webp,application/pdf"
              className="hidden"
              onChange={handleFileChange}
              disabled={busy}
            />
          </label>
          <p className="text-[11px] text-muted-foreground">
            Chấp nhận định dạng: JPG, PNG, WEBP, PDF. Ảnh chụp hiện trạng sẽ tự động lưu trữ bảo mật
            và gắn với lệnh sửa chữa.
          </p>
          {selectedFiles.length > 0 && (
            <div className="flex flex-wrap gap-1.5 pt-1">
              {selectedFiles.map((f, idx) => (
                <span
                  key={idx}
                  className="inline-flex items-center gap-1 text-xs bg-card px-2 py-0.5 rounded border border-border"
                >
                  <Image size={11} className="text-primary" />
                  <span className="truncate max-w-[150px]">{f.name}</span>
                </span>
              ))}
            </div>
          )}
        </div>

        <div className="form-actions account-editor-actions">
          <Button type="button" variant="ghost" disabled={busy} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={busy}>
            <ClipboardCheck size={16} className="mr-1" />
            {busy ? 'Đang lưu biên bản...' : 'Lưu biên bản kiểm tra'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

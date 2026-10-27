import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError } from '../../../shared/api/errors';
import {
  Button,
  ConfirmDialog,
  Field,
  FormError,
  Loading,
  QueryError,
  Sheet,
} from '../../../shared/components/ui';
import type { Service } from '../../../shared/types/contracts';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import {
  useCreateServiceMutation,
  useUpdateServiceMutation,
  useServiceQuery,
} from '../hooks/useCatalog';
import {
  bayTypes,
  commonUnits,
  serviceDefaults,
  serviceFormSchema,
  type ServiceFormValues,
} from '../schemas/catalogSchema';

function ServiceForm({
  existing,
  onSaved,
  onRequestClose,
}: {
  existing?: Service;
  onSaved: () => void;
  onRequestClose: (handler: () => void) => void;
}) {
  const notify = useToast();
  const create = useCreateServiceMutation();
  const update = useUpdateServiceMutation();
  const [discard, setDiscard] = useState(false);

  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors, isDirty },
  } = useForm<ServiceFormValues>({
    resolver: zodResolver(serviceFormSchema),
    mode: 'onBlur',
    defaultValues: serviceDefaults(existing),
  });

  const selectedType = watch('serviceType');
  const minutes = watch('standardMinutes');

  const busy = create.isPending || update.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  useEffect(() => {
    onRequestClose(() => {
      if (busy) return;
      if (isDirty) setDiscard(true);
      else onSaved();
    });
  }, [onRequestClose, busy, isDirty, onSaved]);

  async function onSubmit(values: ServiceFormValues) {
    if (busy) return;
    try {
      if (existing) {
        await update.mutateAsync({
          id: existing.id,
          values: {
            serviceName: values.serviceName.trim(),
            serviceType: values.serviceType,
            description: values.description?.trim() || null,
            unit: values.unit?.trim() || 'lượt',
            unitPrice: Number(values.unitPrice),
            standardMinutes: Number(values.standardMinutes),
            requiredBayType: values.requiredBayType?.trim() || null,
            intervalKm: values.intervalKm ? Number(values.intervalKm) : null,
            intervalMonths: values.intervalMonths ? Number(values.intervalMonths) : null,
            warrantyDays: Number(values.warrantyDays ?? 0),
            warrantyKm: Number(values.warrantyKm ?? 0),
            warrantyTerms: values.warrantyTerms?.trim() || null,
          },
        });
        notify('Cập nhật dịch vụ thành công');
      } else {
        await create.mutateAsync({
          serviceCode: values.serviceCode?.trim() || null,
          serviceName: values.serviceName.trim(),
          serviceType: values.serviceType,
          description: values.description?.trim() || null,
          unit: values.unit?.trim() || 'lượt',
          unitPrice: Number(values.unitPrice),
          standardMinutes: Number(values.standardMinutes),
          requiredBayType: values.requiredBayType?.trim() || null,
          intervalKm: values.intervalKm ? Number(values.intervalKm) : null,
          intervalMonths: values.intervalMonths ? Number(values.intervalMonths) : null,
          warrantyDays: Number(values.warrantyDays ?? 0),
          warrantyKm: Number(values.warrantyKm ?? 0),
          warrantyTerms: values.warrantyTerms?.trim() || null,
        });
        notify('Thêm dịch vụ mới thành công');
      }
      onSaved();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const hoursDisplay = minutes ? (minutes / 60).toFixed(2) : '0.00';

  return (
    <>
      <form
        className="editor-form account-editor-form"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <FormError message={errors.root?.server?.message} />

        <div className="form-section-label">Thông tin dịch vụ & Phân loại</div>

        <Field
          label="Tên dịch vụ / Gói bảo dưỡng"
          required
          error={errors.serviceName?.message}
          hint="Ví dụ: Thay nhớt & lọc nhớt động cơ, Cân chỉnh góc đặt bánh xe..."
        >
          <input
            type="text"
            className="input"
            placeholder="Nhập tên dịch vụ..."
            {...register('serviceName')}
          />
        </Field>

        <div className="form-row-2">
          <Field
            label="Mã dịch vụ"
            error={errors.serviceCode?.message}
            hint={existing ? 'Mã dịch vụ đã định danh' : 'Để trống hệ thống sẽ tự sinh tự động'}
          >
            <input
              type="text"
              className="input font-mono"
              placeholder="DV-001, BD-5K..."
              disabled={Boolean(existing)}
              {...register('serviceCode')}
            />
          </Field>

          <Field label="Phân loại dịch vụ" required error={errors.serviceType?.message}>
            <select className="select" {...register('serviceType')}>
              <option value="SINGLE">Dịch vụ đơn lẻ (Single)</option>
              <option value="PACKAGE">Gói bảo dưỡng định kỳ (Package)</option>
            </select>
          </Field>
        </div>

        <div className="form-section-label">Giá tiền công & Định mức kỹ thuật</div>

        <div className="form-row-3">
          <Field
            label="Đơn giá tiền công (VNĐ)"
            required
            error={errors.unitPrice?.message}
            hint="Chưa bao gồm phụ tùng"
          >
            <input
              type="number"
              className="input"
              min={0}
              step={1000}
              placeholder="150000"
              {...register('unitPrice')}
            />
          </Field>

          <Field
            label="Thời gian định mức (phút)"
            required
            error={errors.standardMinutes?.message}
            hint={`Tương đương ~${hoursDisplay} giờ công`}
          >
            <input
              type="number"
              className="input"
              min={1}
              placeholder="30"
              {...register('standardMinutes')}
            />
          </Field>

          <Field label="Đơn vị tính" error={errors.unit?.message}>
            <input
              type="text"
              className="input"
              placeholder="lượt, gói, giờ..."
              list="units-list"
              {...register('unit')}
            />
            <datalist id="units-list">
              {commonUnits.map((u) => (
                <option key={u} value={u} />
              ))}
            </datalist>
          </Field>
        </div>

        <Field label="Yêu cầu loại khoang kỹ thuật" error={errors.requiredBayType?.message}>
          <select className="select" {...register('requiredBayType')}>
            <option value="">-- Không yêu cầu khoang cụ thể --</option>
            {bayTypes.map((bay) => (
              <option key={bay} value={bay}>
                {bay}
              </option>
            ))}
          </select>
        </Field>

        {selectedType === 'PACKAGE' && (
          <>
            <div className="form-section-label">Chu kỳ bảo dưỡng định kỳ</div>
            <div className="form-row-2">
              <Field
                label="Chu kỳ số km"
                error={errors.intervalKm?.message}
                hint="Ví dụ: 5.000 km, 10.000 km, 20.000 km..."
              >
                <input
                  type="number"
                  className="input"
                  min={1}
                  placeholder="5000"
                  {...register('intervalKm', { valueAsNumber: true })}
                />
              </Field>

              <Field
                label="Chu kỳ thời gian (tháng)"
                error={errors.intervalMonths?.message}
                hint="Ví dụ: 3 tháng, 6 tháng, 12 tháng..."
              >
                <input
                  type="number"
                  className="input"
                  min={1}
                  placeholder="6"
                  {...register('intervalMonths', { valueAsNumber: true })}
                />
              </Field>
            </div>
          </>
        )}

        <div className="form-section-label">Chính sách bảo hành dịch vụ</div>

        <div className="form-row-2">
          <Field
            label="Thời hạn bảo hành (ngày)"
            error={errors.warrantyDays?.message}
            hint="0 nếu không có bảo hành"
          >
            <input
              type="number"
              className="input"
              min={0}
              placeholder="30"
              {...register('warrantyDays')}
            />
          </Field>

          <Field
            label="Giới hạn bảo hành (km)"
            error={errors.warrantyKm?.message}
            hint="0 nếu không giới hạn theo km"
          >
            <input
              type="number"
              className="input"
              min={0}
              placeholder="1000"
              {...register('warrantyKm')}
            />
          </Field>
        </div>

        <Field label="Điều khoản bảo hành" error={errors.warrantyTerms?.message}>
          <input
            type="text"
            className="input"
            placeholder="Bảo hành lỗi kỹ thuật lắp ráp, rò rỉ dầu nhớt..."
            {...register('warrantyTerms')}
          />
        </Field>

        <Field label="Mô tả chi tiết hạng mục công việc" error={errors.description?.message}>
          <textarea
            className="input min-h-[75px] resize-none"
            placeholder="Liệt kê các bước thực hiện, ghi chú kỹ thuật hoặc phạm vi áp dụng..."
            {...register('description')}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button
            type="button"
            variant="ghost"
            disabled={busy}
            onClick={() => {
              if (isDirty) setDiscard(true);
              else onSaved();
            }}
          >
            Hủy
          </Button>
          <Button type="submit" variant="primary" disabled={busy}>
            {busy ? 'Đang lưu...' : existing ? 'Lưu thay đổi' : 'Thêm dịch vụ'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={discard}
        title="Hủy các thay đổi?"
        description="Dữ liệu dịch vụ chưa lưu sẽ bị mất nếu bạn đóng biểu mẫu này."
        confirmLabel="Bỏ thay đổi"
        danger
        onConfirm={() => {
          setDiscard(false);
          onSaved();
        }}
        onCancel={() => setDiscard(false)}
      />

      <ConfirmDialog
        open={blocker.state === 'blocked'}
        title="Rời khỏi trang?"
        description="Bạn có thay đổi chưa lưu trên thông tin dịch vụ. Bạn có chắc muốn rời đi?"
        confirmLabel="Rời đi"
        danger
        onConfirm={() => blocker.proceed?.()}
        onCancel={() => blocker.reset?.()}
      />
    </>
  );
}

export function ServiceEditor({
  serviceId,
  open,
  onClose,
}: {
  serviceId: string | null;
  open: boolean;
  onClose: () => void;
}) {
  const isEditing = Boolean(serviceId);
  const detail = useServiceQuery(serviceId);
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);
  const registerCloseHandler = useCallback(
    (handler: () => void) => setCloseHandler(() => handler),
    [],
  );

  const title = isEditing ? 'Chỉnh sửa dịch vụ & đơn giá' : 'Thêm dịch vụ kỹ thuật mới';
  const description = isEditing
    ? 'Cập nhật tiền công, định mức thời gian và điều kiện bảo hành dịch vụ.'
    : 'Khai báo dịch vụ sửa chữa hoặc gói bảo dưỡng định kỳ vào danh mục AutoGarage.';

  return (
    <Sheet
      open={open}
      onClose={() => {
        if (closeHandler) closeHandler();
        else onClose();
      }}
      title={title}
      description={description}
      placement="center"
    >
      {isEditing && detail.isLoading ? (
        <Loading label="Đang tải dữ liệu dịch vụ…" />
      ) : isEditing && detail.isError ? (
        <QueryError error={detail.error} retry={() => detail.refetch()} />
      ) : (
        <ServiceForm
          key={serviceId ?? 'new'}
          existing={detail.data}
          onSaved={onClose}
          onRequestClose={registerCloseHandler}
        />
      )}
    </Sheet>
  );
}

import { useCallback, useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
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

  const hours = ((Number(minutes) || 0) / 60).toFixed(1);
  const generalError = create.error || update.error;

  return (
    <>
      <form
        className="editor-form account-editor-form"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THÔNG TIN DỊCH VỤ CƠ BẢN</div>

        <Field label="Tên dịch vụ / Hạng mục" required error={errors.serviceName?.message}>
          <input
            type="text"
            className="input"
            placeholder="Ví dụ: Thay dầu động cơ & lọc nhớt"
            disabled={busy}
            autoFocus
            {...register('serviceName')}
          />
        </Field>

        <Field
          label="Mã dịch vụ"
          error={errors.serviceCode?.message}
          hint={existing ? 'Mã dịch vụ cố định' : 'Để trống hệ thống sẽ tự sinh (VD: SRV-...) '}
        >
          <input
            type="text"
            className="input"
            placeholder="SRV-OIL-01"
            disabled={Boolean(existing) || busy}
            {...register('serviceCode')}
          />
        </Field>

        <Field label="Phân loại dịch vụ" required error={errors.serviceType?.message}>
          <select className="select" disabled={busy} {...register('serviceType')}>
            <option value="SINGLE">Dịch vụ đơn lẻ (Single)</option>
            <option value="PACKAGE">Gói bảo dưỡng định kỳ (Package)</option>
          </select>
        </Field>

        <Field label="Đơn vị tính" error={errors.unit?.message}>
          <select className="select" disabled={busy} {...register('unit')}>
            {commonUnits.map((u) => (
              <option key={u} value={u}>
                {u}
              </option>
            ))}
          </select>
        </Field>

        <Field label="Đơn giá tiền công (VND)" required error={errors.unitPrice?.message} hint="Tiền công kỹ thuật, chưa gồm phụ tùng">
          <input
            type="number"
            className="input"
            step="10000"
            min="0"
            disabled={busy}
            {...register('unitPrice')}
          />
        </Field>

        <Field
          label="Định mức thời gian (phút)"
          required
          error={errors.standardMinutes?.message}
          hint={`Tương đương ~${hours} giờ công tiêu chuẩn`}
        >
          <input
            type="number"
            className="input"
            min="5"
            step="5"
            disabled={busy}
            {...register('standardMinutes')}
          />
        </Field>

        <div className="full-width-field">
          <Field label="Yêu cầu loại khoang kỹ thuật" error={errors.requiredBayType?.message}>
            <select className="select" disabled={busy} {...register('requiredBayType')}>
              <option value="">Khoang tự do (Không yêu cầu đặc biệt)</option>
              {bayTypes.map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </select>
          </Field>
        </div>

        {selectedType === 'PACKAGE' && (
          <>
            <div className="form-section-label">CẤU HÌNH GÓI BẢO DƯỠNG ĐỊNH KỲ</div>
            <Field label="Chu kỳ bảo dưỡng theo số km" error={errors.intervalKm?.message} hint="Ví dụ: 5000, 10000, 20000 km">
              <input
                type="number"
                className="input"
                step="1000"
                min="0"
                placeholder="5000"
                disabled={busy}
                {...register('intervalKm')}
              />
            </Field>

            <Field label="Chu kỳ bảo dưỡng theo tháng" error={errors.intervalMonths?.message} hint="Ví dụ: 3, 6, 12 tháng">
              <input
                type="number"
                className="input"
                min="0"
                placeholder="6"
                disabled={busy}
                {...register('intervalMonths')}
              />
            </Field>
          </>
        )}

        <div className="form-section-label">CHÍNH SÁCH BẢO HÀNH DỊCH VỤ</div>
        <Field label="Thời hạn bảo hành (Ngày)" error={errors.warrantyDays?.message} hint="Số ngày bảo hành kỹ thuật sau xuất xưởng">
          <input
            type="number"
            className="input"
            min="0"
            disabled={busy}
            {...register('warrantyDays')}
          />
        </Field>

        <Field label="Giới hạn bảo hành theo km" error={errors.warrantyKm?.message} hint="Số km tối đa còn hiệu lực bảo hành">
          <input
            type="number"
            className="input"
            min="0"
            step="500"
            disabled={busy}
            {...register('warrantyKm')}
          />
        </Field>

        <div className="full-width-field">
          <Field label="Mô tả chi tiết nội dung công việc" error={errors.description?.message}>
            <textarea
              className="input min-h-[75px] resize-none"
              placeholder="Ghi chú chi tiết các bước kiểm tra, bảo dưỡng trong hạng mục dịch vụ này..."
              disabled={busy}
              {...register('description')}
            />
          </Field>
        </div>

        <div className="form-actions account-editor-actions">
          <Button
            disabled={busy}
            onClick={() => {
              if (isDirty) setDiscard(true);
              else onSaved();
            }}
          >
            Hủy
          </Button>
          <Button
            variant="primary"
            type="submit"
            pending={busy}
            disabled={Boolean(existing && !isDirty)}
          >
            {busy ? 'Đang lưu…' : existing ? 'Lưu thay đổi' : 'Thêm dịch vụ'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung đang nhập sẽ được xóa. Dữ liệu đã lưu trên máy chủ không thay đổi."
        confirmLabel="Bỏ thay đổi"
        onConfirm={() => {
          setDiscard(false);
          if (blocker.state === 'blocked') blocker.proceed?.();
          else onSaved();
        }}
        onCancel={() => {
          setDiscard(false);
          blocker.reset?.();
        }}
      />
    </>
  );
}

export function ServiceEditor({
  serviceId,
  open,
  onClose,
  onRestoreFocus,
}: {
  serviceId: string | null;
  open: boolean;
  onClose: () => void;
  onRestoreFocus?: () => void;
}) {
  const isEditing = Boolean(serviceId);
  const detail = useServiceQuery(serviceId);
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);
  const registerCloseHandler = useCallback(
    (handler: () => void) => setCloseHandler(() => handler),
    [],
  );

  const title = isEditing ? 'Chỉnh sửa dịch vụ' : 'Thêm dịch vụ kỹ thuật';
  const description = isEditing
    ? 'Cập nhật tiền công, định mức thời gian và điều kiện bảo hành dịch vụ.'
    : 'Khai báo dịch vụ sửa chữa hoặc gói bảo dưỡng định kỳ vào danh mục gara.';

  return (
    <Sheet
      open={open}
      onClose={() => {
        if (closeHandler) closeHandler();
        else onClose();
      }}
      onRestoreFocus={onRestoreFocus}
      title={title}
      description={description}
      placement="center"
    >
      {isEditing && detail.isLoading ? (
        <Loading label="Đang lấy phiên bản mới nhất…" />
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

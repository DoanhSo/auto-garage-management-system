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
import type { Vehicle } from '../../../shared/types/contracts';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useCustomersQuery } from '../../customer/hooks/useCustomers';
import {
  useCreateVehicleMutation,
  useUpdateVehicleMutation,
  useVehicleQuery,
} from '../hooks/useVehicles';
import {
  fuelTypes,
  vehicleDefaults,
  vehicleFormSchema,
  type VehicleFormValues,
} from '../schemas/vehicleSchema';

function VehicleForm({
  existing,
  preselectedCustomerId,
  onSaved,
  onRequestClose,
}: {
  existing?: Vehicle;
  preselectedCustomerId?: string;
  onSaved: () => void;
  onRequestClose: (handler: () => void) => void;
}) {
  const notify = useToast();
  const create = useCreateVehicleMutation();
  const update = useUpdateVehicleMutation();
  const [discard, setDiscard] = useState(false);

  const { data: customers = [], isLoading: isLoadingCustomers } = useCustomersQuery({
    isActive: true,
  });

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isDirty },
  } = useForm<VehicleFormValues>({
    resolver: zodResolver(vehicleFormSchema),
    mode: 'onBlur',
    defaultValues: {
      ...vehicleDefaults(existing),
      customerId: existing?.customerId ?? preselectedCustomerId ?? '',
    },
  });

  const busy = create.isPending || update.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  useEffect(() => {
    onRequestClose(() => {
      if (busy) return;
      if (isDirty) setDiscard(true);
      else onSaved();
    });
  }, [onRequestClose, busy, isDirty, onSaved]);

  async function onSubmit(values: VehicleFormValues) {
    if (busy) return;
    try {
      if (existing) {
        await update.mutateAsync({
          id: existing.id,
          values: {
            brand: values.brand.trim(),
            model: values.model.trim(),
            manufactureYear: values.manufactureYear ? Number(values.manufactureYear) : null,
            color: values.color?.trim() || null,
            fuelType: values.fuelType?.trim() || null,
            odometer: Number(values.odometer),
            inspectionDueDate: values.inspectionDueDate || null,
          },
        });
        notify('Cập nhật thông tin phương tiện thành công');
      } else {
        await create.mutateAsync({
          customerId: values.customerId,
          licensePlate: values.licensePlate.trim().toUpperCase(),
          vin: values.vin?.trim() || null,
          engineNumber: values.engineNumber?.trim() || null,
          brand: values.brand.trim(),
          model: values.model.trim(),
          manufactureYear: values.manufactureYear ? Number(values.manufactureYear) : null,
          color: values.color?.trim() || null,
          fuelType: values.fuelType?.trim() || null,
          odometer: Number(values.odometer),
          inspectionDueDate: values.inspectionDueDate || null,
        });
        notify('Đăng ký phương tiện mới thành công');
      }
      onSaved();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const generalError = create.error || update.error;

  return (
    <>
      <form
        className="editor-form account-editor-form"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THÔNG TIN SỞ HỮU & ĐỊNH DANH</div>

        <Field
          label="Chủ sở hữu phương tiện"
          required={!existing}
          error={errors.customerId?.message}
          hint={
            existing
              ? 'Chủ xe được cố định. Để đổi chủ, hãy dùng tính năng Đổi chủ.'
              : 'Chọn khách hàng đứng tên chủ xe'
          }
        >
          {existing ? (
            <input
              type="text"
              className="input"
              value={`${existing.customerName} (${existing.customerPhone})`}
              disabled
            />
          ) : (
            <select className="select" disabled={isLoadingCustomers || busy} {...register('customerId')}>
              <option value="">-- Chọn khách hàng chủ xe --</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.customerCode ? `[${c.customerCode}] ` : ''}
                  {c.fullName} - {c.phone}
                </option>
              ))}
            </select>
          )}
        </Field>

        <Field
          label="Biển số xe"
          required={!existing}
          error={errors.licensePlate?.message}
          hint={
            existing
              ? 'Biển số xe cố định sau khi đăng ký'
              : 'Ví dụ: 30A-12345, 51K-99999'
          }
        >
          <input
            type="text"
            className="input"
            placeholder="30A-12345"
            disabled={Boolean(existing) || busy}
            {...register('licensePlate')}
          />
        </Field>

        <Field label="Số khung (VIN)" error={errors.vin?.message} hint="17 ký tự chuẩn quốc tế (không bắt buộc)">
          <input
            type="text"
            className="input"
            placeholder="1HGCR2F83HA000000"
            disabled={busy}
            maxLength={17}
            {...register('vin')}
          />
        </Field>

        <Field label="Số máy (Engine No)" error={errors.engineNumber?.message} hint="Số hiệu động cơ trên đăng kiểm">
          <input
            type="text"
            className="input"
            placeholder="2AR-FE-1234567"
            disabled={busy}
            {...register('engineNumber')}
          />
        </Field>

        <div className="form-section-label">THÔNG SỐ KỸ THUẬT PHƯƠNG TIỆN</div>

        <Field label="Hãng sản xuất" required error={errors.brand?.message} hint="Toyota, Honda, Mazda, Hyundai, Kia, VinFast...">
          <input
            type="text"
            className="input"
            placeholder="Toyota"
            disabled={busy}
            {...register('brand')}
          />
        </Field>

        <Field label="Dòng xe (Model)" required error={errors.model?.message} hint="Camry, Civic, CX-5, SantaFe, VF8...">
          <input
            type="text"
            className="input"
            placeholder="Camry 2.5Q"
            disabled={busy}
            {...register('model')}
          />
        </Field>

        <Field label="Năm sản xuất" error={errors.manufactureYear?.message}>
          <input
            type="number"
            className="input"
            placeholder="2022"
            min="1990"
            max={new Date().getFullYear() + 1}
            disabled={busy}
            {...register('manufactureYear')}
          />
        </Field>

        <Field label="Màu sơn xe" error={errors.color?.message}>
          <input
            type="text"
            className="input"
            placeholder="Trắng ngọc trai, Đen, Đỏ..."
            disabled={busy}
            {...register('color')}
          />
        </Field>

        <Field label="Loại nhiên liệu" error={errors.fuelType?.message}>
          <select className="select" disabled={busy} {...register('fuelType')}>
            <option value="">-- Chọn loại nhiên liệu --</option>
            {fuelTypes.map((ft) => (
              <option key={ft} value={ft}>
                {ft}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="Số ODO hiện tại (km)"
          required
          error={errors.odometer?.message}
          hint="Số km công-tơ-mét ghi nhận thực tế"
        >
          <input
            type="number"
            className="input"
            min="0"
            step="100"
            disabled={busy}
            {...register('odometer')}
          />
        </Field>

        <div className="full-width-field">
          <Field
            label="Hạn đăng kiểm tiếp theo"
            error={errors.inspectionDueDate?.message}
            hint="Ngày hết hạn trên tem đăng kiểm dán kính lái"
          >
            <input
              type="date"
              className="input"
              disabled={busy}
              {...register('inspectionDueDate')}
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
            {busy ? 'Đang lưu…' : existing ? 'Lưu thay đổi' : 'Đăng ký xe'}
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

export function VehicleEditor({
  vehicleId,
  preselectedCustomerId,
  open,
  onClose,
  onRestoreFocus,
}: {
  vehicleId: string | null;
  preselectedCustomerId?: string;
  open: boolean;
  onClose: () => void;
  onRestoreFocus?: () => void;
}) {
  const isEditing = Boolean(vehicleId);
  const detail = useVehicleQuery(vehicleId);
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);
  const registerCloseHandler = useCallback(
    (handler: () => void) => setCloseHandler(() => handler),
    [],
  );

  const title = isEditing ? 'Chỉnh sửa phương tiện' : 'Đăng ký phương tiện';
  const description = isEditing
    ? 'Cập nhật thông số kỹ thuật, số ODO và hạn đăng kiểm của xe.'
    : 'Nhập thông tin biển số, chủ sở hữu và thông số xe để lưu vào gara.';

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
        <VehicleForm
          key={vehicleId ?? 'new'}
          existing={detail.data}
          preselectedCustomerId={preselectedCustomerId}
          onSaved={onClose}
          onRequestClose={registerCloseHandler}
        />
      )}
    </Sheet>
  );
}

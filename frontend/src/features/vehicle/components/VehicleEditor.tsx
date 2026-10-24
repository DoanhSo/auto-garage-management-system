import { useEffect, useState } from 'react';
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

  const { data: customers = [], isLoading: isLoadingCustomers } = useCustomersQuery({ isActive: true });

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
        if (values.odometer < existing.odometer) {
          setError('odometer', {
            message: `Số ODO không được nhỏ hơn ODO hiện tại (${existing.odometer.toLocaleString('vi-VN')} km)`,
          });
          return;
        }

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
        notify.success('Cập nhật thông tin phương tiện thành công');
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
        notify.success('Đăng ký phương tiện mới thành công');
      }
      onSaved();
    } catch (err: unknown) {
      const field = applyFormError(err, setError);
      if (!field) {
        notify.error(errorMessage(err) ?? 'Lưu thông tin phương tiện thất bại');
      }
    }
  }

  return (
    <>
      <form className="editor-form account-editor-form" onSubmit={handleSubmit(onSubmit)} noValidate>
        <FormError message={errors.root?.message} />

        <div className="form-section-label">Thông tin sở hữu & Định danh</div>

        <Field
          label="Chủ sở hữu xe"
          required={!existing}
          error={errors.customerId?.message}
          hint={existing ? 'Chủ xe được cố định. Để đổi chủ sở hữu, hãy dùng chức năng Chuyển nhượng.' : undefined}
        >
          {existing ? (
            <input
              type="text"
              className="input"
              value={`${existing.customerName} (${existing.customerPhone})`}
              disabled
            />
          ) : (
            <select
              className="select"
              disabled={isLoadingCustomers}
              {...register('customerId')}
            >
              <option value="">-- Chọn khách hàng chủ xe --</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.customerCode ? `[${c.customerCode}] ` : ''}{c.fullName} - {c.phone}
                </option>
              ))}
            </select>
          )}
        </Field>

        <div className="form-row-2">
          <Field
            label="Biển số xe"
            required={!existing}
            error={errors.licensePlate?.message}
            hint={existing ? 'Biển số xe không thể thay đổi sau khi đăng ký' : 'Ví dụ: 30F-12345, 51K-99999'}
          >
            <input
              type="text"
              className="input uppercase font-mono"
              placeholder="30A-12345"
              disabled={Boolean(existing)}
              {...register('licensePlate')}
            />
          </Field>

          <Field label="Số ODO hiện tại (km)" required error={errors.odometer?.message}>
            <input
              type="number"
              className="input"
              min={existing ? existing.odometer : 0}
              placeholder="0"
              {...register('odometer')}
            />
          </Field>
        </div>

        <div className="form-row-2">
          <Field label="Số khung (VIN)" error={errors.vin?.message}>
            <input
              type="text"
              className="input font-mono"
              placeholder="17 ký tự VIN..."
              disabled={Boolean(existing)}
              {...register('vin')}
            />
          </Field>

          <Field label="Số máy (Engine No.)" error={errors.engineNumber?.message}>
            <input
              type="text"
              className="input font-mono"
              placeholder="Số máy..."
              disabled={Boolean(existing)}
              {...register('engineNumber')}
            />
          </Field>
        </div>

        <div className="form-section-label">Thông số kỹ thuật xe</div>

        <div className="form-row-2">
          <Field label="Hãng xe (Brand)" required error={errors.brand?.message}>
            <input
              type="text"
              className="input"
              placeholder="Toyota, Hyundai, Ford, Kia..."
              {...register('brand')}
            />
          </Field>

          <Field label="Dòng xe (Model)" required error={errors.model?.message}>
            <input
              type="text"
              className="input"
              placeholder="Vios, Tucson, Ranger, Morning..."
              {...register('model')}
            />
          </Field>
        </div>

        <div className="form-row-3">
          <Field label="Năm sản xuất" error={errors.manufactureYear?.message}>
            <input
              type="number"
              className="input"
              placeholder="2022"
              {...register('manufactureYear')}
            />
          </Field>

          <Field label="Màu sơn" error={errors.color?.message}>
            <input
              type="text"
              className="input"
              placeholder="Trắng, Đen, Đỏ..."
              {...register('color')}
            />
          </Field>

          <Field label="Nhiên liệu" error={errors.fuelType?.message}>
            <select className="select" {...register('fuelType')}>
              <option value="">-- Chọn --</option>
              {fuelTypes.map((fuel) => (
                <option key={fuel} value={fuel}>
                  {fuel}
                </option>
              ))}
            </select>
          </Field>
        </div>

        <Field
          label="Hạn đăng kiểm tiếp theo"
          error={errors.inspectionDueDate?.message}
          hint="Hệ thống sẽ nhắc hạn trước 15 ngày"
        >
          <input
            type="date"
            className="input"
            {...register('inspectionDueDate')}
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
            {busy ? 'Đang lưu...' : existing ? 'Lưu thay đổi' : 'Đăng ký xe'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={discard}
        title="Hủy các thay đổi?"
        description="Dữ liệu thông tin xe chưa lưu sẽ bị mất nếu bạn đóng biểu mẫu này."
        confirmLabel="Bỏ thay đổi"
        cancelLabel="Tiếp tục chỉnh sửa"
        destructive
        onConfirm={() => {
          setDiscard(false);
          onSaved();
        }}
        onCancel={() => setDiscard(false)}
      />

      <ConfirmDialog
        open={blocker.state === 'blocked'}
        title="Rời khỏi trang?"
        description="Bạn có thay đổi chưa lưu trên thông tin phương tiện. Bạn có chắc muốn rời đi?"
        confirmLabel="Rời đi"
        cancelLabel="Ở lại"
        destructive
        onConfirm={() => blocker.proceed?.()}
        onCancel={() => blocker.reset?.()}
      />
    </>
  );
}

export function VehicleEditor({
  vehicleId,
  preselectedCustomerId,
  open,
  onClose,
}: {
  vehicleId: string | null;
  preselectedCustomerId?: string;
  open: boolean;
  onClose: () => void;
}) {
  const isEditing = Boolean(vehicleId);
  const detail = useVehicleQuery(vehicleId);
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);

  const title = isEditing ? 'Chỉnh sửa thông tin phương tiện' : 'Đăng ký phương tiện mới';
  const description = isEditing
    ? 'Cập nhật thông số kỹ thuật, số ODO và hạn đăng kiểm của xe.'
    : 'Nhập thông tin biển số, chủ sở hữu và thông số xe để lưu vào hệ thống AutoGarage.';

  return (
    <Sheet
      open={open}
      onClose={() => {
        if (closeHandler) closeHandler();
        else onClose();
      }}
      title={title}
      description={description}
      placement="side"
    >
      {isEditing && detail.isLoading ? (
        <Loading label="Đang tải thông tin phương tiện…" />
      ) : isEditing && detail.isError ? (
        <QueryError
          error={detail.error}
          retry={() => detail.refetch()}
        />
      ) : (
        <VehicleForm
          key={vehicleId ?? 'new'}
          existing={detail.data}
          preselectedCustomerId={preselectedCustomerId}
          onSaved={onClose}
          onRequestClose={setCloseHandler}
        />
      )}
    </Sheet>
  );
}

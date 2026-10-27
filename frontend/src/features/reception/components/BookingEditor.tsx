import { useCallback, useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError } from '../../../shared/api/errors';
import { Button, ConfirmDialog, Field, FormError, Sheet } from '../../../shared/components/ui';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useCustomersQuery, useMyCustomerQuery } from '../../customer/hooks/useCustomers';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { useServicesQuery } from '../../catalog/hooks/useCatalog';
import { useCreateBookingMutation } from '../hooks/useReception';
import {
  bookingDefaults,
  bookingFormSchema,
  type BookingFormValues,
} from '../schemas/receptionSchema';

function BookingForm({
  onSaved,
  onRequestClose,
}: {
  onSaved: () => void;
  onRequestClose: (handler: () => void) => void;
}) {
  const notify = useToast();
  const { user } = useAuth();
  const isCustomer = user?.role === 'CUSTOMER';
  const create = useCreateBookingMutation();
  const [discard, setDiscard] = useState(false);

  const directory = useCustomersQuery({ isActive: true }, !isCustomer);
  const ownProfile = useMyCustomerQuery(isCustomer);
  const customers = isCustomer
    ? ownProfile.data
      ? [ownProfile.data]
      : []
    : (directory.data ?? []);
  const isLoadingCustomers = isCustomer ? ownProfile.isLoading : directory.isLoading;
  const { data: allVehicles = [], isLoading: isLoadingVehicles } = useVehiclesQuery({
    isActive: true,
  });
  const { data: services = [], isLoading: isLoadingServices } = useServicesQuery({
    isActive: true,
  });

  const {
    register,
    handleSubmit,
    watch,
    setError,
    setValue,
    formState: { errors, isDirty },
  } = useForm<BookingFormValues>({
    resolver: zodResolver(bookingFormSchema),
    mode: 'onBlur',
    defaultValues: bookingDefaults(),
  });

  useEffect(() => {
    if (isCustomer && ownProfile.data) setValue('customerId', ownProfile.data.id);
  }, [isCustomer, ownProfile.data, setValue]);
  const selectedCustomerId = watch('customerId');

  // Lọc xe theo khách hàng đã chọn
  const customerVehicles = useMemo(() => {
    if (!selectedCustomerId) return [];
    return allVehicles.filter((v) => v.customerId === selectedCustomerId);
  }, [allVehicles, selectedCustomerId]);

  const busy = create.isPending;
  const blocker = useUnsavedChangesGuard(isDirty && !busy);

  useEffect(() => {
    onRequestClose(() => {
      if (busy) return;
      if (isDirty) setDiscard(true);
      else onSaved();
    });
  }, [onRequestClose, busy, isDirty, onSaved]);

  async function onSubmit(values: BookingFormValues) {
    if (busy) return;
    try {
      // Chuyển đổi định dạng ISO string cho appointmentAt
      const date = new Date(values.appointmentAt);
      if (isNaN(date.getTime())) {
        setError('appointmentAt', { message: 'Thời gian hẹn không hợp lệ' });
        return;
      }

      await create.mutateAsync({
        customerId: values.customerId,
        vehicleId: values.vehicleId,
        serviceId: values.serviceId || null,
        bayId: values.bayId || null,
        appointmentAt: date.toISOString(),
        requestDescription: values.requestDescription?.trim() || null,
      });

      notify('Đặt lịch hẹn thành công');
      onSaved();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  return (
    <>
      <form
        className="editor-form account-editor-form"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <FormError message={errors.root?.server?.message} />

        <div className="form-section-label">Thông tin khách hàng & Phương tiện</div>

        <Field
          label="Khách hàng đặt hẹn"
          required
          error={errors.customerId?.message}
          hint="Chọn khách hàng từ hệ thống"
        >
          <select
            className="select"
            disabled={isLoadingCustomers || isCustomer}
            {...register('customerId', {
              onChange: () => setValue('vehicleId', ''),
            })}
          >
            <option value="">-- Chọn khách hàng --</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.customerCode ? `[${c.customerCode}] ` : ''}
                {c.fullName} - {c.phone}
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="Phương tiện hẹn sửa chữa"
          required
          error={errors.vehicleId?.message}
          hint={
            selectedCustomerId && customerVehicles.length === 0
              ? 'Khách hàng này chưa có xe đăng ký trong hệ thống. Hãy đăng ký xe trước.'
              : undefined
          }
        >
          <select
            className="select"
            disabled={isLoadingVehicles || !selectedCustomerId}
            {...register('vehicleId')}
          >
            <option value="">-- Chọn xe --</option>
            {customerVehicles.map((v) => (
              <option key={v.id} value={v.id}>
                {v.licensePlate} ({v.brand} {v.model})
              </option>
            ))}
          </select>
        </Field>

        <div className="form-section-label">Thời gian & Dịch vụ yêu cầu</div>

        <Field
          label="Thời gian hẹn tiếp nhận"
          required
          error={errors.appointmentAt?.message}
          hint="Thời gian khách hàng dự kiến đưa xe vào xưởng"
        >
          <input type="datetime-local" className="input" {...register('appointmentAt')} />
        </Field>

        <Field label="Hạng mục / Gói dịch vụ đăng ký" error={errors.serviceId?.message}>
          <select className="select" disabled={isLoadingServices} {...register('serviceId')}>
            <option value="">-- Khám tổng quát / Chưa xác định dịch vụ cụ thể --</option>
            {services.map((s) => (
              <option key={s.id} value={s.id}>
                [{s.serviceCode}] {s.serviceName} ({s.standardMinutes} phút)
              </option>
            ))}
          </select>
        </Field>

        <Field
          label="Mô tả hiện tượng / Yêu cầu của khách hàng"
          error={errors.requestDescription?.message}
        >
          <textarea
            className="input min-h-[80px] resize-none"
            placeholder="Khách báo xe phát ra tiếng ồn khi tăng tốc, có mùi khét, cần thay nhớt..."
            {...register('requestDescription')}
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
            {busy ? 'Đang lưu...' : 'Xác nhận đặt lịch'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={discard}
        title="Hủy đặt lịch hẹn?"
        description="Dữ liệu lịch hẹn chưa lưu sẽ bị mất nếu bạn đóng biểu mẫu này."
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
        description="Bạn có thay đổi chưa lưu trên biểu mẫu đặt lịch hẹn."
        confirmLabel="Rời đi"
        danger
        onConfirm={() => blocker.proceed?.()}
        onCancel={() => blocker.reset?.()}
      />
    </>
  );
}

export function BookingEditor({ open, onClose }: { open: boolean; onClose: () => void }) {
  const [closeHandler, setCloseHandler] = useState<(() => void) | null>(null);
  const registerCloseHandler = useCallback(
    (handler: () => void) => setCloseHandler(() => handler),
    [],
  );

  return (
    <Sheet
      open={open}
      onClose={() => {
        if (closeHandler) closeHandler();
        else onClose();
      }}
      title="Đặt lịch hẹn dịch vụ mới"
      description="Ghi nhận lịch hẹn bảo dưỡng, sửa chữa cho khách hàng và phân bổ khoang dự kiến."
      placement="side"
    >
      <BookingForm onSaved={onClose} onRequestClose={registerCloseHandler} />
    </Sheet>
  );
}

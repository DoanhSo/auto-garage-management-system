import { useCallback, useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
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
    if (isCustomer && ownProfile.data?.id) {
      setValue('customerId', ownProfile.data.id);
    }
  }, [isCustomer, ownProfile.data, setValue]);

  const selectedCustomerId = watch('customerId');

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
      const appointmentDate = new Date(values.appointmentAt);
      if (isNaN(appointmentDate.getTime())) {
        setError('appointmentAt', { message: 'Thời gian hẹn không hợp lệ' });
        return;
      }

      await create.mutateAsync({
        customerId: values.customerId,
        vehicleId: values.vehicleId,
        appointmentAt: appointmentDate.toISOString(),
        serviceId: values.serviceId || null,
        bayId: values.bayId || null,
        requestDescription: values.requestDescription?.trim() || null,
      });

      notify('Đặt lịch hẹn thành công');
      onSaved();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const generalError = create.error;

  return (
    <>
      <form
        className="editor-form account-editor-form"
        onSubmit={handleSubmit(onSubmit)}
        noValidate
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THÔNG TIN KHÁCH HÀNG & PHƯƠNG TIỆN</div>

        <Field label="Khách hàng đặt hẹn" required error={errors.customerId?.message} hint="Chọn hồ sơ khách hàng đã có trên hệ thống">
          {isCustomer ? (
            <input
              type="text"
              className="input"
              value={user?.fullName ?? 'Hồ sơ cá nhân của bạn'}
              disabled
            />
          ) : (
            <select
              className="select"
              disabled={isLoadingCustomers || busy}
              {...register('customerId')}
            >
              <option value="">-- Chọn khách hàng --</option>
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
          label="Phương tiện dịch vụ"
          required
          error={errors.vehicleId?.message}
          hint={
            !selectedCustomerId
              ? 'Vui lòng chọn khách hàng trước'
              : customerVehicles.length === 0
                ? 'Khách hàng này chưa có xe đăng ký trên hệ thống'
                : 'Chọn xe cần bảo dưỡng / sửa chữa'
          }
        >
          <select
            className="select"
            disabled={!selectedCustomerId || customerVehicles.length === 0 || isLoadingVehicles || busy}
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

        <div className="form-section-label">THỜI GIAN & HẠNG MỤC DỊCH VỤ</div>

        <Field
          label="Thời gian hẹn tiếp nhận"
          required
          error={errors.appointmentAt?.message}
          hint="Chọn ngày giờ tiếp nhận xe vào gara"
        >
          <input
            type="datetime-local"
            className="input"
            disabled={busy}
            {...register('appointmentAt')}
          />
        </Field>

        <Field label="Dịch vụ yêu cầu chính" error={errors.serviceId?.message} hint="Gói bảo dưỡng hoặc dịch vụ dự kiến">
          <select className="select" disabled={isLoadingServices || busy} {...register('serviceId')}>
            <option value="">Khám & Chẩn đoán tổng quát</option>
            {services.map((s) => (
              <option key={s.id} value={s.id}>
                {s.serviceName} ({s.serviceType === 'PACKAGE' ? 'Gói BD' : 'Lẻ'})
              </option>
            ))}
          </select>
        </Field>

        <div className="full-width-field">
          <Field
            label="Mô tả hiện tượng / Yêu cầu cụ thể"
            error={errors.requestDescription?.message}
            hint="Mô tả rõ yêu cầu của khách hàng hoặc triệu chứng hư hỏng của xe"
          >
            <textarea
              className="input min-h-[75px] resize-none"
              placeholder="Ví dụ: Xe kêu cọt kẹt khi qua gờ giảm tốc, bảo dưỡng định kỳ mốc 20.000 km..."
              disabled={busy}
              {...register('requestDescription')}
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
          <Button type="submit" variant="primary" pending={busy}>
            {busy ? 'Đang lưu…' : 'Xác nhận đặt lịch'}
          </Button>
        </div>
      </form>

      <ConfirmDialog
        open={discard || blocker.state === 'blocked'}
        title="Bỏ thay đổi chưa lưu?"
        description="Nội dung lịch hẹn chưa lưu sẽ bị xóa nếu bạn đóng biểu mẫu này."
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

export function BookingEditor({
  open,
  onClose,
  onRestoreFocus,
}: {
  open: boolean;
  onClose: () => void;
  onRestoreFocus?: () => void;
}) {
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
      onRestoreFocus={onRestoreFocus}
      title="Đặt lịch hẹn dịch vụ mới"
      description="Ghi nhận lịch hẹn bảo dưỡng, sửa chữa cho khách hàng và phân bổ khoang dự kiến."
      placement="center"
    >
      <BookingForm onSaved={onClose} onRequestClose={registerCloseHandler} />
    </Sheet>
  );
}

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import type { Booking, Vehicle } from '../../../shared/types/contracts';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { useCheckInVehicleMutation } from '../hooks/useReception';
import { checkInFormSchema, type CheckInFormValues } from '../schemas/receptionSchema';
import { LogIn, Car } from 'lucide-react';

export function CheckInModal({
  booking,
  vehicle,
  open,
  onClose,
  onCheckInSuccess,
}: {
  booking?: Booking | null;
  vehicle?: Vehicle | null;
  open: boolean;
  onClose: () => void;
  onCheckInSuccess?: (orderCode: string, repairOrderId: string) => void;
}) {
  const notify = useToast();
  const checkIn = useCheckInVehicleMutation();
  const { data: vehicles = [], isLoading: isLoadingVehicles } = useVehiclesQuery({
    isActive: true,
  });

  const activeVehicle =
    vehicle || (booking ? vehicles.find((v) => v.id === booking.vehicleId) : null);

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<CheckInFormValues>({
    resolver: zodResolver(checkInFormSchema),
    defaultValues: {
      bookingId: booking?.id ?? '',
      vehicleId: booking?.vehicleId ?? vehicle?.id ?? '',
      odometerIn: activeVehicle?.odometer ?? 0,
      fuelLevelPercent: 50,
      customerComplaint: booking?.requestDescription ?? '',
    },
  });

  async function onSubmit(values: CheckInFormValues) {
    if (activeVehicle && values.odometerIn < activeVehicle.odometer) {
      setError('odometerIn', {
        message: `Số ODO vào xưởng không được nhỏ hơn ODO ghi nhận trước (${activeVehicle.odometer.toLocaleString('vi-VN')} km)`,
      });
      return;
    }

    try {
      const result = await checkIn.mutateAsync({
        bookingId: values.bookingId || null,
        vehicleId: values.vehicleId,
        odometerIn: Number(values.odometerIn),
        fuelLevelPercent: Number.isFinite(values.fuelLevelPercent)
          ? Number(values.fuelLevelPercent)
          : null,
        customerComplaint: values.customerComplaint?.trim() || null,
      });

      notify(`Tiếp nhận xe thành công! Mã lệnh sửa chữa: #${result.orderCode}`);
      reset();
      onClose();
      onCheckInSuccess?.(result.orderCode, result.repairOrderId);
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const title = booking
    ? `Tiếp nhận xe hẹn #${booking.bookingCode}`
    : 'Tiếp nhận xe vào xưởng (Check-in)';
  const description = booking
    ? `Khách hàng: ${booking.customerName} • Xe: ${booking.licensePlate}`
    : 'Ghi nhận hiện trạng xe, số ODO và khởi tạo lệnh sửa chữa mới.';

  const generalError = checkIn.error;

  return (
    <Sheet open={open} onClose={onClose} placement="center" title={title} description={description}>
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        {activeVehicle && (
          <div className="info-strip">
            <Car size={18} />
            <span>
              <strong>{activeVehicle.licensePlate}</strong> ({activeVehicle.brand} {activeVehicle.model}) &bull; Chủ xe: {activeVehicle.customerName} ({activeVehicle.customerPhone}) &bull; ODO trước: {activeVehicle.odometer.toLocaleString('vi-VN')} km
            </span>
          </div>
        )}

        {!booking && !vehicle && (
          <div className="full-width-field">
            <Field
              label="Phương tiện tiếp nhận"
              required
              error={errors.vehicleId?.message}
              hint="Chọn xe cần tiếp nhận vào gara"
            >
              <select
                className="select"
                disabled={isLoadingVehicles || checkIn.isPending}
                {...register('vehicleId')}
              >
                <option value="">-- Chọn phương tiện --</option>
                {vehicles.map((v) => (
                  <option key={v.id} value={v.id}>
                    {v.licensePlate} ({v.brand} {v.model}) - Chủ xe: {v.customerName}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        )}

        <div className="form-section-label">THÔNG SỐ TIẾP NHẬN VÀO XƯỞNG</div>

        <Field
          label="Số ODO vào xưởng (km)"
          required
          error={errors.odometerIn?.message}
          hint={
            activeVehicle
              ? `Tối thiểu ${activeVehicle.odometer.toLocaleString('vi-VN')} km`
              : 'Ghi nhận số km trên đồng hồ táp-lô'
          }
        >
          <input
            type="number"
            className="input"
            min={activeVehicle ? activeVehicle.odometer : 0}
            disabled={checkIn.isPending}
            {...register('odometerIn')}
          />
        </Field>

        <Field
          label="Mức nhiên liệu hiện tại (%)"
          error={errors.fuelLevelPercent?.message}
          hint="Ước tính từ vạch kim xăng (0–100%)"
        >
          <input
            type="number"
            className="input"
            min={0}
            max={100}
            placeholder="50"
            disabled={checkIn.isPending}
            {...register('fuelLevelPercent', { valueAsNumber: true })}
          />
        </Field>

        <div className="full-width-field">
          <Field
            label="Hiện tượng / Yêu cầu sửa chữa của khách hàng"
            error={errors.customerComplaint?.message}
            hint="Ghi chép chính xác mô tả triệu chứng của khách hàng để kỹ thuật viên chẩn đoán"
          >
            <textarea
              className="input min-h-[85px] resize-none"
              placeholder="Khách báo xe rung giật ở dải tốc độ 60km/h, điều hòa mát kém, có tiếng rít phanh..."
              disabled={checkIn.isPending}
              {...register('customerComplaint')}
            />
          </Field>
        </div>

        <div className="form-actions account-editor-actions">
          <Button disabled={checkIn.isPending} onClick={onClose}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={checkIn.isPending}>
            <LogIn size={16} />
            {checkIn.isPending ? 'Đang tiếp nhận…' : 'Tiếp nhận xe & Mở lệnh'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

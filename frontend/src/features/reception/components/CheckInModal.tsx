import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import {
  Button,
  Field,
  FormError,
  Sheet,
} from '../../../shared/components/ui';
import type { Booking, Vehicle } from '../../../shared/types/contracts';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { useCheckInVehicleMutation } from '../hooks/useReception';
import {
  checkInFormSchema,
  type CheckInFormValues,
} from '../schemas/receptionSchema';
import { LogIn, Gauge, Fuel } from 'lucide-react';

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
  const { data: vehicles = [], isLoading: isLoadingVehicles } = useVehiclesQuery({ isActive: true });

  const activeVehicle = vehicle || (booking ? vehicles.find((v) => v.id === booking.vehicleId) : null);

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
        fuelLevelPercent: values.fuelLevelPercent ? Number(values.fuelLevelPercent) : null,
        customerComplaint: values.customerComplaint?.trim() || null,
      });

      notify.success(`Tiếp nhận xe thành công! Mã lệnh sửa chữa: #${result.orderCode}`);
      reset();
      onClose();
      onCheckInSuccess?.(result.orderCode, result.repairOrderId);
    } catch (err: unknown) {
      const field = applyFormError(err, setError);
      if (!field) {
        notify.error(errorMessage(err) ?? 'Tiếp nhận xe vào xưởng thất bại');
      }
    }
  }

  const title = booking
    ? `Tiếp nhận xe hẹn #${booking.bookingCode}`
    : 'Tiếp nhận xe vào xưởng (Check-in)';
  const description = booking
    ? `Khách hàng: ${booking.customerName} • Xe: ${booking.licensePlate}`
    : 'Ghi nhận hiện trạng xe, số ODO và khởi tạo lệnh sửa chữa mới.';

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      title={title}
      description={description}
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="editor-form account-editor-form">
        <FormError message={errors.root?.message} />

        {!booking && !vehicle && (
          <Field
            label="Phương tiện tiếp nhận *"
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
        )}

        {activeVehicle && (
          <div className="p-3 bg-muted/60 border border-border rounded-md text-xs space-y-1">
            <div className="flex justify-between font-medium">
              <span>Phương tiện: {activeVehicle.licensePlate} ({activeVehicle.brand} {activeVehicle.model})</span>
              <span>ODO gần nhất: {activeVehicle.odometer.toLocaleString('vi-VN')} km</span>
            </div>
            <div className="text-muted-foreground">
              Chủ sở hữu: {activeVehicle.customerName} &bull; SĐT: {activeVehicle.customerPhone}
            </div>
          </div>
        )}

        <div className="form-row-2">
          <Field
            label="Số ODO vào xưởng (km) *"
            error={errors.odometerIn?.message}
            hint={activeVehicle ? `Tối thiểu ${activeVehicle.odometer.toLocaleString('vi-VN')} km` : undefined}
          >
            <div className="relative">
              <input
                type="number"
                className="input"
                min={activeVehicle ? activeVehicle.odometer : 0}
                disabled={checkIn.isPending}
                {...register('odometerIn')}
              />
            </div>
          </Field>

          <Field
            label="Mức nhiên liệu hiện tại (%)"
            error={errors.fuelLevelPercent?.message}
            hint="Nhập từ 0 đến 100%"
          >
            <input
              type="number"
              className="input"
              min={0}
              max={100}
              placeholder="50"
              disabled={checkIn.isPending}
              {...register('fuelLevelPercent')}
            />
          </Field>
        </div>

        <Field
          label="Hiện tượng / Yêu cầu sửa chữa của khách hàng"
          error={errors.customerComplaint?.message}
          hint="Ghi chép chính xác mô tả triệu chứng của khách hàng để kỹ thuật viên chẩn đoán"
        >
          <textarea
            className="input min-h-[90px] resize-none"
            placeholder="Khách báo xe rung giật ở dải tốc độ 60km/h, điều hòa mát kém, có tiếng rít phanh..."
            disabled={checkIn.isPending}
            {...register('customerComplaint')}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button
            type="button"
            variant="ghost"
            disabled={checkIn.isPending}
            onClick={onClose}
          >
            Hủy
          </Button>
          <Button
            type="submit"
            variant="primary"
            disabled={checkIn.isPending}
          >
            <LogIn size={16} className="mr-1" />
            {checkIn.isPending ? 'Đang tiếp nhận...' : 'Xác nhận vào xưởng & Mở lệnh'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

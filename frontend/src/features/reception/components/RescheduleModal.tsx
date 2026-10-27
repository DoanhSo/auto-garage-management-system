import { dateTimeInput } from '../../../shared/utils/dates';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import type { Booking } from '../../../shared/types/contracts';
import { useRescheduleBookingMutation } from '../hooks/useReception';
import { rescheduleFormSchema, type RescheduleFormValues } from '../schemas/receptionSchema';
import { CalendarClock } from 'lucide-react';

export function RescheduleModal({
  booking,
  open,
  onClose,
}: {
  booking: Booking | null;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const reschedule = useRescheduleBookingMutation();

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<RescheduleFormValues>({
    resolver: zodResolver(rescheduleFormSchema),
    defaultValues: {
      newAppointmentAt: booking?.appointmentAt ? dateTimeInput(booking.appointmentAt) : '',
    },
  });

  if (!booking) return null;

  async function onSubmit(values: RescheduleFormValues) {
    if (!booking) return;
    try {
      const date = new Date(values.newAppointmentAt);
      if (isNaN(date.getTime())) {
        setError('newAppointmentAt', { message: 'Thời gian hẹn mới không hợp lệ' });
        return;
      }

      await reschedule.mutateAsync({
        id: booking.id,
        values: {
          newAppointmentAt: date.toISOString(),
          newBayId: values.newBayId || null,
        },
      });

      notify(`Đã dời lịch hẹn #${booking.bookingCode} thành công`);
      reset();
      onClose();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const generalError = reschedule.error;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      title={`Dời lịch hẹn #${booking.bookingCode}`}
      description={`Khách hàng: ${booking.customerName} • Xe: ${booking.licensePlate}`}
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">THỜI GIAN TIẾP NHẬN MỚI</div>

        <div className="full-width-field">
          <Field
            label="Thời gian hẹn mới"
            required
            error={errors.newAppointmentAt?.message}
            hint="Chọn lại ngày và giờ dự kiến tiếp nhận xe vào xưởng"
          >
            <input
              type="datetime-local"
              className="input"
              disabled={reschedule.isPending}
              {...register('newAppointmentAt')}
            />
          </Field>
        </div>

        <div className="form-actions account-editor-actions">
          <Button disabled={reschedule.isPending} onClick={onClose}>
            Hủy
          </Button>
          <Button type="submit" variant="primary" pending={reschedule.isPending}>
            <CalendarClock size={16} />
            {reschedule.isPending ? 'Đang cập nhật…' : 'Xác nhận dời lịch'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

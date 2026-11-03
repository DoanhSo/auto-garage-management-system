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
import { BookingTimeSlotPicker } from './BookingTimeSlotPicker';
import { PICKER_TIME_SLOTS, formatAppointmentDisplay, getInitialAppointmentSlot, isSlotInPast, todayKey } from '../constants/bookingSlots';

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
    handleSubmit,
    setError,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<RescheduleFormValues>({
    resolver: zodResolver(rescheduleFormSchema),
    defaultValues: {
      newAppointmentAt: getInitialAppointmentSlot(booking?.appointmentAt ? dateTimeInput(booking.appointmentAt) : undefined),
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
      if (date.getTime() < Date.now()) {
        setError('newAppointmentAt', { message: 'Thời gian hẹn mới không được ở trong quá khứ' });
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

  const currentNewAppointmentAt = watch('newAppointmentAt');
  const selectedRescheduleDate = currentNewAppointmentAt ? currentNewAppointmentAt.slice(0, 10) : todayKey();
  const selectedRescheduleTime =
    currentNewAppointmentAt && currentNewAppointmentAt.includes('T') ? currentNewAppointmentAt.slice(11, 16) : '';

  const generalError = reschedule.error;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="lg"
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

        <div className="modal-split-columns">
          <div className="modal-column-stack">
            <Field
              label="Ngày hẹn mới"
              required
              error={errors.newAppointmentAt?.message}
              hint="Chọn ngày đưa xe vào gara"
            >
              <input
                type="date"
                className="input"
                min={todayKey()}
                disabled={reschedule.isPending}
                value={selectedRescheduleDate}
                onChange={(e) => {
                  const newDate = e.target.value;
                  if (!newDate) return;
                  const time = selectedRescheduleTime || '08:30';
                  if (isSlotInPast(newDate, time)) {
                    const validSlot = PICKER_TIME_SLOTS.find((s) => !isSlotInPast(newDate, s));
                    setValue(
                      'newAppointmentAt',
                      validSlot ? `${newDate}T${validSlot}` : `${newDate}T08:30`,
                      { shouldDirty: true, shouldValidate: true },
                    );
                  } else {
                    setValue('newAppointmentAt', `${newDate}T${time}`, {
                      shouldDirty: true,
                      shouldValidate: true,
                    });
                  }
                }}
              />
            </Field>

            <div className="appointment-selected-card" role="status">
              <CalendarClock size={16} />
              <span>
                Khung giờ đã chọn:{' '}
                <strong>
                  {selectedRescheduleTime && !isSlotInPast(selectedRescheduleDate, selectedRescheduleTime)
                    ? formatAppointmentDisplay(currentNewAppointmentAt)
                    : 'Vui lòng chọn khung giờ bên phải'}
                </strong>
              </span>
            </div>

            <div className="info-strip">
              <CalendarClock size={18} />
              <span>
                <strong>Khách hàng:</strong> {booking.customerName}<br />
                <strong>Phương tiện:</strong> {booking.licensePlate}
              </span>
            </div>
          </div>

          <div>
            <BookingTimeSlotPicker
              value={watch('newAppointmentAt')}
              excludeBookingId={booking.id}
              disabled={reschedule.isPending}
              onChange={(next) =>
                setValue('newAppointmentAt', next, { shouldDirty: true, shouldValidate: true })
              }
            />
          </div>
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

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import type { Booking } from '../../../shared/types/contracts';
import { useCancelBookingMutation } from '../hooks/useReception';
import { cancelFormSchema, type CancelFormValues } from '../schemas/receptionSchema';
import { CalendarX } from 'lucide-react';

export function CancelBookingDialog({
  booking,
  open,
  onClose,
}: {
  booking: Booking | null;
  open: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const cancel = useCancelBookingMutation();

  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm<CancelFormValues>({
    resolver: zodResolver(cancelFormSchema),
    defaultValues: {
      cancellationReason: '',
    },
  });

  if (!booking) return null;

  async function onSubmit(values: CancelFormValues) {
    if (!booking) return;
    try {
      await cancel.mutateAsync({
        id: booking.id,
        values: {
          cancellationReason: values.cancellationReason.trim(),
        },
      });

      notify(`Đã hủy lịch hẹn #${booking.bookingCode}`);
      reset();
      onClose();
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const generalError = cancel.error;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="md"
      title={`Hủy lịch hẹn #${booking.bookingCode}`}
      description={`Khách hàng: ${booking.customerName} • Xe: ${booking.licensePlate}`}
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        <div className="form-section-label">LÝ DO HỦY LỊCH HẸN</div>

        <div className="full-width-field">
          <Field
            label="Lý do hủy lịch hẹn"
            required
            error={errors.cancellationReason?.message}
            hint="Ghi nhận lý do hủy (Khách bận, đã sửa nơi khác, muốn dời lịch dài hạn...)"
          >
            <textarea
              className="input min-h-[85px] resize-none"
              placeholder="Nhập lý do khách hàng hủy hẹn..."
              disabled={cancel.isPending}
              {...register('cancellationReason')}
            />
          </Field>
        </div>

        <div className="form-actions account-editor-actions">
          <Button disabled={cancel.isPending} onClick={onClose}>
            Đóng
          </Button>
          <Button
            type="submit"
            variant="danger"
            pending={cancel.isPending}
          >
            <CalendarX size={16} />
            {cancel.isPending ? 'Đang hủy…' : 'Xác nhận hủy hẹn'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

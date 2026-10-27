import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError } from '../../../shared/api/errors';
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

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      title={`Hủy lịch hẹn #${booking.bookingCode}`}
      description={`Khách hàng: ${booking.customerName} • Xe: ${booking.licensePlate}`}
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form"
      >
        <FormError message={errors.root?.server?.message} />

        <Field
          label="Lý do hủy lịch hẹn *"
          error={errors.cancellationReason?.message}
          hint="Ghi nhận lý do hủy (Khách bận, xe đã sửa nơi khác, hoãn lịch...)"
        >
          <textarea
            className="input min-h-[85px] resize-none"
            placeholder="Nhập lý do khách hàng hủy hẹn..."
            disabled={cancel.isPending}
            {...register('cancellationReason')}
          />
        </Field>

        <div className="form-actions account-editor-actions">
          <Button type="button" variant="ghost" disabled={cancel.isPending} onClick={onClose}>
            Đóng
          </Button>
          <Button
            type="submit"
            variant="primary"
            className="bg-red-600 hover:bg-red-700"
            disabled={cancel.isPending}
          >
            <CalendarX size={16} className="mr-1" />
            {cancel.isPending ? 'Đang hủy...' : 'Xác nhận hủy hẹn'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

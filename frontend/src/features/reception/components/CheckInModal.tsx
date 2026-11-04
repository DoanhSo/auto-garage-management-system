import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useToast } from '../../../app/providers/ToastProvider';
import { applyFormError, errorMessage } from '../../../shared/api/errors';
import { Button, Combobox, Field, FormError, Sheet } from '../../../shared/components/ui';
import type { Booking, Vehicle } from '../../../shared/types/contracts';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import {
  useActiveReservationsQuery,
  useBayDayRosterQuery,
  useOnShiftTechniciansQuery,
  useServiceBaysQuery,
} from '../../scheduling/hooks/useScheduling';
import {
  RECEPTION_BAY_HOLD_HOURS,
  SKILL_LABELS,
  availabilityLabels,
  shiftTypeLabels,
} from '../../scheduling/types/scheduling';
import { useState } from 'react';
import { useCheckInVehicleMutation, useUploadOrderAttachmentMutation } from '../hooks/useReception';
import { AttachmentFilePicker } from './AttachmentFilePicker';
import { checkInFormSchema, type CheckInFormValues } from '../schemas/receptionSchema';
import { LogIn, Car } from 'lucide-react';

const hhmm = (iso: string) =>
  new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

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
  const uploadPhoto = useUploadOrderAttachmentMutation();
  const [photos, setPhotos] = useState<File[]>([]);
  const { data: vehicles = [], isLoading: isLoadingVehicles } = useVehiclesQuery({
    isActive: true,
  });

  const bays = useServiceBaysQuery({ isActive: true, page: 0, size: 100 });
  const reservations = useActiveReservationsQuery();
  // Khoang được giữ cả khung giờ sắp tới nên chỉ coi là trống khi không có lịch nào chồng lên khung đó.
  const occupiedBayIds = new Set(
    (reservations.data ?? [])
      .filter((r) => {
        const from = Date.now();
        const to = from + RECEPTION_BAY_HOLD_HOURS * 3_600_000;
        return new Date(r.startsAt).getTime() < to && new Date(r.endsAt).getTime() > from;
      })
      .map((r) => String(r.bayId)),
  );

  const activeVehicle =
    vehicle || (booking ? vehicles.find((v) => v.id === booking.vehicleId) : null);

  const {
    register,
    watch,
    setValue,
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
      diagnosticTechnicianId: '',
      serviceBayId: '',
    },
  });

  const selectedBayId = watch('serviceBayId') || null;
  const today = (() => {
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
  })();
  const roster = useBayDayRosterQuery(selectedBayId, today);
  const onShift = useOnShiftTechniciansQuery(open);
  const bayCrew = (roster.data ?? []).filter((tech) => tech.onShiftNow);
  const mobileTechs = (onShift.data ?? []).filter((tech) => !tech.serviceBayId);
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
        diagnosticTechnicianId: values.diagnosticTechnicianId || null,
        serviceBayId: values.serviceBayId || null,
      });

      // Lệnh đã mở; ảnh lỗi thì báo riêng, không hủy kết quả tiếp nhận.
      let failed = 0;
      for (const file of photos) {
        try {
          await uploadPhoto.mutateAsync({
            repairOrderId: result.repairOrderId,
            category: 'CHECK_IN',
            file,
          });
        } catch {
          failed += 1;
        }
      }
      notify(
        failed > 0
          ? `Đã tiếp nhận xe (lệnh #${result.orderCode}) nhưng ${failed}/${photos.length} ảnh tải lên thất bại.`
          : `Tiếp nhận xe thành công! Mã lệnh sửa chữa: #${result.orderCode}`,
      );
      reset();
      setPhotos([]);
      onClose();
      onCheckInSuccess?.(result.orderCode, result.repairOrderId);
    } catch (err: unknown) {
      applyFormError(err, setError, Object.keys(values));
    }
  }

  const title = booking ? `Tiếp nhận xe hẹn #${booking.bookingCode}` : 'Tiếp nhận xe vào xưởng';
  const description = booking
    ? `Khách hàng: ${booking.customerName} • Xe: ${booking.licensePlate}`
    : 'Ghi nhận hiện trạng xe, số ODO và khởi tạo lệnh sửa chữa mới.';

  const generalError = checkIn.error;
  const busy = checkIn.isPending || uploadPhoto.isPending;

  return (
    <Sheet
      open={open}
      onClose={onClose}
      placement="center"
      size="lg"
      title={title}
      description={description}
    >
      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="editor-form account-editor-form grid-4col"
      >
        <FormError message={generalError ? errorMessage(generalError) : undefined} />

        {activeVehicle && (
          <div className="info-strip">
            <Car size={18} />
            <span>
              <strong>{activeVehicle.licensePlate}</strong> ({activeVehicle.brand}{' '}
              {activeVehicle.model}) &bull; Chủ xe: {activeVehicle.customerName} (
              {activeVehicle.customerPhone}) &bull; ODO trước:{' '}
              {activeVehicle.odometer.toLocaleString('vi-VN')} km
            </span>
          </div>
        )}

        <div className="form-section-label">THÔNG SỐ & ĐIỀU PHỐI</div>

        {!booking && !vehicle && (
          <div className="col-span-2">
            <Field label="Phương tiện tiếp nhận" required error={errors.vehicleId?.message}>
              <Combobox
                disabled={isLoadingVehicles || checkIn.isPending}
                placeholder="Gõ biển số hoặc tên chủ xe để tìm…"
                value={watch('vehicleId') ?? ''}
                onChange={(next) => setValue('vehicleId', next, { shouldValidate: true })}
                options={vehicles.map((v) => ({
                  value: String(v.id),
                  label: `${v.licensePlate} (${v.brand} ${v.model})`,
                  description: `Chủ xe: ${v.customerName}`,
                }))}
              />
            </Field>
          </div>
        )}

        <Field
          label="Số ODO vào xưởng (km)"
          required
          error={errors.odometerIn?.message}
          hint={
            activeVehicle
              ? `Tối thiểu ${activeVehicle.odometer.toLocaleString('vi-VN')} km`
              : 'Số km trên đồng hồ táp-lô'
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
          label="Mức nhiên liệu (%)"
          error={errors.fuelLevelPercent?.message}
          hint="Ước tính theo kim xăng, 0–100"
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

        <div className="col-span-2">
          <Field
            label="Khoang tiếp nhận"
            hint={`Khoang có lịch trong ${RECEPTION_BAY_HOLD_HOURS} giờ tới được đánh dấu bận`}
          >
            <Combobox
              disabled={busy}
              placeholder="Gõ mã khoang để tìm…"
              value={watch('serviceBayId') ?? ''}
              onChange={(next) => {
                setValue('serviceBayId', next);
                setValue('diagnosticTechnicianId', '');
              }}
              options={(bays.data?.content ?? []).map((bay) => ({
                value: String(bay.id),
                label: `${bay.bayCode} · ${bay.bayName}`,
                description: occupiedBayIds.has(String(bay.id)) ? 'Đang bận' : undefined,
                disabled: occupiedBayIds.has(String(bay.id)),
              }))}
            />
          </Field>
        </div>

        <div className="col-span-2">
          <Field
            label="Kỹ thuật viên chẩn đoán"
            hint={
              !selectedBayId
                ? 'Chọn khoang để xem thợ trực; thợ cơ động luôn điều động được'
                : roster.isLoading
                  ? 'Đang tải thợ trực khoang…'
                  : bayCrew.length === 0
                    ? 'Khoang chưa có thợ trực ca này; có thể điều động thợ cơ động'
                    : 'Chọn thợ trực khoang hoặc thợ cơ động đang trong ca'
            }
          >
            <Combobox
              disabled={busy}
              placeholder="Gõ tên thợ để tìm…"
              value={watch('diagnosticTechnicianId') ?? ''}
              onChange={(next) => setValue('diagnosticTechnicianId', next)}
              options={[
                ...(selectedBayId ? (roster.data ?? []) : []).map((tech) => ({
                  value: String(tech.employeeId),
                  label: tech.fullName,
                  group: 'Thợ trực tại khoang này',
                  disabled: !tech.onShiftNow,
                  description: [
                    `${tech.shiftType ? `${shiftTypeLabels[tech.shiftType]} ` : 'Ca '}${hhmm(tech.shiftStartsAt)}–${hhmm(tech.shiftEndsAt)}`,
                    tech.skills.map((skill) => SKILL_LABELS[skill] ?? skill).join(', '),
                    tech.onShiftNow ? '' : 'ngoài giờ ca',
                  ]
                    .filter(Boolean)
                    .join(' · '),
                })),
                ...mobileTechs.map((tech) => ({
                  value: String(tech.employeeId),
                  label: tech.fullName,
                  group: 'Thợ đang đi làm (chưa phân khoang - sẵn sàng điều động)',
                  description: [
                    availabilityLabels[tech.availabilityStatus],
                    `${tech.activeOrdersCount} xe đang làm`,
                    tech.skills.map((skill) => SKILL_LABELS[skill] ?? skill).join(', '),
                  ]
                    .filter(Boolean)
                    .join(' · '),
                })),
              ]}
            />
          </Field>
        </div>

        <div className="form-section-label">TÌNH TRẠNG XE KHI NHẬN</div>

        <div className="col-span-2">
          <Field label="Hiện tượng / Yêu cầu của khách" error={errors.customerComplaint?.message}>
            <textarea
              className="input min-h-[85px] resize-none"
              placeholder="Khách báo xe rung giật ở dải tốc độ 60km/h, điều hòa mát kém, có tiếng rít phanh..."
              disabled={checkIn.isPending}
              {...register('customerComplaint')}
            />
          </Field>
        </div>

        <div className="col-span-2">
          <Field
            label="Ảnh hiện trạng"
            hint="Ngoại thất, ODO, kim xăng, đèn táp-lô; ảnh được gắn vào lệnh sau khi tiếp nhận."
          >
            <AttachmentFilePicker files={photos} onChange={setPhotos} disabled={busy} />
          </Field>
        </div>

        <div className="form-actions account-editor-actions">
          <Button disabled={busy} onClick={onClose}>
            Hủy
          </Button>
          <Button variant="primary" type="submit" pending={busy}>
            <LogIn size={16} />
            {uploadPhoto.isPending
              ? 'Đang tải ảnh…'
              : checkIn.isPending
                ? 'Đang tiếp nhận…'
                : 'Tiếp nhận xe & Mở lệnh'}
          </Button>
        </div>
      </form>
    </Sheet>
  );
}

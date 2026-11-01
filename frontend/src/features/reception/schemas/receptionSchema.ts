import { dateTimeInput } from '../../../shared/utils/dates';
import { z } from 'zod';
import type { Booking, BookingCreate } from '../../../shared/types/contracts';

export const MIN_MANUFACTURE_YEAR = 1900;

export function normalizePhone(value: string | undefined | null): string {
  return (value ?? '').replace(/[^0-9+]/g, '');
}

export function normalizePlate(value: string | undefined | null): string {
  return (value ?? '').replace(/[\s.-]/g, '').toUpperCase();
}

const optionalText = (max: number, message: string) => z.string().trim().max(max, message).optional();

export const bookingFormSchema = z
  .object({
    customerMode: z.enum(['existing', 'new']),
    customerId: z.string().optional(),
    newCustomer: z.object({
      fullName: z.string().trim().max(150, 'Họ tên tối đa 150 ký tự'),
      phone: z.string().trim().max(30, 'Số điện thoại tối đa 30 ký tự'),
      email: optionalText(254, 'Email tối đa 254 ký tự'),
    }),
    vehicleMode: z.enum(['existing', 'new']),
    vehicleId: z.string().optional(),
    newVehicle: z.object({
      licensePlate: z.string().trim().max(20, 'Biển số tối đa 20 ký tự'),
      brand: z.string().trim().max(80, 'Hãng xe tối đa 80 ký tự'),
      model: z.string().trim().max(80, 'Dòng xe tối đa 80 ký tự'),
      manufactureYear: z.string().trim().optional(),
      color: optionalText(50, 'Màu xe tối đa 50 ký tự'),
    }),
    serviceId: z.string().optional(),
    bayId: z.string().optional(),
    appointmentAt: z.string(),
    requestDescription: z.string().trim().max(500, 'Mô tả yêu cầu tối đa 500 ký tự').optional(),
  })
  .superRefine((values, ctx) => {
    const required = (path: (string | number)[], message: string) =>
      ctx.addIssue({ code: 'custom', path, message });

    if (values.customerMode === 'existing') {
      if (!values.customerId) required(['customerId'], 'Khách hàng là bắt buộc');
    } else {
      const { fullName, phone, email } = values.newCustomer;
      if (!fullName.trim()) required(['newCustomer', 'fullName'], 'Họ tên khách hàng là bắt buộc');
      if (!phone.trim()) required(['newCustomer', 'phone'], 'Số điện thoại là bắt buộc');
      else if (!/^\+?[0-9]+$/.test(normalizePhone(phone)))
        required(['newCustomer', 'phone'], 'Số điện thoại không hợp lệ');
      if (email?.trim() && !z.email().safeParse(email.trim()).success)
        required(['newCustomer', 'email'], 'Email không hợp lệ');
      if (values.vehicleMode !== 'new')
        required(['vehicleMode'], 'Khách hàng mới cần nhập thông tin xe mới');
    }

    if (values.vehicleMode === 'existing') {
      if (!values.vehicleId) required(['vehicleId'], 'Phương tiện là bắt buộc');
    } else {
      const { licensePlate, brand, model, manufactureYear } = values.newVehicle;
      if (!licensePlate.trim()) required(['newVehicle', 'licensePlate'], 'Biển số là bắt buộc');
      if (!brand.trim()) required(['newVehicle', 'brand'], 'Hãng xe là bắt buộc');
      if (!model.trim()) required(['newVehicle', 'model'], 'Dòng xe là bắt buộc');
      if (manufactureYear) {
        const year = Number(manufactureYear);
        const maxYear = new Date().getFullYear() + 1;
        if (!Number.isInteger(year) || year < MIN_MANUFACTURE_YEAR || year > maxYear)
          required(
            ['newVehicle', 'manufactureYear'],
            `Năm sản xuất phải từ ${MIN_MANUFACTURE_YEAR} đến ${maxYear}`,
          );
      }
    }

    if (!values.appointmentAt) required(['appointmentAt'], 'Thời gian hẹn là bắt buộc');
  });

export type BookingFormValues = z.infer<typeof bookingFormSchema>;

/** Fields validated before leaving step 1 of the booking dialog. */
export const bookingStepOneFields = [
  'customerMode',
  'customerId',
  'newCustomer.fullName',
  'newCustomer.phone',
  'newCustomer.email',
  'vehicleMode',
  'vehicleId',
  'newVehicle.licensePlate',
  'newVehicle.brand',
  'newVehicle.model',
  'newVehicle.manufactureYear',
  'newVehicle.color',
] as const;

const blankToNull = (value: string | undefined) => value?.trim() || null;

export function bookingPayload(values: BookingFormValues, appointmentAtIso: string): BookingCreate {
  const isNewCustomer = values.customerMode === 'new';
  const isNewVehicle = isNewCustomer || values.vehicleMode === 'new';
  return {
    customerId: isNewCustomer ? null : values.customerId || null,
    newCustomer: isNewCustomer
      ? {
          fullName: values.newCustomer.fullName.trim(),
          phone: normalizePhone(values.newCustomer.phone),
          email: blankToNull(values.newCustomer.email),
        }
      : null,
    vehicleId: isNewVehicle ? null : values.vehicleId || null,
    newVehicle: isNewVehicle
      ? {
          licensePlate: values.newVehicle.licensePlate.trim(),
          brand: values.newVehicle.brand.trim(),
          model: values.newVehicle.model.trim(),
          manufactureYear: values.newVehicle.manufactureYear
            ? Number(values.newVehicle.manufactureYear)
            : null,
          color: blankToNull(values.newVehicle.color),
        }
      : null,
    serviceId: values.serviceId || null,
    bayId: values.bayId || null,
    appointmentAt: appointmentAtIso,
    requestDescription: blankToNull(values.requestDescription),
  };
}

export const rescheduleFormSchema = z.object({
  newAppointmentAt: z.string().min(1, 'Thời gian hẹn mới là bắt buộc'),
  newBayId: z.string().optional(),
});

export type RescheduleFormValues = z.infer<typeof rescheduleFormSchema>;

export const cancelFormSchema = z.object({
  cancellationReason: z
    .string()
    .trim()
    .min(3, 'Lý do hủy tối thiểu 3 ký tự')
    .max(500, 'Lý do hủy tối đa 500 ký tự'),
});

export type CancelFormValues = z.infer<typeof cancelFormSchema>;

export const checkInFormSchema = z.object({
  bookingId: z.string().optional(),
  vehicleId: z.string().min(1, 'Phương tiện tiếp nhận là bắt buộc'),
  odometerIn: z.coerce
    .number<number>({ error: 'Số ODO phải là số' })
    .int('Số ODO phải là số nguyên')
    .min(0, 'Số ODO không được âm'),
  fuelLevelPercent: z.coerce
    .number<number>({ error: 'Mức nhiên liệu phải là số' })
    .min(0, 'Mức nhiên liệu tối thiểu 0%')
    .max(100, 'Mức nhiên liệu tối đa 100%')
    .optional()
    .or(z.nan()),
  customerComplaint: z.string().trim().max(500, 'Yêu cầu kiểm tra tối đa 500 ký tự').optional(),
});

export type CheckInFormValues = z.infer<typeof checkInFormSchema>;

export const vehicleCheckFormSchema = z.object({
  repairOrderId: z.string().min(1, 'Mã lệnh sửa chữa là bắt buộc'),
  checkType: z.enum(['CHECK_IN', 'QC', 'HANDOVER']),
  odometer: z.coerce.number<number>().int().min(0).optional(),
  notes: z.string().trim().max(1000).optional(),
  exteriorCondition: z.string().trim().optional(),
  interiorCondition: z.string().trim().optional(),
  warningLights: z.string().trim().optional(),
});

export type VehicleCheckFormValues = z.infer<typeof vehicleCheckFormSchema>;

export function bookingDefaults(existing?: Booking): BookingFormValues {
  return {
    customerMode: 'existing',
    customerId: existing?.customerId ?? '',
    newCustomer: { fullName: '', phone: '', email: '' },
    vehicleMode: 'existing',
    vehicleId: existing?.vehicleId ?? '',
    newVehicle: { licensePlate: '', brand: '', model: '', manufactureYear: '', color: '' },
    serviceId: existing?.serviceId ?? '',
    bayId: existing?.bayId ?? '',
    appointmentAt: existing?.appointmentAt ? dateTimeInput(existing.appointmentAt) : '',
    requestDescription: existing?.requestDescription ?? '',
  };
}

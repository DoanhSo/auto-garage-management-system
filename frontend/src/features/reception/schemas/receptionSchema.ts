import { z } from 'zod';
import type { Booking } from '../../../shared/types/contracts';

export const bookingFormSchema = z.object({
  customerId: z.string().min(1, 'Khách hàng là bắt buộc'),
  vehicleId: z.string().min(1, 'Phương tiện là bắt buộc'),
  serviceId: z.string().optional(),
  bayId: z.string().optional(),
  appointmentAt: z.string().min(1, 'Thời gian hẹn là bắt buộc'),
  requestDescription: z.string().trim().max(500, 'Mô tả yêu cầu tối đa 500 ký tự').optional(),
});

export type BookingFormValues = z.infer<typeof bookingFormSchema>;

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
    .number({ invalid_type_error: 'Số ODO phải là số' })
    .int('Số ODO phải là số nguyên')
    .min(0, 'Số ODO không được âm'),
  fuelLevelPercent: z.coerce
    .number({ invalid_type_error: 'Mức nhiên liệu phải là số' })
    .min(0, 'Mức nhiên liệu tối thiểu 0%')
    .max(100, 'Mức nhiên liệu tối đa 100%')
    .optional()
    .or(z.nan()),
  customerComplaint: z
    .string()
    .trim()
    .max(500, 'Yêu cầu kiểm tra tối đa 500 ký tự')
    .optional(),
});

export type CheckInFormValues = z.infer<typeof checkInFormSchema>;

export const vehicleCheckFormSchema = z.object({
  repairOrderId: z.string().min(1, 'Mã lệnh sửa chữa là bắt buộc'),
  checkType: z.enum(['CHECK_IN', 'QC', 'HANDOVER']),
  odometer: z.coerce.number().int().min(0).optional(),
  notes: z.string().trim().max(1000).optional(),
  exteriorCondition: z.string().trim().optional(),
  interiorCondition: z.string().trim().optional(),
  warningLights: z.string().trim().optional(),
});

export type VehicleCheckFormValues = z.infer<typeof vehicleCheckFormSchema>;

export function bookingDefaults(existing?: Booking): BookingFormValues {
  return {
    customerId: existing?.customerId ?? '',
    vehicleId: existing?.vehicleId ?? '',
    serviceId: existing?.serviceId ?? '',
    bayId: existing?.bayId ?? '',
    appointmentAt: existing?.appointmentAt ? existing.appointmentAt.slice(0, 16) : '',
    requestDescription: existing?.requestDescription ?? '',
  };
}

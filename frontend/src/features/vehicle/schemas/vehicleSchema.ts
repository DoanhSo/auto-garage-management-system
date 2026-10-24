import { z } from 'zod';
import type { Vehicle } from '../../../shared/types/contracts';

export const fuelTypes = ['Xăng', 'Dầu (Diesel)', 'Điện', 'Hybrid'] as const;

export const vehicleFormSchema = z.object({
  customerId: z.string().min(1, 'Chủ xe là bắt buộc'),
  licensePlate: z
    .string()
    .trim()
    .min(1, 'Biển số xe là bắt buộc')
    .max(20, 'Biển số xe tối đa 20 ký tự')
    .transform((val) => val.toUpperCase().replace(/\s+/g, '')),
  vin: z.string().trim().max(30, 'Số khung (VIN) tối đa 30 ký tự').optional(),
  engineNumber: z.string().trim().max(30, 'Số máy tối đa 30 ký tự').optional(),
  brand: z.string().trim().min(1, 'Hãng xe là bắt buộc').max(50, 'Hãng xe tối đa 50 ký tự'),
  model: z.string().trim().min(1, 'Dòng xe (Model) là bắt buộc').max(50, 'Dòng xe tối đa 50 ký tự'),
  manufactureYear: z.coerce
    .number({ invalid_type_error: 'Năm sản xuất phải là số' })
    .int('Năm sản xuất phải là số nguyên')
    .min(1900, 'Năm sản xuất không hợp lệ')
    .max(new Date().getFullYear() + 1, 'Năm sản xuất không vượt quá năm tiếp theo')
    .optional()
    .or(z.nan()),
  color: z.string().trim().max(30, 'Màu sơn tối đa 30 ký tự').optional(),
  fuelType: z.string().trim().max(20, 'Loại nhiên liệu tối đa 20 ký tự').optional(),
  odometer: z.coerce
    .number({ invalid_type_error: 'Số ODO phải là số' })
    .int('Số ODO phải là số nguyên')
    .min(0, 'Số ODO không được âm'),
  inspectionDueDate: z.string().trim().optional(),
});

export type VehicleFormValues = z.infer<typeof vehicleFormSchema>;

export const transferOwnershipSchema = z.object({
  newCustomerId: z.string().min(1, 'Vui lòng chọn chủ xe mới'),
  transferOdometer: z.coerce
    .number({ invalid_type_error: 'Số ODO phải là số' })
    .int('Số ODO phải là số nguyên')
    .min(0, 'Số ODO không được âm')
    .optional(),
  transferNote: z.string().trim().max(500, 'Ghi chú chuyển nhượng tối đa 500 ký tự').optional(),
});

export type TransferOwnershipFormValues = z.infer<typeof transferOwnershipSchema>;

export function vehicleDefaults(existing?: Vehicle): VehicleFormValues {
  return {
    customerId: existing?.customerId ?? '',
    licensePlate: existing?.licensePlate ?? '',
    vin: existing?.vin ?? '',
    engineNumber: existing?.engineNumber ?? '',
    brand: existing?.brand ?? '',
    model: existing?.model ?? '',
    manufactureYear: existing?.manufactureYear ?? ('' as unknown as number),
    color: existing?.color ?? '',
    fuelType: existing?.fuelType ?? 'Xăng',
    odometer: existing?.odometer ?? 0,
    inspectionDueDate: existing?.inspectionDueDate ? existing.inspectionDueDate.slice(0, 10) : '',
  };
}

export function transferDefaults(vehicle?: Vehicle): TransferOwnershipFormValues {
  return {
    newCustomerId: '',
    transferOdometer: vehicle?.odometer ?? 0,
    transferNote: '',
  };
}

import { z } from 'zod';
import { serviceTypes, type Service } from '../../../shared/types/contracts';

export const bayTypes = [
  'Khoang tiêu chuẩn',
  'Cầu nâng 2 trụ',
  'Cầu nâng cắt kéo',
  'Cầu nâng 4 trụ',
  'Phòng sơn sấy',
  'Khoang chẩn đoán điện',
  'Khoang chăm sóc xe (Detailing)',
] as const;

export const commonUnits = ['lượt', 'giờ', 'gói', 'bộ', 'hạng mục'] as const;

export const serviceFormSchema = z.object({
  serviceCode: z.string().trim().max(30, 'Mã dịch vụ tối đa 30 ký tự').optional(),
  serviceName: z
    .string()
    .trim()
    .min(1, 'Tên dịch vụ là bắt buộc')
    .max(150, 'Tên dịch vụ tối đa 150 ký tự'),
  serviceType: z.enum(serviceTypes),
  description: z.string().trim().max(1000, 'Mô tả tối đa 1000 ký tự').optional(),
  unit: z.string().trim().max(30, 'Đơn vị tính tối đa 30 ký tự'),
  unitPrice: z.coerce
    .number<number>({ error: 'Đơn giá phải là số' })
    .min(0, 'Đơn giá không được âm'),
  standardMinutes: z.coerce
    .number<number>({ error: 'Thời gian định mức phải là số' })
    .int('Thời gian phải là số nguyên phút')
    .min(1, 'Thời gian định mức phải từ 1 phút trở lên'),
  requiredBayType: z.string().trim().max(30, 'Loại khoang tối đa 30 ký tự').optional(),
  intervalKm: z.coerce
    .number<number>({ error: 'Chu kỳ km phải là số' })
    .int('Chu kỳ km phải là số nguyên')
    .min(1, 'Chu kỳ km phải lớn hơn 0')
    .optional()
    .or(z.nan()),
  intervalMonths: z.coerce
    .number<number>({ error: 'Chu kỳ tháng phải là số' })
    .int('Chu kỳ tháng phải là số nguyên')
    .min(1, 'Chu kỳ tháng phải lớn hơn 0')
    .optional()
    .or(z.nan()),
  warrantyDays: z.coerce
    .number<number>({ error: 'Số ngày bảo hành phải là số' })
    .int('Số ngày bảo hành phải là số nguyên')
    .min(0, 'Số ngày bảo hành không được âm'),
  warrantyKm: z.coerce
    .number<number>({ error: 'Số km bảo hành phải là số' })
    .int('Số km bảo hành phải là số nguyên')
    .min(0, 'Số km bảo hành không được âm'),
  warrantyTerms: z.string().trim().max(1000, 'Điều khoản bảo hành tối đa 1000 ký tự').optional(),
});

export type ServiceFormValues = z.infer<typeof serviceFormSchema>;

export function serviceDefaults(existing?: Service): ServiceFormValues {
  return {
    serviceCode: existing?.serviceCode ?? '',
    serviceName: existing?.serviceName ?? '',
    serviceType: existing?.serviceType ?? 'SINGLE',
    description: existing?.description ?? '',
    unit: existing?.unit ?? 'lượt',
    unitPrice: existing ? Number(existing.unitPrice) : 0,
    standardMinutes: existing?.standardMinutes ?? 30,
    requiredBayType: existing?.requiredBayType ?? '',
    intervalKm: existing?.intervalKm ?? Number.NaN,
    intervalMonths: existing?.intervalMonths ?? Number.NaN,
    warrantyDays: existing?.warrantyDays ?? 0,
    warrantyKm: existing?.warrantyKm ?? 0,
    warrantyTerms: existing?.warrantyTerms ?? '',
  };
}

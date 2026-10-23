import { z } from 'zod';
import { customerTypes, type Customer, type CustomerType } from '../../../shared/types/contracts';

export const customerFormSchema = z
  .object({
    customerCode: z.string().max(30, 'Mã khách hàng tối đa 30 ký tự').optional(),
    fullName: z
      .string()
      .trim()
      .min(1, 'Họ và tên khách hàng là bắt buộc')
      .max(150, 'Họ và tên tối đa 150 ký tự'),
    customerType: z.enum(customerTypes),
    phone: z
      .string()
      .trim()
      .min(9, 'Số điện thoại tối thiểu 9 chữ số')
      .max(20, 'Số điện thoại tối đa 20 chữ số')
      .regex(/^[0-9+() -]{9,20}$/, 'Số điện thoại không đúng định dạng'),
    email: z
      .string()
      .trim()
      .email('Email không đúng định dạng')
      .max(254, 'Email tối đa 254 ký tự')
      .or(z.literal(''))
      .optional(),
    address: z.string().trim().max(500, 'Địa chỉ tối đa 500 ký tự').optional(),
    companyName: z.string().trim().max(255, 'Tên công ty tối đa 255 ký tự').optional(),
    taxCode: z.string().trim().max(30, 'Mã số thuế tối đa 30 ký tự').optional(),
    creditLimit: z.coerce
      .number({ invalid_type_error: 'Hạn mức công nợ phải là số' })
      .min(0, 'Hạn mức công nợ không được âm'),
    paymentTermDays: z.coerce
      .number({ invalid_type_error: 'Hạn nợ (ngày) phải là số' })
      .int('Hạn nợ phải là số nguyên ngày')
      .min(0, 'Hạn nợ không được âm'),
  })
  .superRefine((data, ctx) => {
    if (data.customerType === 'BUSINESS' && (!data.companyName || data.companyName.trim().length === 0)) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        path: ['companyName'],
        message: 'Khách hàng doanh nghiệp bắt buộc phải có tên công ty',
      });
    }
  });

export type CustomerFormValues = z.infer<typeof customerFormSchema>;

export function customerDefaults(existing?: Customer): CustomerFormValues {
  return {
    customerCode: existing?.customerCode ?? '',
    fullName: existing?.fullName ?? '',
    customerType: existing?.customerType ?? 'INDIVIDUAL',
    phone: existing?.phone ?? '',
    email: existing?.email ?? '',
    address: existing?.address ?? '',
    companyName: existing?.companyName ?? '',
    taxCode: existing?.taxCode ?? '',
    creditLimit: existing ? Number(existing.creditLimit) : 0,
    paymentTermDays: existing?.paymentTermDays ?? 0,
  };
}

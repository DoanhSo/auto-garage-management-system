import { z } from 'zod';
import type { UiPolicy } from '../../../shared/types/contracts';
export const loginSchema = z.object({
  identifier: z
    .string()
    .trim()
    .min(1, 'Nhập tên đăng nhập hoặc email.')
    .max(254, 'Tối đa 254 ký tự.'),
  password: z.string().min(1, 'Nhập mật khẩu.').max(72, 'Mật khẩu vượt giới hạn 72 ký tự.'),
});
export function validNewPassword(value: string, policy: UiPolicy) {
  return (
    value.trim().length > 0 &&
    Array.from(value).length >= policy.passwordMinLength &&
    new TextEncoder().encode(value).length <= policy.passwordMaxUtf8Bytes
  );
}
export function passwordRequirement(policy: UiPolicy) {
  return `Ít nhất ${policy.passwordMinLength} ký tự, tối đa ${policy.passwordMaxUtf8Bytes} byte UTF-8.`;
}
export function registrationSchema(policy: UiPolicy) {
  return z
    .object({
      fullName: z.string().trim().min(1, 'Nhập họ và tên.').max(150, 'Tối đa 150 ký tự.'),
      email: z
        .string()
        .trim()
        .min(1, 'Nhập email.')
        .email('Email không hợp lệ.')
        .max(254, 'Tối đa 254 ký tự.'),
      password: z
        .string()
        .refine((value) => validNewPassword(value, policy), passwordRequirement(policy)),
      confirmPassword: z.string(),
    })
    .refine((value) => value.password === value.confirmPassword, {
      path: ['confirmPassword'],
      message: 'Mật khẩu xác nhận chưa khớp.',
    });
}
export function passwordChangeSchema(policy: UiPolicy) {
  return z
    .object({
      currentPassword: z
        .string()
        .min(1, 'Nhập mật khẩu hiện tại.')
        .max(72, 'Mật khẩu vượt giới hạn.'),
      newPassword: z
        .string()
        .refine((value) => validNewPassword(value, policy), passwordRequirement(policy)),
      confirmPassword: z.string(),
    })
    .refine((value) => value.newPassword === value.confirmPassword, {
      path: ['confirmPassword'],
      message: 'Mật khẩu xác nhận chưa khớp.',
    });
}

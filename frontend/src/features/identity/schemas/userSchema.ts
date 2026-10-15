import { z } from 'zod';
import { roles, type UiPolicy, type User } from '../../../shared/types/contracts';
import { passwordRequirement, validNewPassword } from '../../auth/schemas/authSchemas';
export const userFormSchema = (policy: UiPolicy, create: boolean) =>
  z
    .object({
      fullName: z
        .string()
        .max(150, 'Họ tên tối đa 150 ký tự.')
        .refine((value) => !!value.trim(), 'Nhập họ và tên.'),
      username: z
        .string()
        .regex(
          /^[A-Za-z0-9._-]{3,100}$/,
          'Dùng 3–100 chữ/số ASCII, dấu chấm, gạch dưới hoặc gạch ngang.',
        ),
      email: z
        .string()
        .max(254, 'Email tối đa 254 ký tự.')
        .refine(
          (value) => !value.trim() || z.email().safeParse(value.trim()).success,
          'Email chưa đúng định dạng.',
        ),
      role: z
        .enum([...roles, ''], { error: 'Chọn vai trò cho tài khoản.' })
        .refine((value): boolean => value !== '', 'Chọn vai trò cho tài khoản.'),
      active: z.boolean(),
      password: z.string(),
      confirmPassword: z.string(),
    })
    .superRefine((value, context) => {
      if (!create) return;
      if (!validNewPassword(value.password, policy))
        context.addIssue({
          code: 'custom',
          path: ['password'],
          message: passwordRequirement(policy),
        });
      if (value.password !== value.confirmPassword)
        context.addIssue({
          code: 'custom',
          path: ['confirmPassword'],
          message: 'Mật khẩu xác nhận chưa khớp.',
        });
    });
export type UserFormValues = z.infer<ReturnType<typeof userFormSchema>>;
export function userDefaults(user?: User): UserFormValues {
  return {
    fullName: user?.fullName ?? '',
    username: user?.username ?? '',
    email: user?.email ?? '',
    role: user?.role ?? '',
    active: user?.active ?? true,
    password: '',
    confirmPassword: '',
  };
}

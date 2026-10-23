import { z } from 'zod';
import { roles, type User } from '../../../shared/types/contracts';
export const userFormSchema = () =>
  z.object({
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
    password: z
      .string()
      .max(72, 'Mật khẩu tối đa 72 ký tự.')
      .refine(
        (value) => !value.trim() || value.trim().length >= 8,
        'Mật khẩu khởi tạo tối thiểu 8 ký tự (hoặc để trống).',
      ),
    active: z.boolean(),
  });
export type UserFormValues = z.infer<ReturnType<typeof userFormSchema>>;
export function userDefaults(user?: User): UserFormValues {
  return {
    fullName: user?.fullName ?? '',
    username: user?.username ?? '',
    email: user?.email ?? '',
    role: user?.role ?? '',
    password: '',
    active: user?.active ?? true,
  };
}

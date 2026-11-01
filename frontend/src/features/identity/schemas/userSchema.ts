import { z } from 'zod';
import { roles, type UiPolicy, type User } from '../../../shared/types/contracts';
import { usernameSchema } from '../../../shared/validation/username';
export const userFormSchema = (policy?: UiPolicy) => {
  const minLength = policy?.passwordMinLength ?? 12;
  const maxUtf8Bytes = policy?.passwordMaxUtf8Bytes ?? 72;
  return z.object({
    fullName: z
      .string()
      .max(150, 'Họ tên tối đa 150 ký tự.')
      .refine((value) => !!value.trim(), 'Nhập họ và tên.'),
    username: usernameSchema,
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
      .refine(
        (value) => !value.trim() || Array.from(value.trim()).length >= minLength,
        `Mật khẩu khởi tạo tối thiểu ${minLength} ký tự (hoặc để trống).`,
      )
      .refine(
        (value) => new TextEncoder().encode(value.trim()).length <= maxUtf8Bytes,
        `Mật khẩu tối đa ${maxUtf8Bytes} ký tự (nên dùng chữ không dấu).`,
      ),
    active: z.boolean(),
  });
};
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

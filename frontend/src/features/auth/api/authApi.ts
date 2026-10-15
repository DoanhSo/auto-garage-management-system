import { request } from '../../../shared/api/client';
import { loginResultSchema, userSchema } from '../../../shared/types/schemas';
export const authApi = {
  login: async (values: { identifier: string; password: string }) =>
    loginResultSchema.parse(
      await request<unknown>('/auth/login', {
        method: 'POST',
        body: values,
        authenticated: false,
      }),
    ),
  me: async (signal?: AbortSignal) =>
    userSchema.parse(await request<unknown>('/auth/me', { signal })),
  logout: () => request<null>('/auth/logout', { method: 'POST' }),
  changePassword: (values: { currentPassword: string; newPassword: string }) =>
    request<null>('/auth/password', { method: 'POST', body: values }),
};

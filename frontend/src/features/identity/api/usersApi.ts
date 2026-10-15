import { request, queryString } from '../../../shared/api/client';
import { numericVersion } from '../../../shared/api/json';
import { pageSchema, userSchema } from '../../../shared/types/schemas';
import type { UserCreate, UserUpdate } from '../../../shared/types/contracts';
export interface UsersParams {
  keyword?: string;
  active?: boolean;
  page: number;
  size: number;
  sortBy: string;
  sortDir: string;
}
export const usersApi = {
  list: async (params: UsersParams, signal?: AbortSignal) =>
    pageSchema(userSchema).parse(
      await request<unknown>('/admin/users' + queryString({ ...params }), {
        signal,
      }),
    ),
  get: async (id: string, signal?: AbortSignal) =>
    userSchema.parse(await request<unknown>(`/admin/users/${id}`, { signal })),
  create: async (values: UserCreate) =>
    userSchema.parse(
      await request<unknown>('/admin/users', {
        method: 'POST',
        body: {
          username: values.username,
          email: values.email,
          fullName: values.fullName,
          role: values.role,
          password: values.password,
        },
      }),
    ),
  update: async (id: string, values: UserUpdate) =>
    userSchema.parse(
      await request<unknown>(`/admin/users/${id}`, {
        method: 'PUT',
        body: {
          fullName: values.fullName,
          email: values.email,
          role: values.role,
          active: values.active,
          version: numericVersion(values.version),
        },
      }),
    ),
  remove: (id: string, version: string) =>
    request<null>(`/admin/users/${id}` + queryString({ version }), {
      method: 'DELETE',
    }),
};

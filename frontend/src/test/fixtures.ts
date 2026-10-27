import { http, HttpResponse } from 'msw';
export const policy = {
  passwordMinLength: 12,
  passwordMaxUtf8Bytes: 72,
  paginationDefaultSize: 20,
  paginationMaxSize: 100,
  paginationMaxPage: 100000,
  reportMaxPeriodSeconds: 31622400,
  garageTimeZone: 'Asia/Bangkok',
};
export const admin = {
  id: 1,
  username: 'admin',
  fullName: 'Quản trị Gara',
  email: 'admin@example.test',
  role: 'ADMIN',
  active: true,
  version: 0,
  createdAt: '2026-10-15T01:00:00Z',
};
export const member = {
  ...admin,
  id: 2,
  username: 'reception',
  fullName: 'Nguyễn Minh Anh',
  role: 'RECEPTIONIST',
};
export const envelope = (data: unknown, code = 1000) => ({
  code,
  message: 'Safe',
  success: code < 2000,
  data,
  timestamp: '2026-10-15T01:00:00Z',
  path: '/api/v1/test',
});
export const page = (content: unknown[]) => ({
  content,
  totalElements: content.length,
  totalPages: content.length ? 1 : 0,
  number: 0,
  size: 20,
});
export function baseHandlers() {
  return [
    http.post('/api/v1/auth/session', () =>
      HttpResponse.json(
        envelope({ accessToken: 'test-only-token', expiresIn: 1800, tokenType: 'Bearer' }),
      ),
    ),
    http.post('/api/v1/auth/session/refresh', () =>
      HttpResponse.json(envelope(null, 3003), { status: 401 }),
    ),
    http.delete('/api/v1/auth/session', () => HttpResponse.json(envelope(null))),
    http.get('/api/v1/public/ui-policy', () => HttpResponse.json(envelope(policy))),
    http.post('/realms/autogarage-realm/protocol/openid-connect/token', () =>
      HttpResponse.json({
        access_token: 'test-only-token',
        refresh_token: 'test-refresh-token',
        expires_in: 1800,
        token_type: 'Bearer',
      }),
    ),
    http.post(
      '/realms/autogarage-realm/protocol/openid-connect/logout',
      () => new HttpResponse(null, { status: 204 }),
    ),
    http.get('/api/v1/auth/me', () => HttpResponse.json(envelope(admin))),
    http.post('/api/v1/auth/logout', () => HttpResponse.json(envelope(null))),
    http.get('/api/v1/admin/users', () => HttpResponse.json(envelope(page([admin, member])))),
    http.get('/api/v1/admin/users/2', () => HttpResponse.json(envelope(member))),
    http.get('/api/v1/notifications', () => HttpResponse.json(envelope(page([])))),
  ];
}

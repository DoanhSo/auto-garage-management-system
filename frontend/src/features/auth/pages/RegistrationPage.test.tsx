import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../../../app/providers/AuthProvider';
import { RegistrationPage } from './RegistrationPage';
import { server } from '../../../test/server';
import { admin, baseHandlers, envelope } from '../../../test/fixtures';
import { authApi } from '../api/authApi';

beforeEach(() => server.use(...baseHandlers()));
function app() {
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}
    >
      <AuthProvider>
        <MemoryRouter initialEntries={['/register']}>
          <Routes>
            <Route path="/register" element={<RegistrationPage />} />
            <Route path="/notifications" element={<div>Customer workspace</div>} />
            <Route path="/login" element={<div>Login page</div>} />
          </Routes>
        </MemoryRouter>
      </AuthProvider>
    </QueryClientProvider>,
  );
}
async function fill() {
  const user = userEvent.setup();
  await screen.findByRole('heading', { name: 'Đăng ký tài khoản' });
  await user.type(screen.getByLabelText('Họ và tên', { exact: false }), 'Customer Name');
  await user.type(screen.getByLabelText('Email', { exact: false }), 'customer@gmail.com');
  await user.type(
    screen.getByLabelText(/^Mật khẩu\s*\*/, { selector: 'input' }),
    'test-password-123',
  );
  await user.type(
    screen.getByLabelText('Xác nhận mật khẩu', { exact: false }),
    'test-password-123',
  );
  return user;
}
describe('Registration inside AutoGarage', () => {
  it('sends only registration fields and logs into the customer workspace without redirecting to Keycloak', async () => {
    const create = vi.fn();
    const redirect = vi.spyOn(authApi, 'startRegistrationRedirect');
    server.use(
      http.post('/api/v1/auth/register', async ({ request }) => {
        create(await request.json());
        return HttpResponse.json(envelope(null, 1001), { status: 201 });
      }),
      http.get('/api/v1/auth/me', () =>
        HttpResponse.json(envelope({ ...admin, role: 'CUSTOMER' })),
      ),
    );
    app();
    const user = await fill();
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }));
    expect(await screen.findByText('Customer workspace')).toBeVisible();
    expect(create).toHaveBeenCalledExactlyOnceWith({
      fullName: 'Customer Name',
      email: 'customer@gmail.com',
      password: 'test-password-123',
    });
    expect(redirect).not.toHaveBeenCalled();
  });
  it('keeps identity fields on duplicate email and never attempts login', async () => {
    const login = vi.spyOn(authApi, 'loginWithPassword');
    server.use(
      http.post('/api/v1/auth/register', () =>
        HttpResponse.json(envelope(null, 2003), { status: 409 }),
      ),
    );
    app();
    const user = await fill();
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Dữ liệu trùng');
    expect(screen.getByLabelText('Email', { exact: false })).toHaveValue('customer@gmail.com');
    expect(screen.getByLabelText(/^Mật khẩu\s*\*/, { selector: 'input' })).toHaveValue('');
    expect(login).not.toHaveBeenCalled();
  });
  it('rejects a mismatched password confirmation before sending a registration request', async () => {
    const create = vi.spyOn(authApi, 'register');
    app();
    const user = await fill();
    await user.clear(screen.getByLabelText('Xác nhận mật khẩu', { exact: false }));
    await user.type(
      screen.getByLabelText('Xác nhận mật khẩu', { exact: false }),
      'different-password',
    );
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }));
    expect(await screen.findByText('Mật khẩu xác nhận chưa khớp.')).toBeVisible();
    expect(create).not.toHaveBeenCalled();
  });
  it('does not register twice if the account was created but automatic login fails', async () => {
    const create = vi.fn();
    server.use(
      http.post('/api/v1/auth/register', () => {
        create();
        return HttpResponse.json(envelope(null, 1001), { status: 201 });
      }),
      http.post('/realms/autogarage-realm/protocol/openid-connect/token', () =>
        HttpResponse.json({ error: 'temporarily_unavailable' }, { status: 503 }),
      ),
    );
    app();
    const user = await fill();
    await user.click(screen.getByRole('button', { name: 'Đăng ký' }));
    await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Vui lòng đăng nhập'));
    expect(screen.queryByRole('button', { name: 'Đăng ký' })).not.toBeInTheDocument();
    expect(create).toHaveBeenCalledOnce();
  });
});

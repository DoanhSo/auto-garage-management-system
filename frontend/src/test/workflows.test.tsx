import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider } from '../app/providers/AuthProvider';
import { ToastProvider } from '../app/providers/ToastProvider';
import { LoginPage } from '../features/auth/pages/LoginPage';
import { UsersPage } from '../features/identity/pages/UsersPage';
import { AccountPage } from '../features/auth/pages/AccountPage';
import { NotificationsPage } from '../features/notification/pages/NotificationsPage';
import { ServicesPage } from '../features/catalog/pages/ServicesPage';
import { BookingsPage } from '../features/reception/pages/BookingsPage';
import { authApi } from '../features/auth/api/authApi';
import { admin, baseHandlers, envelope, member, page } from './fixtures';
import { server } from './server';

function app(destination: React.ReactNode = <UsersPage />) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const router = createMemoryRouter(
    [
      { path: '/login', element: <LoginPage /> },
      { path: '/admin/reports', element: destination },
      { path: '/notifications', element: destination },
    ],
    { initialEntries: ['/login'] },
  );
  render(
    <QueryClientProvider client={client}>
      <AuthProvider>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>,
  );
  return { client, router };
}
async function login() {
  const user = userEvent.setup();
  await user.type(
    await screen.findByLabelText('Tên đăng nhập hoặc email', { exact: false }),
    'admin',
  );
  await user.type(screen.getByLabelText(/^Mật khẩu\s*\*/, { selector: 'input' }), 'valid-password');
  await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));
  return user;
}
beforeEach(() => server.use(...baseHandlers()));
describe('Forms against HTTP contracts', () => {
  it('logs in from the local credentials form without opening the Keycloak portal', async () => {
    const redirect = vi.spyOn(authApi, 'startPkceRedirect');
    app();
    expect(screen.queryByRole('button', { name: /Keycloak/ })).not.toBeInTheDocument();
    await login();
    expect(await screen.findByRole('button', { name: 'Thêm tài khoản' })).toBeVisible();
    expect(redirect).not.toHaveBeenCalled();
  });

  it('keeps a new service editor open and submits with optional maintenance fields empty', async () => {
    const create = vi.fn();
    server.use(
      http.get('/api/v1/services', () => HttpResponse.json(envelope([]))),
      http.post('/api/v1/services', async ({ request }) => {
        const values = await request.json();
        create(values);
        return HttpResponse.json(
          envelope(
            {
              ...(values as object),
              id: 4,
              serviceCode: 'SVC-4',
              requiredSkillsJson: null,
              isActive: true,
              createdAt: admin.createdAt,
              updatedAt: admin.createdAt,
              version: 0,
            },
            1001,
          ),
          { status: 201 },
        );
      }),
    );
    app(<ServicesPage />);
    const user = await login();
    await user.click(await screen.findByRole('button', { name: 'Thêm dịch vụ mới' }));
    const dialog = await screen.findByRole('dialog');
    await user.type(
      within(dialog).getByLabelText('Tên dịch vụ / Gói bảo dưỡng', { exact: false }),
      'Kiểm tra định kỳ',
    );
    await user.click(within(dialog).getByRole('button', { name: /Thêm dịch vụ/ }));
    await waitFor(() => expect(create).toHaveBeenCalledTimes(1));
    expect(create.mock.calls[0][0]).toMatchObject({
      intervalKm: null,
      intervalMonths: null,
      warrantyDays: 0,
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });

  it('loads a customer booking form from their own profile without exposing the customer directory', async () => {
    const ownCustomer = {
      id: 9,
      userId: 2,
      customerCode: 'CUS-9',
      fullName: 'Khách hàng',
      customerType: 'INDIVIDUAL',
      phone: null,
      email: null,
      address: null,
      companyName: null,
      taxCode: null,
      creditLimit: 0,
      paymentTermDays: 0,
      isActive: true,
      createdAt: admin.createdAt,
      updatedAt: admin.createdAt,
      version: 0,
    };
    const directory = vi.fn();
    server.use(
      http.get('/api/v1/auth/me', () =>
        HttpResponse.json(envelope({ ...member, role: 'CUSTOMER' })),
      ),
      http.get('/api/v1/bookings', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/services', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/vehicles', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/customers/me', () => HttpResponse.json(envelope(ownCustomer))),
      http.get('/api/v1/customers', () => {
        directory();
        return HttpResponse.json(envelope(null, 3002), { status: 403 });
      }),
    );
    app(<BookingsPage />);
    const user = await login();
    await user.click(await screen.findByRole('button', { name: 'Đặt lịch hẹn mới' }));
    const dialog = await screen.findByRole('dialog');
    const customer = within(dialog).getByLabelText('Khách hàng đặt hẹn', { exact: false });
    await waitFor(() => expect(customer).toHaveValue('9'));
    expect(customer).toBeDisabled();
    expect(directory).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: 'Tiếp nhận vãng lai' })).not.toBeInTheDocument();
  });
  it('validates login and prevents duplicate requests while pending', async () => {
    let calls = 0;
    server.use(
      http.post('/realms/autogarage-realm/protocol/openid-connect/token', async () => {
        calls++;
        await delay(100);
        return HttpResponse.json({ error: 'invalid_grant' }, { status: 401 });
      }),
    );
    app();
    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByText('Nhập tên đăng nhập hoặc email.')).toBeVisible();
    expect(calls).toBe(0);
    await user.type(screen.getByLabelText('Tên đăng nhập hoặc email', { exact: false }), 'admin');
    await user.type(screen.getByLabelText(/^Mật khẩu\s*\*/, { selector: 'input' }), 'wrong');
    await user.dblClick(screen.getByRole('button', { name: 'Đăng nhập' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('không đúng');
    expect(calls).toBe(1);
  });
  it('validates the create form and sends only allowed API fields', async () => {
    const create = vi.fn();
    server.use(
      http.post('/api/v1/admin/users', async ({ request }) => {
        create(await request.json());
        return HttpResponse.json(envelope({ ...member, id: 3, username: 'new.member' }, 1001), {
          status: 201,
        });
      }),
    );
    app();
    const user = await login();
    await user.click(await screen.findByRole('button', { name: 'Thêm tài khoản' }));
    const dialog = screen.getByRole('dialog');
    await user.click(
      within(dialog).getByRole('button', {
        name: 'Thêm tài khoản',
      }),
    );
    expect(await within(dialog).findByText('Nhập họ và tên.')).toBeVisible();
    expect(within(dialog).queryByText('3–100 ký tự: chữ/số, dấu . _ -')).not.toBeInTheDocument();
    expect(create).not.toHaveBeenCalled();
    await user.type(within(dialog).getByLabelText('Họ và tên', { exact: false }), 'New Member');
    await user.type(within(dialog).getByLabelText('Tên đăng nhập', { exact: false }), 'new.member');
    await user.selectOptions(
      within(dialog).getByLabelText('Vai trò', { exact: false }),
      'RECEPTIONIST',
    );
    await user.click(
      within(dialog).getByRole('button', {
        name: 'Thêm tài khoản',
      }),
    );
    await waitFor(() => expect(create).toHaveBeenCalledTimes(1));
    expect(create.mock.calls[0][0]).toEqual({
      username: 'new.member',
      email: null,
      fullName: 'New Member',
      role: 'RECEPTIONIST',
    });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
  it('preserves a conflict draft and only replaces it on explicit selection', async () => {
    server.use(
      http.put('/api/v1/admin/users/2', () =>
        HttpResponse.json(envelope(null, 2005), { status: 409 }),
      ),
    );
    app();
    const user = await login();
    await user.click(
      await screen.findByRole('button', {
        name: 'Sửa tài khoản Nguyễn Minh Anh',
      }),
    );
    let dialog = screen.getByRole('dialog');
    const name = await within(dialog).findByLabelText('Họ và tên', {
      exact: false,
    });
    await user.clear(name);
    await user.type(name, 'Bản nháp của tôi');
    await user.click(within(dialog).getByRole('button', { name: 'Lưu thay đổi' }));
    expect(await within(dialog).findByText('Dữ liệu đã thay đổi')).toBeVisible();
    expect(name).toHaveValue('Bản nháp của tôi');
    server.use(
      http.get('/api/v1/admin/users/2', () =>
        HttpResponse.json(envelope({ ...member, fullName: 'Bản trên máy chủ', version: 2 })),
      ),
    );
    await user.click(within(dialog).getByRole('button', { name: 'Xem bản mới trên máy chủ' }));
    await within(dialog).findByText('Bản trên máy chủ');
    expect(name).toHaveValue('Bản nháp của tôi');
    await user.click(within(dialog).getByRole('button', { name: 'Dùng bản mới' }));
    dialog = screen.getByRole('dialog');
    expect(within(dialog).getByLabelText('Họ và tên', { exact: false })).toHaveValue(
      'Bản trên máy chủ',
    );
  });
  it('guards dirty drawer close and keeps field values when cancelled', async () => {
    app();
    const user = await login();
    await user.click(await screen.findByRole('button', { name: 'Thêm tài khoản' }));
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByLabelText('Họ và tên', { exact: false }), 'Unsaved');
    await user.click(within(dialog).getByRole('button', { name: 'Đóng form' }));
    const confirm = screen.getByRole('alertdialog');
    await user.click(within(confirm).getByRole('button', { name: 'Hủy' }));
    expect(within(dialog).getByLabelText('Họ và tên', { exact: false })).toHaveValue('Unsaved');
  });
  it('keeps a last-admin error visible and does not remove the row', async () => {
    server.use(
      http.delete('/api/v1/admin/users/1', () =>
        HttpResponse.json(envelope(null, 2006), { status: 409 }),
      ),
    );
    app();
    const user = await login();
    await user.click(
      await screen.findByRole('button', {
        name: 'Xóa tài khoản Quản trị Gara',
      }),
    );
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', {
        name: 'Xóa tài khoản',
      }),
    );
    expect(await screen.findByRole('alert')).toHaveTextContent('cuối cùng');
    expect(screen.getByText(admin.fullName)).toBeInTheDocument();
  });
  it('toggles lock and unlock through PUT and deletes separately through DELETE', async () => {
    let current = { ...member };
    let deleted = false;
    const writes: Record<string, unknown>[] = [];
    let deletes = 0;
    server.use(
      http.get('/api/v1/admin/users', () =>
        HttpResponse.json(envelope(page(deleted ? [admin] : [admin, current]))),
      ),
      http.put('/api/v1/admin/users/2', async ({ request }) => {
        const values = (await request.json()) as Record<string, unknown>;
        writes.push(values);
        current = { ...current, active: values.active as boolean, version: current.version + 1 };
        return HttpResponse.json(envelope(current, 1002));
      }),
      http.delete('/api/v1/admin/users/2', () => {
        deletes++;
        deleted = true;
        return HttpResponse.json(envelope(null, 1003));
      }),
    );
    app();
    const user = await login();
    await user.click(
      await screen.findByRole('button', { name: `Khóa tài khoản ${member.fullName}` }),
    );
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Khóa tài khoản' }),
    );
    await user.click(
      await screen.findByRole('button', { name: `Mở khóa tài khoản ${member.fullName}` }),
    );
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Mở khóa tài khoản' }),
    );
    await screen.findByRole('button', { name: `Khóa tài khoản ${member.fullName}` });
    expect(writes.map((value) => value.active)).toEqual([false, true]);
    expect(deletes).toBe(0);
    await user.click(screen.getByRole('button', { name: `Xóa tài khoản ${member.fullName}` }));
    await user.click(
      within(screen.getByRole('alertdialog')).getByRole('button', { name: 'Xóa tài khoản' }),
    );
    await waitFor(() =>
      expect(
        screen.queryByRole('button', { name: `Sửa tài khoản ${member.fullName}` }),
      ).not.toBeInTheDocument(),
    );
    expect(deletes).toBe(1);
  });
  it('renders Keycloak security panel and revokes all active sessions', async () => {
    let logoutCalls = 0;
    server.use(
      http.post('/api/v1/auth/logout', () => {
        logoutCalls++;
        return HttpResponse.json(envelope(null));
      }),
    );
    app(<AccountPage />);
    const user = await login();
    expect(await screen.findByRole('heading', { name: 'Tài khoản của tôi' })).toBeVisible();
    expect(screen.getByRole('link', { name: /Mở cổng tài khoản Keycloak/i })).toHaveAttribute(
      'href',
      '/realms/autogarage-realm/account',
    );
    await user.click(screen.getByRole('button', { name: /Thu hồi mọi phiên đăng nhập/i }));
    await waitFor(() => expect(logoutCalls).toBe(1));
  });
  it('renders notification payload as text and saves read state once', async () => {
    const notice = {
      id: 3,
      channel: 'WEB',
      subject: 'Lịch hẹn',
      payloadJson: '{"message":"<img src=x onerror=alert(1)>"}',
      status: 'DELIVERED',
      createdAt: '2026-10-15T01:00:00Z',
      readAt: null,
    };
    let reads = 0;
    server.use(
      http.get('/api/v1/notifications', () => HttpResponse.json(envelope(page([notice])))),
      http.get('/api/v1/notifications/3', () => HttpResponse.json(envelope(notice))),
      http.post('/api/v1/notifications/3/read', () => {
        reads++;
        return HttpResponse.json(envelope({ ...notice, readAt: '2026-10-15T02:00:00Z' }, 1002));
      }),
    );
    app(<NotificationsPage />);
    const user = await login();
    await user.click(await screen.findByRole('button', { name: /Lịch hẹn/ }));
    await waitFor(() => expect(reads).toBe(1));
    expect(await screen.findByText('Đã đọc')).toBeVisible();
    expect(document.querySelector('.notice-body img')).toBeNull();
    expect(screen.getAllByText('<img src=x onerror=alert(1)>').length).toBeGreaterThan(0);
  });
});

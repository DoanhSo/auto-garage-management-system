import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { ToastProvider } from '../../../app/providers/ToastProvider';
import { configureSession } from '../../../shared/api/client';
import { envelope } from '../../../test/fixtures';
import { server } from '../../../test/server';
import { CustomersPage } from './CustomersPage';

const base = {
  customerType: 'INDIVIDUAL',
  address: null,
  companyName: null,
  taxCode: null,
  creditLimit: '0',
  paymentTermDays: 0,
  isActive: true,
  createdAt: '2026-10-01T00:00:00Z',
  updatedAt: '2026-10-01T00:00:00Z',
  version: 0,
};
const walkIn = {
  ...base,
  id: 1,
  userId: null,
  customerCode: 'KH-0001',
  fullName: 'Khách Vãng Lai',
  phone: '0900000001',
  email: 'vanglai@example.test',
  account: { status: 'NONE', username: null, lastLoginAt: null },
};
const registered = {
  ...base,
  id: 2,
  userId: 20,
  customerCode: 'KH-0002',
  fullName: 'Khách Đã Đăng Ký',
  phone: '0900000002',
  email: 'dangky@example.test',
  account: { status: 'ACTIVE', username: 'khach2', lastLoginAt: '2026-11-10T08:15:00Z' },
};

let lastQuery = '';

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter>
          <CustomersPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
  return userEvent.setup();
}

beforeEach(() => {
  configureSession('test-token', () => {});
  lastQuery = '';
  server.use(
    http.get('/api/v1/customers', ({ request }) => {
      lastQuery = new URL(request.url).search;
      return HttpResponse.json(envelope([walkIn, registered]));
    }),
  );
});

describe('CustomersPage account actions', () => {
  it('creates an account for a walk-in customer with the suggested username', async () => {
    let body: Record<string, unknown> | null = null;
    server.use(
      http.post('/api/v1/customers/1/account', async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(
          envelope({
            ...walkIn,
            userId: 30,
            account: { status: 'PENDING', username: 'vanglai', lastLoginAt: null },
          }),
        );
      }),
    );
    const user = renderPage();
    const row = (await screen.findByText('Khách Vãng Lai')).closest('tr') as HTMLElement;
    await user.click(within(row).getByRole('button', { name: /Tạo tài khoản cho/ }));

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByLabelText(/Tên đăng nhập/)).toHaveValue('vanglai');
    await user.click(within(dialog).getByRole('button', { name: /Sinh mật khẩu/ }));
    await user.click(within(dialog).getByRole('button', { name: 'Tạo tài khoản' }));

    await waitFor(() => expect(body).not.toBeNull());
    expect(body).toMatchObject({ username: 'vanglai', email: 'vanglai@example.test' });
    expect(
      String((body as unknown as { password: string }).password).length,
    ).toBeGreaterThanOrEqual(12);
  });

  it('offers no account buttons to customers who already have one', async () => {
    renderPage();
    const row = (await screen.findByText('Khách Đã Đăng Ký')).closest('tr') as HTMLElement;
    expect(within(row).queryByRole('button', { name: /Tạo tài khoản cho/ })).toBeNull();
    expect(within(row).queryByRole('button', { name: /Nối tài khoản cho/ })).toBeNull();
  });
});

describe('CustomersPage account column', () => {
  it('shows who has a sign-in account and warns that self-registration creates a new profile', async () => {
    renderPage();

    const walkInRow = (await screen.findByText('Khách Vãng Lai')).closest('tr') as HTMLElement;
    expect(within(walkInRow).getByText('Chưa có tài khoản')).toBeVisible();
    expect(within(walkInRow).getByText(/Nối tài khoản nếu khách đã tự đăng ký/)).toBeVisible();
    expect(within(walkInRow).getByRole('button', { name: /Tạo tài khoản cho/ })).toBeVisible();

    const registeredRow = screen.getByText('Khách Đã Đăng Ký').closest('tr') as HTMLElement;
    expect(within(registeredRow).getByText('Đang hoạt động')).toBeVisible();
    expect(within(registeredRow).getByText(/Đăng nhập gần nhất/)).toBeVisible();
  });

  it('asks the server for customers without an account', async () => {
    const user = renderPage();
    await screen.findByText('Khách Vãng Lai');

    await user.selectOptions(screen.getByLabelText('Tài khoản đăng nhập'), 'false');

    await waitFor(() => expect(lastQuery).toContain('hasAccount=false'));
  });
});

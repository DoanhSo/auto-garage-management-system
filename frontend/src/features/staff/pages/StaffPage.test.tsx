import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../../../app/providers/ToastProvider';
import { configureSession } from '../../../shared/api/client';
import { envelope, page } from '../../../test/fixtures';
import { server } from '../../../test/server';
import { StaffPage } from './StaffPage';

let role: 'ADMIN' | 'RECEPTIONIST' = 'ADMIN';
vi.mock('../../../app/providers/AuthProvider', () => ({
  useAuth: () => ({ user: { role, fullName: 'Quản lý' } }),
}));

const employee = {
  id: 5,
  userId: 50,
  employeeCode: 'EMP-005',
  fullName: 'Nguyễn Văn An',
  joinedAt: '2024-01-10',
  skillsJson: '["ENGINE"]',
  canQc: true,
  hourlyCost: 80000,
  salaryCoefficient: 1.2,
  bankName: 'VCB',
  bankAccountNumber: '0011223344',
  isActive: true,
  createdAt: '2024-01-10T00:00:00Z',
};

function Where() {
  const location = useLocation();
  return <output aria-label="địa chỉ">{location.pathname + location.search}</output>;
}

function renderPage(entry = '/staff') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[entry]}>
          <StaffPage />
          <Where />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
  return userEvent.setup();
}

beforeEach(() => {
  role = 'ADMIN';
  configureSession('test-token', () => {});
  server.use(
    http.get('/api/v1/employees', () => HttpResponse.json(envelope(page([employee])))),
    http.get('/api/v1/employee-shifts', () => HttpResponse.json(envelope(page([])))),
  );
});

describe('StaffPage', () => {
  it('lists employees with skills, QC, bank and rate on the profiles tab', async () => {
    renderPage();

    expect(await screen.findByRole('button', { name: 'Nguyễn Văn An' })).toBeVisible();
    expect(screen.getByText('Quyền QC')).toBeVisible();
    expect(screen.getByText(/VCB · 0011223344/)).toBeVisible();
    expect(screen.getByRole('button', { name: /Thêm nhân viên/ })).toBeVisible();
    expect(screen.getByRole('button', { name: /Hệ số & đơn giá lương/ })).toBeVisible();
  });

  it('hides the create button from receptionists', async () => {
    role = 'RECEPTIONIST';
    renderPage();

    await screen.findByRole('button', { name: 'Nguyễn Văn An' });
    expect(screen.queryByRole('button', { name: /Thêm nhân viên/ })).toBeNull();
  });

  it('opens the detail panel with links to the shifts and the monthly work of that employee', async () => {
    const user = renderPage();
    await user.click(await screen.findByRole('button', { name: 'Nguyễn Văn An' }));

    const panel = await screen.findByRole('dialog');
    expect(within(panel).getByText('EMP-005 · Đang làm việc')).toBeVisible();
    expect(await within(panel).findByText('Chưa có ca nào sắp tới.')).toBeVisible();
    expect(within(panel).getByRole('link', { name: 'Xem lịch ca' })).toHaveAttribute(
      'href',
      '/scheduling?tab=shifts&employeeId=5',
    );
    expect(within(panel).getByRole('link', { name: /Công & lương tháng/ })).toHaveAttribute(
      'href',
      '/payroll?tab=monthly&employeeId=5',
    );
    expect(within(panel).getByRole('button', { name: /Xếp ca/ })).toBeEnabled();
    expect(screen.getByLabelText('địa chỉ')).toHaveTextContent('employeeId=5');
  });

  it('opens the panel straight from a shared address', async () => {
    renderPage('/staff?tab=profiles&employeeId=5');

    const panel = await screen.findByRole('dialog');
    expect(within(panel).getByText('EMP-005 · Đang làm việc')).toBeVisible();
  });

  it('shows the strip of related screens for the role', async () => {
    renderPage();

    const related = await screen.findByRole('navigation', { name: 'Liên quan' });
    expect(within(related).getByRole('link', { name: 'Khoang & ca làm việc' })).toBeVisible();
    expect(within(related).getByRole('link', { name: 'Bảng lương' })).toBeVisible();
  });
});

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../../../app/providers/ToastProvider';
import { configureSession } from '../../../shared/api/client';
import { envelope, page } from '../../../test/fixtures';
import { server } from '../../../test/server';
import { PayrollManagerView } from './PayrollManagerView';

vi.mock('../../../app/providers/AuthProvider', () => ({
  useAuth: () => ({ user: { role: 'ADMIN', fullName: 'Quản lý' } }),
}));

const now = new Date();
const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;

const period = {
  id: '3',
  periodCode: `PAY-${monthKey}`,
  periodMonth: monthKey,
  fromDate: `${monthKey}-01`,
  toDate: `${monthKey}-28`,
  status: 'CALCULATED',
  totalEmployees: 1,
  totalRegularHours: 160,
  totalOvertimeHours: 0,
  totalBasePay: 0,
  totalCommissions: 0,
  totalAllowances: 0,
  totalDeductions: 0,
  totalNetPay: 0,
  completedOrderCount: 0,
};

const item = {
  id: '30',
  payrollPeriodId: '3',
  employeeId: '5',
  employeeCode: 'EMP-005',
  employeeName: 'Nguyễn Văn An',
  baseSalarySnapshot: 0,
  hourlyRateSnapshot: 80000,
  regularHours: 160,
  overtimeHours: 0,
  shiftCount: 20,
  completedOrderCount: 0,
  hourlyPayAmount: 0,
  commissionAmount: 0,
  orderBonusAmount: 0,
  allowanceAmount: 0,
  deductionAmount: 0,
  grossPay: 0,
  netPay: 0,
  status: 'DRAFT',
};

const employee = {
  id: 5,
  userId: 50,
  employeeCode: 'EMP-005',
  fullName: 'Nguyễn Văn An',
  skillsJson: '[]',
  canQc: false,
  hourlyCost: 80000,
  isActive: true,
  createdAt: '2024-01-10T00:00:00Z',
};

function Where() {
  const location = useLocation();
  return <output aria-label="địa chỉ">{location.pathname + location.search}</output>;
}

function renderView(entry: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[entry]}>
          <PayrollManagerView canApprove />
          <Where />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
  return userEvent.setup();
}

beforeEach(() => {
  configureSession('test-token', () => {});
  server.use(
    http.get('/api/v1/payroll/periods/by-month', () =>
      HttpResponse.json(envelope({ period, items: [item] })),
    ),
    http.get('/api/v1/employees', () => HttpResponse.json(envelope(page([employee])))),
    http.get('/api/v1/payroll/work-summary', () =>
      HttpResponse.json(
        envelope({
          employeeId: '5',
          month: monthKey,
          totalMinutes: 120,
          orderCount: 2,
          passedOrderCount: 1,
          entries: [],
        }),
      ),
    ),
  );
});

describe('PayrollManagerView', () => {
  it('opens the work summary of the employee named in ?employeeId=', async () => {
    renderView('/payroll?tab=monthly&employeeId=5');

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent('Nguyễn Văn An');
    expect(await screen.findByText('Không có công đoạn nào trong tháng này.')).toBeVisible();
  });

  it('drops employeeId from the address when the summary is closed', async () => {
    const user = renderView('/payroll?tab=monthly&employeeId=5');
    await screen.findByRole('dialog');

    await user.keyboard('{Escape}');

    await waitFor(() =>
      expect(screen.getByLabelText('địa chỉ')).not.toHaveTextContent('employeeId'),
    );
    expect(screen.getByLabelText('địa chỉ')).toHaveTextContent('tab=monthly');
  });

  it('opens no summary without the parameter and links names to the profile panel', async () => {
    renderView('/payroll?tab=monthly');

    const name = await screen.findByRole('link', { name: 'Nguyễn Văn An' });
    expect(name).toHaveAttribute('href', '/staff?tab=profiles&employeeId=5');
    expect(screen.queryByRole('dialog')).toBeNull();
  });
});

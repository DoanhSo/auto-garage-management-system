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
import { SchedulingPage } from './SchedulingPage';

vi.mock('../../../app/providers/AuthProvider', () => ({
  useAuth: () => ({ user: { role: 'ADMIN', fullName: 'Quản lý' } }),
}));

const policy = {
  passwordMinLength: 8,
  passwordMaxUtf8Bytes: 72,
  paginationDefaultSize: 20,
  paginationMaxSize: 100,
  paginationMaxPage: 1000,
  reportMaxPeriodSeconds: 31536000,
  garageTimeZone: 'Asia/Ho_Chi_Minh',
};

const bay = { id: 10, bayCode: 'BAY-01', bayName: 'Khoang 1', bayType: 'TWO_POST', isActive: true };

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

function renderPage(entry: string) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter initialEntries={[entry]}>
          <SchedulingPage />
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
    http.get('/api/v1/public/ui-policy', () => HttpResponse.json(envelope(policy))),
    http.get('/api/v1/service-bays', () => HttpResponse.json(envelope(page([bay])))),
    http.get('/api/v1/bay-reservations/active', () => HttpResponse.json(envelope([]))),
    http.get('/api/v1/employees', () => HttpResponse.json(envelope(page([employee])))),
    http.get('/api/v1/employee-shifts', () => HttpResponse.json(envelope(page([])))),
    http.get('/api/v1/employee-shifts/reconciliation', () => HttpResponse.json(envelope([]))),
    http.get('/api/v1/scheduling/on-shift-technicians', () => HttpResponse.json(envelope([]))),
    http.get('/api/v1/employee-shifts/pending-registrations', () =>
      HttpResponse.json(envelope(page([]))),
    ),
  );
});

describe('SchedulingPage address parameters', () => {
  it('pre-fills the shift filters from ?employeeId= and ?q=', async () => {
    renderPage('/scheduling?tab=shifts&employeeId=5&q=BAY-01');

    expect(await screen.findByLabelText('Tìm kiếm ca làm việc')).toHaveValue('BAY-01');
    await waitFor(() => expect(screen.getByLabelText('Kỹ thuật viên')).toHaveValue('5'));
  });

  it('leaves the shift filters empty without parameters', async () => {
    renderPage('/scheduling?tab=shifts');

    expect(await screen.findByLabelText('Tìm kiếm ca làm việc')).toHaveValue('');
    expect(screen.getByLabelText('Kỹ thuật viên')).toHaveValue('');
  });

  it('no longer has a technician team tab', async () => {
    renderPage('/scheduling?tab=shifts');

    await screen.findByLabelText('Tìm kiếm ca làm việc');
    expect(screen.queryByRole('button', { name: /Đội ngũ kỹ thuật viên/ })).toBeNull();
  });

  it('allows Admin to check-in an active shift using employee-shifts check-in endpoint', async () => {
    let checkedIn = false;
    const activeShift = {
      id: 15,
      employeeId: 5,
      serviceBayId: 10,
      shiftType: 'FULL_DAY',
      status: 'ACTIVE',
      attendanceStatus: 'SCHEDULED',
      startsAt: new Date(Date.now() - 3600000).toISOString(),
      endsAt: new Date(Date.now() + 3600000).toISOString(),
      notes: 'Test shift',
    };
    server.use(
      http.get('/api/v1/employee-shifts', () => HttpResponse.json(envelope(page([activeShift])))),
      http.post('/api/v1/employee-shifts/15/check-in', () => {
        checkedIn = true;
        return HttpResponse.json(envelope({ ...activeShift, attendanceStatus: 'CHECKED_IN' }));
      }),
    );
    const user = renderPage('/scheduling?tab=shifts');
    await user.click(await screen.findByRole('button', { name: /Danh sách/ }));

    const checkInBtn = await screen.findByRole('button', { name: /Vào ca/ });
    expect(checkInBtn).toBeVisible();
    await user.click(checkInBtn);

    await waitFor(() => expect(checkedIn).toBe(true));
  });

  it('shows the pending state only on the shift being punched', async () => {
    const shift = {
      id: 15,
      employeeId: 5,
      serviceBayId: 10,
      shiftType: 'FULL_DAY',
      status: 'ACTIVE',
      attendanceStatus: 'SCHEDULED',
      startsAt: new Date(Date.now() - 3600000).toISOString(),
      endsAt: new Date(Date.now() + 3600000).toISOString(),
      notes: 'Test shift',
    };
    let release = () => {};
    const held = new Promise<void>((resolve) => {
      release = resolve;
    });
    server.use(
      http.get('/api/v1/employee-shifts', () =>
        HttpResponse.json(envelope(page([shift, { ...shift, id: 16 }]))),
      ),
      http.post('/api/v1/employee-shifts/15/check-in', async () => {
        await held;
        return HttpResponse.json(envelope({ ...shift, attendanceStatus: 'CHECKED_IN' }));
      }),
    );
    const user = renderPage('/scheduling?tab=shifts');
    await user.click(await screen.findByRole('button', { name: /Danh sách/ }));

    const [first] = await screen.findAllByRole('button', { name: /Vào ca/ });
    await user.click(first);

    await waitFor(() => {
      const buttons = screen.getAllByRole('button', { name: /Vào ca/ });
      expect(buttons[0]).toBeDisabled();
      expect(buttons[1]).toBeEnabled();
    });
    release();
  });
});

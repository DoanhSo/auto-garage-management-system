import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it } from 'vitest';
import { ToastProvider } from '../../../app/providers/ToastProvider';
import { configureSession } from '../../../shared/api/client';
import { envelope, page } from '../../../test/fixtures';
import { server } from '../../../test/server';
import { ShiftRegistrationModal } from './ShiftRegistrationModal';

const bay = { id: 10, bayCode: 'BAY-01', bayName: 'Khoang 1', bayType: 'TWO_POST', isActive: true };

const pendingShift = {
  id: 101,
  employeeId: 5,
  startsAt: '2026-11-05T01:00:00Z',
  endsAt: '2026-11-05T10:00:00Z',
  shiftType: 'FULL_DAY',
  serviceBayId: 10,
  status: 'PENDING',
  createdBy: 1,
  createdAt: '2026-11-01T00:00:00Z',
};

const rejectedShift = {
  id: 102,
  employeeId: 5,
  startsAt: '2026-11-03T00:30:00Z',
  endsAt: '2026-11-03T04:30:00Z',
  shiftType: 'MORNING',
  serviceBayId: null,
  status: 'REJECTED',
  rejectionReason: 'Đủ quân số',
  createdBy: 1,
  createdAt: '2026-11-01T00:00:00Z',
};

interface RegisterBody {
  startsAt: string;
  endsAt: string;
  shiftType: string;
  serviceBayId?: number;
  notes?: string;
}

function renderModal() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <ShiftRegistrationModal open onClose={() => {}} />
      </ToastProvider>
    </QueryClientProvider>,
  );
  return userEvent.setup();
}

function localDay(offset: number) {
  const date = new Date();
  date.setDate(date.getDate() + offset);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

beforeEach(() => {
  configureSession('test-token', () => {});
  server.use(
    http.get('/api/v1/service-bays', () => HttpResponse.json(envelope(page([bay])))),
    http.get('/api/v1/employee-shifts/my', () => HttpResponse.json(envelope(page([])))),
    http.post('/api/v1/employee-shifts/register', () =>
      HttpResponse.json(envelope(pendingShift, 1001)),
    ),
  );
});

describe('ShiftRegistrationModal', () => {
  it('keeps the recent shift panel inside the form ahead of the action row', async () => {
    renderModal();
    const dialog = await screen.findByRole('dialog');
    const form = dialog.querySelector('form');
    expect(form).not.toBeNull();
    const section = within(dialog).getByLabelText('Ca đã đăng ký');
    expect(section.closest('form')).toBe(form);
    const submit = within(dialog).getByRole('button', { name: 'Gửi đăng ký ca' });
    expect(section.compareDocumentPosition(submit) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(within(dialog).getByLabelText(/Ngày làm việc/)).toBeVisible();
    expect(within(dialog).getByLabelText(/Khoang mong muốn/)).toBeVisible();
  });

  it('sends the chosen quick date, shift window, bay and notes', async () => {
    let body: RegisterBody | undefined;
    server.use(
      http.post('/api/v1/employee-shifts/register', async ({ request }) => {
        body = (await request.json()) as RegisterBody;
        return HttpResponse.json(envelope(pendingShift, 1001));
      }),
    );
    const user = renderModal();
    const dialog = await screen.findByRole('dialog');

    await user.click(within(dialog).getByRole('button', { name: 'Ngày mai' }));
    const dateInput = within(dialog).getByLabelText(/Ngày làm việc/) as HTMLInputElement;
    expect(dateInput.value).toBe(localDay(1));

    fireEvent.change(dateInput, { target: { value: '2030-01-15' } });
    await user.click(within(dialog).getByRole('radio', { name: /Ca sáng/ }));
    await user.click(within(dialog).getByLabelText(/Khoang mong muốn/));
    await user.click(await within(dialog).findByText('BAY-01 · Cầu nâng 2 trụ'));
    await user.type(within(dialog).getByLabelText('Lý do / ghi chú'), '  cần hỗ trợ  ');
    await user.click(within(dialog).getByRole('button', { name: 'Gửi đăng ký ca' }));

    await waitFor(() => expect(body).toBeTruthy());
    expect(body).toMatchObject({ shiftType: 'MORNING', serviceBayId: 10, notes: 'cần hỗ trợ' });
    expect(body?.startsAt).toBe(new Date('2030-01-15T07:30').toISOString());
    expect(body?.endsAt).toBe(new Date('2030-01-15T11:30').toISOString());
  });

  it('updates the summary strip for date, shift type and bay', async () => {
    const user = renderModal();
    const dialog = await screen.findByRole('dialog');
    const strip = dialog.querySelector('.info-strip') as HTMLElement;
    fireEvent.change(within(dialog).getByLabelText(/Ngày làm việc/), {
      target: { value: '2030-01-15' },
    });
    expect(strip).toHaveTextContent('Cả ngày');
    expect(strip).toHaveTextContent('08:00–17:00');
    expect(strip).toHaveTextContent('9,0 giờ');
    expect(strip).toHaveTextContent('Thợ cơ động');
    await user.click(within(dialog).getByRole('radio', { name: /Ca sáng/ }));
    expect(strip).toHaveTextContent('07:30–11:30');
    expect(strip).toHaveTextContent('4,0 giờ');
    await user.click(within(dialog).getByLabelText(/Khoang mong muốn/));
    await user.click(await within(dialog).findByText('BAY-01 · Cầu nâng 2 trụ'));
    expect(strip).toHaveTextContent('BAY-01');
  });

  it('lists recent shifts with full range, bay, status and rejection reason', async () => {
    server.use(
      http.get('/api/v1/employee-shifts/my', () =>
        HttpResponse.json(envelope(page([pendingShift, rejectedShift]))),
      ),
    );
    renderModal();
    const dialog = await screen.findByRole('dialog');
    const section = within(dialog).getByLabelText('Ca đã đăng ký');
    expect(await within(section).findByText('BAY-01')).toBeVisible();
    expect(within(section).getByText('Thợ cơ động')).toBeVisible();
    expect(within(section).getByText('Chờ duyệt')).toBeVisible();
    expect(within(section).getByText('Bị từ chối')).toBeVisible();
    expect(within(section).getByText('Lý do: Đủ quân số')).toBeVisible();
    expect(within(section).getByRole('columnheader', { name: 'Khoang trực' })).toBeVisible();
  });

  it('shows a tidy empty state when there are no registrations', async () => {
    renderModal();
    const dialog = await screen.findByRole('dialog');
    const section = within(dialog).getByLabelText('Ca đã đăng ký');
    expect(await within(section).findByText(/Bạn chưa đăng ký ca nào/)).toBeVisible();
  });
});

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router-dom';
import { http, HttpResponse } from 'msw';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../../../app/providers/ToastProvider';
import { configureSession } from '../../../shared/api/client';
import { admin, baseHandlers, envelope, page } from '../../../test/fixtures';
import { server } from '../../../test/server';
import { NotificationsPage } from './NotificationsPage';

const auth = vi.hoisted(() => ({ user: { id: '1', role: 'ADMIN' }, sessionKey: 'notice-test' }));
vi.mock('../../../app/providers/AuthProvider', () => ({ useAuth: () => auth }));
const notice = {
  id: 3,
  userId: 1,
  channel: 'WEB',
  subject: 'Lịch hẹn',
  payloadJson: '{"message":"Lịch hẹn ngày mai"}',
  status: 'DELIVERED',
  createdAt: '2026-10-22T01:00:00Z',
  readAt: null,
};
function app(mode = '') {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const router = createMemoryRouter([{ path: '/notifications', element: <NotificationsPage /> }], {
    initialEntries: ['/notifications' + mode],
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <RouterProvider router={router} />
      </ToastProvider>
    </QueryClientProvider>,
  );
  return userEvent.setup();
}
beforeEach(() => {
  auth.user = { id: String(admin.id), role: 'ADMIN' };
  configureSession('test-token', () => {});
  server.use(...baseHandlers());
});
describe('Notification permissions and actions', () => {
  it('does not offer a read marker for an admin row whose recipient is unknown', async () => {
    const received = vi.fn();
    server.use(
      http.get('/api/v1/notifications/admin', () => {
        received();
        return HttpResponse.json(envelope(page([{ ...notice, userId: null }])));
      }),
    );
    app('?mode=admin');
    await waitFor(() => expect(received).toHaveBeenCalled(), { timeout: 3000 });
    expect(
      await screen.findByRole('button', { name: /Lịch hẹn/ }, { timeout: 3000 }),
    ).toBeVisible();
    expect(screen.queryByRole('button', { name: /Đánh dấu đã đọc/ })).not.toBeInTheDocument();
  });
  it.each([null, 2])(
    'previews recipient %s without calling the private reader or changing their read state',
    async (recipient) => {
      const privateRead = vi.fn();
      server.use(
        http.get('/api/v1/notifications/admin', () =>
          HttpResponse.json(envelope(page([{ ...notice, userId: recipient }]))),
        ),
        http.get('/api/v1/notifications/3', () => {
          privateRead();
          return HttpResponse.json(envelope(null, 3002), { status: 403 });
        }),
        http.post('/api/v1/notifications/3/read', () => {
          privateRead();
          return HttpResponse.json(envelope(null, 3002), { status: 403 });
        }),
      );
      const user = app('?mode=admin');
      await user.click(await screen.findByRole('button', { name: /Lịch hẹn/ }));
      expect(
        await within(screen.getByRole('dialog')).findByText('Lịch hẹn ngày mai'),
      ).toBeVisible();
      expect(privateRead).not.toHaveBeenCalled();
    },
  );
  it('marks an owned notice once and refreshes its unread state', async () => {
    let readAt: string | null = null;
    let calls = 0;
    server.use(
      http.get('/api/v1/notifications', () =>
        HttpResponse.json(envelope(page([{ ...notice, readAt }]))),
      ),
      http.get('/api/v1/notifications/3', () => HttpResponse.json(envelope({ ...notice, readAt }))),
      http.post('/api/v1/notifications/3/read', ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer test-token');
        calls++;
        readAt = '2026-10-22T02:00:00Z';
        return HttpResponse.json(envelope({ ...notice, readAt }, 1002));
      }),
    );
    const user = app();
    await user.click(await screen.findByRole('button', { name: /Đánh dấu đã đọc #3/ }));
    await waitFor(() =>
      expect(screen.queryByRole('button', { name: /Đánh dấu đã đọc #3/ })).not.toBeInTheDocument(),
    );
    await user.click(screen.getByRole('button', { name: /Lịch hẹn/ }));
    expect(await within(screen.getByRole('dialog')).findByText('Đã đọc')).toBeVisible();
    expect(calls).toBe(1);
  });
  it('validates the broadcast and sends the selected roles and plain text', async () => {
    const send = vi.fn();
    server.use(
      http.post('/api/v1/notifications/broadcast', async ({ request }) => {
        expect(request.headers.get('Authorization')).toBe('Bearer test-token');
        send(await request.json());
        return HttpResponse.json(envelope({ recipientCount: 2, targetRole: 'CUSTOMER' }, 1001));
      }),
    );
    const user = app();
    await user.click(await screen.findByRole('button', { name: 'Phát thông báo' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Gửi thông báo' }));
    expect(await within(dialog).findByText('Nhập tiêu đề thông báo.')).toBeVisible();
    expect(within(dialog).getByText('Nhập nội dung thông báo.')).toBeVisible();
    expect(send).not.toHaveBeenCalled();
    await user.click(within(dialog).getByRole('checkbox', { name: 'Khách hàng' }));
    await user.type(within(dialog).getByLabelText(/Tiêu đề/), ' Lịch nghỉ ');
    await user.type(within(dialog).getByLabelText(/^Nội dung/), ' Gara nghỉ ngày mai. ');
    await user.click(within(dialog).getByRole('button', { name: 'Gửi thông báo' }));
    await waitFor(() =>
      expect(send).toHaveBeenCalledWith({
        targetRoles: ['CUSTOMER'],
        subject: 'Lịch nghỉ',
        payloadJson: '{"message":"Gara nghỉ ngày mai."}',
      }),
    );
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
  });
  it('keeps a failed recall open and retries the same notice', async () => {
    let calls = 0;
    let removed = false;
    server.use(
      http.get('/api/v1/notifications', () =>
        HttpResponse.json(envelope(page(removed ? [] : [notice]))),
      ),
      http.delete('/api/v1/notifications/3', () => {
        calls++;
        if (calls === 1) return HttpResponse.json(envelope(null, 3002), { status: 403 });
        removed = true;
        return HttpResponse.json(envelope(null, 1003));
      }),
    );
    const user = app();
    await user.click(await screen.findByRole('button', { name: 'Thu hồi thông báo #3' }));
    const dialog = screen.getByRole('alertdialog');
    await user.click(within(dialog).getByRole('button', { name: 'Thu hồi thông báo' }));
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('quyền');
    expect(dialog).toBeVisible();
    await user.click(within(dialog).getByRole('button', { name: 'Thu hồi thông báo' }));
    await waitFor(() => expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument());
    expect(await screen.findByText('Chưa có thông báo')).toBeVisible();
    expect(calls).toBe(2);
  });
  it('hides broadcast, management and recall controls for a customer', async () => {
    auth.user = { id: '1', role: 'CUSTOMER' };
    app('?mode=admin');
    expect(await screen.findByRole('heading', { name: 'Thông báo' })).toBeVisible();
    expect(screen.queryByRole('button', { name: 'Phát thông báo' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Toàn hệ thống' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Thu hồi/ })).not.toBeInTheDocument();
  });
});

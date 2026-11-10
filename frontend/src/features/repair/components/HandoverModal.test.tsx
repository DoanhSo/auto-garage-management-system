import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ToastProvider } from '../../../app/providers/ToastProvider';
import { configureSession } from '../../../shared/api/client';
import { envelope, page } from '../../../test/fixtures';
import { server } from '../../../test/server';
import type { RepairOrder } from '../types/repairTypes';
import { HandoverModal } from './HandoverModal';
import { RepairOrderDetailModal } from './RepairOrderDetailModal';

vi.mock('../../../app/providers/AuthProvider', () => ({
  useAuth: () => ({
    user: { id: 1, username: 'admin', fullName: 'Quản trị', role: 'ADMIN' },
  }),
}));

const order = { id: '5', orderCode: 'RO-5', odometerIn: 12000 } as RepairOrder;

const readiness = (overrides: Record<string, unknown> = {}) => ({
  allowed: true,
  reasons: [],
  invoiceId: 9,
  invoiceBalance: 0,
  customerDebt: 0,
  creditLimit: 0,
  minimumPayment: 0,
  overdueInvoiceCodes: [],
  ...overrides,
});

const invoice = {
  id: '9',
  invoiceCode: 'INV-9',
  repairOrderId: '5',
  customerSnapshot: '{}',
  vehicleSnapshot: '{}',
  status: 'UNPAID',
  items: [
    {
      id: '71',
      lineNo: 1,
      itemType: 'SERVICE',
      itemName: 'Thay má phanh',
      quantity: '1',
      unitPrice: '100000',
      unitCost: '0',
      lineTotal: '100000',
      warrantyDays: 0,
      warrantyKm: 0,
      warrantyTerms: null,
    },
    {
      id: '72',
      lineNo: 2,
      itemType: 'DISCOUNT',
      itemName: 'Giảm giá',
      quantity: '1',
      unitPrice: '0',
      unitCost: '0',
      lineTotal: '0',
      warrantyDays: 0,
      warrantyKm: 0,
      warrantyTerms: null,
    },
  ],
};

function renderModal() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter>
          <HandoverModal order={order} open onClose={() => {}} />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  );
  return userEvent.setup();
}

beforeEach(() => {
  configureSession('test-token', () => {});
  server.use(
    http.get('/api/v1/billing/invoices/order/5', () => HttpResponse.json(envelope(invoice))),
  );
});

describe('HandoverModal', () => {
  it('shows the amount still to collect and disables handover while the credit rule blocks it', async () => {
    server.use(
      http.get('/api/v1/billing/invoices/order/5/handover-readiness', () =>
        HttpResponse.json(
          envelope(
            readiness({
              allowed: false,
              reasons: ['Công nợ của khách là 4.000.000 đ, vượt hạn mức 2.000.000 đ.'],
              customerDebt: 4000000,
              creditLimit: 2000000,
              minimumPayment: 2000000,
              invoiceBalance: 4000000,
            }),
          ),
        ),
      ),
    );
    renderModal();
    const dialog = await screen.findByRole('dialog');

    expect(await within(dialog).findByText('Chưa đủ điều kiện bàn giao')).toBeVisible();
    expect(within(dialog).getByText(/vượt hạn mức 2\.000\.000 đ/)).toBeVisible();
    expect(within(dialog).getByText('Cần thu tối thiểu')).toBeVisible();
    expect(within(dialog).getByRole('button', { name: 'Xác nhận bàn giao xe' })).toBeDisabled();
    const payLink = within(dialog).getByRole('link', { name: /thu tiền/ });
    expect(payLink).toBeVisible();
    await waitFor(() =>
      expect(payLink).toHaveAttribute('href', '/billing/invoices?query=INV-9'),
    );
  });

  it('shows handover and invoice navigation buttons on an INVOICED repair order with unpaid balance', async () => {
    const invoicedOrder = {
      id: 5,
      orderCode: 'RO-5',
      customerId: 1,
      vehicleId: 2,
      orderType: 'NORMAL',
      status: 'INVOICED',
      odometerIn: 12000,
      receivedBy: 1,
      receivedAt: '2026-11-10T01:00:00Z',
      createdAt: '2026-11-10T01:00:00Z',
      updatedAt: '2026-11-10T01:00:00Z',
    };
    const unpaidInvoice = {
      ...invoice,
      totalAmount: '100000',
      paidAmount: '0',
      balanceAmount: '100000',
    };
    server.use(
      http.get('/api/v1/repair-orders/5', () => HttpResponse.json(envelope(invoicedOrder))),
      http.get('/api/v1/repair-orders/5/tasks', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/estimates/repair-order/5', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/quality-checks/order/5', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/vehicle-checks', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/vehicles/2', () =>
        HttpResponse.json(
          envelope({
            id: 2,
            customerId: 1,
            customerName: 'Nguyễn Văn A',
            licensePlate: '30A-123.45',
            brand: 'Toyota',
            model: 'Vios',
          }),
        ),
      ),
      http.get('/api/v1/scheduling/employees', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/attachments', () => HttpResponse.json(envelope([]))),
      http.get('/api/v1/billing/invoices', () =>
        HttpResponse.json(envelope(page([unpaidInvoice]))),
      ),
      http.get('/api/v1/billing/invoices/order/5/handover-readiness', () =>
        HttpResponse.json(
          envelope(
            readiness({
              allowed: false,
              reasons: ['Khách chưa có hạn mức nợ nên phải thanh toán đủ.'],
              invoiceBalance: 100000,
              customerDebt: 100000,
              minimumPayment: 100000,
            }),
          ),
        ),
      ),
    );

    function Locationprobe() {
      const loc = useLocation();
      return <div data-testid="location">{loc.pathname + loc.search}</div>;
    }

    const client = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const user = userEvent.setup();
    render(
      <QueryClientProvider client={client}>
        <ToastProvider>
          <MemoryRouter initialEntries={['/repair-orders']}>
            <Routes>
              <Route
                path="*"
                element={
                  <>
                    <Locationprobe />
                    <RepairOrderDetailModal orderId="5" open onClose={() => {}} />
                  </>
                }
              />
            </Routes>
          </MemoryRouter>
        </ToastProvider>
      </QueryClientProvider>,
    );

    const dialog = await screen.findByRole('dialog');
    const tabGroup = await within(dialog).findByRole('group', {
      name: 'Nội dung lệnh sửa chữa',
    });
    await user.click(within(tabGroup).getByRole('button', { name: 'Bàn giao xe' }));

    expect(await within(dialog).findByLabelText('Trạng thái hóa đơn')).toHaveTextContent(
      /Hóa đơn INV-9 · còn nợ/,
    );
    const handoverBtn = within(dialog).getByRole('button', { name: /Tiến hành bàn giao xe/ });
    expect(handoverBtn).toBeEnabled();

    const goToInvoiceBtns = within(dialog).getAllByRole('button', {
      name: /Đến hóa đơn thanh toán/,
    });
    expect(goToInvoiceBtns.length).toBeGreaterThanOrEqual(1);
    await user.click(goToInvoiceBtns[0]);
    expect(screen.getByTestId('location')).toHaveTextContent('/billing/invoices?query=INV-9');
  });

  it('allows handover when the debt is within the credit limit', async () => {
    server.use(
      http.get('/api/v1/billing/invoices/order/5/handover-readiness', () =>
        HttpResponse.json(envelope(readiness({ creditLimit: 9000000, customerDebt: 4000000 }))),
      ),
    );
    renderModal();
    const dialog = await screen.findByRole('dialog');

    expect(await within(dialog).findByText('Đủ điều kiện bàn giao')).toBeVisible();
    expect(within(dialog).getByRole('button', { name: 'Xác nhận bàn giao xe' })).toBeEnabled();
  });

  it('lists only service and part lines and saves a warranty added before handover', async () => {
    let body: unknown;
    server.use(
      http.get('/api/v1/billing/invoices/order/5/handover-readiness', () =>
        HttpResponse.json(envelope(readiness())),
      ),
      http.patch('/api/v1/billing/invoices/9/items/71/warranty', async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(envelope(invoice));
      }),
    );
    const user = renderModal();
    const dialog = await screen.findByRole('dialog');

    expect(await within(dialog).findByText('Thay má phanh')).toBeVisible();
    expect(within(dialog).queryByText('Giảm giá')).toBeNull();
    expect(within(dialog).getByText('Chưa có bảo hành')).toBeVisible();

    const save = within(dialog).getByRole('button', { name: 'Lưu' });
    expect(save).toBeDisabled();
    const days = within(dialog).getByLabelText('Số ngày');
    await user.clear(days);
    await user.type(days, '90');
    await user.type(within(dialog).getByLabelText('Số km'), '5000');
    await user.click(save);

    await waitFor(() =>
      expect(body).toEqual({ warrantyDays: 90, warrantyKm: 5000, warrantyTerms: '' }),
    );
  });

  it('does not request invoice-by-order when readiness reports no invoice and links to issueOrderId', async () => {
    const invoiceByOrderSpy = vi.fn(() =>
      HttpResponse.json({ code: 2002, message: 'Resource not found', success: false }, { status: 404 }),
    );
    server.use(
      http.get('/api/v1/billing/invoices/order/5', invoiceByOrderSpy),
      http.get('/api/v1/billing/invoices/order/5/handover-readiness', () =>
        HttpResponse.json(
          envelope(
            readiness({
              allowed: false,
              invoiceId: null,
              reasons: ['Phiếu chưa có hóa đơn còn hiệu lực. Hãy lập hóa đơn trước khi bàn giao.'],
            }),
          ),
        ),
      ),
    );
    renderModal();
    const dialog = await screen.findByRole('dialog');

    expect(
      await within(dialog).findByText(
        /Phiếu chưa có hóa đơn còn hiệu lực\. Hãy lập hóa đơn trước khi bàn giao\./,
      ),
    ).toBeVisible();
    const issueLink = within(dialog).getByRole('link', {
      name: /Mở Hóa đơn dịch vụ để lập hóa đơn & thu tiền/,
    });
    expect(issueLink).toHaveAttribute('href', '/billing/invoices?issueOrderId=5');
    expect(invoiceByOrderSpy).not.toHaveBeenCalled();
  });
});

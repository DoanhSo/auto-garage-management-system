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
import type { RepairOrder } from '../types/repairTypes';
import { HandoverModal } from './HandoverModal';

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
    expect(within(dialog).getByRole('link', { name: /thu tiền/ })).toBeVisible();
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
});

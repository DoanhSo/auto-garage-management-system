import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  Invoice,
  InvoiceIssueInput,
  InvoiceVoidInput,
  Payment,
  PaymentReceiveInput,
  SupplierPaymentInput,
  CustomerDebtBalance,
  SupplierDebtBalance,
  InvoiceStatus,
  PaymentDirection,
} from '../types/billing';

export const billingApi = {
  // Invoices
  listInvoices: (params: {
    status?: InvoiceStatus;
    customerId?: string | number;
    orderId?: string | number;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<Invoice>>(
      '/billing/invoices' +
        queryString({
          status: params.status,
          customerId: params.customerId,
          orderId: params.orderId,
          query: params.query,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),

  getInvoice: (id: string | number) =>
    request<Invoice>(`/billing/invoices/${id}`),

  getInvoiceByOrder: (orderId: string | number) =>
    request<Invoice>(`/billing/invoices/order/${orderId}`),

  issueInvoice: (data: InvoiceIssueInput) =>
    request<Invoice>('/billing/invoices', {
      method: 'POST',
      body: data,
    }),

  voidInvoice: (id: string | number, data: InvoiceVoidInput) =>
    request<null>(`/billing/invoices/${id}/void`, {
      method: 'POST',
      body: data,
    }),

  // Payments
  listPayments: (params: {
    direction?: PaymentDirection;
    invoiceId?: string | number;
    documentId?: string | number;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<Payment>>(
      '/billing/payments' +
        queryString({
          direction: params.direction,
          invoiceId: params.invoiceId,
          documentId: params.documentId,
          query: params.query,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),

  receiveCustomerPayment: (data: PaymentReceiveInput) =>
    request<Payment>('/billing/payments/customer', {
      method: 'POST',
      body: data,
    }),

  paySupplier: (data: SupplierPaymentInput) =>
    request<Payment>('/billing/payments/supplier', {
      method: 'POST',
      body: data,
    }),

  getPaymentsForInvoice: (invoiceId: string | number) =>
    request<Payment[]>(`/billing/payments/invoice/${invoiceId}`),

  getPaymentsForDocument: (documentId: string | number) =>
    request<Payment[]>(`/billing/payments/document/${documentId}`),

  // Debts
  getCustomerDebts: (params: {
    customerId?: string | number;
    hasBalance?: boolean;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<CustomerDebtBalance>>(
      '/billing/debts/customers' +
        queryString({
          customerId: params.customerId,
          hasBalance: params.hasBalance,
          query: params.query,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),

  getSupplierDebts: (params: {
    hasBalance?: boolean;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<SupplierDebtBalance>>(
      '/billing/debts/suppliers' +
        queryString({
          hasBalance: params.hasBalance,
          query: params.query,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),
};

import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import { customerApi } from '../../customer/api/customerApi';
import type { Supplier } from '../../inventory/types/inventory';
import type {
  HandoverReadiness,
  Invoice,
  InvoiceItemWarrantyInput,
  InvoiceIssueInput,
  InvoiceReadiness,
  InvoiceVoidInput,
  Payment,
  PaymentReceiveInput,
  SupplierPaymentInput,
  CustomerDebtBalance,
  InvoiceBalanceItem,
  SupplierDebtBalance,
  InvoiceStatus,
  PaymentDirection,
  CustomerSnapshot,
  VehicleSnapshot,
} from '../types/billing';

/** Snapshots are stored and returned as JSON text; empty values were written as the string "null". */
function parseSnapshot<T extends object>(raw: unknown): T {
  let value: unknown = raw;
  if (typeof raw === 'string') {
    try {
      value = JSON.parse(raw);
    } catch {
      value = {};
    }
  }
  if (!value || typeof value !== 'object') return {} as T;
  return Object.fromEntries(
    Object.entries(value).filter(([, v]) => v !== null && v !== 'null' && v !== ''),
  ) as T;
}

function normalizeInvoice(invoice: Invoice): Invoice {
  return {
    ...invoice,
    customerSnapshot: parseSnapshot<CustomerSnapshot>(invoice.customerSnapshot),
    vehicleSnapshot: parseSnapshot<VehicleSnapshot>(invoice.vehicleSnapshot),
  };
}

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
    ).then((page) => ({ ...page, content: page.content.map(normalizeInvoice) })),

  getInvoice: (id: string | number) =>
    request<Invoice>(`/billing/invoices/${id}`).then(normalizeInvoice),

  getInvoiceByOrder: (orderId: string | number) =>
    request<Invoice>(`/billing/invoices/order/${orderId}`).then(normalizeInvoice),

  getInvoiceReadiness: (orderId: string | number) =>
    request<InvoiceReadiness>(`/billing/invoices/order/${orderId}/readiness`),

  getHandoverReadiness: (orderId: string | number) =>
    request<HandoverReadiness>(`/billing/invoices/order/${orderId}/handover-readiness`),

  setItemWarranty: (
    invoiceId: string | number,
    itemId: string | number,
    data: InvoiceItemWarrantyInput,
  ) =>
    request<Invoice>(`/billing/invoices/${invoiceId}/items/${itemId}/warranty`, {
      method: 'PATCH',
      body: data,
    }).then(normalizeInvoice),

  issueInvoice: (data: InvoiceIssueInput) =>
    request<Invoice>('/billing/invoices/issue', {
      method: 'POST',
      body: data,
    }).then(normalizeInvoice),

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
    request<Payment>('/billing/payments/receive', {
      method: 'POST',
      body: data,
    }),

  suggestPaymentCode: (direction: PaymentDirection) =>
    request<string>('/billing/payments/suggest-code' + queryString({ direction })),

  paySupplier: (data: SupplierPaymentInput) =>
    request<Payment>('/billing/payments/pay-supplier', {
      method: 'POST',
      body: data,
    }),

  getPaymentsForInvoice: (invoiceId: string | number) =>
    request<Payment[]>(`/billing/payments/invoice/${invoiceId}`),

  getPaymentsForDocument: (documentId: string | number) =>
    request<Payment[]>(`/billing/payments/document/${documentId}`),

  // Debts
  // Backend chỉ có sổ theo hóa đơn (v_invoice_balances) nên công nợ khách hàng được gộp theo customerId.
  getCustomerDebts: async (params: {
    customerId?: string | number;
    hasBalance?: boolean;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<Page<CustomerDebtBalance>> => {
    const [customers, invoices] = await Promise.all([
      // Thủ kho không có quyền xem danh mục khách hàng: bỏ qua thay vì làm sập cả sổ nợ.
      customerApi.list({ isActive: true }).catch(() => []),
      request<Page<InvoiceBalanceRow>>(
        '/billing/debts/invoices' +
          queryString({
            customerId: params.customerId,
            page: 0,
            size: DEBT_FETCH_SIZE,
            sortBy: 'issuedAt',
            sortDir: 'desc',
          }),
      ),
    ]);
    const totals = new Map<
      string,
      { invoiced: number; paid: number; balance: number; items: InvoiceBalanceItem[] }
    >();
    for (const invoice of invoices.content) {
      const sum = totals.get(invoice.customerId) ?? { invoiced: 0, paid: 0, balance: 0, items: [] };
      sum.invoiced += Number(invoice.totalAmount ?? 0);
      sum.paid += Number(invoice.paidAmount ?? 0);
      sum.balance += Number(invoice.balanceAmount ?? 0);
      sum.items.push({
        invoiceId: invoice.invoiceId,
        invoiceCode: invoice.invoiceCode,
        repairOrderId: invoice.repairOrderId ?? null,
        issuedAt: invoice.issuedAt ?? null,
        dueDate: invoice.dueDate ?? null,
        status: invoice.status ?? '',
        totalAmount: String(invoice.totalAmount ?? 0),
        paidAmount: String(invoice.paidAmount ?? 0),
        balanceAmount: String(invoice.balanceAmount ?? 0),
      });
      totals.set(invoice.customerId, sum);
    }
    const today = new Date().toISOString().slice(0, 10);
    const keyword = params.query?.trim().toLowerCase();
    let rows: CustomerDebtBalance[] = customers
      .filter((customer) => !params.customerId || customer.id === String(params.customerId))
      .map((customer) => {
        const sum = totals.get(customer.id) ?? { invoiced: 0, paid: 0, balance: 0, items: [] };
        const open = sum.items.filter((item) => Number(item.balanceAmount) > 0);
        const overdue = open.filter((item) => item.dueDate && item.dueDate.slice(0, 10) < today);
        const dueDates = open
          .map((item) => item.dueDate)
          .filter((d): d is string => !!d)
          .sort();
        return {
          customerId: customer.id,
          customerCode: customer.customerCode,
          customerName: customer.fullName,
          phone: customer.phone,
          creditLimit: customer.creditLimit,
          paymentTermDays: customer.paymentTermDays,
          totalInvoiced: String(sum.invoiced),
          totalPaid: String(sum.paid),
          balanceAmount: String(sum.balance),
          invoiceCount: open.length,
          overdueCount: overdue.length,
          overdueAmount: String(overdue.reduce((s, i) => s + Number(i.balanceAmount), 0)),
          nearestDueDate: dueDates[0] ?? null,
          invoices: sum.items,
        };
      })
      .filter((row) => !params.hasBalance || Number(row.balanceAmount) > 0)
      .filter(
        (row) =>
          !keyword ||
          row.customerName.toLowerCase().includes(keyword) ||
          row.customerCode.toLowerCase().includes(keyword) ||
          (row.phone ?? '').includes(keyword),
      );
    const sortKey = (params.sortBy ?? 'balanceAmount') as keyof CustomerDebtBalance;
    const direction = params.sortDir === 'asc' ? 1 : -1;
    rows = rows.sort((a, b) => {
      const left = a[sortKey];
      const right = b[sortKey];
      return typeof left === 'string' && Number.isNaN(Number(left))
        ? direction * left.localeCompare(String(right), 'vi')
        : direction * (Number(left) - Number(right));
    });
    return toPage(rows, params.page ?? 0, params.size ?? 20);
  },

  getSupplierDebts: async (params: {
    hasBalance?: boolean;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }): Promise<Page<SupplierDebtBalance>> => {
    const [balances, suppliers] = await Promise.all([
      request<Page<SupplierBalanceRow>>(
        '/billing/debts/suppliers' +
          queryString({
            hasBalance: params.hasBalance,
            query: params.query,
            page: params.page ?? 0,
            size: params.size ?? 20,
            sortBy: params.sortBy ?? 'supplierName',
            sortDir: params.sortDir ?? 'asc',
          }),
      ),
      request<Page<Supplier>>(
        '/inventory/suppliers' + queryString({ page: 0, size: DEBT_FETCH_SIZE }),
      ),
    ]);
    const directory = new Map(suppliers.content.map((supplier) => [supplier.id, supplier]));
    return {
      ...balances,
      content: balances.content.map((row) => ({
        supplierId: row.supplierId,
        supplierCode: row.supplierCode,
        supplierName: row.supplierName,
        phone: directory.get(row.supplierId)?.phone ?? null,
        paymentTermDays: directory.get(row.supplierId)?.paymentTermDays ?? 0,
        totalPurchased: String(row.purchaseAmount ?? 0),
        totalPaid: String(row.paidAmount ?? 0),
        balanceAmount: String(row.balanceAmount ?? 0),
      })),
    };
  },
};

const DEBT_FETCH_SIZE = 100;

interface InvoiceBalanceRow {
  invoiceId: string;
  invoiceCode: string;
  repairOrderId?: string | null;
  issuedAt?: string | null;
  dueDate?: string | null;
  status?: string;
  customerId: string;
  totalAmount: string | number;
  paidAmount: string | number;
  balanceAmount: string | number;
}

interface SupplierBalanceRow {
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  purchaseAmount: string | number | null;
  paidAmount: string | number | null;
  balanceAmount: string | number | null;
}

function toPage<T>(rows: T[], page: number, size: number): Page<T> {
  return {
    content: rows.slice(page * size, page * size + size),
    totalElements: String(rows.length),
    totalPages: Math.max(1, Math.ceil(rows.length / size)),
    number: page,
    size,
  };
}

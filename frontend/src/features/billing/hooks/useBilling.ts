import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '../api/billingApi';
import type {
  InvoiceItemWarrantyInput,
  InvoiceIssueInput,
  InvoiceVoidInput,
  PaymentReceiveInput,
  SupplierPaymentInput,
  InvoiceStatus,
  PaymentDirection,
} from '../types/billing';

export function useInvoicesQuery(
  params: {
    status?: InvoiceStatus;
    customerId?: string | number;
    orderId?: string | number;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  },
  enabled = true,
) {
  return useQuery({
    queryKey: ['invoices', params],
    queryFn: () => billingApi.listInvoices(params),
    enabled,
  });
}

export function useInvoiceQuery(id: string | number) {
  return useQuery({
    queryKey: ['invoice', id],
    queryFn: () => billingApi.getInvoice(id),
    enabled: !!id,
  });
}

export function useInvoiceReadinessQuery(orderId: string | number | null, enabled = true) {
  return useQuery({
    queryKey: ['invoice-readiness', orderId],
    queryFn: () => billingApi.getInvoiceReadiness(orderId as string | number),
    enabled: !!orderId && enabled,
  });
}

/** Payment rule for handing the vehicle over; refetched whenever the handover dialog opens. */
export function useHandoverReadinessQuery(orderId: string | number | null, enabled = true) {
  return useQuery({
    queryKey: ['handover-readiness', orderId],
    queryFn: () => billingApi.getHandoverReadiness(orderId as string | number),
    enabled: !!orderId && enabled,
    staleTime: 0,
  });
}

export function useInvoiceByOrderQuery(orderId: string | number | null, enabled = true) {
  return useQuery({
    queryKey: ['invoice-by-order', orderId],
    queryFn: () => billingApi.getInvoiceByOrder(orderId as string | number),
    enabled: !!orderId && enabled,
    staleTime: 0,
  });
}

export function useSetItemWarrantyMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      invoiceId,
      itemId,
      data,
    }: {
      invoiceId: string | number;
      itemId: string | number;
      data: InvoiceItemWarrantyInput;
    }) => billingApi.setItemWarranty(invoiceId, itemId, data),
    onSuccess: (invoice) => {
      queryClient.invalidateQueries({
        queryKey: ['invoice-by-order', Number(invoice.repairOrderId)],
      });
      queryClient.invalidateQueries({ queryKey: ['invoice-by-order', invoice.repairOrderId] });
      queryClient.invalidateQueries({ queryKey: ['invoice', invoice.id] });
    },
  });
}

export function useIssueInvoiceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: InvoiceIssueInput) => billingApi.issueInvoice(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      // Issuing moves the repair order to INVOICED.
      queryClient.invalidateQueries({ queryKey: ['repair-orders'] });
      queryClient.invalidateQueries({ queryKey: ['repair-order'] });
    },
  });
}

export function useVoidInvoiceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: InvoiceVoidInput }) =>
      billingApi.voidInvoice(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      queryClient.invalidateQueries({ queryKey: ['invoice', id] });
      // Voiding sends the repair order back to COMPLETED so it can be invoiced again.
      queryClient.invalidateQueries({ queryKey: ['repair-orders'] });
      queryClient.invalidateQueries({ queryKey: ['repair-order'] });
    },
  });
}

export function usePaymentsQuery(params: {
  direction?: PaymentDirection;
  invoiceId?: string | number;
  documentId?: string | number;
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['payments', params],
    queryFn: () => billingApi.listPayments(params),
  });
}

export function useReceivePaymentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PaymentReceiveInput) => billingApi.receiveCustomerPayment(data),
    onSuccess: (_, { invoiceId }) => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
      queryClient.invalidateQueries({ queryKey: ['invoice', invoiceId] });
      queryClient.invalidateQueries({ queryKey: ['customer-debts'] });
      queryClient.invalidateQueries({ queryKey: ['handover-readiness'] });
      queryClient.invalidateQueries({ queryKey: ['invoice-by-order'] });
    },
  });
}

/** Lấy mã giao dịch gợi ý mới (khi mở form hoặc bấm "Sinh lại"). */
export function useSuggestPaymentCodeMutation() {
  return useMutation({
    mutationFn: (direction: PaymentDirection) => billingApi.suggestPaymentCode(direction),
  });
}

export function usePaySupplierMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: SupplierPaymentInput) => billingApi.paySupplier(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['payments'] });
      queryClient.invalidateQueries({ queryKey: ['inventory-documents'] });
      queryClient.invalidateQueries({ queryKey: ['supplier-debts'] });
    },
  });
}

export function useCustomerDebtsQuery(params: {
  customerId?: string | number;
  hasBalance?: boolean;
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['customer-debts', params],
    queryFn: () => billingApi.getCustomerDebts(params),
  });
}

export function useSupplierDebtsQuery(params: {
  hasBalance?: boolean;
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['supplier-debts', params],
    queryFn: () => billingApi.getSupplierDebts(params),
  });
}

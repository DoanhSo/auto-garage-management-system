import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { billingApi } from '../api/billingApi';
import type {
  InvoiceIssueInput,
  InvoiceVoidInput,
  PaymentReceiveInput,
  SupplierPaymentInput,
  InvoiceStatus,
  PaymentDirection,
} from '../types/billing';

export function useInvoicesQuery(params: {
  status?: InvoiceStatus;
  customerId?: string | number;
  orderId?: string | number;
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['invoices', params],
    queryFn: () => billingApi.listInvoices(params),
  });
}

export function useInvoiceQuery(id: string | number) {
  return useQuery({
    queryKey: ['invoice', id],
    queryFn: () => billingApi.getInvoice(id),
    enabled: !!id,
  });
}

export function useIssueInvoiceMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: InvoiceIssueInput) => billingApi.issueInvoice(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['invoices'] });
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
    },
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

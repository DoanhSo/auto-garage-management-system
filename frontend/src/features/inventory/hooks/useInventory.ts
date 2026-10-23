import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { inventoryApi } from '../api/inventoryApi';
import type {
  SupplierCreateInput,
  SupplierUpdateInput,
  PartCreateInput,
  PartUpdateInput,
  InventoryDocumentCreateInput,
  DocumentType,
  DocumentStatus,
} from '../types/inventory';

export function useSuppliersQuery(params: {
  query?: string;
  active?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['suppliers', params],
    queryFn: () => inventoryApi.listSuppliers(params),
  });
}

export function useSupplierQuery(id: string | number) {
  return useQuery({
    queryKey: ['supplier', id],
    queryFn: () => inventoryApi.getSupplier(id),
    enabled: !!id,
  });
}

export function useCreateSupplierMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: SupplierCreateInput) => inventoryApi.createSupplier(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    },
  });
}

export function useUpdateSupplierMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: SupplierUpdateInput }) =>
      inventoryApi.updateSupplier(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      queryClient.invalidateQueries({ queryKey: ['supplier', id] });
    },
  });
}

export function useDeactivateSupplierMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string | number) => inventoryApi.deactivateSupplier(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['suppliers'] });
    },
  });
}

export function usePartsQuery(params: {
  query?: string;
  supplierId?: string | number;
  active?: boolean;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['parts', params],
    queryFn: () => inventoryApi.listParts(params),
  });
}

export function useLowStockPartsQuery() {
  return useQuery({
    queryKey: ['parts', 'low-stock'],
    queryFn: () => inventoryApi.getLowStockParts(),
  });
}

export function usePartQuery(id: string | number) {
  return useQuery({
    queryKey: ['part', id],
    queryFn: () => inventoryApi.getPart(id),
    enabled: !!id,
  });
}

export function useCreatePartMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: PartCreateInput) => inventoryApi.createPart(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['parts'] });
    },
  });
}

export function useUpdatePartMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string | number; data: PartUpdateInput }) =>
      inventoryApi.updatePart(id, data),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['parts'] });
      queryClient.invalidateQueries({ queryKey: ['part', id] });
    },
  });
}

export function useDeactivatePartMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string | number) => inventoryApi.deactivatePart(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['parts'] });
    },
  });
}

export function useDocumentsQuery(params: {
  type?: DocumentType;
  status?: DocumentStatus;
  supplierId?: string | number;
  orderId?: string | number;
  query?: string;
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: string;
}) {
  return useQuery({
    queryKey: ['inventory-documents', params],
    queryFn: () => inventoryApi.listDocuments(params),
  });
}

export function useDocumentQuery(id: string | number) {
  return useQuery({
    queryKey: ['inventory-document', id],
    queryFn: () => inventoryApi.getDocument(id),
    enabled: !!id,
  });
}

export function useCreateDocumentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: InventoryDocumentCreateInput) => inventoryApi.createDocument(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['inventory-documents'] });
    },
  });
}

export function usePostDocumentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, requestKey }: { id: string | number; requestKey?: string }) =>
      inventoryApi.postDocument(id, requestKey),
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries({ queryKey: ['inventory-documents'] });
      queryClient.invalidateQueries({ queryKey: ['inventory-document', id] });
      queryClient.invalidateQueries({ queryKey: ['parts'] });
    },
  });
}

export function useCancelDocumentMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string | number) => inventoryApi.cancelDocument(id),
    onSuccess: (_, id) => {
      queryClient.invalidateQueries({ queryKey: ['inventory-documents'] });
      queryClient.invalidateQueries({ queryKey: ['inventory-document', id] });
    },
  });
}

export function useReconciliationQuery(partId?: string | number) {
  return useQuery({
    queryKey: ['inventory-reconciliation', partId],
    queryFn: () =>
      partId ? inventoryApi.reconcilePart(partId).then((r) => [r]) : inventoryApi.reconcileAllParts(),
  });
}

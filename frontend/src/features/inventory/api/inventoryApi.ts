import { request, queryString } from '../../../shared/api/client';
import type { Page } from '../../../shared/types/contracts';
import type {
  Supplier,
  SupplierCreateInput,
  SupplierUpdateInput,
  Part,
  PartCreateInput,
  PartUpdateInput,
  InventoryDocument,
  InventoryDocumentCreateInput,
  InventoryTransaction,
  StockReconciliation,
  DocumentType,
  DocumentStatus,
} from '../types/inventory';

export const inventoryApi = {
  // Suppliers
  listSuppliers: (params: {
    query?: string;
    active?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<Supplier>>(
      '/inventory/suppliers' +
        queryString({
          query: params.query,
          active: params.active,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),

  getSupplier: (id: string | number) =>
    request<Supplier>(`/inventory/suppliers/${id}`),

  createSupplier: (data: SupplierCreateInput) =>
    request<Supplier>('/inventory/suppliers', {
      method: 'POST',
      body: data,
    }),

  updateSupplier: (id: string | number, data: SupplierUpdateInput) =>
    request<Supplier>(`/inventory/suppliers/${id}`, {
      method: 'PUT',
      body: data,
    }),

  deactivateSupplier: (id: string | number) =>
    request<null>(`/inventory/suppliers/${id}`, {
      method: 'DELETE',
    }),

  // Parts
  listParts: (params: {
    query?: string;
    supplierId?: string | number;
    active?: boolean;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<Part>>(
      '/inventory/parts' +
        queryString({
          query: params.query,
          supplierId: params.supplierId,
          active: params.active,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),

  getPart: (id: string | number) =>
    request<Part>(`/inventory/parts/${id}`),

  getLowStockParts: () =>
    request<Part[]>('/inventory/parts/low-stock'),

  createPart: (data: PartCreateInput) =>
    request<Part>('/inventory/parts', {
      method: 'POST',
      body: data,
    }),

  updatePart: (id: string | number, data: PartUpdateInput) =>
    request<Part>(`/inventory/parts/${id}`, {
      method: 'PUT',
      body: data,
    }),

  deactivatePart: (id: string | number) =>
    request<null>(`/inventory/parts/${id}`, {
      method: 'DELETE',
    }),

  // Documents
  listDocuments: (params: {
    type?: DocumentType;
    status?: DocumentStatus;
    supplierId?: string | number;
    orderId?: string | number;
    query?: string;
    page?: number;
    size?: number;
    sortBy?: string;
    sortDir?: string;
  }) =>
    request<Page<InventoryDocument>>(
      '/inventory/documents' +
        queryString({
          type: params.type,
          status: params.status,
          supplierId: params.supplierId,
          orderId: params.orderId,
          query: params.query,
          page: params.page ?? 0,
          size: params.size ?? 20,
          sortBy: params.sortBy ?? 'id',
          sortDir: params.sortDir ?? 'desc',
        }),
    ),

  getDocument: (id: string | number) =>
    request<InventoryDocument>(`/inventory/documents/${id}`),

  createDocument: (data: InventoryDocumentCreateInput) =>
    request<InventoryDocument>('/inventory/documents', {
      method: 'POST',
      body: data,
    }),

  postDocument: (id: string | number, requestKey?: string) =>
    request<null>(`/inventory/documents/${id}/post`, {
      method: 'POST',
      body: { requestKey },
    }),

  cancelDocument: (id: string | number) =>
    request<null>(`/inventory/documents/${id}/cancel`, {
      method: 'POST',
    }),

  getTransactionsForPart: (partId: string | number, page = 0, size = 20) =>
    request<Page<InventoryTransaction>>(
      `/inventory/documents/transactions/part/${partId}` +
        queryString({ page, size }),
    ),

  // Reconciliation
  reconcilePart: (partId: string | number) =>
    request<StockReconciliation>(`/inventory/reconciliation/parts/${partId}`),

  reconcileAllParts: () =>
    request<StockReconciliation[]>('/inventory/reconciliation/parts'),
};

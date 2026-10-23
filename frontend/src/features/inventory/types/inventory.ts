export interface Supplier {
  id: string;
  supplierCode: string;
  supplierName: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  taxCode: string | null;
  paymentTermDays: number;
  isActive: boolean;
  version: string;
  createdAt: string;
  updatedAt: string;
}

export interface SupplierCreateInput {
  supplierCode: string;
  supplierName: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  paymentTermDays?: number;
}

export interface SupplierUpdateInput {
  supplierName: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
  paymentTermDays?: number;
  isActive: boolean;
  version: string;
}

export interface Part {
  id: string;
  sku: string;
  partName: string;
  unit: string;
  description: string | null;
  preferredSupplierId: string | null;
  preferredSupplierName: string | null;
  purchasePrice: string;
  sellingPrice: string;
  stockQuantity: string;
  reservedQuantity: string;
  availableQuantity: string;
  reorderLevel: string;
  warrantyDays: number;
  warrantyKm: number;
  warrantyTerms: string | null;
  isActive: boolean;
  version: string;
  createdAt: string;
  updatedAt: string;
}

export interface PartCreateInput {
  sku: string;
  partName: string;
  unit: string;
  description?: string;
  preferredSupplierId?: string | number;
  purchasePrice: string | number;
  sellingPrice: string | number;
  reorderLevel: string | number;
  warrantyDays?: number;
  warrantyKm?: number;
  warrantyTerms?: string;
}

export interface PartUpdateInput {
  partName: string;
  unit: string;
  description?: string;
  preferredSupplierId?: string | number;
  purchasePrice: string | number;
  sellingPrice: string | number;
  reorderLevel: string | number;
  warrantyDays?: number;
  warrantyKm?: number;
  warrantyTerms?: string;
  isActive: boolean;
  version: string;
}

export type DocumentType = 'IN' | 'OUT' | 'RETURN' | 'ADJUSTMENT' | 'OPENING';
export type DocumentStatus = 'DRAFT' | 'POSTED' | 'CANCELLED';

export interface InventoryDocumentItem {
  id: string;
  lineNo: number;
  partId: string;
  partSku: string;
  partName: string;
  unit: string;
  quantity: string;
  unitCost: string;
  lineTotal: string;
  notes: string | null;
}

export interface InventoryDocument {
  id: string;
  documentCode: string;
  documentType: DocumentType;
  status: DocumentStatus;
  supplierId: string | null;
  supplierName: string | null;
  repairOrderId: string | null;
  sourceDocumentId: string | null;
  supplierInvoiceNumber: string | null;
  supplierInvoiceDate: string | null;
  dueDate: string | null;
  subtotal: string;
  taxAmount: string;
  totalAmount: string;
  reason: string | null;
  createdByName: string | null;
  postedByName: string | null;
  postedAt: string | null;
  createdAt: string;
  version: string;
  items: InventoryDocumentItem[];
}

export interface InventoryDocumentItemInput {
  partId: string | number;
  quantity: string | number;
  unitCost: string | number;
  notes?: string;
}

export interface InventoryDocumentCreateInput {
  documentType: DocumentType;
  supplierId?: string | number;
  repairOrderId?: string | number;
  supplierInvoiceNumber?: string;
  supplierInvoiceDate?: string;
  dueDate?: string;
  taxAmount?: string | number;
  reason?: string;
  items: InventoryDocumentItemInput[];
}

export interface InventoryTransaction {
  id: string;
  documentItemId: string;
  documentCode: string;
  partId: string;
  partSku: string;
  partName: string;
  transactionType: DocumentType;
  quantityChange: string;
  stockAfter: string;
  unitCost: string;
  createdAt: string;
}

export interface StockReconciliation {
  partId: string;
  sku: string;
  partName: string;
  stockQuantity: string;
  ledgerQuantity: string;
  reservedQuantity: string;
  availableQuantity: string;
  discrepancy: string;
  matched: boolean;
}

export type InvoiceStatus = 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'VOID';
export type InvoiceItemType = 'SERVICE' | 'PART' | 'DISCOUNT' | 'SURCHARGE';
export type PaymentDirection = 'IN' | 'OUT';
export type PaymentMethod = 'CASH' | 'CARD' | 'BANK_TRANSFER' | 'VIETQR';

export interface CustomerSnapshot {
  customerCode?: string;
  fullName?: string;
  phone?: string;
  email?: string;
  address?: string;
  taxCode?: string;
}

export interface VehicleSnapshot {
  plateNumber?: string;
  vin?: string;
  make?: string;
  model?: string;
  year?: number;
  color?: string;
  odometer?: number;
}

export interface InvoiceItem {
  id: string;
  lineNo: number;
  itemType: InvoiceItemType;
  repairTaskId: string | null;
  partId: string | null;
  itemName: string;
  unit: string | null;
  quantity: string;
  unitPrice: string;
  unitCost: string;
  lineTotal: string;
  warrantyDays: number;
  warrantyKm: number;
  warrantyTerms: string | null;
}

export interface Invoice {
  id: string;
  invoiceCode: string;
  repairOrderId: string;
  customerId: string;
  customerSnapshot: CustomerSnapshot;
  vehicleSnapshot: VehicleSnapshot;
  status: InvoiceStatus;
  subtotal: string;
  discountAmount: string;
  taxAmount: string;
  totalAmount: string;
  paidAmount?: string;
  balanceAmount?: string;
  issuedByName: string | null;
  issuedAt: string;
  dueDate: string | null;
  voidReason: string | null;
  voidedByName: string | null;
  voidedAt: string | null;
  version: string;
  items: InvoiceItem[];
}

export interface InvoiceIssueInput {
  repairOrderId: string | number;
  discountAmount?: string | number;
  taxAmount?: string | number;
  dueDate?: string;
  requestKey?: string;
}

export interface InvoiceVoidInput {
  voidReason: string;
}

export interface Payment {
  id: string;
  transactionCode: string;
  direction: PaymentDirection;
  invoiceId: string | null;
  invoiceCode: string | null;
  inventoryDocumentId: string | null;
  documentCode: string | null;
  amount: string;
  paymentMethod: PaymentMethod;
  providerReference: string | null;
  paidAt: string;
  recordedByName: string | null;
  notes: string | null;
}

export interface PaymentReceiveInput {
  invoiceId: string | number;
  amount: string | number;
  paymentMethod: PaymentMethod;
  providerReference?: string;
  notes?: string;
  requestKey?: string;
}

export interface SupplierPaymentInput {
  inventoryDocumentId: string | number;
  amount: string | number;
  paymentMethod: PaymentMethod;
  providerReference?: string;
  notes?: string;
  requestKey?: string;
}

export interface CustomerDebtBalance {
  customerId: string;
  customerCode: string;
  customerName: string;
  phone: string | null;
  creditLimit: string;
  paymentTermDays: number;
  totalInvoiced: string;
  totalPaid: string;
  balanceAmount: string;
}

export interface SupplierDebtBalance {
  supplierId: string;
  supplierCode: string;
  supplierName: string;
  phone: string | null;
  paymentTermDays: number;
  totalPurchased: string;
  totalPaid: string;
  balanceAmount: string;
}

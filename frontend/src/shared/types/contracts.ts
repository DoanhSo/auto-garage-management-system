export const roles = [
  'ADMIN',
  'RECEPTIONIST',
  'WAREHOUSE_STAFF',
  'TECHNICIAN',
  'CUSTOMER',
] as const;
export type Role = (typeof roles)[number];
export const roleLabels: Record<Role, string> = {
  ADMIN: 'Quản trị viên',
  RECEPTIONIST: 'Lễ tân',
  WAREHOUSE_STAFF: 'Nhân viên kho',
  TECHNICIAN: 'Kỹ thuật viên',
  CUSTOMER: 'Khách hàng',
};

export const customerTypes = ['INDIVIDUAL', 'BUSINESS'] as const;
export type CustomerType = (typeof customerTypes)[number];
export const customerTypeLabels: Record<CustomerType, string> = {
  INDIVIDUAL: 'Cá nhân',
  BUSINESS: 'Doanh nghiệp',
};

export interface Customer {
  id: string;
  userId: string | null;
  customerCode: string;
  fullName: string;
  customerType: CustomerType;
  phone: string;
  email: string | null;
  address: string | null;
  companyName: string | null;
  taxCode: string | null;
  creditLimit: string;
  paymentTermDays: number;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: string;
}

export interface CustomerCreate {
  userId?: string | null;
  customerCode?: string | null;
  fullName: string;
  customerType: CustomerType;
  phone: string;
  email?: string | null;
  address?: string | null;
  companyName?: string | null;
  taxCode?: string | null;
  creditLimit: number;
  paymentTermDays: number;
}

export interface CustomerUpdate {
  fullName: string;
  customerType?: CustomerType;
  phone: string;
  email?: string | null;
  address?: string | null;
  companyName?: string | null;
  taxCode?: string | null;
  creditLimit?: number;
  paymentTermDays?: number;
  isActive?: boolean;
}

export interface Vehicle {
  id: string;
  customerId: string;
  customerName: string;
  customerPhone: string;
  licensePlate: string;
  vin: string | null;
  engineNumber: string | null;
  brand: string;
  model: string;
  manufactureYear: number | null;
  color: string | null;
  fuelType: string | null;
  odometer: number;
  inspectionDueDate: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: string;
}

export interface VehicleCreate {
  customerId: string;
  licensePlate: string;
  vin?: string | null;
  engineNumber?: string | null;
  brand: string;
  model: string;
  manufactureYear?: number | null;
  color?: string | null;
  fuelType?: string | null;
  odometer: number;
  inspectionDueDate?: string | null;
}

export interface VehicleUpdate {
  brand?: string;
  model?: string;
  manufactureYear?: number | null;
  color?: string | null;
  fuelType?: string | null;
  odometer?: number;
  inspectionDueDate?: string | null;
  isActive?: boolean;
}

export interface TransferOwnership {
  newCustomerId: string;
  transferNote?: string | null;
  transferOdometer?: number | null;
}

export interface TechnicalAttachment {
  id: string;
  fileName: string;
  fileUrl: string;
  category: string;
  fileSize?: number;
  contentType?: string;
}

export interface VehicleHistoryEntry {
  repairOrderId: string;
  orderCode: string;
  receivedAt: string;
  completedAt: string | null;
  odometerIn: number;
  customerComplaint: string | null;
  diagnosis: string | null;
  status: string;
  orderCustomerId: string;
  isCurrentOwnerRecord: boolean;
  totalAmount: string | null;
  tasksDone: string[];
  partsReplaced: string[];
  technicalAttachments: TechnicalAttachment[];
}

export interface VehicleTimeline {
  vehicleId: string;
  licensePlate: string;
  brand: string;
  model: string;
  currentOdometer: number;
  currentCustomerId: string;
  currentCustomerName: string;
  historyEntries: VehicleHistoryEntry[];
}

export type ServiceType = 'SINGLE' | 'PACKAGE';
export const serviceTypes: [ServiceType, ...ServiceType[]] = ['SINGLE', 'PACKAGE'];

export interface Service {
  id: string;
  serviceCode: string;
  serviceName: string;
  serviceType: ServiceType;
  description: string | null;
  unit: string;
  unitPrice: string;
  standardMinutes: number;
  requiredBayType: string | null;
  requiredSkillsJson: string | null;
  intervalKm: number | null;
  intervalMonths: number | null;
  warrantyDays: number;
  warrantyKm: number;
  warrantyTerms: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
  version: string;
}

export interface ServiceCreate {
  serviceCode?: string | null;
  serviceName: string;
  serviceType: ServiceType;
  description?: string | null;
  unit?: string;
  unitPrice: number;
  standardMinutes: number;
  requiredBayType?: string | null;
  requiredSkillsJson?: string | null;
  intervalKm?: number | null;
  intervalMonths?: number | null;
  warrantyDays?: number;
  warrantyKm?: number;
  warrantyTerms?: string | null;
}

export interface ServiceUpdate {
  serviceName?: string;
  serviceType?: ServiceType;
  description?: string | null;
  unit?: string;
  unitPrice?: number;
  standardMinutes?: number;
  requiredBayType?: string | null;
  requiredSkillsJson?: string | null;
  intervalKm?: number | null;
  intervalMonths?: number | null;
  warrantyDays?: number;
  warrantyKm?: number;
  warrantyTerms?: string | null;
  isActive?: boolean;
}

export interface User {
  id: string;
  username: string;
  email: string | null;
  fullName: string;
  role: Role;
  active: boolean;
  version: string;
  createdAt: string;
}
export interface LoginResult {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}
export interface Page<T> {
  content: T[];
  totalElements: string;
  totalPages: number;
  number: number;
  size: number;
}
export interface UiPolicy {
  passwordMinLength: number;
  passwordMaxUtf8Bytes: number;
  paginationDefaultSize: number;
  paginationMaxSize: number;
  paginationMaxPage: number;
  reportMaxPeriodSeconds: number;
  garageTimeZone: string;
}
export interface Notice {
  id: string;
  channel: string;
  subject: string | null;
  payloadJson: string;
  status: string;
  readAt: string | null;
  createdAt: string;
}
export interface AuditEvent {
  id: string;
  actorUserId: string | null;
  action: string;
  entityType: string;
  entityId: string | null;
  afterJson: string | null;
  createdAt: string;
}
export interface Dashboard {
  revenue: string;
  cashIn: string;
  cashOut: string;
  customerDebt: string;
  supplierDebt: string;
  stockValue: string;
  laborHours: string;
  from: string;
  until: string;
  snapshotAt: string;
}
export interface InvoiceBalance {
  id: string;
  invoiceCode: string;
  customerId: string;
  totalAmount: string;
  paidAmount: string;
  balanceAmount: string;
}
export interface UserCreate {
  username: string;
  email: string | null;
  fullName: string;
  role: Role;
  password: string;
}
export interface UserUpdate {
  email: string | null;
  fullName: string;
  role: Role;
  active: boolean;
  version: string;
}

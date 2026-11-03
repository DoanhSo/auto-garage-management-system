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
  orderCustomerId: string | null;
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

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'NO_SHOW' | 'RECEIVED';
export const bookingStatuses: [BookingStatus, ...BookingStatus[]] = [
  'PENDING',
  'CONFIRMED',
  'CANCELLED',
  'NO_SHOW',
  'RECEIVED',
];

export interface Booking {
  id: string;
  bookingCode: string;
  customerId: string;
  customerName: string;
  customerPhone: string;
  vehicleId: string;
  licensePlate: string;
  vehicleModel?: string | null;
  serviceId?: string | null;
  serviceName?: string | null;
  standardMinutes?: number | null;
  bayId?: string | null;
  appointmentAt: string;
  expectedEndAt: string;
  requestDescription?: string | null;
  status: BookingStatus;
  cancellationReason?: string | null;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  version: string;
}

export interface BookingNewCustomer {
  fullName: string;
  phone: string;
  email?: string | null;
}

export interface BookingNewVehicle {
  licensePlate: string;
  brand: string;
  model: string;
  manufactureYear?: number | null;
  color?: string | null;
}

export interface BookingCreate {
  customerId?: string | null;
  newCustomer?: BookingNewCustomer | null;
  vehicleId?: string | null;
  newVehicle?: BookingNewVehicle | null;
  serviceId?: string | null;
  bayId?: string | null;
  appointmentAt: string;
  requestDescription?: string | null;
}

export interface BookingReschedule {
  newAppointmentAt: string;
  newBayId?: string | null;
  actorUserId?: string;
}

export interface BookingCancel {
  cancellationReason: string;
  actorUserId?: string;
}

export interface CheckInVehicle {
  bookingId?: string | null;
  vehicleId: string;
  odometerIn: number;
  fuelLevelPercent?: number | null;
  customerComplaint?: string | null;
  diagnosticTechnicianId?: string | null;
  serviceBayId?: string | null;
  actorUserId?: string;
}

export interface CheckInVehicleResult {
  repairOrderId: string;
  /** Backend chưa trả mã lệnh; khi thiếu thì bằng repairOrderId. */
  orderCode: string;
  bookingId?: string | null;
  vehicleId: string;
  licensePlate?: string | null;
  customerName?: string | null;
  customerComplaint?: string | null;
  status: string;
  receivedAt: string;
}

export type CheckType = 'CHECK_IN' | 'QC' | 'HANDOVER';
export type CheckResult = 'RECORDED' | 'PASSED' | 'FAILED' | 'PASS' | 'FAIL' | 'CONDITIONAL';
export type AttachmentCategory = 'CHECK_IN' | 'DIAGNOSIS' | 'QC' | 'HANDOVER' | 'WARRANTY';

export interface Attachment {
  id: string;
  repairOrderId?: string | null;
  vehicleCheckId?: string | null;
  warrantyClaimId?: string | null;
  category: AttachmentCategory;
  fileName: string;
  fileSize?: number;
  contentType?: string;
  /** Đường dẫn API (tương đối với /api/v1) để tải nội dung kèm token. */
  contentPath: string;
  sha256?: string | null;
  uploadedBy?: string | null;
  createdAt: string;
}

export interface VehicleCheck {
  id: string;
  repairOrderId: string;
  checkType: CheckType;
  attemptNo: number;
  result: CheckResult;
  checklistJson: string;
  schemaVersion: number;
  odometer?: number | null;
  notes?: string | null;
  checkedBy: string;
  checkedAt: string;
  attachments: Attachment[];
}

export interface VehicleCheckCreate {
  repairOrderId: string;
  checkType: CheckType;
  checklistJson: string;
  odometer?: number | null;
  notes?: string | null;
  checkedBy?: string;
}

export interface User {
  id: string;
  identityId?: string | null;
  username: string;
  email: string | null;
  fullName: string;
  role: Role;
  active: boolean;
  version: string;
  createdAt: string;
}
export interface OidcTokenBundle {
  accessToken: string;
  refreshToken: string | null;
  expiresIn: number;
  tokenType: string;
}
export interface LoginResult {
  accessToken: string;
  refreshToken?: string | null;
  expiresIn?: number;
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
  userId?: string | null;
  channel: string;
  subject: string | null;
  payloadJson: string;
  status: string;
  readAt: string | null;
  createdAt: string;
}
export interface NoticeBroadcastRequest {
  targetRoles?: Role[];
  subject: string;
  payloadJson: string;
}
export interface NoticeBroadcastResult {
  eventKey?: string;
  recipientCount: number;
  targetRole: Role | null;
  targetRoles?: Role[];
  channel?: string;
  subject?: string | null;
}
export interface AuditEvent {
  id: string;
  actorUserId: string | null;
  actorFullName?: string | null;
  actorUsername?: string | null;
  actorRole?: Role | null;
  action: string;
  entityType: string;
  entityId: string | null;
  entityLabel?: string | null;
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
  password?: string | null;
  identityId?: string | null;
}
export interface UserUpdate {
  email: string | null;
  fullName: string;
  role: Role;
  active: boolean;
  version: string;
}

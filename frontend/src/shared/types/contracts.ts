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

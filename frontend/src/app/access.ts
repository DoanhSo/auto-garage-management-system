import type { Role } from '../shared/types/contracts';

export const STAFF_ROLES: readonly Role[] = [
  'ADMIN',
  'RECEPTIONIST',
  'WAREHOUSE_STAFF',
  'TECHNICIAN',
];
export const WAREHOUSE_ROLES: readonly Role[] = ['ADMIN', 'WAREHOUSE_STAFF'];
export const FINANCE_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF'];
export const FRONT_DESK_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST'];
export const TECHNICAL_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST', 'TECHNICIAN'];
export const BOOKING_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST', 'CUSTOMER'];
export const REPAIR_VIEW_ROLES: readonly Role[] = [...STAFF_ROLES, 'CUSTOMER'];
export const INVOICE_VIEW_ROLES: readonly Role[] = [
  'ADMIN',
  'RECEPTIONIST',
  'WAREHOUSE_STAFF',
  'CUSTOMER',
];

export function homePathForRole(role?: Role): string {
  switch (role) {
    case 'ADMIN':
      return '/admin/reports';
    case 'RECEPTIONIST':
    case 'CUSTOMER':
      return '/reception';
    case 'TECHNICIAN':
      return '/repair-orders';
    case 'WAREHOUSE_STAFF':
      return '/inventory/parts';
    default:
      return '/notifications';
  }
}

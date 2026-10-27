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
export const BOOKING_ROLES: readonly Role[] = ['ADMIN', 'RECEPTIONIST', 'CUSTOMER'];

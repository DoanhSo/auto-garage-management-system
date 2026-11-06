import {
  Bell,
  CalendarCheck,
  Car,
  ChartNoAxesCombined,
  Contact,
  History,
  Layers,
  ShieldCheck,
  Users,
  Wrench,
  Package,
  Building2,
  FileText,
  Scale,
  Receipt,
  DollarSign,
  CreditCard,
  Banknote,
  UserCog,
  CalendarClock,
} from 'lucide-react';
import type { Role } from '../../shared/types/contracts';
import { roles } from '../../shared/types/contracts';
import {
  FRONT_DESK_ROLES,
  BOOKING_ROLES,
  ESTIMATE_VIEW_ROLES,
  SCHEDULING_ROLES,
  WARRANTY_ROLES,
  FINANCE_ROLES,
  STAFF_ROLES,
  REPAIR_VIEW_ROLES,
  INVOICE_VIEW_ROLES,
  PAYROLL_MANAGE_ROLES,
  STAFF_MANAGE_ROLES,
  ADMIN_ROLES,
} from '../access';
import type { NavChild, NavTarget } from './navMatch';

export const sections = [
  'DỊCH VỤ & XƯỞNG',
  'KHO & VẬT TƯ',
  'NHÂN SỰ & LƯƠNG',
  'TÀI CHÍNH & CÔNG NỢ',
  'QUẢN TRỊ HỆ THỐNG',
  'CÁ NHÂN',
] as const;

export type NavSection = (typeof sections)[number];

export interface NavItem extends NavTarget {
  to: string;
  label: string;
  customerLabel?: string;
  technicianLabel?: string;
  icon: typeof Bell;
  section: NavSection;
  allowedRoles: readonly Role[];
  /** Tabs of the page, shown under the entry while the page is open. */
  children?: ReadonlyArray<NavChild>;
}

export const links: ReadonlyArray<NavItem> = [
  {
    to: '/reception',
    label: 'Tiếp nhận & Lịch hẹn',
    customerLabel: 'Lịch hẹn của tôi',
    icon: CalendarCheck,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: BOOKING_ROLES,
  },
  {
    to: '/repair-orders',
    label: 'Lệnh sửa chữa',
    customerLabel: 'Tiến độ sửa xe',
    icon: Wrench,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: REPAIR_VIEW_ROLES,
  },
  {
    to: '/estimates',
    label: 'Báo giá',
    icon: FileText,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: ESTIMATE_VIEW_ROLES,
  },
  {
    to: '/warranties',
    label: 'Bảo hành',
    customerLabel: 'Sổ bảo hành của tôi',
    icon: ShieldCheck,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: WARRANTY_ROLES,
  },
  {
    to: '/customers',
    label: 'Khách hàng',
    icon: Contact,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: FRONT_DESK_ROLES,
  },
  {
    to: '/vehicles',
    label: 'Phương tiện',
    customerLabel: 'Xe của tôi',
    icon: Car,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: BOOKING_ROLES,
  },
  {
    to: '/services',
    label: 'Dịch vụ',
    icon: Layers,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: roles,
  },
  {
    to: '/inventory/parts',
    label: 'Kho & Phụ tùng',
    icon: Package,
    section: 'KHO & VẬT TƯ',
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/inventory/suppliers',
    label: 'Nhà cung cấp',
    icon: Building2,
    section: 'KHO & VẬT TƯ',
    allowedRoles: ['ADMIN', 'WAREHOUSE_STAFF'],
  },
  {
    to: '/inventory/documents',
    label: 'Chứng từ kho',
    icon: FileText,
    section: 'KHO & VẬT TƯ',
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/inventory/reconciliation',
    label: 'Đối soát kho',
    icon: Scale,
    section: 'KHO & VẬT TƯ',
    allowedRoles: ['ADMIN', 'WAREHOUSE_STAFF'],
  },
  {
    to: '/staff',
    label: 'Quản lý nhân viên',
    icon: UserCog,
    section: 'NHÂN SỰ & LƯƠNG',
    allowedRoles: STAFF_MANAGE_ROLES,
    children: [
      { tab: 'profiles', label: 'Hồ sơ nhân viên', defaultTab: true },
      { tab: 'salary', label: 'Hệ số & đơn giá lương' },
    ],
  },
  {
    to: '/scheduling',
    label: 'Khoang & ca làm việc',
    icon: CalendarClock,
    section: 'NHÂN SỰ & LƯƠNG',
    allowedRoles: SCHEDULING_ROLES,
    children: [
      { tab: 'dispatch', label: 'Điều phối xưởng', defaultTab: true },
      { tab: 'bays', label: 'Khoang sửa chữa' },
      { tab: 'shifts', label: 'Lịch ca làm việc' },
    ],
  },
  {
    to: '/payroll',
    label: 'Bảng lương',
    icon: Banknote,
    section: 'NHÂN SỰ & LƯƠNG',
    allowedRoles: PAYROLL_MANAGE_ROLES,
    children: [
      { tab: 'monthly', label: 'Bảng lương tháng', defaultTab: true },
      { tab: 'bonus', label: 'Chính sách thưởng ca' },
    ],
  },
  {
    to: '/payroll',
    label: 'Phiếu lương của tôi',
    icon: Banknote,
    section: 'NHÂN SỰ & LƯƠNG',
    allowedRoles: ['TECHNICIAN'],
  },
  {
    to: '/billing/invoices',
    label: 'Hóa đơn dịch vụ',
    customerLabel: 'Hóa đơn của tôi',
    icon: Receipt,
    section: 'TÀI CHÍNH & CÔNG NỢ',
    allowedRoles: INVOICE_VIEW_ROLES,
  },
  {
    to: '/billing/payments',
    label: 'Sổ quỹ Thu / Chi',
    icon: DollarSign,
    section: 'TÀI CHÍNH & CÔNG NỢ',
    allowedRoles: FINANCE_ROLES,
  },
  {
    to: '/billing/debt-ledger',
    label: 'Sổ nợ & Tín dụng',
    icon: CreditCard,
    section: 'TÀI CHÍNH & CÔNG NỢ',
    allowedRoles: FINANCE_ROLES,
  },
  {
    to: '/admin/reports',
    label: 'Báo cáo',
    icon: ChartNoAxesCombined,
    section: 'QUẢN TRỊ HỆ THỐNG',
    allowedRoles: ADMIN_ROLES,
  },
  {
    to: '/admin/users',
    label: 'Tài khoản',
    icon: Users,
    section: 'QUẢN TRỊ HỆ THỐNG',
    allowedRoles: ['ADMIN'],
  },
  {
    to: '/admin/audit',
    label: 'Nhật ký',
    icon: History,
    section: 'QUẢN TRỊ HỆ THỐNG',
    allowedRoles: ['ADMIN'],
  },
  {
    to: '/notifications',
    label: 'Thông báo',
    icon: Bell,
    section: 'CÁ NHÂN',
    allowedRoles: roles,
  },
  {
    to: '/account/security',
    label: 'Tài khoản của tôi',
    icon: ShieldCheck,
    section: 'CÁ NHÂN',
    allowedRoles: roles,
  },
];

/** Sidebar entries a role may open. */
export function navForRole(role: Role | undefined): ReadonlyArray<NavItem> {
  return role ? links.filter((link) => link.allowedRoles.includes(role)) : [];
}

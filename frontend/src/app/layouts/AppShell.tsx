import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import {
  Bell,
  CalendarCheck,
  Car,
  ChartNoAxesCombined,
  Contact,
  Globe,
  History,
  House,
  Layers,
  LogOut,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  ShieldCheck,
  Users,
  Wrench,
  X,
  Package,
  Building2,
  FileText,
  Scale,
  Receipt,
  DollarSign,
  CreditCard,
} from 'lucide-react';
import * as Dialog from '@radix-ui/react-dialog';
import { useAuth } from '../providers/AuthProvider';
import { useMeQuery, useLogoutMutation } from '../../features/auth/hooks/useAuthQueries';
import { useUnreadCountQuery } from '../../features/notification/hooks/useNotifications';
import { Button, RoleBadge } from '../../shared/components/ui';
import { roles, type Role } from '../../shared/types/contracts';
import {
  FRONT_DESK_ROLES,
  BOOKING_ROLES,
  FINANCE_ROLES,
  STAFF_ROLES,
  REPAIR_VIEW_ROLES,
  INVOICE_VIEW_ROLES,
} from '../access';
import { initials } from '../../shared/utils/format';

const sections = [
  'DỊCH VỤ & XƯỞNG',
  'KHO & VẬT TƯ',
  'TÀI CHÍNH & CÔNG NỢ',
  'QUẢN TRỊ HỆ THỐNG',
  'CÁ NHÂN',
] as const;

type NavSection = (typeof sections)[number];

const links: ReadonlyArray<{
  to: string;
  label: string;
  customerLabel?: string;
  icon: typeof Bell;
  section: NavSection;
  allowedRoles: readonly Role[];
}> = [
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
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/scheduling',
    label: 'Khoang & Lịch ca',
    icon: CalendarCheck,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/warranties',
    label: 'Bảo hành',
    icon: ShieldCheck,
    section: 'DỊCH VỤ & XƯỞNG',
    allowedRoles: STAFF_ROLES,
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
    allowedRoles: ['ADMIN'],
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

export function AppShell() {
  const { user, setCurrentUser } = useAuth();
  const me = useMeQuery();
  const logout = useLogoutMutation();
  const unread = useUnreadCountQuery();
  const location = useLocation();
  const navigate = useNavigate();
  const [mobile, setMobile] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  const shiftLabel = new Date().getHours() < 12 ? 'Ca sáng' : 'Ca chiều';

  useEffect(() => {
    if (me.data) setCurrentUser(me.data);
  }, [me.data, setCurrentUser]);

  const labelOf = (link: (typeof links)[number]) =>
    user?.role === 'CUSTOMER' && link.customerLabel ? link.customerLabel : link.label;
  const allowed = links.filter((link) => !!user && link.allowedRoles.includes(user.role));
  const currentLink = links.find((link) => location.pathname.startsWith(link.to));
  const title = currentLink ? labelOf(currentLink) : 'Tài khoản cá nhân';

  const nav = (close?: () => void) => (
    <nav className="main-navigation" aria-label="Điều hướng chính">
      {sections.map((section, index) => {
        const items = allowed.filter((link) => link.section === section);
        if (items.length === 0) return null;
        return (
          <div key={section} className="nav-group">
            <span className={`nav-heading ${index > 0 ? 'account-nav-heading' : ''}`}>
              {section}
            </span>
            {items.map((link) => {
              const { to, icon: Icon } = link;
              const label = labelOf(link);
              return (
                <NavLink
                  key={to}
                  to={to}
                  onClick={close}
                  title={collapsed ? label : undefined}
                  className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
                >
                  <Icon size={19} aria-hidden />
                  <span className="nav-item-label">{label}</span>
                  {to === '/notifications' &&
                    unread.data !== undefined &&
                    BigInt(unread.data) > 0n && (
                      <span className="nav-count">
                        {BigInt(unread.data) > 99n ? '99+' : unread.data}
                      </span>
                    )}
                </NavLink>
              );
            })}
          </div>
        );
      })}
    </nav>
  );

  return (
    <div className={`app-layout ${collapsed ? 'sidebar-collapsed' : ''}`}>
      <aside className="sidebar">
        <div className="sidebar-brand-row">
          <NavLink to="/" className="brand" title="Về trang chủ AutoGarage Tây Sơn">
            <span className="brand-mark">
              <Wrench size={20} />
            </span>
            <span className="brand-text">
              AutoGarage Tây Sơn<small>Quản lý gara</small>
            </span>
          </NavLink>
          <button
            type="button"
            className="sidebar-collapse-btn"
            onClick={() => setCollapsed((prev) => !prev)}
            aria-label={collapsed ? 'Mở rộng thanh bên' : 'Thu gọn thanh bên'}
            title={collapsed ? 'Mở rộng thanh bên' : 'Thu gọn thanh bên'}
          >
            {collapsed ? <PanelLeftOpen size={17} /> : <PanelLeftClose size={17} />}
          </button>
        </div>
        {nav()}
        <NavLink to="/" end className="nav-item home-link" title={collapsed ? 'Trang chủ' : undefined}>
          <House size={19} aria-hidden />
          <span className="nav-item-label">Trang chủ</span>
        </NavLink>
        <div className="sidebar-footer">
          <span className="avatar">{initials(user?.fullName ?? '')}</span>
          <div className="sidebar-user-meta">
            <strong>{user?.fullName}</strong>
            <span>@{user?.username}</span>
          </div>
        </div>
      </aside>
      <div className="app-content">
        <header className="app-header">
          <div className="breadcrumb">
            <Button
              ref={trigger}
              variant="ghost"
              className="mobile-menu"
              aria-label="Mở menu điều hướng"
              aria-expanded={mobile}
              onClick={() => setMobile(true)}
            >
              <Menu size={20} />
            </Button>
            <strong>{title}</strong>
          </div>
          <div className="header-actions">
            <span className="garage-shift-pill">
              <Wrench size={14} aria-hidden /> {shiftLabel}
            </span>
            <NavLink
              className="bell-button"
              to="/notifications"
              aria-label={`Thông báo${unread.data ? ' — ' + unread.data + ' chưa đọc' : ''}`}
            >
              <Bell size={19} />
              {unread.data !== undefined && BigInt(unread.data) > 0n && (
                <span className="bell-dot" />
              )}
            </NavLink>
            <div className="header-user-pill">
              <span className="header-name">{user?.fullName}</span>
              {user && <RoleBadge role={user.role} />}
            </div>
            <Link
              to="/"
              className="button button-ghost"
              title="Về trang chủ AutoGarage Tây Sơn"
            >
              <Globe size={16} />
              <span className="logout-label">Trang chủ</span>
            </Link>
            <Button
              variant="ghost"
              onClick={() =>
                logout.mutate(undefined, {
                  onSettled: () => navigate('/', { replace: true }),
                })
              }
              aria-label="Đăng xuất khỏi hệ thống"
              pending={logout.isPending}
            >
              <LogOut size={17} />
              <span className="logout-label">Đăng xuất</span>
            </Button>
          </div>
        </header>
        <main className="workspace" id="main-content" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
      <Dialog.Root open={mobile} onOpenChange={setMobile}>
        <Dialog.Portal>
          <Dialog.Overlay className="overlay" />
          <Dialog.Content
            className="mobile-nav"
            aria-label="Thanh điều hướng di động"
            onCloseAutoFocus={(e) => {
              e.preventDefault();
              trigger.current?.focus();
            }}
          >
            <Dialog.Title className="sr-only">Điều hướng</Dialog.Title>
            <Dialog.Description className="sr-only">Chọn chức năng của gara.</Dialog.Description>
            <div className="drawer-header">
              <span className="brand-mark">
                <Wrench size={20} />
              </span>
              <span>AutoGarage</span>
              <Dialog.Close asChild>
                <Button variant="ghost" aria-label="Đóng thanh điều hướng">
                  <X size={18} />
                </Button>
              </Dialog.Close>
            </div>
            {nav(() => setMobile(false))}
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </div>
  );
}

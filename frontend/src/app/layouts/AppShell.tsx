import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  Bell,
  CalendarCheck,
  Car,
  ChartNoAxesCombined,
  Contact,
  History,
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
import { FRONT_DESK_ROLES, BOOKING_ROLES, STAFF_ROLES } from '../access';
import { initials } from '../../shared/utils/format';

const links: ReadonlyArray<{
  to: string;
  label: string;
  icon: typeof Bell;
  allowedRoles: readonly Role[];
}> = [
  {
    to: '/reception',
    label: 'Tiếp nhận xe',
    icon: CalendarCheck,
    allowedRoles: BOOKING_ROLES,
  },
  {
    to: '/repair-orders',
    label: 'Lệnh sửa chữa',
    icon: Wrench,
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/estimates',
    label: 'Báo giá',
    icon: FileText,
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/scheduling',
    label: 'Khoang & Lịch ca',
    icon: CalendarCheck,
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/warranties',
    label: 'Bảo hành',
    icon: ShieldCheck,
    allowedRoles: STAFF_ROLES,
  },
  {
    to: '/customers',
    label: 'Khách hàng',
    icon: Contact,
    allowedRoles: FRONT_DESK_ROLES,
  },
  { to: '/vehicles', label: 'Phương tiện', icon: Car, allowedRoles: BOOKING_ROLES },
  { to: '/services', label: 'Dịch vụ', icon: Layers, allowedRoles: roles },
  {
    to: '/inventory/parts',
    label: 'Kho & Phụ tùng',
    icon: Package,
    allowedRoles: ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN'],
  },
  {
    to: '/inventory/suppliers',
    label: 'Nhà cung cấp',
    icon: Building2,
    allowedRoles: ['ADMIN', 'WAREHOUSE_STAFF'],
  },
  {
    to: '/inventory/documents',
    label: 'Chứng từ kho',
    icon: FileText,
    allowedRoles: ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN'],
  },
  {
    to: '/inventory/reconciliation',
    label: 'Đối soát kho',
    icon: Scale,
    allowedRoles: ['ADMIN', 'WAREHOUSE_STAFF'],
  },
  {
    to: '/billing/invoices',
    label: 'Hóa đơn dịch vụ',
    icon: Receipt,
    allowedRoles: ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN'],
  },
  {
    to: '/billing/payments',
    label: 'Sổ quỹ Thu / Chi',
    icon: DollarSign,
    allowedRoles: ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF', 'TECHNICIAN'],
  },
  {
    to: '/billing/debt-ledger',
    label: 'Sổ nợ & Tín dụng',
    icon: CreditCard,
    allowedRoles: ['ADMIN', 'RECEPTIONIST', 'WAREHOUSE_STAFF'],
  },
  {
    to: '/admin/reports',
    label: 'Báo cáo',
    icon: ChartNoAxesCombined,
    allowedRoles: ['ADMIN'],
  },
  { to: '/admin/users', label: 'Tài khoản', icon: Users, allowedRoles: ['ADMIN'] },
  { to: '/admin/audit', label: 'Nhật ký', icon: History, allowedRoles: ['ADMIN'] },
  { to: '/notifications', label: 'Thông báo', icon: Bell, allowedRoles: roles },
];

export function AppShell() {
  const { user, setCurrentUser } = useAuth();
  const me = useMeQuery();
  const logout = useLogoutMutation();
  const unread = useUnreadCountQuery();
  const location = useLocation();
  const [mobile, setMobile] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (me.data) setCurrentUser(me.data);
  }, [me.data, setCurrentUser]);

  const allowed = links.filter((link) => !!user && link.allowedRoles.includes(user.role));
  const title =
    links.find((link) => location.pathname.startsWith(link.to))?.label ?? 'Tài khoản cá nhân';

  const nav = (close?: () => void) => (
    <nav className="main-navigation" aria-label="Điều hướng chính">
      <span className="nav-heading">KHÔNG GIAN LÀM VIỆC</span>
      {allowed.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={close}
          title={collapsed ? label : undefined}
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <Icon size={19} aria-hidden />
          <span className="nav-item-label">{label}</span>
          {to === '/notifications' && unread.data !== undefined && BigInt(unread.data) > 0n && (
            <span className="nav-count">{BigInt(unread.data) > 99n ? '99+' : unread.data}</span>
          )}
        </NavLink>
      ))}
      <span className="nav-heading account-nav-heading">CÁ NHÂN</span>
      <NavLink
        to="/account/security"
        onClick={close}
        title={collapsed ? 'Tài khoản của tôi' : undefined}
        className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
      >
        <ShieldCheck size={19} />
        <span className="nav-item-label">Tài khoản của tôi</span>
      </NavLink>
    </nav>
  );

  return (
    <div className={`app-layout ${collapsed ? 'sidebar-collapsed' : ''}`}>
      <aside className="sidebar">
        <div className="sidebar-brand-row">
          <NavLink
            to={user?.role === 'ADMIN' ? '/admin/reports' : '/notifications'}
            className="brand"
          >
            <span className="brand-mark">
              <Wrench size={20} />
            </span>
            <span className="brand-text">
              AutoGarage<small>Quản lý gara</small>
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
            <Button
              variant="ghost"
              onClick={() => logout.mutate()}
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

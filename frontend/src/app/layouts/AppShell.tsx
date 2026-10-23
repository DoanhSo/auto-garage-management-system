import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation } from 'react-router-dom';
import {
  Bell,
  Car,
  ChartNoAxesCombined,
  ChevronRight,
  Contact,
  History,
  Layers,
  LogOut,
  Menu,
  ShieldCheck,
  Users,
  Wrench,
  X,
} from 'lucide-react';
import * as Dialog from '@radix-ui/react-dialog';
import { useAuth } from '../providers/AuthProvider';
import { useMeQuery, useLogoutMutation } from '../../features/auth/hooks/useAuthQueries';
import { useUnreadCountQuery } from '../../features/notification/hooks/useNotifications';
import { Button } from '../../shared/components/ui';
import { initials } from '../../shared/utils/format';
const links = [
  { to: '/customers', label: 'Khách hàng', icon: Contact, admin: false },
  { to: '/vehicles', label: 'Phương tiện', icon: Car, admin: false },
  { to: '/services', label: 'Dịch vụ & Tiền công', icon: Layers, admin: false },
  {
    to: '/admin/reports',
    label: 'Báo cáo',
    icon: ChartNoAxesCombined,
    admin: true,
  },
  { to: '/admin/users', label: 'Tài khoản', icon: Users, admin: true },
  { to: '/admin/audit', label: 'Nhật ký', icon: History, admin: true },
  { to: '/notifications', label: 'Thông báo', icon: Bell, admin: false },
];
export function AppShell() {
  const { user, setCurrentUser } = useAuth();
  const me = useMeQuery();
  const logout = useLogoutMutation();
  const unread = useUnreadCountQuery();
  const location = useLocation();
  const [mobile, setMobile] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (me.data) setCurrentUser(me.data);
  }, [me.data, setCurrentUser]);
  const allowed = links.filter((link) => !link.admin || user?.role === 'ADMIN');
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
          className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
        >
          <Icon size={19} aria-hidden />
          <span>{label}</span>
          {to === '/notifications' && unread.data !== undefined && BigInt(unread.data) > 0n && (
            <span className="nav-count">{BigInt(unread.data) > 99n ? '99+' : unread.data}</span>
          )}
        </NavLink>
      ))}
      <span className="nav-heading account-nav-heading">CÁ NHÂN</span>
      <NavLink
        to="/account/security"
        onClick={close}
        className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}
      >
        <ShieldCheck size={19} />
        <span>Tài khoản của tôi</span>
      </NavLink>
    </nav>
  );
  return (
    <div className="app-layout">
      <aside className="sidebar">
        <NavLink
          to={user?.role === 'ADMIN' ? '/admin/reports' : '/notifications'}
          className="brand"
        >
          <span className="brand-mark">
            <Wrench size={22} />
          </span>
          <span>
            AutoGarage<small>GARAGE WORKSPACE</small>
          </span>
        </NavLink>
        {nav()}
        <div className="sidebar-footer">
          <span className="avatar">{initials(user?.fullName ?? '')}</span>
          <div>
            <strong>{user?.fullName}</strong>
            <span>{user?.username}</span>
          </div>
        </div>
      </aside>
      <div className="app-content">
        <header className="app-header">
          <div className="breadcrumb">
            <Button
              ref={trigger}
              variant="ghost"
              className="menu-button"
              aria-label="Mở menu điều hướng"
              aria-expanded={mobile}
              onClick={() => setMobile(true)}
            >
              <Menu size={20} />
            </Button>
            <span>{title}</span>
          </div>
          <div className="header-actions">
            <span className="avatar-chip">
              <span className="avatar">{initials(user?.fullName ?? '')}</span>
              <span>{user?.fullName}</span>
            </span>
            <Button
              variant="ghost"
              onClick={() => logout.mutate()}
              aria-label="Đăng xuất khỏi hệ thống"
              pending={logout.isPending}
            >
              <LogOut size={16} />
              <span>Đăng xuất</span>
            </Button>
          </div>
        </header>
        <main className="main-viewport" id="main-content" tabIndex={-1}>
          <Outlet />
        </main>
      </div>
      <Dialog.Root open={mobile} onOpenChange={setMobile}>
        <Dialog.Portal>
          <Dialog.Overlay className="dialog-backdrop" />
          <Dialog.Content
            className="mobile-drawer"
            aria-label="Thanh điều hướng di động"
            onCloseAutoFocus={(e) => {
              e.preventDefault();
              trigger.current?.focus();
            }}
          >
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

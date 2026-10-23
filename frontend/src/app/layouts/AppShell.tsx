import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useSearchParams } from 'react-router-dom';
import {
  Bell,
  ChartNoAxesCombined,
  ChevronLeft,
  ChevronRight,
  History,
  LogOut,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  ShieldCheck,
  Sparkles,
  Users,
  Wrench,
  X,
} from 'lucide-react';
import * as Dialog from '@radix-ui/react-dialog';
import { useAuth } from '../providers/AuthProvider';
import { useMeQuery, useLogoutMutation } from '../../features/auth/hooks/useAuthQueries';
import { useUnreadCountQuery } from '../../features/notification/hooks/useNotifications';
import { Button, RoleBadge } from '../../shared/components/ui';
import { roles, type Role } from '../../shared/types/contracts';
import { initials } from '../../shared/utils/format';

const UI_VARIANTS = [
  { key: 'A', label: 'A · Modern Navy & Slate' },
  { key: 'B', label: 'B · Clean Light Minimal' },
  { key: 'C', label: 'C · Automotive Industrial' },
] as const;
type UiVariantKey = (typeof UI_VARIANTS)[number]['key'];

const links: ReadonlyArray<{
  to: string;
  label: string;
  icon: typeof Bell;
  allowedRoles: readonly Role[];
}> = [
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
  const [searchParams, setSearchParams] = useSearchParams();
  const [mobile, setMobile] = useState(false);
  const [collapsed, setCollapsed] = useState(false);
  const trigger = useRef<HTMLButtonElement>(null);

  const rawVariant = searchParams.get('variant')?.toUpperCase();
  const variant: UiVariantKey = rawVariant === 'B' || rawVariant === 'C' ? rawVariant : 'A';

  const setVariant = (next: UiVariantKey) => {
    const updated = new URLSearchParams(searchParams);
    if (next === 'A') updated.delete('variant');
    else updated.set('variant', next);
    setSearchParams(updated, { replace: true });
  };

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.tagName === 'SELECT' ||
          target.isContentEditable)
      ) {
        return;
      }
      if (!event.altKey) return;
      const index = UI_VARIANTS.findIndex((item) => item.key === variant);
      if (event.key === 'ArrowLeft') {
        event.preventDefault();
        const prev = UI_VARIANTS[(index - 1 + UI_VARIANTS.length) % UI_VARIANTS.length];
        setVariant(prev.key);
      } else if (event.key === 'ArrowRight') {
        event.preventDefault();
        const next = UI_VARIANTS[(index + 1) % UI_VARIANTS.length];
        setVariant(next.key);
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  });

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

  const currentVariantIndex = UI_VARIANTS.findIndex((item) => item.key === variant);
  const currentVariant = UI_VARIANTS[currentVariantIndex] ?? UI_VARIANTS[0];

  return (
    <div className={`app-layout ${collapsed ? 'sidebar-collapsed' : ''}`} data-ui-variant={variant}>
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
              AutoGarage<small>ENTERPRISE WORKSPACE</small>
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
              className="mobile-menu"
              variant="ghost"
              aria-label="Mở menu"
              onClick={() => setMobile(true)}
              ref={trigger}
            >
              <Menu size={20} />
            </Button>
            <span>Không gian làm việc</span>
            <ChevronRight size={14} aria-hidden />
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
              pending={logout.isPending}
              onClick={() => logout.mutate()}
              aria-label="Đăng xuất"
            >
              <LogOut size={17} />
              <span className="logout-label">Đăng xuất</span>
            </Button>
          </div>
        </header>
        {me.error && (
          <div className="session-banner" role="status">
            Chưa thể kiểm tra lại thông tin tài khoản.{' '}
            <button
              type="button"
              onClick={() => {
                void me.refetch();
              }}
            >
              Thử lại
            </button>
          </div>
        )}
        <main id="main-content" className="workspace">
          <Outlet />
        </main>
      </div>
      {import.meta.env.MODE !== 'production' && (
        <div className="prototype-switcher" role="region" aria-label="Bộ chuyển giao diện mẫu">
          <Sparkles size={14} className="prototype-switcher-icon" aria-hidden />
          <button
            type="button"
            className="prototype-switcher-arrow"
            aria-label="Giao diện trước"
            onClick={() => {
              const prev =
                UI_VARIANTS[(currentVariantIndex - 1 + UI_VARIANTS.length) % UI_VARIANTS.length];
              setVariant(prev.key);
            }}
          >
            <ChevronLeft size={15} />
          </button>
          <span className="prototype-switcher-label">{currentVariant.label}</span>
          <button
            type="button"
            className="prototype-switcher-arrow"
            aria-label="Giao diện tiếp theo"
            onClick={() => {
              const next = UI_VARIANTS[(currentVariantIndex + 1) % UI_VARIANTS.length];
              setVariant(next.key);
            }}
          >
            <ChevronRight size={15} />
          </button>
        </div>
      )}
      <Dialog.Root open={mobile} onOpenChange={setMobile}>
        <Dialog.Portal>
          <Dialog.Overlay className="overlay" />
          <Dialog.Content
            className="mobile-nav"
            onCloseAutoFocus={(event) => {
              event.preventDefault();
              trigger.current?.focus();
            }}
          >
            <div className="mobile-nav-header">
              <Dialog.Title>AutoGarage</Dialog.Title>
              <Dialog.Close asChild>
                <Button variant="ghost" aria-label="Đóng menu">
                  <X size={20} />
                </Button>
              </Dialog.Close>
            </div>
            <Dialog.Description className="sr-only">Chọn phân hệ làm việc</Dialog.Description>
            {nav(() => setMobile(false))}
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog.Root>
    </div>
  );
}

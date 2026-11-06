import { Fragment, useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Globe, House, LogOut, Menu, PanelLeftClose, PanelLeftOpen, Wrench, X } from 'lucide-react';
import * as Dialog from '@radix-ui/react-dialog';
import { useAuth } from '../providers/AuthProvider';
import { useMeQuery, useLogoutMutation } from '../../features/auth/hooks/useAuthQueries';
import { useUnreadCountQuery } from '../../features/notification/hooks/useNotifications';
import { NotificationPopover } from '../../features/notification/components/NotificationPopover';
import { Button, RoleBadge } from '../../shared/components/ui';
import { initials } from '../../shared/utils/format';
import { isChildActive, isNavActive, navHref } from './navMatch';
import { navForRole, sections, type NavItem } from './navItems';

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

  const labelOf = (link: NavItem) =>
    user?.role === 'CUSTOMER' && link.customerLabel
      ? link.customerLabel
      : user?.role === 'TECHNICIAN' && link.technicianLabel
        ? link.technicianLabel
        : link.label;

  const allowed = navForRole(user?.role);
  const currentLink = allowed.find((link) =>
    isNavActive(link, location.pathname, location.search, allowed),
  );
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
              const href = navHref(link);
              // Several entries can open the same page on different tabs, so highlight by tab, not path.
              const active = isNavActive(link, location.pathname, location.search, allowed);
              return (
                <Fragment key={href}>
                  <NavLink
                    to={href}
                    onClick={close}
                    title={collapsed ? label : undefined}
                    aria-current={active ? 'page' : undefined}
                    className={`nav-item ${active ? 'active' : ''}`}
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
                  {active && link.children && !collapsed && (
                    <div className="nav-children" role="group" aria-label={label}>
                      {link.children.map((child) => {
                        const childActive = isChildActive(
                          link,
                          child,
                          location.pathname,
                          location.search,
                        );
                        return (
                          <NavLink
                            key={child.tab}
                            to={`${link.to}?tab=${child.tab}`}
                            onClick={close}
                            aria-current={childActive ? 'page' : undefined}
                            className={`nav-subitem ${childActive ? 'active' : ''}`}
                          >
                            {child.label}
                          </NavLink>
                        );
                      })}
                    </div>
                  )}
                </Fragment>
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
          <NavLink to="/" className="brand" title="Về trang chủ AutoGarage">
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
            {collapsed ? <PanelLeftOpen size={15} /> : <PanelLeftClose size={17} />}
          </button>
        </div>
        {nav()}
        <NavLink
          to="/"
          end
          className="nav-item home-link"
          title={collapsed ? 'Trang chủ' : undefined}
        >
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
            <NotificationPopover />
            <div className="header-user-pill">
              <span className="header-name">{user?.fullName}</span>
              {user && <RoleBadge role={user.role} />}
            </div>
            <Link to="/" className="button button-ghost" title="Về trang chủ AutoGarage">
              <Globe size={16} />
              <span className="btn-label-desktop">Trang chủ</span>
            </Link>
            <Button
              variant="ghost"
              onClick={async () => {
                await logout.mutateAsync();
                navigate('/login');
              }}
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

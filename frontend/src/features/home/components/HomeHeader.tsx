import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Menu, Phone, Wrench, X } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { homePathForRole } from '../../../app/access';
import { useLogoutMutation } from '../../auth/hooks/useAuthQueries';
import { Button } from '../../../shared/components/ui';
import { GARAGE_INFO, NAV_ANCHORS } from '../constants/homeData';

export function HomeHeader() {
  const { user } = useAuth();
  const logout = useLogoutMutation();
  const [open, setOpen] = useState(false);

  return (
    <header className="home-header">
      <div className="home-container home-header-inner">
        <Link to="/" className="home-brand" aria-label={`${GARAGE_INFO.name} - Trang chủ`}>
          <span className="home-brand-mark">
            <Wrench size={20} aria-hidden />
          </span>
          <span>{GARAGE_INFO.name}</span>
        </Link>
        <nav
          id="home-nav"
          className={`home-nav ${open ? 'is-open' : ''}`}
          aria-label="Mục trên trang chủ"
        >
          {NAV_ANCHORS.map((anchor) => (
            <a key={anchor.id} href={`#${anchor.id}`} onClick={() => setOpen(false)}>
              {anchor.label}
            </a>
          ))}
        </nav>
        <a className="home-hotline" href={`tel:${GARAGE_INFO.hotline.replace(/\s/g, '')}`}>
          <Phone size={16} aria-hidden />
          <span>{GARAGE_INFO.hotline}</span>
        </a>
        <div className="home-actions">
          {user ? (
            <>
              <span className="home-greeting">Xin chào, {user.fullName}</span>
              <Link className="button button-primary" to={homePathForRole(user.role)}>
                Vào xưởng / Quản lý
              </Link>
              <Button variant="ghost" onClick={() => logout.mutate()} pending={logout.isPending}>
                Đăng xuất
              </Button>
            </>
          ) : (
            <>
              <Link className="button button-ghost" to="/register">
                Đăng ký
              </Link>
              <Link className="button button-primary" to="/login">
                Đăng nhập
              </Link>
            </>
          )}
        </div>
        <button
          type="button"
          className="home-menu-toggle"
          aria-label={open ? 'Đóng menu' : 'Mở menu'}
          aria-expanded={open}
          aria-controls="home-nav"
          onClick={() => setOpen((prev) => !prev)}
        >
          {open ? <X size={22} /> : <Menu size={22} />}
        </button>
      </div>
    </header>
  );
}

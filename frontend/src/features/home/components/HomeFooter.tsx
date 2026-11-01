import { Link } from 'react-router-dom';
import { GARAGE_INFO, NAV_ANCHORS } from '../constants/homeData';

export function HomeFooter() {
  return (
    <footer className="home-footer">
      <div className="home-container home-footer-inner">
        <div>
          <strong>{GARAGE_INFO.name}</strong>
          <p>{GARAGE_INFO.address}</p>
          <p>Giờ mở cửa: {GARAGE_INFO.openingHours}</p>
        </div>
        <nav aria-label="Liên kết chân trang">
          {NAV_ANCHORS.map((anchor) => (
            <a key={anchor.id} href={`#${anchor.id}`}>
              {anchor.label}
            </a>
          ))}
          <Link to="/login">Đăng nhập</Link>
        </nav>
        <div>
          <strong>Hotline & Cứu hộ 24/7</strong>
          <p>
            <a
              href={`tel:${GARAGE_INFO.hotline.replace(/\s/g, '')}`}
              style={{ color: '#ffffff', textDecoration: 'none', fontWeight: 600 }}
            >
              {GARAGE_INFO.hotline}
            </a>
          </p>
        </div>
      </div>
      <p className="home-copyright">
        © {new Date().getFullYear()} {GARAGE_INFO.name}. Đại lý dịch vụ ô tô được chứng nhận.
      </p>
    </footer>
  );
}

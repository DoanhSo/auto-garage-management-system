import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { MapPin, Phone, Wrench } from 'lucide-react';
import '../auth.css';

const HOTLINE = '0963 943 872';
const GARAGE_ADDRESS = 'Số 226 Phố Tây Sơn, P. Trung Liệt, Q. Đống Đa, Hà Nội';

export function AuthLayout({
  title,
  description,
  variant = 'login',
  children,
}: {
  title: string;
  description: string;
  variant?: 'login' | 'register';
  children: ReactNode;
}) {
  return (
    <div className="auth-page">
      <header className="auth-header">
        <div className="auth-header-inner">
          <Link to="/" className="auth-brand" aria-label="AutoGarage - Về trang chủ">
            <span className="auth-brand-mark">
              <Wrench size={18} aria-hidden />
            </span>
            AutoGarage
          </Link>
          <div className="auth-header-right">
            <a href="tel:0963943872" className="auth-hotline">
              <Phone size={15} aria-hidden />
              <span>{HOTLINE}</span>
            </a>
            <Link to="/" className="button button-ghost">
              Trang chủ
            </Link>
          </div>
        </div>
      </header>

      <main className="auth-main" id="main-content" tabIndex={-1}>
        <div className={`auth-card auth-card-${variant}`}>
          <aside className="auth-card-media" aria-label="Hình ảnh xưởng AutoGarage">
            <img
              src="/images/home/garage-lounge.jpg"
              alt="Khu vực tiếp nhận và phòng chờ khách hàng AutoGarage"
            />
            <div className="auth-card-media-overlay">
              <span className="auth-media-badge">Xưởng dịch vụ tiêu chuẩn hãng</span>
              <p>Chẩn đoán số hóa OBD-II · Cân chỉnh 3D Hunter · 100% Phụ tùng chính hãng</p>
              <small>
                <MapPin size={13} aria-hidden /> {GARAGE_ADDRESS} · Hotline {HOTLINE}
              </small>
            </div>
          </aside>

          <section className="auth-card-form">
            <header className="auth-form-header">
              <h1>{title}</h1>
              <p>{description}</p>
            </header>
            {children}
            <p className="auth-legal-text">
              Khi tiếp tục, tôi đồng ý với các <a href="/#services">Điều khoản sử dụng</a> và{' '}
              <a href="/#contact">Chính sách bảo mật</a> của AutoGarage.
            </p>
          </section>
        </div>
      </main>
    </div>
  );
}

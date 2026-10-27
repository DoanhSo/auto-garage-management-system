import type { ReactNode } from 'react';
import { Wrench } from 'lucide-react';
import '../auth.css';

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
      <div className="auth-shell">
        <main className={`auth-main auth-main-${variant}`} id="main-content" tabIndex={-1}>
          <div className={`auth-content auth-content-${variant}`}>
            <div className="auth-brand">
              <span className="auth-brand-icon">
                <Wrench size={22} aria-hidden />
              </span>
              <span>AutoGarage</span>
            </div>
            <header className="auth-heading">
              <h1>{title}</h1>
              <p>{description}</p>
            </header>
            {children}
          </div>
        </main>
        <aside className="auth-visual" aria-label="Giới thiệu AutoGarage">
          <span className="auth-eyebrow">GARA CỦA BẠN, TRONG TẦM TAY</span>
          <h2>
            Chăm sóc xe,
            <br />
            thật đơn giản.
          </h2>
          <p>Đặt lịch thuận tiện, theo dõi dịch vụ và đồng hành cùng chiếc xe của bạn.</p>
          <img
            className="auth-photo"
            src="/images/login-background.jpg"
            alt="Xe ô tô trong xưởng dịch vụ hiện đại"
            width={1920}
            height={1080}
          />
          <div className="auth-visual-footer">
            <span className="auth-visual-line" aria-hidden />
            Một nơi cho mọi nhu cầu chăm sóc xe.
          </div>
        </aside>
      </div>
      <p className="auth-footer">AutoGarage · Quản lý và chăm sóc xe</p>
    </div>
  );
}

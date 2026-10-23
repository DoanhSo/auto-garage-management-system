import { ExternalLink, KeyRound, LogOut, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { env } from '../../../shared/config/env';
import {
  Button,
  PageHeader,
  ReadOnlyField,
  RoleBadge,
  StatusBadge,
} from '../../../shared/components/ui';
import { initials } from '../../../shared/utils/format';
import { useLogoutMutation } from '../hooks/useAuthQueries';

export function AccountPage() {
  const { user } = useAuth();
  const logout = useLogoutMutation();
  if (!user) return null;
  const keycloakAccountUrl = `${env.keycloakPublicOrigin.replace(/\/$/, '')}/account`;
  return (
    <>
      <PageHeader
        eyebrow="KHÔNG GIAN CÁ NHÂN"
        title="Tài khoản của tôi"
        description="Thông tin truy cập và bảo mật tài khoản qua Keycloak OIDC."
      />
      <div className="account-profile-banner panel">
        <span className="avatar avatar-lg">{initials(user.fullName)}</span>
        <div className="account-profile-info">
          <strong>{user.fullName}</strong>
          <span>
            @{user.username} {user.email ? `· ${user.email}` : ''}
          </span>
        </div>
        <div className="account-profile-badges">
          <RoleBadge role={user.role} />
          <StatusBadge active={user.active} />
        </div>
      </div>
      <div className="account-grid">
        <section className="panel">
          <div className="section-heading">
            <ShieldCheck size={20} />
            <h2>Thông tin tài khoản</h2>
          </div>
          <dl className="detail-list">
            <ReadOnlyField label="Họ và tên">{user.fullName}</ReadOnlyField>
            <ReadOnlyField label="Tên đăng nhập">{user.username}</ReadOnlyField>
            <ReadOnlyField label="Email">{user.email || 'Chưa cung cấp'}</ReadOnlyField>
            <ReadOnlyField label="Vai trò">
              <RoleBadge role={user.role} />
            </ReadOnlyField>
          </dl>
          <p className="field-hint">
            Liên hệ quản trị viên khi cần cập nhật thông tin hoặc quyền truy cập.
          </p>
        </section>
        <section className="panel">
          <div className="section-heading">
            <KeyRound size={20} />
            <h2>Bảo mật &amp; Phiên đăng nhập (Keycloak OIDC)</h2>
          </div>
          <p className="field-hint">
            Mật khẩu và phương thức xác thực của bạn được quản lý tập trung trên máy chủ định danh
            Keycloak (<code>autogarage-realm</code>). Bạn cũng có thể thu hồi toàn bộ phiên đăng
            nhập hiện tại trên hệ thống AutoGarage bất kỳ lúc nào.
          </p>
          <div className="form-actions">
            <a
              className="button"
              href={keycloakAccountUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              <ExternalLink size={16} />
              Mở cổng tài khoản Keycloak
            </a>
            <Button
              variant="primary"
              pending={logout.isPending}
              onClick={() => {
                void logout.mutateAsync();
              }}
            >
              <LogOut size={16} />
              Thu hồi mọi phiên đăng nhập
            </Button>
          </div>
        </section>
      </div>
    </>
  );
}

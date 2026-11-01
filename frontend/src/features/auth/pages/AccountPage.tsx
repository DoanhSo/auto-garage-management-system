import { ExternalLink, KeyRound, LogOut, ShieldCheck, UserRound } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { env } from '../../../shared/config/env';
import {
  Button,
  Loading,
  PageHeader,
  QueryError,
  ReadOnlyField,
  RoleBadge,
  StatusBadge,
} from '../../../shared/components/ui';
import { initials, money } from '../../../shared/utils/format';
import { customerTypeLabels } from '../../../shared/types/contracts';
import { useMyCustomerQuery } from '../../customer/hooks/useCustomers';
import { useLogoutMutation } from '../hooks/useAuthQueries';

export function AccountPage() {
  const { user } = useAuth();
  const logout = useLogoutMutation();
  const isCustomer = user?.role === 'CUSTOMER';
  const customer = useMyCustomerQuery(isCustomer);
  if (!user) return null;
  const keycloakAccountUrl = `${env.keycloakPublicOrigin.replace(/\/$/, '')}/account`;
  return (
    <>
      <PageHeader
        eyebrow="KHÔNG GIAN CÁ NHÂN"
        title="Tài khoản của tôi"
        description="Quản lý thông tin hồ sơ cá nhân, bảo mật mật khẩu và các phiên đăng nhập trên hệ thống."
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
        {isCustomer && (
          <section className="panel">
            <div className="section-heading">
              <UserRound size={20} />
              <h2>Hồ sơ khách hàng &amp; Chính sách công nợ</h2>
            </div>
            {customer.isPending ? (
              <Loading label="Đang tải hồ sơ khách hàng…" />
            ) : customer.error ? (
              <QueryError
                error={customer.error}
                retry={() => {
                  void customer.refetch();
                }}
              />
            ) : (
              <dl className="detail-list">
                <ReadOnlyField label="Mã khách hàng">{customer.data.customerCode}</ReadOnlyField>
                <ReadOnlyField label="Số điện thoại">{customer.data.phone}</ReadOnlyField>
                <ReadOnlyField label="Phân loại">
                  {customerTypeLabels[customer.data.customerType]}
                </ReadOnlyField>
                <ReadOnlyField label="Hạn mức tín dụng">
                  {money(customer.data.creditLimit)}
                </ReadOnlyField>
                <ReadOnlyField label="Kỳ hạn thanh toán">
                  {customer.data.paymentTermDays} ngày
                </ReadOnlyField>
              </dl>
            )}
          </section>
        )}
        <section className="panel">
          <div className="section-heading">
            <KeyRound size={20} />
            <h2>Bảo mật &amp; Phiên đăng nhập</h2>
          </div>
          <p className="field-hint">
            Mật khẩu, xác thực hai lớp (2FA) và thông tin liên hệ được bảo vệ tập trung. Bạn có thể
            cập nhật mật khẩu hoặc thu hồi các phiên đang hoạt động bất cứ lúc nào.
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

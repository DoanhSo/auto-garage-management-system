import { useState } from 'react';
import { Phone } from 'lucide-react';
import { Modal } from '../../../shared/components/ui';
import { GARAGE_INFO } from '../../home/constants/homeData';

type Audience = 'customer' | 'staff';

const TABS: { id: Audience; label: string }[] = [
  { id: 'customer', label: 'Khách hàng' },
  { id: 'staff', label: 'Nhân viên xưởng' },
];

export function ForgotPasswordModal({
  open,
  onOpenChange,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [audience, setAudience] = useState<Audience>('customer');
  const hotlineHref = `tel:${GARAGE_INFO.hotline.replace(/\s/g, '')}`;
  return (
    <Modal
      open={open}
      onOpenChange={onOpenChange}
      title="Quên mật khẩu"
      description="Chọn nhóm tài khoản của bạn để xem cách lấy lại quyền truy cập."
      size="sm"
    >
      <div className="auth-forgot-tabs" role="tablist" aria-label="Nhóm tài khoản">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={audience === tab.id}
            className={`auth-forgot-tab${audience === tab.id ? ' is-active' : ''}`}
            onClick={() => setAudience(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div className="auth-forgot-body" role="tabpanel">
        {audience === 'customer' ? (
          <>
            <p>
              Liên hệ gara và cung cấp email hoặc số điện thoại đã đăng ký. Nhân viên sẽ xác minh và
              hướng dẫn bạn đặt lại mật khẩu.
            </p>
            <a className="button button-primary" href={hotlineHref}>
              <Phone size={16} aria-hidden />
              Gọi {GARAGE_INFO.hotline}
            </a>
          </>
        ) : (
          <p>
            Tài khoản nhân viên do quản trị viên cấp. Hãy liên hệ quản đốc xưởng hoặc quản trị viên
            hệ thống để được cấp lại mật khẩu tạm thời.
          </p>
        )}
      </div>
    </Modal>
  );
}

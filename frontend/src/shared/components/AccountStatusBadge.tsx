import type { AccountStatus, AccountSummary } from '../types/contracts';
import { formatDateTime } from '../utils/format';
import { Badge } from './ui';

const STATUS: Record<
  AccountStatus,
  { label: string; variant: 'secondary' | 'warning' | 'success' | 'danger' }
> = {
  NONE: { label: 'Chưa có tài khoản', variant: 'secondary' },
  PENDING: { label: 'Chưa đăng nhập lần nào', variant: 'warning' },
  ACTIVE: { label: 'Đang hoạt động', variant: 'success' },
  LOCKED: { label: 'Bị khóa', variant: 'danger' },
};

/** Sign-in state of a customer, employee or user, with the last sign-in when there is one. */
export function AccountStatusBadge({
  account,
}: {
  account?: Pick<AccountSummary, 'status' | 'lastLoginAt'> | null;
}) {
  if (!account) return null;
  const { label, variant } = STATUS[account.status];
  return (
    <span className="account-status">
      <Badge variant={variant}>{label}</Badge>
      {account.lastLoginAt && account.status !== 'NONE' && (
        <span className="muted">Đăng nhập gần nhất {formatDateTime(account.lastLoginAt)}</span>
      )}
    </span>
  );
}

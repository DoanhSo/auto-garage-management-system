import { DEMO_ACCOUNTS, type DemoAccount } from '../constants/demoAccounts';

export function DemoAccountSelector({
  disabled,
  onSelect,
}: {
  disabled?: boolean;
  onSelect: (account: DemoAccount) => void;
}) {
  return (
    <div className="demo-accounts-panel" aria-label="Điền nhanh tài khoản kiểm thử">
      <span className="demo-accounts-title">Tài khoản kiểm thử</span>
      <div className="demo-accounts-list">
        {DEMO_ACCOUNTS.map((account) => (
          <button
            key={account.role}
            type="button"
            className="demo-account-chip"
            title={`${account.label} — ${account.note}`}
            disabled={disabled}
            onClick={() => onSelect(account)}
          >
            {account.badge}
          </button>
        ))}
      </div>
    </div>
  );
}

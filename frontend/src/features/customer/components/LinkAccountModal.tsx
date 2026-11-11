import { useState } from 'react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { Button, Empty, FormError, Loading, Sheet } from '../../../shared/components/ui';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import type { Customer } from '../../../shared/types/contracts';
import { useCustomersQuery, useLinkCustomerAccountMutation } from '../hooks/useCustomers';

function LinkForm({ target, onDone }: { target: Customer; onDone: () => void }) {
  const notify = useToast();
  const link = useLinkCustomerAccountMutation();
  const [search, setSearch] = useState(target.email ?? target.phone ?? '');
  const [sourceId, setSourceId] = useState<string | null>(null);
  const debounced = useDebouncedValue(search);
  const candidates = useCustomersQuery({
    search: debounced.trim() || undefined,
    hasAccount: true,
    isActive: true,
  });
  const options = (candidates.data ?? []).filter((c) => c.id !== target.id);

  async function onLink() {
    if (!sourceId) return;
    try {
      await link.mutateAsync({ id: target.id, sourceCustomerId: sourceId });
      notify(`Đã nối tài khoản vào hồ sơ ${target.customerCode}`);
      onDone();
    } catch {
      // The error is shown in the form.
    }
  }

  return (
    <div className="editor-form">
      <FormError message={link.error ? errorMessage(link.error) : undefined} />
      <label className="field-label" htmlFor="link-account-search">
        Tìm hồ sơ khách đã tự đăng ký (tên, số điện thoại, mã khách)
      </label>
      <input
        id="link-account-search"
        className="input"
        type="search"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
      />
      {candidates.isLoading && <Loading label="Đang tìm hồ sơ có tài khoản…" />}
      {candidates.data && options.length === 0 && (
        <Empty
          title="Không có hồ sơ phù hợp"
          description="Chỉ hiện hồ sơ đang hoạt động và đã có tài khoản đăng nhập."
        />
      )}
      {options.length > 0 && (
        <ul className="link-account-options" aria-label="Hồ sơ đang giữ tài khoản">
          {options.map((c) => (
            <li key={c.id}>
              <label>
                <input
                  type="radio"
                  name="link-account-source"
                  value={c.id}
                  checked={sourceId === c.id}
                  onChange={() => setSourceId(c.id)}
                />
                <span>
                  <strong>{c.fullName}</strong> · {c.customerCode}
                  <span className="muted">
                    {' '}
                    · {c.phone}
                    {c.email ? ` · ${c.email}` : ''}
                    {c.account?.username ? ` · tài khoản ${c.account.username}` : ''}
                  </span>
                </span>
              </label>
            </li>
          ))}
        </ul>
      )}
      <p className="field-hint">
        Tài khoản được chuyển sang hồ sơ {target.customerCode}; hồ sơ đã chọn bị ngừng hoạt động.
        Không gộp được nếu hồ sơ đó đã có xe, lịch hẹn, phiếu sửa chữa, hóa đơn hoặc bảo hành.
      </p>
      <div className="form-actions">
        <Button type="button" disabled={link.isPending} onClick={onDone}>
          Hủy
        </Button>
        <Button variant="primary" pending={link.isPending} disabled={!sourceId} onClick={onLink}>
          Nối tài khoản
        </Button>
      </div>
    </div>
  );
}

/** Moves the account of a self-registered duplicate profile onto an existing customer. */
export function LinkAccountModal({
  customer,
  onClose,
}: {
  customer: Customer | null;
  onClose: () => void;
}) {
  return (
    <Sheet
      open={Boolean(customer)}
      title="Nối tài khoản đã đăng ký"
      description={
        customer ? `Dùng khi ${customer.fullName} đã tự đăng ký và hệ thống tạo hồ sơ riêng.` : ''
      }
      onClose={onClose}
      placement="center"
      size="md"
    >
      {customer && <LinkForm key={customer.id} target={customer} onDone={onClose} />}
    </Sheet>
  );
}

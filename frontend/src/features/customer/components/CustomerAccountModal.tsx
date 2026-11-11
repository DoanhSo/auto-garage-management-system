import { useState, type FormEvent } from 'react';
import { KeyRound } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { useUiPolicy } from '../../../shared/config/policy';
import { Button, Field, FormError, Sheet } from '../../../shared/components/ui';
import type { Customer } from '../../../shared/types/contracts';
import { useCreateCustomerAccountMutation } from '../hooks/useCustomers';

const PASSWORD_ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789';

/** Random password with letters, digits and a symbol, long enough for the password policy. */
function generatePassword(length: number): string {
  const values = new Uint32Array(length);
  crypto.getRandomValues(values);
  const body = Array.from(values, (v) => PASSWORD_ALPHABET[v % PASSWORD_ALPHABET.length]).join('');
  return `${body.slice(0, length - 3)}#${values[0] % 10}${values[1] % 10}`;
}

/** Suggested username: the email's local part, otherwise the phone number. */
function suggestUsername(customer: Customer): string {
  const fromEmail = customer.email
    ?.split('@')[0]
    ?.toLowerCase()
    .replace(/[^a-z0-9._-]/g, '.');
  if (fromEmail && fromEmail.length >= 3) return fromEmail;
  return `kh${customer.phone.replace(/\D/g, '')}`;
}

function AccountForm({ customer, onDone }: { customer: Customer; onDone: () => void }) {
  const notify = useToast();
  const { data: policy } = useUiPolicy();
  const minLength = policy?.passwordMinLength ?? 12;
  const create = useCreateCustomerAccountMutation();
  const [username, setUsername] = useState(() => suggestUsername(customer));
  const [email, setEmail] = useState(customer.email ?? '');
  const [password, setPassword] = useState('');

  async function onSubmit(event: FormEvent) {
    event.preventDefault();
    try {
      await create.mutateAsync({
        id: customer.id,
        values: { username: username.trim(), email: email.trim() || null, password },
      });
      notify(`Đã tạo tài khoản ${username.trim()} cho ${customer.fullName}`);
      onDone();
    } catch {
      // The error is shown in the form.
    }
  }

  return (
    <form onSubmit={onSubmit} className="editor-form account-editor-form" noValidate>
      <FormError message={create.error ? errorMessage(create.error) : undefined} />
      <Field label="Tên đăng nhập" required hint="3–100 ký tự: chữ, số và . _ -">
        <input
          className="input"
          value={username}
          autoComplete="off"
          disabled={create.isPending}
          onChange={(e) => setUsername(e.target.value)}
        />
      </Field>
      <Field label="Email" hint="Để trống nếu khách không dùng email">
        <input
          className="input"
          type="email"
          value={email}
          disabled={create.isPending}
          onChange={(e) => setEmail(e.target.value)}
        />
      </Field>
      <div className="full-width-field">
        <Field
          label="Mật khẩu ban đầu"
          required
          hint={`Tối thiểu ${minLength} ký tự. Đưa mật khẩu này cho khách để đăng nhập lần đầu.`}
        >
          <div className="inline-input-action">
            <input
              className="input"
              value={password}
              autoComplete="new-password"
              disabled={create.isPending}
              onChange={(e) => setPassword(e.target.value)}
            />
            <Button
              type="button"
              disabled={create.isPending}
              onClick={() => setPassword(generatePassword(Math.max(minLength, 14)))}
            >
              <KeyRound size={15} /> Sinh mật khẩu
            </Button>
          </div>
        </Field>
      </div>
      <div className="form-actions account-editor-actions">
        <Button type="button" disabled={create.isPending} onClick={onDone}>
          Hủy
        </Button>
        <Button
          variant="primary"
          type="submit"
          pending={create.isPending}
          disabled={!username.trim() || password.length < minLength}
        >
          Tạo tài khoản
        </Button>
      </div>
    </form>
  );
}

/** Front desk creates a sign-in account for a customer profile that has none. */
export function CustomerAccountModal({
  customer,
  onClose,
}: {
  customer: Customer | null;
  onClose: () => void;
}) {
  return (
    <Sheet
      open={Boolean(customer)}
      title="Tạo tài khoản đăng nhập"
      description={
        customer
          ? `Tài khoản gắn ngay với hồ sơ ${customer.customerCode} · ${customer.fullName}, khách thấy đúng xe và lịch sử của mình.`
          : ''
      }
      onClose={onClose}
      placement="center"
      size="md"
    >
      {customer && <AccountForm key={customer.id} customer={customer} onDone={onClose} />}
    </Sheet>
  );
}

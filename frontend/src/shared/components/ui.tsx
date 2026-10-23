import {
  cloneElement,
  isValidElement,
  useId,
  useState,
  type ComponentPropsWithRef,
  type InputHTMLAttributes,
  type ReactNode,
} from 'react';
import * as Dialog from '@radix-ui/react-dialog';
import * as AlertDialog from '@radix-ui/react-alert-dialog';
import { AlertCircle, Eye, EyeOff, LoaderCircle, X } from 'lucide-react';
import type { Role } from '../types/contracts';
import { roleLabels } from '../types/contracts';
import { errorMessage } from '../api/errors';

export function Button({
  variant = 'secondary',
  pending = false,
  children,
  className = '',
  disabled,
  ...props
}: ComponentPropsWithRef<'button'> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  pending?: boolean;
}) {
  return (
    <button
      type="button"
      {...props}
      disabled={disabled || pending}
      aria-busy={pending || undefined}
      className={`button button-${variant} ${className}`}
    >
      {pending && <LoaderCircle className="spin" size={16} aria-hidden />}
      {children}
    </button>
  );
}
export function Field({
  label,
  error,
  hint,
  children,
  required = false,
}: {
  label: string;
  error?: string;
  hint?: ReactNode;
  children: ReactNode;
  required?: boolean;
}) {
  const generated = useId();
  const element = isValidElement<InputHTMLAttributes<HTMLInputElement>>(children) ? children : null;
  const id = element?.props.id ?? generated;
  const showHint = !!hint && !error;
  const control = element
    ? cloneElement(element, {
        id,
        'aria-required': required || undefined,
        'aria-invalid': !!error,
        'aria-describedby':
          [showHint ? `${id}-hint` : '', error ? `${id}-error` : ''].filter(Boolean).join(' ') ||
          undefined,
      })
    : children;
  return (
    <div className="field">
      <label className="field-label" htmlFor={id}>
        {label}
        {required && <span className="required"> *</span>}
      </label>
      {control}
      {showHint && (
        <span className="field-hint" id={`${id}-hint`}>
          {hint}
        </span>
      )}
      {error && (
        <span className="field-error" id={`${id}-error`} role="alert">
          {error}
        </span>
      )}
    </div>
  );
}
export function PasswordInput(props: ComponentPropsWithRef<'input'>) {
  const [visible, setVisible] = useState(false);
  return (
    <span className="password-field">
      <input {...props} className="input" type={visible ? 'text' : 'password'} />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((value) => !value)}
        aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
        aria-pressed={visible}
      >
        {visible ? <EyeOff size={18} /> : <Eye size={18} />}
      </button>
    </span>
  );
}
export function FormError({ message }: { message?: string }) {
  return message ? (
    <div className="alert alert-error" role="alert">
      <AlertCircle size={18} aria-hidden />
      <span>{message}</span>
    </div>
  ) : null;
}
export function RoleBadge({ role }: { role: Role }) {
  return (
    <span className={`badge badge-neutral badge-role-${role.toLowerCase()}`}>
      {roleLabels[role]}
    </span>
  );
}
export function StatusBadge({ active }: { active: boolean }) {
  return (
    <span className={`badge ${active ? 'badge-success' : 'badge-neutral'}`}>
      <span className="status-dot" aria-hidden />
      {active ? 'Hoạt động' : 'Đã khóa'}
    </span>
  );
}
export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  actions?: ReactNode;
}) {
  return (
    <div className="page-header">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        <p className="description">{description}</p>
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}
export function Loading({ label = 'Đang tải dữ liệu…' }: { label?: string }) {
  return (
    <div className="loading-state" role="status">
      <LoaderCircle className="spin" size={20} />
      <span>{label}</span>
    </div>
  );
}
export function QueryError({ error, retry }: { error: unknown; retry?: () => void }) {
  return (
    <div className="empty-state" role="alert">
      <AlertCircle size={28} />
      <h2>Chưa thể tải dữ liệu</h2>
      <p>{errorMessage(error)}</p>
      {retry && <Button onClick={retry}>Thử lại</Button>}
    </div>
  );
}
export function Empty({
  title = 'Chưa có dữ liệu',
  description = 'Dữ liệu sẽ xuất hiện tại đây khi có phát sinh.',
  action,
}: {
  title?: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="empty-state">
      <div className="empty-mark" aria-hidden>
        —
      </div>
      <h2>{title}</h2>
      <p>{description}</p>
      {action}
    </div>
  );
}
export function Sheet({
  open,
  title,
  description,
  children,
  onClose,
  pending = false,
  onRestoreFocus,
  placement = 'side',
}: {
  open: boolean;
  title: string;
  description: string;
  children: ReactNode;
  onClose: () => void;
  pending?: boolean;
  onRestoreFocus?: () => void;
  placement?: 'side' | 'center';
}) {
  return (
    <Dialog.Root
      open={open}
      onOpenChange={(value) => {
        if (!value && !pending) onClose();
      }}
    >
      <Dialog.Portal>
        <Dialog.Overlay className="overlay" />
        <Dialog.Content
          className={`sheet ${placement === 'center' ? 'centered-dialog' : ''}`}
          onCloseAutoFocus={(event) => {
            if (onRestoreFocus) {
              event.preventDefault();
              onRestoreFocus();
            }
          }}
          onEscapeKeyDown={(event) => {
            if (pending) event.preventDefault();
          }}
          onPointerDownOutside={(event) => {
            if (pending) event.preventDefault();
          }}
        >
          <header className="sheet-header">
            <div>
              <Dialog.Title>{title}</Dialog.Title>
              <Dialog.Description>{description}</Dialog.Description>
            </div>
            <Button variant="ghost" aria-label="Đóng form" disabled={pending} onClick={onClose}>
              <X size={20} />
            </Button>
          </header>
          {children}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
export function ConfirmDialog({
  open,
  title,
  description,
  onCancel,
  onConfirm,
  pending = false,
  error,
  confirmLabel = 'Xác nhận',
  danger = false,
}: {
  open: boolean;
  title: string;
  description: string;
  onCancel: () => void;
  onConfirm: () => void;
  pending?: boolean;
  error?: string;
  confirmLabel?: string;
  danger?: boolean;
}) {
  return (
    <AlertDialog.Root
      open={open}
      onOpenChange={(value) => {
        if (!value && !pending) onCancel();
      }}
    >
      <AlertDialog.Portal>
        <AlertDialog.Overlay className="overlay" />
        <AlertDialog.Content className="confirm-dialog">
          <AlertDialog.Title>{title}</AlertDialog.Title>
          <AlertDialog.Description>{description}</AlertDialog.Description>
          <FormError message={error} />
          <div className="form-actions">
            <AlertDialog.Cancel asChild>
              <Button disabled={pending} onClick={onCancel}>
                Hủy
              </Button>
            </AlertDialog.Cancel>
            <AlertDialog.Action asChild>
              <Button
                variant={danger ? 'danger' : 'primary'}
                pending={pending}
                onClick={(event) => {
                  event.preventDefault();
                  onConfirm();
                }}
              >
                {confirmLabel}
              </Button>
            </AlertDialog.Action>
          </div>
        </AlertDialog.Content>
      </AlertDialog.Portal>
    </AlertDialog.Root>
  );
}
export function ReadOnlyField({ label, children }: { label: string; children: ReactNode }) {
  const id = useId();
  return (
    <div className="detail-field">
      <dt id={id}>{label}</dt>
      <dd aria-labelledby={id}>{children}</dd>
    </div>
  );
}

export function Badge({
  children,
  variant = 'secondary',
  className = '',
}: {
  children: ReactNode;
  variant?: 'primary' | 'secondary' | 'success' | 'warning' | 'danger' | 'outline';
  className?: string;
}) {
  const variantClass = {
    primary: 'badge-primary',
    secondary: 'badge-neutral',
    success: 'badge-success',
    warning: 'badge-warning',
    danger: 'badge-danger',
    outline: 'badge-outline',
  }[variant] || 'badge-neutral';

  return <span className={`badge ${variantClass} ${className}`}>{children}</span>;
}

export function Modal({
  open,
  title,
  children,
  onOpenChange,
  description = '',
}: {
  open: boolean;
  title: string;
  description?: string;
  children: ReactNode;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <Sheet
      open={open}
      title={title}
      description={description}
      placement="center"
      onClose={() => onOpenChange(false)}
    >
      {children}
    </Sheet>
  );
}

export function ConfirmModal({
  open,
  title,
  description,
  confirmLabel = 'Xác nhận',
  variant = 'primary',
  pending = false,
  onConfirm,
  onOpenChange,
}: {
  open: boolean;
  title: string;
  description: string;
  confirmLabel?: string;
  variant?: 'primary' | 'danger';
  pending?: boolean;
  onConfirm: () => void;
  onOpenChange: (open: boolean) => void;
}) {
  return (
    <ConfirmDialog
      open={open}
      title={title}
      description={description}
      confirmLabel={confirmLabel}
      danger={variant === 'danger'}
      pending={pending}
      onConfirm={onConfirm}
      onCancel={() => onOpenChange(false)}
    />
  );
}


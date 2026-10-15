import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { KeyRound, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Button,
  ConfirmDialog,
  Field,
  FormError,
  Loading,
  PageHeader,
  PasswordInput,
  QueryError,
  ReadOnlyField,
  RoleBadge,
} from '../../../shared/components/ui';
import { useChangePasswordMutation } from '../hooks/useAuthQueries';
import { passwordChangeSchema, passwordRequirement } from '../schemas/authSchemas';
import { ApiError, applyFormError } from '../../../shared/api/errors';
import type { UiPolicy } from '../../../shared/types/contracts';
import { useUnsavedChangesGuard } from '../../../shared/hooks/useUnsavedChangesGuard';
function PasswordForm({ policy }: { policy: UiPolicy }) {
  const { clearSession } = useAuth();
  const mutation = useChangePasswordMutation();
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isDirty },
  } = useForm<{
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
  }>({
    resolver: zodResolver(passwordChangeSchema(policy)),
    mode: 'onBlur',
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });
  const blocker = useUnsavedChangesGuard(isDirty && !mutation.isPending);
  const { reset: resetMutation } = mutation;
  useEffect(() => () => resetMutation(), [resetMutation]);
  const submit = handleSubmit(async (values) => {
    if (mutation.isPending) return;
    try {
      await mutation.mutateAsync({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      reset();
      clearSession('Mật khẩu đã thay đổi. Vui lòng đăng nhập lại bằng mật khẩu mới.');
    } catch (error) {
      if (error instanceof ApiError && error.code === 3005)
        setError(
          'currentPassword',
          { message: 'Mật khẩu hiện tại chưa đúng.' },
          { shouldFocus: true },
        );
      else applyFormError(error, setError, ['currentPassword', 'newPassword']);
    } finally {
      mutation.reset();
    }
  });
  return (
    <>
      <form noValidate onSubmit={submit} className="form-stack">
        <Field label="Mật khẩu hiện tại" required error={errors.currentPassword?.message}>
          <PasswordInput
            {...register('currentPassword')}
            autoComplete="current-password"
            disabled={mutation.isPending}
          />
        </Field>
        <Field
          label="Mật khẩu mới"
          required
          hint={passwordRequirement(policy)}
          error={errors.newPassword?.message}
        >
          <PasswordInput
            {...register('newPassword')}
            autoComplete="new-password"
            disabled={mutation.isPending}
          />
        </Field>
        <Field label="Xác nhận mật khẩu mới" required error={errors.confirmPassword?.message}>
          <PasswordInput
            {...register('confirmPassword')}
            autoComplete="new-password"
            disabled={mutation.isPending}
          />
        </Field>
        <FormError message={errors.root?.server?.message} />
        <p className="field-hint">
          Sau khi thay đổi, mọi token của tài khoản sẽ bị thu hồi. Bạn cần đăng nhập lại.
        </p>
        <div className="form-actions">
          <Button disabled={!isDirty || mutation.isPending} onClick={() => reset()}>
            Đặt lại
          </Button>
          <Button variant="primary" type="submit" pending={mutation.isPending}>
            <KeyRound size={16} />
            Đổi mật khẩu
          </Button>
        </div>
      </form>
      <ConfirmDialog
        open={blocker.state === 'blocked'}
        title="Rời form đổi mật khẩu?"
        description="Thông tin chưa lưu sẽ được xóa."
        onCancel={() => blocker.reset?.()}
        onConfirm={() => blocker.proceed?.()}
        confirmLabel="Rời trang"
      />
    </>
  );
}
export function AccountPage() {
  const { user } = useAuth();
  const policy = useUiPolicy();
  if (!user) return null;
  return (
    <>
      <PageHeader
        eyebrow="KHÔNG GIAN CÁ NHÂN"
        title="Tài khoản của tôi"
        description="Thông tin truy cập và bảo mật tài khoản."
      />
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
            <h2>Đổi mật khẩu</h2>
          </div>
          {policy.isPending ? (
            <Loading />
          ) : policy.error ? (
            <QueryError
              error={policy.error}
              retry={() => {
                void policy.refetch();
              }}
            />
          ) : (
            <PasswordForm policy={policy.data} />
          )}
        </section>
      </div>
    </>
  );
}

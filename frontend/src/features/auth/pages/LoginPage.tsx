import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, Navigate, useLocation } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useLoginMutation } from '../hooks/useAuthQueries';
import { loginSchema } from '../schemas/authSchemas';
import { Button, Field, FormError, PasswordInput } from '../../../shared/components/ui';
import { errorMessage } from '../../../shared/api/errors';
import { safeReturnTo } from '../../../shared/utils/format';
import { AuthLayout } from '../components/AuthLayout';

export function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const mutation = useLoginMutation();
  const {
    register,
    handleSubmit,
    resetField,
    setError,
    formState: { errors },
  } = useForm<{ identifier: string; password: string }>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    defaultValues: { identifier: '', password: '' },
  });
  const { reset } = mutation;
  useEffect(() => () => reset(), [reset]);
  if (auth.user)
    return (
      <Navigate
        to={safeReturnTo(
          (location.state as { returnTo?: unknown } | null)?.returnTo,
          auth.user.role === 'ADMIN' ? '/admin/reports' : '/notifications',
        )}
        replace
      />
    );
  const submit = handleSubmit(async (values) => {
    if (mutation.isPending) return;
    try {
      await mutation.mutateAsync(values);
      resetField('password');
    } catch (error) {
      setError('root.server', { message: errorMessage(error) });
    } finally {
      mutation.reset();
    }
  });
  return (
    <AuthLayout title="Đăng nhập" description="Chào mừng bạn trở lại. Nhập thông tin để tiếp tục.">
      {auth.notice && (
        <div className="alert" role="status">
          {auth.notice}
        </div>
      )}
      <form noValidate onSubmit={submit} className="form-stack">
        <Field label="Tên đăng nhập hoặc email" required error={errors.identifier?.message}>
          <input
            className="input"
            {...register('identifier')}
            autoComplete="username"
            autoFocus
            maxLength={254}
            placeholder="Tên đăng nhập hoặc email"
            disabled={mutation.isPending}
          />
        </Field>
        <Field label="Mật khẩu" required error={errors.password?.message}>
          <PasswordInput
            {...register('password')}
            autoComplete="current-password"
            placeholder="Nhập mật khẩu của bạn"
            disabled={mutation.isPending}
          />
        </Field>
        <FormError message={errors.root?.server?.message} />
        <Button type="submit" variant="primary" pending={mutation.isPending} className="full-width">
          {mutation.isPending ? 'Đang đăng nhập…' : 'Đăng nhập'}
          <ArrowRight size={17} aria-hidden />
        </Button>
      </form>
      <p className="auth-switch">
        Chưa có tài khoản? <Link to="/register">Đăng ký tài khoản</Link>
      </p>
      <p className="auth-note">Tài khoản nhân viên do quản trị viên gara cấp.</p>
    </AuthLayout>
  );
}

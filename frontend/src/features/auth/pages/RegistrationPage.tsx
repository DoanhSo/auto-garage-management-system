import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../../../app/providers/AuthProvider';
import { authApi } from '../api/authApi';
import { registrationSchema, passwordRequirement } from '../schemas/authSchemas';
import { useUiPolicy } from '../../../shared/config/policy';
import type { UiPolicy } from '../../../shared/types/contracts';
import {
  Button,
  Field,
  FormError,
  Loading,
  PasswordInput,
  QueryError,
} from '../../../shared/components/ui';
import { applyFormError } from '../../../shared/api/errors';
import { AuthLayout } from '../components/AuthLayout';
import { USERNAME_HINT, USERNAME_MAX_LENGTH } from '../../../shared/validation/username';

function ReadyRegistration({ policy }: { policy: UiPolicy }) {
  const auth = useAuth();
  const [created, setCreated] = useState(false);
  const {
    register,
    handleSubmit,
    resetField,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<{
    username: string;
    fullName: string;
    email: string;
    password: string;
    confirmPassword: string;
  }>({
    resolver: zodResolver(registrationSchema(policy)),
    mode: 'onBlur',
  });
  if (auth.user)
    return (
      <Navigate to={auth.user.role === 'ADMIN' ? '/admin/reports' : '/notifications'} replace />
    );
  const submit = handleSubmit(async (values) => {
    if (created) return;
    let registrationCompleted = false;
    try {
      await authApi.register({
        username: values.username,
        fullName: values.fullName,
        email: values.email,
        password: values.password,
      });
      registrationCompleted = true;
      setCreated(true);
      await auth.login({ identifier: values.username, password: values.password });
      resetField('password');
      resetField('confirmPassword');
    } catch (error) {
      resetField('password');
      resetField('confirmPassword');
      if (registrationCompleted) return;
      applyFormError(error, setError, ['username', 'fullName', 'email', 'password']);
    }
  });
  return (
    <AuthLayout
      title="Đăng ký tài khoản"
      description="Tạo tài khoản khách hàng để bắt đầu chăm sóc xe."
      variant="register"
    >
      {created ? (
        <div className="alert" role="status">
          {isSubmitting
            ? 'Đã tạo tài khoản. Đang đăng nhập…'
            : 'Tài khoản đã được tạo. Vui lòng đăng nhập bằng tên đăng nhập hoặc email và mật khẩu vừa đăng ký.'}
        </div>
      ) : null}
      <form noValidate onSubmit={submit} className="form-stack auth-registration-form">
        <Field
          label="Họ và tên"
          required
          error={errors.fullName?.message}
          className="auth-form-wide"
        >
          <input
            className="input"
            {...register('fullName')}
            autoComplete="name"
            maxLength={150}
            disabled={isSubmitting || created}
          />
        </Field>
        <Field label="Tên đăng nhập" required error={errors.username?.message} hint={USERNAME_HINT}>
          <input
            className="input"
            {...register('username')}
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            maxLength={USERNAME_MAX_LENGTH}
            placeholder="Ví dụ: nguyen.an"
            disabled={isSubmitting || created}
          />
        </Field>
        <Field label="Email" required error={errors.email?.message}>
          <input
            type="email"
            className="input"
            {...register('email')}
            autoComplete="email"
            maxLength={254}
            disabled={isSubmitting || created}
          />
        </Field>
        <Field
          label="Mật khẩu"
          required
          error={errors.password?.message}
          hint={passwordRequirement(policy)}
        >
          <PasswordInput
            {...register('password')}
            autoComplete="new-password"
            disabled={isSubmitting || created}
          />
        </Field>
        <Field label="Xác nhận mật khẩu" required error={errors.confirmPassword?.message}>
          <PasswordInput
            {...register('confirmPassword')}
            autoComplete="new-password"
            disabled={isSubmitting || created}
          />
        </Field>
        <FormError message={errors.root?.server?.message} />
        {!created && (
          <Button type="submit" variant="primary" className="full-width" pending={isSubmitting}>
            {isSubmitting ? 'Đang tạo tài khoản…' : 'Đăng ký'}
          </Button>
        )}
      </form>
      <p className="auth-switch">
        Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
      </p>
    </AuthLayout>
  );
}

export function RegistrationPage() {
  const policy = useUiPolicy();
  return policy.isPending ? (
    <AuthLayout
      title="Chuẩn bị đăng ký"
      description="Vui lòng đợi trong giây lát."
      variant="register"
    >
      <Loading label="Đang tải chính sách hệ thống…" />
    </AuthLayout>
  ) : policy.error ? (
    <AuthLayout
      title="Không thể tải đăng ký"
      description="Vui lòng thử tải lại."
      variant="register"
    >
      <QueryError
        error={policy.error}
        retry={() => {
          void policy.refetch();
        }}
      />
    </AuthLayout>
  ) : (
    <ReadyRegistration policy={policy.data} />
  );
}

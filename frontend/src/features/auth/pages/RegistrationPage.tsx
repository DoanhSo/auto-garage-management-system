import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, Navigate } from 'react-router-dom';
import { Wrench } from 'lucide-react';
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

function ReadyRegistration({ policy }: { policy: UiPolicy }) {
  const auth = useAuth();
  const [created, setCreated] = useState(false);
  const {
    register,
    handleSubmit,
    resetField,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<{ fullName: string; email: string; password: string; confirmPassword: string }>({
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
        fullName: values.fullName,
        email: values.email,
        password: values.password,
      });
      registrationCompleted = true;
      setCreated(true);
      await auth.login({ identifier: values.email, password: values.password });
      resetField('password');
      resetField('confirmPassword');
    } catch (error) {
      resetField('password');
      resetField('confirmPassword');
      if (registrationCompleted) return;
      applyFormError(error, setError, ['fullName', 'email', 'password']);
    }
  });
  return (
    <div className="login-layout">
      <main className="login-main" id="main-content" tabIndex={-1}>
        <div className="login-card">
          <div className="login-brand">
            <Wrench size={22} aria-hidden />
            <span>AutoGarage</span>
          </div>
          <div className="login-card-header">
            <h1>Đăng ký tài khoản</h1>
            <p className="description">Dùng email và mật khẩu riêng để tạo tài khoản khách hàng.</p>
          </div>
          {created ? (
            <div className="alert" role="status">
              {isSubmitting
                ? 'Đã tạo tài khoản. Đang đăng nhập…'
                : 'Tài khoản đã được tạo. Vui lòng đăng nhập bằng email và mật khẩu vừa đăng ký.'}
            </div>
          ) : null}
          <form noValidate onSubmit={submit} className="form-stack">
            <Field label="Họ và tên" required error={errors.fullName?.message}>
              <input
                className="input"
                {...register('fullName')}
                autoComplete="name"
                maxLength={150}
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
          <div className="login-customer-box">
            <Link className="button button-secondary full-width" to="/login">
              Đã có tài khoản? Đăng nhập
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}

export function RegistrationPage() {
  const policy = useUiPolicy();
  return policy.isPending ? (
    <Loading label="Đang tải chính sách hệ thống…" />
  ) : policy.error ? (
    <QueryError
      error={policy.error}
      retry={() => {
        void policy.refetch();
      }}
    />
  ) : (
    <ReadyRegistration policy={policy.data} />
  );
}

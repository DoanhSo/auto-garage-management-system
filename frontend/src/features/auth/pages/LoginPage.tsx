import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { ArrowRight, Bell, ChartNoAxesCombined, ShieldCheck, Wrench } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useLoginMutation } from '../hooks/useAuthQueries';
import { loginSchema } from '../schemas/authSchemas';
import { Button, Field, FormError, PasswordInput } from '../../../shared/components/ui';
import { errorMessage } from '../../../shared/api/errors';
import { safeReturnTo } from '../../../shared/utils/format';
export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
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
  const { reset: resetMutation } = mutation;
  useEffect(() => () => resetMutation(), [resetMutation]);
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
      const user = await mutation.mutateAsync(values);
      resetField('password');
      navigate(
        safeReturnTo(
          (location.state as { returnTo?: unknown } | null)?.returnTo,
          user.role === 'ADMIN' ? '/admin/reports' : '/notifications',
        ),
        { replace: true },
      );
    } catch (error) {
      setError('root.server', { message: errorMessage(error) });
    } finally {
      mutation.reset();
    }
  });
  return (
    <div className="login-layout">
      <aside className="login-story">
        <div className="brand">
          <span className="brand-mark">
            <Wrench size={23} />
          </span>
          <span>
            AutoGarage<small>GARAGE WORKSPACE</small>
          </span>
        </div>
        <div className="login-story-body">
          <p className="eyebrow">DÀNH CHO ĐỘI NGŨ CỦA BẠN</p>
          <h1>
            Mọi thông tin.
            <br />
            Một không gian làm việc.
          </h1>
          <p>Quản lý truy cập, theo dõi thông báo và nắm bắt số liệu vận hành gara.</p>
          <div className="story-features">
            <span>
              <ShieldCheck size={19} />
              Quyền truy cập theo vai trò
            </span>
            <span>
              <Bell size={19} />
              Thông báo dành riêng cho bạn
            </span>
            <span>
              <ChartNoAxesCombined size={19} />
              Báo cáo tập trung, rõ ràng
            </span>
          </div>
        </div>
        <p className="login-story-footer">AutoGarage · Hệ thống quản lý gara ô tô</p>
      </aside>
      <main className="login-main">
        <div className="login-card">
          <p className="eyebrow">CHÀO MỪNG TRỞ LẠI</p>
          <h2>Đăng nhập</h2>
          <p className="description">Sử dụng tài khoản được quản trị viên cấp để tiếp tục.</p>
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
                placeholder="Nhập tài khoản của bạn"
                disabled={mutation.isPending}
              />
            </Field>
            <Field label="Mật khẩu" required error={errors.password?.message}>
              <PasswordInput
                {...register('password')}
                autoComplete="current-password"
                disabled={mutation.isPending}
                placeholder="Nhập mật khẩu"
              />
            </Field>
            <FormError message={errors.root?.server?.message} />
            <Button
              type="submit"
              variant="primary"
              pending={mutation.isPending}
              className="full-width"
            >
              {mutation.isPending ? 'Đang đăng nhập…' : 'Đăng nhập'}
              <ArrowRight size={17} aria-hidden />
            </Button>
          </form>
          <p className="login-note">Cần hỗ trợ truy cập? Liên hệ quản trị viên gara.</p>
        </div>
        <p className="login-security">Thông tin hiển thị theo quyền truy cập của bạn.</p>
      </main>
    </div>
  );
}

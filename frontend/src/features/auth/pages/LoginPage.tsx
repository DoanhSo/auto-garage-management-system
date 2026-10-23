import { useEffect, useRef } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import {
  ArrowRight,
  Bell,
  ChartNoAxesCombined,
  KeyRound,
  ShieldCheck,
  Sparkles,
  UserPlus,
  Wrench,
} from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { authApi } from '../api/authApi';
import { useLoginMutation, usePkceCallbackMutation } from '../hooks/useAuthQueries';
import { loginSchema } from '../schemas/authSchemas';
import { Button, Field, FormError, PasswordInput } from '../../../shared/components/ui';
import { errorMessage } from '../../../shared/api/errors';
import { safeReturnTo } from '../../../shared/utils/format';

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const mutation = useLoginMutation();
  const pkceMutation = usePkceCallbackMutation();
  const processedCodeRef = useRef<string | null>(null);
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
  const { mutateAsync: completePkce } = pkceMutation;
  useEffect(() => () => resetMutation(), [resetMutation]);

  useEffect(() => {
    const oidcError = searchParams.get('error');
    if (oidcError) {
      const desc = searchParams.get('error_description');
      setSearchParams({}, { replace: true });
      setError('root.server', {
        message:
          oidcError === 'access_denied'
            ? 'Bạn đã hủy đăng nhập qua cổng xác thực.'
            : `Xác thực OIDC không thành công (${desc || oidcError}).`,
      });
      return;
    }
    const code = searchParams.get('code');
    const state = searchParams.get('state');
    if (!code || processedCodeRef.current === code) return;
    processedCodeRef.current = code;
    const pkceState = authApi.consumePkceState(state);
    setSearchParams({}, { replace: true });
    if (!pkceState) {
      setError('root.server', {
        message: 'Phiên xác thực OIDC PKCE không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.',
      });
      return;
    }
    void completePkce({
      code,
      codeVerifier: pkceState.verifier,
      redirectUri: pkceState.redirectUri,
    })
      .then((user) => {
        navigate(
          safeReturnTo(
            pkceState.returnTo,
            user.role === 'ADMIN' ? '/admin/reports' : '/notifications',
          ),
          { replace: true },
        );
      })
      .catch((error: unknown) => {
        setError('root.server', { message: errorMessage(error) });
      });
  }, [completePkce, navigate, searchParams, setError, setSearchParams]);

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
  const busy = mutation.isPending || pkceMutation.isPending;
  const submit = handleSubmit(async (values) => {
    if (busy) return;
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
          <span className="login-pill">
            <Sparkles size={14} aria-hidden />
            NỀN TẢNG QUẢN TRỊ & DỊCH VỤ GARA Ô TÔ
          </span>
          <h1>
            Vận hành gara đồng bộ.
            <br />
            Trải nghiệm khách hàng liền mạch.
          </h1>
          <p>
            Kết nối đội ngũ kỹ thuật, lễ tân, kho phụ tùng và khách hàng trên cùng một hệ thống xác
            thực tập trung Keycloak OIDC.
          </p>
          <div className="story-features">
            <span>
              <ShieldCheck size={19} />
              Phân quyền 5 vai trò bảo mật theo tiêu chuẩn OAuth2 / PKCE
            </span>
            <span>
              <Bell size={19} />
              Thông báo thời gian thực theo từng nhóm vai trò & cá nhân
            </span>
            <span>
              <ChartNoAxesCombined size={19} />
              Theo dõi lịch hẹn, tiếp nhận xe và báo cáo vận hành trực quan
            </span>
          </div>
        </div>
        <p className="login-story-footer">AutoGarage · Hệ thống quản lý gara ô tô chuyên nghiệp</p>
      </aside>
      <main className="login-main">
        <div className="login-card">
          <div className="login-card-header">
            <p className="eyebrow">CỔNG XÁC THỰC TẬP TRUNG</p>
            <h2>Đăng nhập</h2>
            <p className="description">
              Sử dụng tài khoản nội bộ hoặc đăng nhập nhanh dành cho khách hàng.
            </p>
          </div>
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
                placeholder="Ví dụ: garage.admin hoặc email@domain.com"
                disabled={busy}
              />
            </Field>
            <Field label="Mật khẩu" required error={errors.password?.message}>
              <PasswordInput
                {...register('password')}
                autoComplete="current-password"
                disabled={busy}
                placeholder="Nhập mật khẩu của bạn"
              />
            </Field>
            <FormError message={errors.root?.server?.message} />
            <Button type="submit" variant="primary" pending={busy} className="full-width">
              {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
              <ArrowRight size={17} aria-hidden />
            </Button>
          </form>

          <div className="login-divider" role="separator">
            <span>HOẶC TIẾP TỤC VỚI</span>
          </div>

          <div className="login-sso-stack">
            <Button
              type="button"
              disabled={busy}
              className="full-width button-google"
              onClick={() => {
                const returnTo = (location.state as { returnTo?: unknown } | null)?.returnTo;
                void authApi.startGoogleRedirect(
                  typeof returnTo === 'string' ? returnTo : undefined,
                );
              }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="#4285F4"
                  d="M23.49 12.27c0-.79-.07-1.54-.19-2.27H12v4.51h6.47c-.29 1.48-1.14 2.73-2.4 3.58v3h3.86c2.26-2.09 3.56-5.17 3.56-8.82Z"
                />
                <path
                  fill="#34A853"
                  d="M12 24c3.24 0 5.95-1.08 7.93-2.91l-3.86-3c-1.08.72-2.45 1.16-4.07 1.16-3.13 0-5.78-2.11-6.73-4.96H1.29v3.09C3.26 21.3 7.31 24 12 24Z"
                />
                <path
                  fill="#FBBC05"
                  d="M5.27 14.29c-.25-.72-.38-1.49-.38-2.29s.13-1.57.38-2.29V6.62H1.29C.47 8.24 0 10.06 0 12s.47 3.76 1.29 5.38l3.98-3.09Z"
                />
                <path
                  fill="#EA4335"
                  d="M12 4.75c1.77 0 3.35.61 4.6 1.8l3.42-3.42C17.95 1.19 15.24 0 12 0 7.31 0 3.26 2.7 1.29 6.62l3.98 3.09c.95-2.85 3.6-4.96 6.73-4.96Z"
                />
              </svg>
              Đăng nhập bằng Google (Khách hàng)
            </Button>
            <Button
              type="button"
              disabled={busy}
              className="full-width button-sso"
              onClick={() => {
                const returnTo = (location.state as { returnTo?: unknown } | null)?.returnTo;
                void authApi.startPkceRedirect(typeof returnTo === 'string' ? returnTo : undefined);
              }}
            >
              <KeyRound size={16} aria-hidden />
              Đăng nhập qua cổng Keycloak SSO (PKCE)
            </Button>
          </div>

          <div className="login-customer-box">
            <div className="login-customer-text">
              <strong>Khách hàng mới?</strong>
              <span>Tạo tài khoản trực tuyến để đặt lịch hẹn & nhận thông báo bảo dưỡng xe.</span>
            </div>
            <Button
              type="button"
              variant="secondary"
              disabled={busy}
              className="full-width"
              onClick={() => {
                const returnTo = (location.state as { returnTo?: unknown } | null)?.returnTo;
                void authApi.startRegistrationRedirect(
                  typeof returnTo === 'string' ? returnTo : undefined,
                );
              }}
            >
              <UserPlus size={16} aria-hidden />
              Đăng ký tài khoản khách hàng
            </Button>
          </div>

          <p className="login-note">
            Tài khoản nhân sự nội bộ (`ADMIN`, `RECEPTIONIST`, `WAREHOUSE_STAFF`, `TECHNICIAN`) do
            Quản trị viên gara cấp phát.
          </p>
        </div>
        <p className="login-security">Thông tin hiển thị theo quyền truy cập của bạn.</p>
      </main>
    </div>
  );
}

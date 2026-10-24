import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Navigate, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowRight, KeyRound, UserPlus, Wrench } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { authApi } from '../api/authApi';
import { useLoginMutation, usePkceCallbackMutation } from '../hooks/useAuthQueries';
import { loginSchema } from '../schemas/authSchemas';
import { Button, Field, FormError, PasswordInput } from '../../../shared/components/ui';
import { env } from '../../../shared/config/env';
import { errorMessage } from '../../../shared/api/errors';
import { safeReturnTo } from '../../../shared/utils/format';

export function LoginPage() {
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const mutation = useLoginMutation();
  const pkceMutation = usePkceCallbackMutation();
  const [redirecting, setRedirecting] = useState(false);
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
      authApi.consumePkceState(searchParams.get('state'));
      setSearchParams({}, { replace: true });
      setError('root.server', {
        message:
          oidcError === 'access_denied'
            ? 'Bạn đã hủy đăng nhập qua cổng xác thực.'
            : 'Chưa thể xác thực tài khoản. Vui lòng thử lại.',
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
  const busy = mutation.isPending || pkceMutation.isPending || redirecting;
  const startRedirect = async (mode: 'sso' | 'google' | 'register') => {
    if (busy) return;
    setRedirecting(true);
    try {
      const returnTo = (location.state as { returnTo?: unknown } | null)?.returnTo;
      const destination = typeof returnTo === 'string' ? returnTo : undefined;
      if (mode === 'google') await authApi.startGoogleRedirect(destination);
      else if (mode === 'register') await authApi.startRegistrationRedirect(destination);
      else await authApi.startPkceRedirect(destination);
    } catch {
      setRedirecting(false);
      setError('root.server', { message: 'Chưa thể mở cổng đăng nhập. Vui lòng thử lại.' });
    }
  };
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
      <main className="login-main" id="main-content" tabIndex={-1}>
        <div className="login-card">
          <div className="login-brand">
            <Wrench size={22} aria-hidden />
            <span>AutoGarage</span>
          </div>
          <div className="login-card-header">
            <h1>Chào mừng trở lại</h1>
            <p className="description">Đăng nhập để quản lý công việc và lịch hẹn của bạn.</p>
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
                placeholder="Tên đăng nhập hoặc email"
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
            <span>hoặc</span>
          </div>

          <div className="login-sso-stack">
            {env.googleLoginEnabled && (
              <Button
                type="button"
                disabled={busy}
                className="full-width button-google"
                onClick={() => void startRedirect('google')}
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
                Tiếp tục với Google
              </Button>
            )}
            <Button
              type="button"
              disabled={busy}
              className="full-width button-sso"
              onClick={() => void startRedirect('sso')}
            >
              <KeyRound size={16} aria-hidden />
              Đăng nhập với Keycloak
            </Button>
          </div>

          <div className="login-customer-box">
            <div className="login-customer-text">
              <strong>Khách hàng mới?</strong>
              <span>Tạo tài khoản để đặt lịch và theo dõi dịch vụ.</span>
            </div>
            <Button
              type="button"
              variant="secondary"
              disabled={busy}
              className="full-width"
              onClick={() => void startRedirect('register')}
            >
              <UserPlus size={16} aria-hidden />
              Đăng ký tài khoản khách hàng
            </Button>
          </div>

          <p className="login-note">Tài khoản nhân viên do quản trị viên gara cấp.</p>
        </div>
        <p className="login-security">Thông tin hiển thị theo quyền truy cập của bạn.</p>
      </main>
    </div>
  );
}

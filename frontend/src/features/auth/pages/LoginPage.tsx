import { useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, Navigate, useLocation, useSearchParams } from 'react-router-dom';
import { ArrowRight, ShieldCheck, UserCheck } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useLoginMutation, usePkceCallbackMutation } from '../hooks/useAuthQueries';
import { authApi } from '../api/authApi';
import { loginSchema } from '../schemas/authSchemas';
import { Button, Field, FormError, PasswordInput } from '../../../shared/components/ui';
import { errorMessage } from '../../../shared/api/errors';
import { safeReturnTo } from '../../../shared/utils/format';
import { AuthLayout } from '../components/AuthLayout';
import { DemoAccountSelector } from '../components/DemoAccountSelector';
import { ForgotPasswordModal } from '../components/ForgotPasswordModal';
import { homePathForRole } from '../../../app/access';
import { rememberMeStorage } from '../api/rememberMe';

export function LoginPage() {
  const auth = useAuth();
  const location = useLocation();
  const mutation = useLoginMutation();
  const pkce = usePkceCallbackMutation();
  const [searchParams, setSearchParams] = useSearchParams();
  const callbackCode = searchParams.get('code');
  const callbackState = searchParams.get('state');
  const callbackError = searchParams.get('error');
  const callbackHandled = useRef(false);
  const [callbackReturnTo, setCallbackReturnTo] = useState<string | undefined>();
  const [pageError, setPageError] = useState('');
  const [savedIdentifier] = useState(rememberMeStorage.get);
  const [rememberMe, setRememberMe] = useState(true);
  const [forgotOpen, setForgotOpen] = useState(false);

  const {
    register,
    handleSubmit,
    resetField,
    setError,
    setValue,
    setFocus,
    formState: { errors },
  } = useForm<{ identifier: string; password: string }>({
    resolver: zodResolver(loginSchema),
    mode: 'onBlur',
    defaultValues: { identifier: savedIdentifier, password: '' },
  });

  const { reset } = mutation;
  useEffect(() => () => reset(), [reset]);
  const { mutateAsync: completePkce } = pkce;

  useEffect(() => {
    if (callbackHandled.current || (!callbackCode && !callbackError)) return;
    callbackHandled.current = true;
    const session = authApi.consumePkceState(callbackState);
    setSearchParams({}, { replace: true });
    if (callbackError || !callbackCode || !session) {
      setPageError('Đăng nhập chưa hoàn tất hoặc phiên đã hết hạn. Vui lòng thử lại.');
      return;
    }
    setCallbackReturnTo(session.returnTo);
    completePkce({
      code: callbackCode,
      codeVerifier: session.verifier,
      redirectUri: session.redirectUri,
    }).catch((error: unknown) => {
      setCallbackReturnTo(undefined);
      setPageError(errorMessage(error));
    });
  }, [callbackCode, callbackState, callbackError, completePkce, setSearchParams]);

  if (auth.user) {
    const target = safeReturnTo(
      (location.state as { returnTo?: unknown } | null)?.returnTo ?? callbackReturnTo,
      homePathForRole(auth.user.role),
    );
    // '/' là trang chủ công khai; sau đăng nhập đưa người dùng vào không gian làm việc theo vai trò.
    return <Navigate to={target === '/' ? '/workspace' : target} replace />;
  }

  const submit = handleSubmit(async (values) => {
    if (mutation.isPending) return;
    try {
      await mutation.mutateAsync(values);
      if (rememberMe) rememberMeStorage.set(values.identifier);
      else rememberMeStorage.clear();
      resetField('password');
    } catch (error) {
      setError('root.server', { message: errorMessage(error) });
    } finally {
      mutation.reset();
    }
  });

  return (
    <AuthLayout
      title="Đăng nhập"
      description="Chào mừng bạn quay lại. Đăng nhập để theo dõi lệnh sửa chữa và dịch vụ."
    >
      {pageError && (
        <div className="alert" role="alert">
          {pageError}
        </div>
      )}
      {auth.notice && (
        <div className="alert" role="status">
          {auth.notice}
        </div>
      )}

      <div className="auth-role-hint">
        <UserCheck size={16} aria-hidden />
        <span>
          Cổng dịch vụ dành cho <strong>Khách hàng</strong> và <strong>Nhân viên</strong> xưởng ô
          tô.
        </span>
      </div>

      <form noValidate onSubmit={submit} className="form-stack">
        <Field label="Tên đăng nhập hoặc email" required error={errors.identifier?.message}>
          <input
            className="input"
            {...register('identifier')}
            autoComplete="username"
            autoFocus
            maxLength={254}
            placeholder="Nhập tên đăng nhập hoặc email"
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

        <div className="auth-form-options">
          <label className="auth-checkbox-label">
            <input
              type="checkbox"
              checked={rememberMe}
              onChange={(e) => setRememberMe(e.target.checked)}
            />
            <span>Ghi nhớ đăng nhập</span>
          </label>
          <button type="button" className="auth-forgot-link" onClick={() => setForgotOpen(true)}>
            Quên mật khẩu?
          </button>
        </div>

        <FormError message={errors.root?.server?.message} />

        <Button
          type="submit"
          variant="primary"
          pending={mutation.isPending}
          className="full-width auth-submit-btn"
        >
          {mutation.isPending ? 'Đang đăng nhập…' : 'Đăng nhập'}
          <ArrowRight size={17} aria-hidden />
        </Button>
      </form>

      {import.meta.env.DEV && (
        <DemoAccountSelector
          disabled={mutation.isPending}
          onSelect={(account) => {
            setValue('identifier', account.identifier, { shouldValidate: true });
            setFocus('password');
          }}
        />
      )}

      <div className="auth-divider" aria-hidden>
        hoặc
      </div>
      <Link to="/register" className="auth-alt-action">
        Tạo tài khoản khách hàng mới
      </Link>

      <div className="auth-security-footer-note">
        <ShieldCheck size={14} aria-hidden />
        <span>Tài khoản nhân viên gara do quản trị viên cấp quyền trực tiếp.</span>
      </div>
      <ForgotPasswordModal open={forgotOpen} onOpenChange={setForgotOpen} />
    </AuthLayout>
  );
}

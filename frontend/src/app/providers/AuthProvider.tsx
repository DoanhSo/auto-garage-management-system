import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { configureSession } from '../../shared/api/client';
import { authApi } from '../../features/auth/api/authApi';
import type { OidcTokenBundle, User } from '../../shared/types/contracts';
import { Loading, QueryError } from '../../shared/components/ui';
import { ApiError } from '../../shared/api/errors';
import { logoutPending, markLogoutPending } from '../../features/auth/api/sessionIntent';

interface AuthContextValue {
  user: User | null;
  sessionKey: number;
  notice: string;
  login: (values: { identifier: string; password: string }) => Promise<User>;
  completePkceLogin: (values: {
    code: string;
    codeVerifier: string;
    redirectUri: string;
  }) => Promise<User>;
  logout: () => Promise<void>;
  clearSession: (message?: string) => void;
  setCurrentUser: (user: User) => void;
}
const Context = createContext<AuthContextValue | null>(null);
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<User | null>(null);
  const [sessionKey, setSessionKey] = useState(0);
  const [notice, setNotice] = useState('');
  const [restoring, setRestoring] = useState(true);
  const [restoreFailure, setRestoreFailure] = useState<unknown>(null);
  const bootstrapRequest = useRef<Promise<OidcTokenBundle | null> | null>(null);
  const attempt = useRef(0);
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const channel = useRef<BroadcastChannel | null>(null);

  const stopRefreshTimer = useCallback(() => {
    if (refreshTimerRef.current !== null) {
      clearTimeout(refreshTimerRef.current);
      refreshTimerRef.current = null;
    }
  }, []);

  const clearSession = useCallback(
    (message = 'Phiên đăng nhập đã kết thúc.') => {
      attempt.current++;
      stopRefreshTimer();
      markLogoutPending(true);
      configureSession(null, () => {});
      queryClient.clear();
      setUser(null);
      setRestoring(false);
      setRestoreFailure(null);
      setSessionKey((value) => value + 1);
      setNotice(message);
    },
    [queryClient, stopRefreshTimer],
  );
  const expire = useCallback(
    () => clearSession('Phiên đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.'),
    [clearSession],
  );

  const scheduleRefresh = useCallback(
    (bundle: OidcTokenBundle, sessionAttempt: number) => {
      stopRefreshTimer();
      const refreshLeadSeconds = Math.min(30, bundle.expiresIn / 5);
      const delayMs = Math.max(1, (bundle.expiresIn - refreshLeadSeconds) * 1000);
      refreshTimerRef.current = setTimeout(async () => {
        if (sessionAttempt !== attempt.current) return;
        try {
          const nextBundle = await authApi.refreshBrowserSession();
          if (sessionAttempt !== attempt.current) return;
          if (!nextBundle) {
            expire();
            return;
          }
          configureSession(nextBundle.accessToken, expire);
          scheduleRefresh(nextBundle, sessionAttempt);
        } catch (error) {
          if (sessionAttempt === attempt.current) setRestoreFailure(error);
        }
      }, delayMs);
    },
    [expire, stopRefreshTimer],
  );

  const establishSession = useCallback(
    async (bundle: OidcTokenBundle, id: number): Promise<User> => {
      if (id !== attempt.current) throw new DOMException('Login cancelled', 'AbortError');
      configureSession(bundle.accessToken, expire);
      try {
        if (bundle.refreshToken) {
          bundle = await authApi.openBrowserSession(bundle.refreshToken);
          if (id !== attempt.current) throw new DOMException('Login cancelled', 'AbortError');
          configureSession(bundle.accessToken, expire);
        }
        const currentUser = await authApi.me();
        if (id !== attempt.current) throw new DOMException('Login cancelled', 'AbortError');
        queryClient.clear();
        scheduleRefresh(bundle, id);
        markLogoutPending(false);
        setUser(currentUser);
        setSessionKey((value) => value + 1);
        setNotice('');
        return currentUser;
      } catch (error) {
        if (id === attempt.current) {
          stopRefreshTimer();
          configureSession(null, expire);
        }
        throw error;
      }
    },
    [expire, queryClient, scheduleRefresh, stopRefreshTimer],
  );

  const restoreSession = useCallback(async () => {
    const id = ++attempt.current;
    setRestoring(true);
    setRestoreFailure(null);
    const pending = (bootstrapRequest.current ??= (async () => {
      if (logoutPending()) {
        await authApi.closeBrowserSession();
        markLogoutPending(false);
        return null;
      }
      return authApi.refreshBrowserSession();
    })());
    try {
      const bundle = await pending;
      if (id !== attempt.current) return;
      if (bundle) await establishSession(bundle, id);
      else configureSession(null, expire);
    } catch (error) {
      if (id !== attempt.current) return;
      if (error instanceof ApiError && error.status === 401) clearSession();
      else setRestoreFailure(error);
    } finally {
      if (id === attempt.current) setRestoring(false);
      if (bootstrapRequest.current === pending) bootstrapRequest.current = null;
    }
  }, [clearSession, establishSession, expire]);

  useEffect(() => {
    void restoreSession();
  }, [restoreSession]);

  useEffect(() => {
    const current =
      typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('autogarage-session');
    channel.current = current;
    if (current)
      current.onmessage = (event) => {
        if (event.data === 'logout') clearSession('Bạn đã đăng xuất ở cửa sổ khác.');
      };
    return () => {
      current?.close();
      channel.current = null;
    };
  }, [clearSession]);

  useEffect(
    () => () => {
      attempt.current++;
      stopRefreshTimer();
    },
    [stopRefreshTimer],
  );

  const login = useCallback(
    async (values: { identifier: string; password: string }) => {
      const id = ++attempt.current;
      const bundle = await authApi.loginWithPassword(values);
      return establishSession(bundle, id);
    },
    [establishSession],
  );

  const completePkceLogin = useCallback(
    async (values: { code: string; codeVerifier: string; redirectUri: string }) => {
      const id = ++attempt.current;
      const bundle = await authApi.exchangeAuthorizationCode(values);
      return establishSession(bundle, id);
    },
    [establishSession],
  );

  const logout = useCallback(async () => {
    let confirmed = true;
    markLogoutPending(true);
    attempt.current++;
    stopRefreshTimer();
    try {
      await authApi.logout();
    } catch {
      confirmed = false;
    } finally {
      try {
        await authApi.closeBrowserSession();
      } catch {
        confirmed = false;
      }
      clearSession(
        confirmed
          ? 'Đã đăng xuất khỏi hệ thống.'
          : 'Đã thoát trên thiết bị này. Chưa xác nhận thu hồi token trên máy chủ do lỗi kết nối.',
      );
      channel.current?.postMessage('logout');
    }
  }, [clearSession, stopRefreshTimer]);

  const setCurrentUser = useCallback(
    (next: User) => setUser((current) => (current?.id === next.id ? next : current)),
    [],
  );

  const value = useMemo(
    () => ({
      user,
      sessionKey,
      notice,
      login,
      completePkceLogin,
      logout,
      clearSession,
      setCurrentUser,
    }),
    [user, sessionKey, notice, login, completePkceLogin, logout, clearSession, setCurrentUser],
  );
  return (
    <Context value={value}>
      {restoring ? (
        <Loading label="Đang khôi phục phiên đăng nhập…" />
      ) : restoreFailure ? (
        <QueryError
          error={restoreFailure}
          retry={() => {
            void restoreSession();
          }}
        />
      ) : (
        children
      )}
    </Context>
  );
}
export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error('Auth provider missing');
  return value;
}

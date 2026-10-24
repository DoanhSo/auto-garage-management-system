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
  const attempt = useRef(0);
  const refreshTokenRef = useRef<string | null>(null);
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
      refreshTokenRef.current = null;
      configureSession(null, () => {});
      queryClient.clear();
      setUser(null);
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
      refreshTokenRef.current = bundle.refreshToken;
      if (!bundle.refreshToken) return;
      const refreshLeadSeconds = Math.min(30, bundle.expiresIn / 5);
      const delayMs = Math.max(1, (bundle.expiresIn - refreshLeadSeconds) * 1000);
      refreshTimerRef.current = setTimeout(async () => {
        const currentRefreshToken = refreshTokenRef.current;
        if (!currentRefreshToken || sessionAttempt !== attempt.current) return;
        try {
          const nextBundle = await authApi.refreshToken(currentRefreshToken);
          if (sessionAttempt !== attempt.current) return;
          configureSession(nextBundle.accessToken, expire);
          scheduleRefresh(nextBundle, sessionAttempt);
        } catch {
          if (sessionAttempt === attempt.current) expire();
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
        const currentUser = await authApi.me();
        if (id !== attempt.current) throw new DOMException('Login cancelled', 'AbortError');
        queryClient.clear();
        scheduleRefresh(bundle, id);
        setUser(currentUser);
        setSessionKey((value) => value + 1);
        setNotice('');
        return currentUser;
      } catch (error) {
        if (id === attempt.current) {
          stopRefreshTimer();
          refreshTokenRef.current = null;
          configureSession(null, expire);
        }
        throw error;
      }
    },
    [expire, queryClient, scheduleRefresh, stopRefreshTimer],
  );

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
      refreshTokenRef.current = null;
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
    const activeRefreshToken = refreshTokenRef.current;
    attempt.current++;
    stopRefreshTimer();
    try {
      await authApi.logout(activeRefreshToken);
    } catch {
      confirmed = false;
    } finally {
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
  return <Context value={value}>{children}</Context>;
}
export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error('Auth provider missing');
  return value;
}

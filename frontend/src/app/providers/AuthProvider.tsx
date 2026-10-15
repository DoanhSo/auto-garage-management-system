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
import type { User } from '../../shared/types/contracts';

interface AuthContextValue {
  user: User | null;
  sessionKey: number;
  notice: string;
  login: (values: { identifier: string; password: string }) => Promise<User>;
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
  const channel = useRef<BroadcastChannel | null>(null);
  const clearSession = useCallback(
    (message = 'Phiên đăng nhập đã kết thúc.') => {
      attempt.current++;
      configureSession(null, () => {});
      queryClient.clear();
      setUser(null);
      setSessionKey((value) => value + 1);
      setNotice(message);
    },
    [queryClient],
  );
  const expire = useCallback(
    () => clearSession('Phiên đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại.'),
    [clearSession],
  );
  useEffect(() => {
    const attemptState = attempt;
    configureSession(null, expire);
    const current =
      typeof BroadcastChannel === 'undefined' ? null : new BroadcastChannel('autogarage-session');
    channel.current = current;
    if (current)
      current.onmessage = (event) => {
        if (event.data === 'logout') clearSession('Bạn đã đăng xuất ở cửa sổ khác.');
      };
    return () => {
      attemptState.current++;
      current?.close();
      channel.current = null;
      configureSession(null, () => {});
    };
  }, [clearSession, expire]);
  const login = useCallback(
    async (values: { identifier: string; password: string }) => {
      const id = ++attempt.current;
      const result = await authApi.login(values);
      if (id !== attempt.current) throw new DOMException('Login cancelled', 'AbortError');
      queryClient.clear();
      configureSession(result.accessToken, expire);
      setUser(result.user);
      setSessionKey((value) => value + 1);
      setNotice('');
      return result.user;
    },
    [queryClient, expire],
  );
  const logout = useCallback(async () => {
    let confirmed = true;
    try {
      await authApi.logout();
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
  }, [clearSession]);
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
      logout,
      clearSession,
      setCurrentUser,
    }),
    [user, sessionKey, notice, login, logout, clearSession, setCurrentUser],
  );
  return <Context value={value}>{children}</Context>;
}
export function useAuth() {
  const value = useContext(Context);
  if (!value) throw new Error('Auth provider missing');
  return value;
}

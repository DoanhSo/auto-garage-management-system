import { act, render } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthProvider';
import { authApi } from '../../features/auth/api/authApi';
import type { User } from '../../shared/types/contracts';
import * as transport from '../../shared/api/client';

beforeEach(() => {
  vi.spyOn(authApi, 'refreshBrowserSession').mockResolvedValue(null);
});

describe('Authentication refresh lifecycle', () => {
  it('ignores an in-flight refresh while backend logout is pending', async () => {
    vi.useFakeTimers();
    const bundle = {
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      expiresIn: 10,
      tokenType: 'Bearer',
    };
    vi.spyOn(authApi, 'loginWithPassword').mockResolvedValue(bundle);
    vi.spyOn(authApi, 'openBrowserSession').mockResolvedValue({ ...bundle, refreshToken: null });
    vi.spyOn(authApi, 'closeBrowserSession').mockResolvedValue(null);
    vi.spyOn(authApi, 'me').mockResolvedValue({ id: '7', role: 'CUSTOMER' } as User);
    let finishRefresh!: (value: typeof bundle) => void;
    let finishLogout!: () => void;
    let refreshCalls = 0;
    vi.mocked(authApi.refreshBrowserSession).mockImplementation(() => {
      if (++refreshCalls === 1) return Promise.resolve(null);
      return new Promise((resolve) => {
        finishRefresh = resolve;
      });
    });
    vi.spyOn(authApi, 'logout').mockImplementation(
      () =>
        new Promise<void>((resolve) => {
          finishLogout = resolve;
        }),
    );
    const configure = vi.spyOn(transport, 'configureSession');
    let auth!: ReturnType<typeof useAuth>;
    function Probe() {
      auth = useAuth();
      return null;
    }
    render(
      <QueryClientProvider client={new QueryClient()}>
        <AuthProvider>
          <Probe />
        </AuthProvider>
      </QueryClientProvider>,
    );
    await act(async () => {
      await Promise.resolve();
    });
    await act(async () => {
      await auth.login({ identifier: 'fixture', password: 'test-only' });
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(8000);
    });
    let logout!: Promise<void>;
    act(() => {
      logout = auth.logout();
    });
    await act(async () => {
      finishRefresh({ ...bundle, accessToken: 'late-token' });
    });
    expect(configure.mock.calls.some(([token]) => token === 'late-token')).toBe(false);
    await act(async () => {
      finishLogout();
      await logout;
    });
    expect(auth.user).toBeNull();
    expect(auth.notice).toBe('Đã đăng xuất khỏi hệ thống.');
  });
  it('refreshes short-lived tokens before expiry and stops after unmount', async () => {
    vi.useFakeTimers();
    const bundle = {
      accessToken: 'test-access',
      refreshToken: 'test-refresh',
      expiresIn: 10,
      tokenType: 'Bearer',
    };
    vi.spyOn(authApi, 'loginWithPassword').mockResolvedValue(bundle);
    vi.spyOn(authApi, 'openBrowserSession').mockResolvedValue({ ...bundle, refreshToken: null });
    vi.spyOn(authApi, 'me').mockResolvedValue({ id: '7', role: 'CUSTOMER' } as User);
    const refresh = vi
      .mocked(authApi.refreshBrowserSession)
      .mockResolvedValueOnce(null)
      .mockResolvedValue({ ...bundle, refreshToken: null });
    let auth!: ReturnType<typeof useAuth>;
    function Probe() {
      auth = useAuth();
      return null;
    }
    const mounted = render(
      <QueryClientProvider client={new QueryClient()}>
        <AuthProvider>
          <Probe />
        </AuthProvider>
      </QueryClientProvider>,
    );
    await act(async () => {
      await Promise.resolve();
    });
    await act(async () => {
      await auth.login({ identifier: 'fixture', password: 'test-only' });
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(8000);
    });
    expect(refresh).toHaveBeenCalledTimes(2);
    mounted.unmount();
    await vi.advanceTimersByTimeAsync(20000);
    expect(refresh).toHaveBeenCalledTimes(2);
  });
});

import { StrictMode } from 'react';
import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { http, HttpResponse } from 'msw';
import { beforeEach, expect, it, vi } from 'vitest';
import { AuthProvider, useAuth } from './AuthProvider';
import { server } from '../../test/server';
import { admin, baseHandlers, envelope } from '../../test/fixtures';
import { authApi } from '../../features/auth/api/authApi';
import { ApiError } from '../../shared/api/errors';

const restored = { accessToken: 'restored-access', expiresIn: 1800, tokenType: 'Bearer' };
beforeEach(() => server.use(...baseHandlers()));
let auth: ReturnType<typeof useAuth>;
function Workspace() {
  auth = useAuth();
  return <div>{auth.user?.username ?? 'Login required'}</div>;
}
function mount(strict = false) {
  const provider = (
    <AuthProvider>
      <Workspace />
    </AuthProvider>
  );
  return render(
    <QueryClientProvider client={new QueryClient()}>
      {strict ? <StrictMode>{provider}</StrictMode> : provider}
    </QueryClientProvider>,
  );
}

it('restores the existing browser session before rendering the protected workspace after reload', async () => {
  server.use(
    http.post('/api/v1/auth/session/refresh', () => {
      return HttpResponse.json(envelope(restored));
    }),
  );
  const fetchCall = vi.spyOn(globalThis, 'fetch');
  mount();
  expect(screen.queryByText('Login required')).not.toBeInTheDocument();
  expect(await screen.findByText(admin.username)).toBeVisible();
  expect(fetchCall).toHaveBeenCalledWith(
    expect.stringContaining('/auth/session/refresh'),
    expect.objectContaining({ credentials: 'include' }),
  );
});

it('does not rotate twice during StrictMode initialization', async () => {
  let calls = 0;
  server.use(
    http.post('/api/v1/auth/session/refresh', () => {
      calls++;
      return HttpResponse.json(envelope(restored));
    }),
  );
  mount(true);
  expect(await screen.findByText(admin.username)).toBeVisible();
  expect(calls).toBe(1);
});

it('keeps the restore screen during an outage and retries instead of redirecting to login', async () => {
  let unavailable = true;
  server.use(
    http.post('/api/v1/auth/session/refresh', () =>
      unavailable
        ? HttpResponse.json(envelope(null, 4002), { status: 503 })
        : HttpResponse.json(envelope(restored)),
    ),
  );
  mount();
  const retry = await screen.findByRole('button', { name: 'Thử lại' });
  expect(screen.queryByText('Login required')).not.toBeInTheDocument();
  unavailable = false;
  await userEvent.click(retry);
  expect(await screen.findByText(admin.username)).toBeVisible();
});

it('requires login for an expired cookie without loading private identity data', async () => {
  const me = vi.spyOn(authApi, 'me');
  mount();
  expect(await screen.findByText('Login required')).toBeVisible();
  expect(me).not.toHaveBeenCalled();
});

it('rejects a revoked identity even when the provider exchanged its token', async () => {
  server.use(
    http.post('/api/v1/auth/session/refresh', () => HttpResponse.json(envelope(restored))),
    http.get('/api/v1/auth/me', () => HttpResponse.json(envelope(null, 3003), { status: 401 })),
  );
  mount();
  expect(await screen.findByText('Login required')).toBeVisible();
  expect(auth.user).toBeNull();
});

it('does not resurrect a session after an offline logout and reload', async () => {
  let restores = 0;
  server.use(
    http.post('/api/v1/auth/session/refresh', () => {
      restores++;
      return HttpResponse.json(envelope(restored));
    }),
  );
  const first = mount();
  await screen.findByText(admin.username);
  vi.spyOn(authApi, 'logout').mockRejectedValue(new ApiError(0, 0));
  const close = vi
    .spyOn(authApi, 'closeBrowserSession')
    .mockRejectedValueOnce(new ApiError(0, 0))
    .mockResolvedValue(null);
  await act(async () => {
    await auth.logout();
  });
  expect(localStorage.getItem('autogarage-logout-pending')).toBe('1');
  first.unmount();
  mount();
  expect(await screen.findByText('Login required')).toBeVisible();
  expect(close).toHaveBeenCalledTimes(2);
  expect(restores).toBe(1);
});

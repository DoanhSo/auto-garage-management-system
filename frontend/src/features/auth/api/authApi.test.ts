/// <reference types="node" />
import { webcrypto } from 'node:crypto';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { http, HttpResponse } from 'msw';
import { authApi, createPkceAuthorizationRequest } from './authApi';
import { env } from '../../../shared/config/env';
import { server } from '../../../test/server';
import { envelope } from '../../../test/fixtures';

const storageKey = 'autogarage-oidc-pkce';
beforeEach(() => {
  sessionStorage.clear();
  vi.stubGlobal('crypto', webcrypto);
});

describe('Keycloak authorization code flow', () => {
  it.each(['auth', 'registrations'] as const)(
    'builds %s with S256 and a same-origin callback',
    async (mode) => {
      const { url, session } = await createPkceAuthorizationRequest({
        mode,
        returnTo: '/reception',
      });
      const parsed = new URL(url);
      expect(parsed.pathname).toBe(`/realms/autogarage-realm/protocol/openid-connect/${mode}`);
      expect(parsed.searchParams.get('client_id')).toBe(env.keycloakClientId);
      expect(parsed.searchParams.get('response_type')).toBe('code');
      expect(parsed.searchParams.get('redirect_uri')).toBe(`${location.origin}/login`);
      expect(parsed.searchParams.get('code_challenge_method')).toBe('S256');
      const digest = await webcrypto.subtle.digest(
        'SHA-256',
        new TextEncoder().encode(session.verifier),
      );
      expect(parsed.searchParams.get('code_challenge')).toBe(
        Buffer.from(digest).toString('base64url'),
      );
      expect(parsed.searchParams.get('state')).toBe(session.state);
      expect(session.returnTo).toBe('/reception');
    },
  );

  it('rejects external return paths and consumes callback state only once', async () => {
    const { session } = await createPkceAuthorizationRequest({ returnTo: '//attacker.test' });
    sessionStorage.setItem(storageKey, JSON.stringify(session));
    expect(authApi.consumePkceState(session.state)?.returnTo).toBe('/');
    expect(authApi.consumePkceState(session.state)).toBeNull();
  });

  it.each(['mismatch', 'expired', 'future', 'redirect', 'verifier', 'malformed'])(
    'rejects %s callback state',
    async (scenario) => {
      const { session } = await createPkceAuthorizationRequest();
      if (scenario === 'expired') session.createdAt -= env.pkceSessionMaxAgeMs + 1;
      if (scenario === 'future') session.createdAt += 60000;
      if (scenario === 'redirect') session.redirectUri = 'https://attacker.test/login';
      if (scenario === 'verifier') session.verifier = 'short';
      sessionStorage.setItem(storageKey, scenario === 'malformed' ? '{' : JSON.stringify(session));
      expect(
        authApi.consumePkceState(scenario === 'mismatch' ? 'wrong' : session.state),
      ).toBeNull();
      expect(sessionStorage.getItem(storageKey)).toBeNull();
    },
  );

  it('attempts Keycloak logout even when application logout fails', async () => {
    let revoked = false;
    server.use(
      http.post('/api/v1/auth/logout', () =>
        HttpResponse.json(envelope(null, 4002), { status: 503 }),
      ),
      http.post('/realms/autogarage-realm/protocol/openid-connect/logout', async ({ request }) => {
        expect(new URLSearchParams(await request.text()).get('refresh_token')).toBe('test-refresh');
        revoked = true;
        return new HttpResponse(null, { status: 204 });
      }),
    );
    await expect(authApi.logout('test-refresh')).rejects.toMatchObject({ code: 4002 });
    expect(revoked).toBe(true);
  });

  it('does not revoke an already revoked refresh token after backend confirms logout', async () => {
    let duplicateLogout = false;
    server.use(
      http.post('/api/v1/auth/logout', () => HttpResponse.json(envelope(null))),
      http.post('/realms/autogarage-realm/protocol/openid-connect/logout', () => {
        duplicateLogout = true;
        return new HttpResponse(null, { status: 400 });
      }),
    );
    await expect(authApi.logout('already-revoked')).resolves.toBeUndefined();
    expect(duplicateLogout).toBe(false);
  });
});

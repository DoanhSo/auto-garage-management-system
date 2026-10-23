import { request } from '../../../shared/api/client';
import { ApiError } from '../../../shared/api/errors';
import { env } from '../../../shared/config/env';
import type { OidcTokenBundle } from '../../../shared/types/contracts';
import { oidcTokenSchema, userSchema } from '../../../shared/types/schemas';

const PKCE_STORAGE_KEY = 'autogarage-oidc-pkce';

export interface PkceSessionState {
  verifier: string;
  state: string;
  redirectUri: string;
  returnTo?: string;
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function randomUrlSafe(byteLength = 32): string {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return base64UrlEncode(bytes);
}

async function computeS256Challenge(verifier: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(verifier));
  return base64UrlEncode(new Uint8Array(digest));
}

async function exchangeOidcToken(params: Record<string, string>): Promise<OidcTokenBundle> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), env.requestTimeoutMs);
  try {
    const response = await fetch(
      `${env.keycloakIssuer.replace(/\/$/, '')}/protocol/openid-connect/token`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          Accept: 'application/json',
        },
        body: new URLSearchParams(params).toString(),
        signal: controller.signal,
        credentials: 'omit',
        cache: 'no-store',
      },
    );
    if (!response.ok) {
      if (response.status === 400 || response.status === 401) {
        throw new ApiError(401, 3005);
      }
      throw new ApiError(response.status, 4002);
    }
    const parsed = oidcTokenSchema.parse(await response.json());
    return {
      accessToken: parsed.access_token,
      refreshToken: parsed.refresh_token ?? null,
      expiresIn: parsed.expires_in,
      tokenType: parsed.token_type,
    };
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(0, 0, {}, true);
  } finally {
    clearTimeout(timeout);
  }
}

export const authApi = {
  loginWithPassword: (values: { identifier: string; password: string }) =>
    exchangeOidcToken({
      grant_type: 'password',
      client_id: env.keycloakClientId,
      username: values.identifier.trim(),
      password: values.password,
      scope: 'openid',
    }),
  exchangeAuthorizationCode: (values: {
    code: string;
    codeVerifier: string;
    redirectUri: string;
  }) =>
    exchangeOidcToken({
      grant_type: 'authorization_code',
      client_id: env.keycloakClientId,
      code: values.code,
      code_verifier: values.codeVerifier,
      redirect_uri: values.redirectUri,
    }),
  refreshToken: (refreshToken: string) =>
    exchangeOidcToken({
      grant_type: 'refresh_token',
      client_id: env.keycloakClientId,
      refresh_token: refreshToken,
    }),
  startPkceRedirect: async (
    options?:
      | string
      | {
          returnTo?: string;
          idpHint?: string;
          mode?: 'auth' | 'registrations';
        },
  ) => {
    const opts = typeof options === 'string' ? { returnTo: options } : (options ?? {});
    const verifier = randomUrlSafe(32);
    const state = randomUrlSafe(24);
    const challenge = await computeS256Challenge(verifier);
    const redirectUri = `${window.location.origin}/login`;
    const payload: PkceSessionState = { verifier, state, redirectUri, returnTo: opts.returnTo };
    sessionStorage.setItem(PKCE_STORAGE_KEY, JSON.stringify(payload));
    const endpoint = opts.mode === 'registrations' ? 'registrations' : 'auth';
    const authorizeUrl = new URL(
      `${env.keycloakPublicOrigin.replace(/\/$/, '')}/protocol/openid-connect/${endpoint}`,
      window.location.origin,
    );
    authorizeUrl.searchParams.set('client_id', env.keycloakClientId);
    authorizeUrl.searchParams.set('response_type', 'code');
    authorizeUrl.searchParams.set('scope', 'openid');
    authorizeUrl.searchParams.set('redirect_uri', redirectUri);
    authorizeUrl.searchParams.set('state', state);
    authorizeUrl.searchParams.set('code_challenge', challenge);
    authorizeUrl.searchParams.set('code_challenge_method', 'S256');
    if (endpoint === 'auth') {
      authorizeUrl.searchParams.set('prompt', 'login');
    }
    if (opts.idpHint) {
      authorizeUrl.searchParams.set('kc_idp_hint', opts.idpHint);
    }
    window.location.assign(authorizeUrl.toString());
  },
  startGoogleRedirect: async (returnTo?: string) =>
    authApi.startPkceRedirect({ returnTo, idpHint: 'google', mode: 'auth' }),
  startRegistrationRedirect: async (returnTo?: string) =>
    authApi.startPkceRedirect({ returnTo, mode: 'registrations' }),
  consumePkceState: (state: string | null): PkceSessionState | null => {
    const raw = sessionStorage.getItem(PKCE_STORAGE_KEY);
    sessionStorage.removeItem(PKCE_STORAGE_KEY);
    if (!raw || !state) return null;
    try {
      const parsed = JSON.parse(raw) as PkceSessionState;
      return parsed.state === state && parsed.verifier && parsed.redirectUri ? parsed : null;
    } catch {
      return null;
    }
  },
  me: async (signal?: AbortSignal) =>
    userSchema.parse(await request<unknown>('/auth/me', { signal })),
  logout: async (refreshToken?: string | null) => {
    await request<null>('/auth/logout', { method: 'POST' });
    if (refreshToken) {
      try {
        await fetch(`${env.keycloakIssuer.replace(/\/$/, '')}/protocol/openid-connect/logout`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: new URLSearchParams({
            client_id: env.keycloakClientId,
            refresh_token: refreshToken,
          }).toString(),
          credentials: 'omit',
          cache: 'no-store',
        });
      } catch {
        // Resource server auth_version is already incremented; ignore best-effort OIDC end-session failure.
      }
    }
  },
};

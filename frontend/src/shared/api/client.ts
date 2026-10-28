import { env } from '../config/env';
import { ApiError } from './errors';
import { decodeJson, encodeJson } from './json';

interface TransportSession {
  token: string | null;
  onExpired: () => void;
  generation: number;
}
const session: TransportSession = {
  token: null,
  onExpired: () => {},
  generation: 0,
};
export function configureSession(token: string | null, onExpired: () => void) {
  session.token = token;
  session.onExpired = onExpired;
  session.generation++;
}
interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  body?: unknown;
  signal?: AbortSignal;
  authenticated?: boolean;
  binary?: boolean;
  credentials?: 'include' | 'omit';
}
function record(value: unknown): value is Record<string, unknown> {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const generation = session.generation;
  const token = options.authenticated === false ? null : session.token;
  const headers: Record<string, string> = {
    Accept: options.binary ? 'text/csv, application/json' : 'application/json',
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  const multipart = options.body instanceof FormData;
  if (options.body !== undefined && !multipart) headers['Content-Type'] = 'application/json';
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), env.requestTimeoutMs);
  const signal = options.signal
    ? AbortSignal.any([options.signal, controller.signal])
    : controller.signal;
  const assertSession = () => {
    if (options.authenticated !== false && generation !== session.generation)
      throw new DOMException('Session changed', 'AbortError');
  };
  try {
    const base = env.apiBase.replace(/\/$/, '');
    const cleanPath = base.endsWith('/api/v1') && path.startsWith('/api/v1/') ? path.slice('/api/v1'.length) : path;
    const url = `${base}${cleanPath.startsWith("/") ? "" : "/"}${cleanPath}`;
    const response = await fetch(url, {
      method: options.method ?? 'GET',
      headers,
      signal,
      credentials: options.credentials ?? 'omit',
      cache: 'no-store',
      body: multipart
        ? (options.body as FormData)
        : options.body === undefined
          ? undefined
          : encodeJson(options.body),
    });
    assertSession();
    if (
      options.binary &&
      response.ok &&
      response.headers.get('content-type')?.includes('text/csv')
    ) {
      const blob = await response.blob();
      assertSession();
      return blob as T;
    }
    const raw = await response.text();
    assertSession();
    let payload: unknown;
    try {
      payload = decodeJson(raw);
    } catch {
      throw new ApiError(response.status || 500, 4001);
    }
    if (
      !record(payload) ||
      typeof payload.code !== 'number' ||
      typeof payload.success !== 'boolean'
    )
      throw new ApiError(500, 4001);
    if (!response.ok || !payload.success) {
      const fields = record(payload.data)
        ? Object.fromEntries(
            Object.entries(payload.data).filter(
              (entry): entry is [string, string] => typeof entry[1] === 'string',
            ),
          )
        : {};
      const error = new ApiError(response.status, payload.code, fields);
      if (
        token &&
        response.status === 401 &&
        [3001, 3003].includes(payload.code) &&
        generation === session.generation
      )
        session.onExpired();
      throw error;
    }
    if (options.binary) throw new ApiError(500, 4001);
    return payload.data as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    assertSession();
    if (
      options.signal?.aborted ||
      (error instanceof DOMException && error.name === 'AbortError' && !controller.signal.aborted)
    )
      throw error;
    throw new ApiError(0, 0, {}, options.method !== undefined && options.method !== 'GET');
  } finally {
    clearTimeout(timeout);
  }
}
export function queryString(
  params: Record<string, string | number | boolean | undefined | null>,
): string {
  const values = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') values.set(key, String(value));
  });
  return `?${values.toString()}`;
}

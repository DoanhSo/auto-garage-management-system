import { describe, expect, it, vi } from 'vitest';
import { delay, http, HttpResponse } from 'msw';
import { server } from '../../test/server';
import { envelope } from '../../test/fixtures';
import { configureSession, request } from './client';
import { ApiError } from './errors';
describe('HTTP/session boundary', () => {
  it('uses bearer for private calls and omits it for public calls', async () => {
    const headers: (string | null)[] = [];
    server.use(
      http.get('/api/v1/probe', ({ request }) => {
        headers.push(request.headers.get('Authorization'));
        return HttpResponse.json(envelope({ ok: true }));
      }),
    );
    configureSession('memory-token', () => {});
    await request('/probe');
    await request('/probe', { authenticated: false });
    expect(headers).toEqual(['Bearer memory-token', null]);
  });
  it.each([3001, 3003])('expires an authenticated session exactly once on %s', async (code) => {
    const expired = vi.fn(() => configureSession(null, () => {}));
    configureSession('memory', expired);
    server.use(
      http.get('/api/v1/probe', () => HttpResponse.json(envelope(null, code), { status: 401 })),
    );
    await expect(request('/probe')).rejects.toMatchObject({ code });
    await expect(request('/probe')).rejects.toBeInstanceOf(ApiError);
    expect(expired).toHaveBeenCalledTimes(1);
  });
  it.each([
    [401, 3005],
    [503, 4002],
    [403, 3002],
  ])('keeps the session on status %s code %s', async (status, code) => {
    const expired = vi.fn();
    configureSession('memory', expired);
    server.use(
      http.post('/api/v1/probe', () => HttpResponse.json(envelope(null, code), { status })),
    );
    await expect(request('/probe', { method: 'POST' })).rejects.toMatchObject({
      status,
      code,
    });
    expect(expired).not.toHaveBeenCalled();
  });
  it('rejects old session responses after logout rather than leaking cached data', async () => {
    server.use(
      http.get('/api/v1/probe', async () => {
        await delay(30);
        return HttpResponse.json(envelope({ secret: 'old-user' }));
      }),
    );
    configureSession('old', () => {});
    const result = request('/probe');
    configureSession(null, () => {});
    await expect(result).rejects.toMatchObject({ name: 'AbortError' });
  });
  it('also rejects a session change while response body streams', async () => {
    let finish!: () => void;
    const body = new ReadableStream({
      start(controller) {
        finish = () => {
          controller.enqueue(new TextEncoder().encode(JSON.stringify(envelope({ private: true }))));
          controller.close();
        };
      },
    });
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response(body, {
            headers: { 'Content-Type': 'application/json' },
          }),
      ),
    );
    configureSession('old', () => {});
    const result = request('/probe');
    await Promise.resolve();
    configureSession(null, () => {});
    finish();
    await expect(result).rejects.toMatchObject({ name: 'AbortError' });
  });
  it('does not download JSON errors as CSV or expose raw server messages', async () => {
    server.use(
      http.get('/api/v1/export', () =>
        HttpResponse.json(
          { ...envelope(null, 4003), message: 'SQL password=private' },
          { status: 503 },
        ),
      ),
    );
    await expect(request('/export', { binary: true })).rejects.toMatchObject({
      code: 4003,
    });
    try {
      await request('/export', { binary: true });
    } catch (error) {
      expect((error as Error).message).not.toContain('private');
    }
  });
  it('rejects malformed success contract', async () => {
    server.use(http.get('/api/v1/probe', () => new HttpResponse('<html>proxy error</html>')));
    await expect(request('/probe')).rejects.toMatchObject({ code: 4001 });
  });
});

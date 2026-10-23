// @vitest-environment node
import { createServer as createHttpServer, type Server } from 'node:http';
import type { AddressInfo } from 'node:net';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { createServer, type ViteDevServer } from 'vite';
import viteConfig from '../vite.config';

let backend: Server;
let vite: ViteDevServer;
let origin: string;
beforeAll(async () => {
  backend = createHttpServer((req, res) => {
    const sameOrigin = req.headers.origin === `http://${req.headers.host}`;
    res.writeHead(sameOrigin ? 200 : 403, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ allowed: sameOrigin }));
  });
  await new Promise<void>((resolve) => backend.listen(0, '127.0.0.1', resolve));
  const config = await viteConfig({ command: 'serve', mode: 'test' });
  const apiProxy = config.server?.proxy?.['/api'];
  if (!apiProxy || typeof apiProxy === 'string')
    throw new Error('Expected explicit API proxy options');
  vite = await createServer({
    configFile: false,
    plugins: [],
    optimizeDeps: { noDiscovery: true, include: [] },
    server: {
      ...config.server,
      port: 0,
      host: '127.0.0.1',
      watch: null,
      proxy: {
        '/api': {
          ...apiProxy,
          target: `http://127.0.0.1:${(backend.address() as AddressInfo).port}`,
        },
      },
    },
  });
  await vite.listen();
  origin = `http://127.0.0.1:${(vite.httpServer!.address() as AddressInfo).port}`;
}, 20000);
afterAll(async () => {
  await vite?.close();
  if (backend) await new Promise<void>((resolve) => backend.close(() => resolve()));
});
describe('Same-origin development API proxy', () => {
  it.each([
    ['POST', '/api/v1/notifications/3/read'],
    ['DELETE', '/api/v1/notifications/3'],
    ['POST', '/api/v1/notifications/broadcast'],
  ])('preserves browser origin for %s %s', async (method, path) => {
    const response = await fetch(origin + path, { method, headers: { Origin: origin } });
    expect(response.status).toBe(200);
  });
  it('keeps an untrusted origin distinguishable from the browser host', async () => {
    const response = await fetch(origin + '/api/v1/notifications/broadcast', {
      method: 'POST',
      headers: { Origin: 'https://untrusted.example' },
    });
    expect(response.status).toBe(403);
  });
});

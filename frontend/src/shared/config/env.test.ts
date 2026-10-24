import { afterEach, describe, expect, it, vi } from 'vitest';

afterEach(() => {
  delete window.__AUTOGARAGE_CONFIG__;
  vi.resetModules();
});

describe('Docker runtime configuration', () => {
  it('uses the public Keycloak URL supplied at container startup without rebuilding the bundle', async () => {
    vi.resetModules();
    window.__AUTOGARAGE_CONFIG__ = { keycloakPublicOrigin: 'http://localhost:18081/realms/autogarage-realm' };
    const { env } = await import('./env');
    expect(env.keycloakPublicOrigin).toBe('http://localhost:18081/realms/autogarage-realm');
    expect(env.keycloakIssuer).toBe('/realms/autogarage-realm');
  });

  it('rejects runtime URLs containing embedded credentials', async () => {
    vi.resetModules();
    window.__AUTOGARAGE_CONFIG__ = { keycloakPublicOrigin: 'http://user:secret@localhost:18081/realms/autogarage-realm' };
    await expect(import('./env')).rejects.toThrow();
  });
});

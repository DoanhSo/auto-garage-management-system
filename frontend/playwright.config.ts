import { defineConfig, devices } from '@playwright/test';
const api = process.env.E2E_API_URL ?? 'http://127.0.0.1:55818/api/v1';
if (new URL(api).hostname !== '127.0.0.1')
  throw new Error('E2E requires an isolated local backend.');
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60000,
  reporter: [['line'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:5183',
    trace: 'off',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        viewport: { width: 1440, height: 1000 },
      },
    },
  ],
  webServer: {
    command: `"${process.execPath}" node_modules/vite/bin/vite.js --host 127.0.0.1 --port 5183`,
    url: 'http://127.0.0.1:5183',
    reuseExistingServer: process.env.E2E_USE_RUNNING_SERVER === 'true',
    env: { DEV_API_PROXY: new URL(api).origin },
  },
});

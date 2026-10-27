import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react(), tailwindcss()],
    server: {
      port: 5173,
      strictPort: true,
      proxy: {
        '/api': {
          target: env.DEV_API_PROXY ?? 'http://127.0.0.1:8080',
          // Keep the browser Host/Origin pair so same-origin writes stay same-origin upstream.
          changeOrigin: false,
        },
        '/realms': {
          target: env.DEV_KEYCLOAK_PROXY ?? 'http://localhost:8081',
          changeOrigin: true,
        },
      },
    },
    build: { sourcemap: false },
  };
});

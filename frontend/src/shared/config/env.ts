import { z } from 'zod';
const schema = z.object({
  apiBase: z.string().refine((value) => {
    if (
      value.startsWith('/') &&
      !value.startsWith('//') &&
      !value.includes('\\') &&
      !value.includes('?') &&
      !value.includes('#')
    )
      return true;
    try {
      const url = new URL(value);
      return (
        ['http:', 'https:'].includes(url.protocol) &&
        !url.username &&
        !url.password &&
        !url.search &&
        !url.hash
      );
    } catch {
      return false;
    }
  }, 'API base URL không hợp lệ'),
  notificationPollMs: z.coerce.number().int().min(15000).max(600000),
  requestTimeoutMs: z.coerce.number().int().min(1000).max(120000),
});
export const env = schema.parse({
  apiBase: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  notificationPollMs: import.meta.env.VITE_NOTIFICATION_POLL_MS ?? 60000,
  requestTimeoutMs: import.meta.env.VITE_REQUEST_TIMEOUT_MS ?? 20000,
});

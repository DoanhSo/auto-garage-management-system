import { payloadMessage } from './notificationParser';

/** Reader text of a notice payload; malformed or unknown payloads fall back to a generic line. */
export function notificationMessage(raw: string): string {
  return payloadMessage(raw);
}

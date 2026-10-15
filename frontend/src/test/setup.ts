import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, vi } from 'vitest';
import { configureSession } from '../shared/api/client';
import { server } from './server';
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  configureSession(null, () => {});
  vi.useRealTimers();
  vi.unstubAllGlobals();
});
afterAll(() => server.close());
if (!window.PointerEvent) window.PointerEvent = MouseEvent as typeof PointerEvent;
HTMLElement.prototype.scrollIntoView = vi.fn();
HTMLElement.prototype.hasPointerCapture = () => false;
HTMLElement.prototype.setPointerCapture = () => {};
HTMLElement.prototype.releasePointerCapture = () => {};

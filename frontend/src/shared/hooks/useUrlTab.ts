import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

/**
 * Keeps a page's sub-view in the URL (`?tab=` / `?view=`) so the sidebar tree and the page's own
 * tab strip always agree. A missing or unknown value falls back to `fallback`.
 */
export function useUrlTab<T extends string>(
  key: 'tab' | 'view',
  values: readonly T[],
  fallback: T = values[0],
): [T, (next: T) => void] {
  const [params, setParams] = useSearchParams();
  const raw = params.get(key);
  const current =
    raw !== null && (values as readonly string[]).includes(raw) ? (raw as T) : fallback;
  const select = useCallback(
    (next: T) =>
      setParams(
        (previous) => {
          const updated = new URLSearchParams(previous);
          updated.set(key, next);
          return updated;
        },
        { replace: true },
      ),
    [key, setParams],
  );
  return [current, select];
}

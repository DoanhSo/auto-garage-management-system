import { useSearchParams } from 'react-router-dom';
import type { UiPolicy } from '../types/contracts';
function bounded(value: string | null, fallback: number, min: number, max: number) {
  if (!value || !/^\d+$/.test(value)) return fallback;
  const parsed = Number(value);
  return Number.isSafeInteger(parsed) && parsed >= min && parsed <= max ? parsed : fallback;
}
export function useTableSearchParams(policy: UiPolicy) {
  const [params, setParams] = useSearchParams();
  const page = bounded(params.get('page'), 0, 0, policy.paginationMaxPage);
  const size = bounded(
    params.get('size'),
    policy.paginationDefaultSize,
    1,
    policy.paginationMaxSize,
  );
  function update(values: Record<string, string | number | undefined>) {
    setParams(
      (previous) => {
        const next = new URLSearchParams(previous);
        Object.entries(values).forEach(([key, value]) =>
          value === undefined || value === '' ? next.delete(key) : next.set(key, String(value)),
        );
        return next;
      },
      { replace: true },
    );
  }
  return { params, page, size, update };
}

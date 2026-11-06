import { useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';

/** Which employee's detail panel is open, kept in `?employeeId=` so it can be linked to. */
export function useStaffEmployee(): [string | null, (id: string | number | null) => void] {
  const [params, setParams] = useSearchParams();
  const selected = params.get('employeeId');
  const select = useCallback(
    (id: string | number | null) =>
      setParams(
        (previous) => {
          const updated = new URLSearchParams(previous);
          if (id === null) updated.delete('employeeId');
          else updated.set('employeeId', String(id));
          return updated;
        },
        { replace: true },
      ),
    [setParams],
  );
  return [selected, select];
}

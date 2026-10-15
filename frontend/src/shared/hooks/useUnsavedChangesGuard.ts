import { useEffect } from 'react';
import { useBlocker } from 'react-router-dom';
export function useUnsavedChangesGuard(dirty: boolean) {
  const blocker = useBlocker(dirty);
  useEffect(() => {
    if (!dirty) return;
    const guard = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };
    window.addEventListener('beforeunload', guard);
    return () => window.removeEventListener('beforeunload', guard);
  }, [dirty]);
  return blocker;
}

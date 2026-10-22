import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
const Context = createContext<(message: string) => void>(() => {});
export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState('');
  const notify = useCallback((value: string) => setMessage(value), []);
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(() => setMessage(''), 5000);
    return () => clearTimeout(timer);
  }, [message]);
  return (
    <Context value={notify}>
      {children}
      <div className="toast-region" role="status" aria-live="polite">
        {message && <div className="toast">{message}</div>}
      </div>
    </Context>
  );
}
export function useToast() {
  return useContext(Context);
}

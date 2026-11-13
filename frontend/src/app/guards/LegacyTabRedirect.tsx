import type { ReactNode } from 'react';
import { Navigate, useSearchParams } from 'react-router-dom';

/**
 * Keeps old `?tab=` addresses working after a tab moved to another screen: when the tab is listed in
 * `moved`, go to its new address (other query parameters travel along), otherwise show the page.
 */
export function LegacyTabRedirect({
  moved,
  children,
}: {
  moved: Readonly<Record<string, string>>;
  children: ReactNode;
}) {
  const [params] = useSearchParams();
  const tab = params.get('tab');
  const target = tab ? moved[tab] : undefined;
  if (!target) return <>{children}</>;

  const rest = new URLSearchParams(params);
  rest.delete('tab');
  const extra = rest.toString();
  const joiner = target.includes('?') ? '&' : '?';
  return <Navigate to={extra ? `${target}${joiner}${extra}` : target} replace />;
}

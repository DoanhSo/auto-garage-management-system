import { useEffect } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../providers/AuthProvider';
import { useToast } from '../providers/ToastProvider';
import { ADMIN_ROLES, homePathForRole } from '../access';
import type { Role } from '../../shared/types/contracts';

export const FORBIDDEN_PATH = '/403';
const DENIED_MESSAGE = 'Bạn không có quyền mở trang này, đã chuyển về trang chính của bạn';

/** Sends a signed-in user who opened a page outside their role back to their own workspace. */
function RoleHomeRedirect({ role }: { role: Role }) {
  const notify = useToast();
  const location = useLocation();
  const home = homePathForRole(role);
  // A role whose home page is itself off-limits would loop; the status page breaks the cycle.
  const target = location.pathname === home ? FORBIDDEN_PATH : home;
  useEffect(() => {
    if (target !== FORBIDDEN_PATH) notify(DENIED_MESSAGE);
  }, [notify, target]);
  return <Navigate to={target} replace />;
}

export function RequireAuth() {
  const { user } = useAuth();
  const location = useLocation();
  return user ? (
    <Outlet />
  ) : (
    <Navigate to="/login" replace state={{ returnTo: location.pathname + location.search }} />
  );
}

export function AuthGuard({ allowedRoles }: { allowedRoles: readonly Role[] }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user)
    return (
      <Navigate to="/login" replace state={{ returnTo: location.pathname + location.search }} />
    );
  return allowedRoles.includes(user.role) ? <Outlet /> : <RoleHomeRedirect role={user.role} />;
}

export const RequireRoles = AuthGuard;

export function RequireAdmin() {
  return <AuthGuard allowedRoles={ADMIN_ROLES} />;
}

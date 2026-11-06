import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../../app/providers/AuthProvider';
import { STAFF_MANAGE_ROLES } from '../../../app/access';
import { employeeHref } from '../utils/links';

/** An employee's name that opens their profile panel, plain bold text for roles without access. */
export function EmployeeLink({ id, children }: { id: string | number; children: ReactNode }) {
  const { user } = useAuth();
  if (!user || !STAFF_MANAGE_ROLES.includes(user.role)) return <strong>{children}</strong>;
  return (
    <Link className="link-button" to={employeeHref(id)}>
      {children}
    </Link>
  );
}

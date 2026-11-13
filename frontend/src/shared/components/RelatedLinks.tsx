import { Link } from 'react-router-dom';
import { useAuth } from '../../app/providers/AuthProvider';
import type { Role } from '../types/contracts';

export interface RelatedLink {
  to: string;
  label: string;
  roles: readonly Role[];
}

/** A strip of links to the screens that depend on the current one, limited to what the role may open. */
export function RelatedLinks({ links }: { links: readonly RelatedLink[] }) {
  const { user } = useAuth();
  const visible = links.filter((link) => !!user && link.roles.includes(user.role));
  if (visible.length === 0) return null;
  return (
    <nav className="related-links" aria-label="Liên quan">
      <span className="muted">Liên quan:</span>
      {visible.map((link) => (
        <Link key={link.to} to={link.to}>
          {link.label}
        </Link>
      ))}
    </nav>
  );
}

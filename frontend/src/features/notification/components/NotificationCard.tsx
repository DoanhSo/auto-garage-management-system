import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowRight,
  Car,
  Coins,
  FileText,
  Megaphone,
  Package,
  Settings,
  ShieldAlert,
  Wrench,
} from 'lucide-react';
import { Badge } from '../../../shared/components/ui';
import type {
  NotificationCategory,
  NotificationTone,
  ParsedNotification,
} from '../utils/notificationParser';

const icons: Record<NotificationCategory, typeof Wrench> = {
  maintenance: Wrench,
  warranty: ShieldAlert,
  estimate: FileText,
  repair: Car,
  payroll: Coins,
  inventory: Package,
  system: Settings,
  broadcast: Megaphone,
};

export const toneBadge = {
  info: 'primary',
  success: 'success',
  warning: 'warning',
  danger: 'danger',
  neutral: 'secondary',
} as const satisfies Record<NotificationTone, string>;

export function NotificationIcon({
  parsed,
  size = 18,
}: {
  parsed: ParsedNotification;
  size?: number;
}) {
  const Icon = icons[parsed.category];
  return (
    <span className={`notification-icon tone-${parsed.tone}`} aria-hidden>
      <Icon size={size} />
    </span>
  );
}

export function NotificationCard({
  parsed,
  onOpen,
  actions,
  recipient,
  preview = false,
}: {
  parsed: ParsedNotification;
  /** Opens the detail dialog; omitted in the broadcast preview. */
  onOpen?: () => void;
  actions?: ReactNode;
  recipient?: ReactNode;
  preview?: boolean;
}) {
  return (
    <article className={`notification-card-item ${parsed.isUnread ? 'is-unread' : ''}`}>
      <NotificationIcon parsed={parsed} />
      <div className="notification-card-body">
        <div className="notification-card-header">
          <Badge variant={toneBadge[parsed.tone]} className="notification-badge-chip">
            {parsed.categoryLabel}
          </Badge>
          <time title={parsed.timeFormatted}>{parsed.timeRelative}</time>
          {recipient}
          <span className="notification-read-state">
            {parsed.isUnread ? (
              <>
                <span className="unread-dot" aria-hidden />
                Chưa đọc
              </>
            ) : (
              'Đã đọc'
            )}
          </span>
        </div>
        {onOpen ? (
          <button type="button" className="notification-card-title" onClick={onOpen}>
            {parsed.title}
          </button>
        ) : (
          <strong className="notification-card-title">{parsed.title}</strong>
        )}
        <p className="notification-card-summary">{parsed.summary}</p>
        {(parsed.linkUrl || actions) && (
          <div className="notification-card-footer">
            {parsed.linkUrl && parsed.linkText && !preview && (
              <Link to={parsed.linkUrl} className="notification-cta-button">
                {parsed.linkText}
                <ArrowRight size={14} aria-hidden />
              </Link>
            )}
            {parsed.linkUrl && parsed.linkText && preview && (
              <span className="notification-cta-button">
                {parsed.linkText}
                <ArrowRight size={14} aria-hidden />
              </span>
            )}
            {actions && <div className="row-actions">{actions}</div>}
          </div>
        )}
      </div>
    </article>
  );
}

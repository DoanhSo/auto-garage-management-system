import { useEffect, useId, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Bell, CheckCheck } from 'lucide-react';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { useUiPolicy } from '../../../shared/config/policy';
import { Button, Loading } from '../../../shared/components/ui';
import type { Notice } from '../../../shared/types/contracts';
import {
  useInboxQuery,
  useMarkAllReadMutation,
  useMarkReadMutation,
  useUnreadCountQuery,
} from '../hooks/useNotifications';
import { parseNotification } from '../utils/notificationParser';
import { NotificationIcon } from './NotificationCard';

const POPOVER_SIZE = 5;
const INBOX_PATH = '/notifications';

export function NotificationPopover() {
  const [open, setOpen] = useState(false);
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const menuId = useId();
  const navigate = useNavigate();
  const notify = useToast();
  const policy = useUiPolicy();
  const unread = useUnreadCountQuery();
  const latest = useInboxQuery({ unread: false, page: 0, size: POPOVER_SIZE }, open);
  const mark = useMarkReadMutation();
  const markAll = useMarkAllReadMutation();
  const hasUnread = !!unread.data && BigInt(unread.data) > 0n;
  const zone = policy.data?.garageTimeZone ?? Intl.DateTimeFormat().resolvedOptions().timeZone;

  useEffect(() => {
    if (!open) return;
    const outside = (event: PointerEvent) => {
      if (root.current && !root.current.contains(event.target as Node)) setOpen(false);
    };
    const escape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return;
      setOpen(false);
      trigger.current?.focus();
    };
    document.addEventListener('pointerdown', outside);
    document.addEventListener('keydown', escape);
    return () => {
      document.removeEventListener('pointerdown', outside);
      document.removeEventListener('keydown', escape);
    };
  }, [open]);

  function openNotice(notice: Notice, linkUrl?: string) {
    if (!notice.readAt) mark.mutate(notice.id);
    setOpen(false);
    navigate(linkUrl ?? INBOX_PATH);
  }

  return (
    <div className="notification-popover" ref={root}>
      <button
        ref={trigger}
        type="button"
        className="bell-button"
        aria-label={`Thông báo${hasUnread ? ' — ' + unread.data + ' chưa đọc' : ''}`}
        aria-haspopup="true"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        <Bell size={19} />
        {hasUnread && <span className="bell-dot" />}
      </button>
      {open && (
        <div
          className="notification-popover-menu"
          id={menuId}
          role="dialog"
          aria-label="Thông báo mới"
        >
          <div className="notification-popover-head">
            <strong>Thông báo</strong>
            {hasUnread && <span className="tab-count">{unread.data}</span>}
            {hasUnread && (
              <Button
                variant="ghost"
                className="notification-popover-readall"
                pending={markAll.isPending}
                onClick={() =>
                  markAll.mutate(undefined, { onError: (error) => notify(errorMessage(error)) })
                }
              >
                <CheckCheck size={15} />
                Đọc tất cả
              </Button>
            )}
          </div>
          <div className="notification-popover-body">
            {latest.isPending ? (
              <Loading />
            ) : latest.error ? (
              <p className="notification-popover-empty">Chưa tải được thông báo.</p>
            ) : !latest.data.content.length ? (
              <p className="notification-popover-empty">Bạn chưa có thông báo nào.</p>
            ) : (
              latest.data.content.map((notice) => {
                const parsed = parseNotification(notice, zone);
                return (
                  <button
                    key={notice.id}
                    type="button"
                    className={`notification-popover-item ${parsed.isUnread ? 'is-unread' : ''}`}
                    onClick={() => openNotice(notice, parsed.linkUrl)}
                  >
                    <NotificationIcon parsed={parsed} size={16} />
                    <span className="notification-popover-text">
                      <strong>{parsed.title}</strong>
                      <span>{parsed.summary}</span>
                      <small title={parsed.timeFormatted}>{parsed.timeRelative}</small>
                    </span>
                    {parsed.isUnread && <span className="unread-dot" aria-label="Chưa đọc" />}
                  </button>
                );
              })
            )}
          </div>
          <Link
            to={INBOX_PATH}
            className="notification-popover-footer"
            onClick={() => setOpen(false)}
          >
            Xem tất cả thông báo trong hộp thư →
          </Link>
        </div>
      )}
    </div>
  );
}

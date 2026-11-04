import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, Check } from 'lucide-react';
import { Badge, Button, FormError, Loading, QueryError } from '../../../shared/components/ui';
import { dateTime } from '../../../shared/utils/format';
import type { Notice } from '../../../shared/types/contracts';
import { useMarkReadMutation, useNotificationQuery } from '../hooks/useNotifications';
import { parseNotification } from '../utils/notificationParser';
import { NotificationIcon, toneBadge } from './NotificationCard';

export function NoticeContent({
  notice,
  zone,
  management = false,
}: {
  notice: Notice;
  zone: string;
  management?: boolean;
}) {
  const parsed = parseNotification(notice, zone);
  return (
    <article className="notification-reader">
      <div className="notification-reader-head">
        <NotificationIcon parsed={parsed} size={20} />
        <div className="notification-meta">
          <Badge variant={toneBadge[parsed.tone]}>{parsed.categoryLabel}</Badge>
          <time dateTime={notice.createdAt} title={parsed.timeFormatted}>
            {dateTime(notice.createdAt, zone)}
          </time>
          {management && (
            <span>
              Người nhận: {notice.userId ? `#${notice.userId}` : 'Chưa xác định'} · {notice.channel}
            </span>
          )}
        </div>
      </div>
      <h2>{parsed.title}</h2>
      <p className="notice-body">{parsed.fullMessage}</p>
      {parsed.metadata.length > 0 && (
        <dl className="notification-meta-grid">
          {parsed.metadata.map((item) => (
            <div key={item.label}>
              <dt>{item.label}</dt>
              <dd>{item.value}</dd>
            </div>
          ))}
        </dl>
      )}
      {parsed.linkUrl && parsed.linkText && !management && (
        <Link to={parsed.linkUrl} className="button button-primary notification-reader-cta">
          {parsed.linkText}
          <ArrowRight size={15} aria-hidden />
        </Link>
      )}
    </article>
  );
}
export function NoticeDetails({ id, zone }: { id: string; zone: string }) {
  const detail = useNotificationQuery(id);
  const mark = useMarkReadMutation();
  const attempted = useRef(false);
  const { mutate } = mark;
  useEffect(() => {
    if (detail.data && !detail.data.readAt && !attempted.current) {
      attempted.current = true;
      mutate(id);
    }
  }, [detail.data, id, mutate]);
  if (detail.isPending) return <Loading />;
  if (detail.error)
    return (
      <QueryError
        error={detail.error}
        retry={() => {
          void detail.refetch();
        }}
      />
    );
  return (
    <>
      <NoticeContent notice={detail.data} zone={zone} />
      {mark.error ? (
        <>
          <FormError message="Chưa lưu được trạng thái đã đọc. Bạn có thể thử lại." />
          <Button pending={mark.isPending} onClick={() => mark.mutate(id)}>
            Thử đánh dấu đã đọc
          </Button>
        </>
      ) : (
        <p className="read-status" role="status">
          <Check size={15} />
          {detail.data.readAt ? 'Đã đọc' : 'Đang lưu trạng thái…'}
        </p>
      )}
    </>
  );
}

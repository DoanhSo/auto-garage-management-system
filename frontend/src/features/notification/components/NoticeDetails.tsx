import { useEffect, useRef } from 'react';
import { Check } from 'lucide-react';
import { Button, FormError, Loading, QueryError } from '../../../shared/components/ui';
import { dateTime } from '../../../shared/utils/format';
import type { Notice } from '../../../shared/types/contracts';
import { useMarkReadMutation, useNotificationQuery } from '../hooks/useNotifications';
import { notificationMessage } from '../utils/message';

export function NoticeContent({
  notice,
  zone,
  management = false,
}: {
  notice: Notice;
  zone: string;
  management?: boolean;
}) {
  return (
    <article className="notification-reader">
      <div className="notification-meta">
        <time dateTime={notice.createdAt}>{dateTime(notice.createdAt, zone)}</time>
        {management && (
          <span>
            Người nhận: {notice.userId ? `#${notice.userId}` : 'Chưa xác định'} · {notice.channel}
          </span>
        )}
      </div>
      <h2>{notice.subject || 'Thông báo từ gara'}</h2>
      <p className="notice-body">{notificationMessage(notice.payloadJson)}</p>
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

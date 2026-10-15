import { useEffect, useRef, useState } from 'react';
import { Bell, Check, RefreshCw } from 'lucide-react';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Button,
  Empty,
  FormError,
  Loading,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { Pagination } from '../../../shared/components/DataTable';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import { dateTime } from '../../../shared/utils/format';
import type { UiPolicy } from '../../../shared/types/contracts';
import {
  useInboxQuery,
  useNotificationQuery,
  useMarkReadMutation,
} from '../hooks/useNotifications';
function message(raw: string): string {
  try {
    const data: unknown = JSON.parse(raw);
    if (data && typeof data === 'object' && 'message' in data && typeof data.message === 'string')
      return data.message;
  } catch {
    /* Display a safe fallback instead of raw JSON. */
  }
  return 'Thông báo từ gara. Liên hệ nhân viên phụ trách nếu cần thêm thông tin.';
}
function NoticeDetails({ id, zone }: { id: string; zone: string }) {
  const detail = useNotificationQuery(id);
  const mark = useMarkReadMutation();
  const attempted = useRef(false);
  const { mutate: markRead } = mark;
  useEffect(() => {
    if (detail.data && !detail.data.readAt && !attempted.current) {
      attempted.current = true;
      markRead(id);
    }
  }, [detail.data, id, markRead]);
  return detail.isPending ? (
    <Loading />
  ) : detail.error ? (
    <QueryError
      error={detail.error}
      retry={() => {
        void detail.refetch();
      }}
    />
  ) : (
    <article className="notice-detail">
      <span className="notice-detail-icon">
        <Bell size={24} />
      </span>
      <p className="eyebrow">THÔNG BÁO CỦA BẠN</p>
      <h2>{detail.data.subject || 'Thông báo từ gara'}</h2>
      <time dateTime={detail.data.createdAt}>{dateTime(detail.data.createdAt, zone)}</time>
      <p className="notice-body">{message(detail.data.payloadJson)}</p>
      {mark.error ? (
        <>
          <FormError message="Chưa lưu được trạng thái đã đọc. Nội dung thông báo vẫn hiển thị." />
          <Button pending={mark.isPending} onClick={() => mark.mutate(id)}>
            Thử đánh dấu đã đọc
          </Button>
        </>
      ) : (
        <p className="read-status">
          <Check size={15} />
          {detail.data.readAt
            ? 'Đã đọc'
            : mark.isPending
              ? 'Đang lưu trạng thái…'
              : 'Đang xác nhận trạng thái đã đọc…'}
        </p>
      )}
    </article>
  );
}
function ReadyInbox({ policy }: { policy: UiPolicy }) {
  const table = useTableSearchParams(policy);
  const unread = table.params.get('unread') === 'true';
  const inbox = useInboxQuery({ unread, page: table.page, size: table.size });
  const [selected, setSelected] = useState<string | null>(null);
  return (
    <>
      <PageHeader
        eyebrow="KẾT NỐI VỚI GARA"
        title="Thông báo"
        description="Các cập nhật và nhắc lịch dành riêng cho tài khoản của bạn."
        actions={
          <Button
            pending={inbox.isFetching}
            onClick={() => {
              void inbox.refetch();
            }}
          >
            <RefreshCw size={16} />
            Làm mới
          </Button>
        }
      />
      <section className="panel inbox-panel">
        <div className="tabs" role="group" aria-label="Lọc thông báo">
          <button
            type="button"
            aria-pressed={!unread}
            onClick={() => table.update({ unread: undefined, page: 0 })}
          >
            Tất cả
          </button>
          <button
            type="button"
            aria-pressed={unread}
            onClick={() => table.update({ unread: 'true', page: 0 })}
          >
            Chưa đọc
          </button>
        </div>
        {inbox.isPending ? (
          <Loading />
        ) : inbox.error ? (
          <QueryError
            error={inbox.error}
            retry={() => {
              void inbox.refetch();
            }}
          />
        ) : !inbox.data.content.length && !selected ? (
          <Empty
            title={unread ? 'Bạn đã đọc hết thông báo' : 'Chưa có thông báo'}
            description={
              unread
                ? 'Thông báo mới sẽ xuất hiện tại đây.'
                : 'Thông báo và nhắc lịch từ gara sẽ được hiển thị tại đây.'
            }
          />
        ) : (
          <>
            <div className={`inbox-grid ${selected ? 'has-selection' : ''}`}>
              <ul className="notice-list" aria-label="Danh sách thông báo">
                {!inbox.data.content.length && (
                  <li>
                    <Empty
                      title="Không còn thông báo trong bộ lọc"
                      description="Bạn có thể tiếp tục xem nội dung đang mở."
                    />
                  </li>
                )}
                {inbox.data.content.map((notice) => (
                  <li key={notice.id}>
                    <button
                      type="button"
                      className={`notice-item ${!notice.readAt ? 'unread' : ''} ${selected === notice.id ? 'selected' : ''}`}
                      onClick={() => setSelected(notice.id)}
                      aria-pressed={selected === notice.id}
                    >
                      <span className="notice-small-icon">
                        <Bell size={18} />
                      </span>
                      <span className="notice-summary">
                        <span className="notice-title">
                          {notice.subject || 'Thông báo từ gara'}
                          {!notice.readAt && <span className="unread-dot" aria-label="Chưa đọc" />}
                        </span>
                        <span className="notice-preview">{message(notice.payloadJson)}</span>
                        <time dateTime={notice.createdAt}>
                          {dateTime(notice.createdAt, policy.garageTimeZone)}
                        </time>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
              <div className="notice-detail-panel">
                {selected ? (
                  <>
                    <Button
                      variant="ghost"
                      className="notice-back"
                      onClick={() => setSelected(null)}
                    >
                      Quay về danh sách
                    </Button>
                    <NoticeDetails key={selected} id={selected} zone={policy.garageTimeZone} />
                  </>
                ) : (
                  <Empty
                    title="Chọn một thông báo"
                    description="Nội dung chi tiết sẽ hiển thị ở đây."
                  />
                )}
              </div>
            </div>
            <Pagination
              page={table.page}
              size={table.size}
              data={inbox.data}
              policy={policy}
              disabled={inbox.isFetching}
              onChange={(page, size) => {
                setSelected(null);
                table.update({ page, size });
              }}
            />
          </>
        )}
      </section>
    </>
  );
}
export function NotificationsPage() {
  const policy = useUiPolicy();
  return policy.isPending ? (
    <Loading />
  ) : policy.error ? (
    <QueryError
      error={policy.error}
      retry={() => {
        void policy.refetch();
      }}
    />
  ) : (
    <ReadyInbox policy={policy.data} />
  );
}
export { message as notificationMessage };

import { useMemo, useState } from 'react';
import { Check, CheckCheck, Plus, RefreshCw, Trash2 } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Button,
  ConfirmDialog,
  Empty,
  Loading,
  PageHeader,
  QueryError,
  Sheet,
} from '../../../shared/components/ui';
import { Pagination } from '../../../shared/components/DataTable';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import type { Notice, UiPolicy } from '../../../shared/types/contracts';
import {
  useAdminNotificationsQuery,
  useDeleteNotificationMutation,
  useInboxQuery,
  useMarkAllReadMutation,
  useMarkReadMutation,
  useUnreadCountQuery,
} from '../hooks/useNotifications';
import { BroadcastModal } from '../components/BroadcastModal';
import { NoticeContent, NoticeDetails } from '../components/NoticeDetails';
import { NotificationCard } from '../components/NotificationCard';
import { notificationMessage } from '../utils/message';
import {
  NOTIFICATION_GROUPS,
  parseNotification,
  timeGroup,
  timeGroupLabels,
  type NotificationGroupKey,
  type TimeGroup,
} from '../utils/notificationParser';

type InboxTab = 'all' | 'unread' | NotificationGroupKey | 'admin';
const TIME_GROUP_ORDER: readonly TimeGroup[] = ['today', 'yesterday', 'week', 'older'];

function ReadyInbox({ policy }: { policy: UiPolicy }) {
  const { user } = useAuth();
  const notify = useToast();
  const isAdmin = user?.role === 'ADMIN';
  const table = useTableSearchParams(policy);
  const mode = table.params.get('mode') === 'admin' && isAdmin ? 'admin' : 'inbox';
  const unread = mode === 'inbox' && table.params.get('unread') === 'true';
  const group = NOTIFICATION_GROUPS.find(
    (item) => mode === 'inbox' && !unread && item.key === table.params.get('group'),
  );
  const inbox = useInboxQuery({ unread, page: table.page, size: table.size }, mode === 'inbox');
  const adminInbox = useAdminNotificationsQuery({
    page: table.page,
    size: table.size,
    enabled: mode === 'admin',
  });
  const unreadCount = useUnreadCountQuery();
  const hasUnread = !!unreadCount.data && BigInt(unreadCount.data) > 0n;
  const mark = useMarkReadMutation();
  const markAll = useMarkAllReadMutation();
  const remove = useDeleteNotificationMutation();
  const [selected, setSelected] = useState<{ notice: Notice; owned: boolean } | null>(null);
  const [broadcastOpen, setBroadcastOpen] = useState(false);
  const [revoke, setRevoke] = useState<Notice | null>(null);
  const query = mode === 'admin' ? adminInbox : inbox;
  const zone = policy.garageTimeZone;
  const cards = useMemo(() => {
    const now = new Date();
    return (query.data?.content ?? [])
      .map((notice) => ({ notice, parsed: parseNotification(notice, zone, now) }))
      .filter(
        ({ parsed }) => !group || (group.categories as readonly string[]).includes(parsed.category),
      );
  }, [query.data, zone, group]);
  const sections = useMemo(() => {
    const now = new Date();
    return TIME_GROUP_ORDER.map((key) => ({
      key,
      items: cards.filter(({ notice }) => timeGroup(notice.createdAt, zone, now) === key),
    })).filter((section) => section.items.length > 0);
  }, [cards, zone]);
  const activeTab: InboxTab =
    mode === 'admin' ? 'admin' : unread ? 'unread' : (group?.key ?? 'all');
  const canRead = (notice: Notice) =>
    notice.channel === 'WEB' && (mode === 'inbox' || notice.userId === user?.id);
  function chooseTab(next: InboxTab) {
    setSelected(null);
    table.update({
      mode: next === 'admin' ? 'admin' : undefined,
      unread: next === 'unread' ? 'true' : undefined,
      group: next === 'service' || next === 'money' ? next : undefined,
      page: 0,
    });
  }
  function readAll() {
    markAll.mutate(undefined, {
      onSuccess: (count) =>
        notify(
          count ? `Đã đánh dấu ${count} thông báo là đã đọc.` : 'Không còn thông báo chưa đọc.',
        ),
      onError: (error) => notify(errorMessage(error)),
    });
  }
  function confirmRevoke(notice: Notice) {
    remove.reset();
    setRevoke(notice);
  }
  function cardActions(notice: Notice) {
    return (
      <>
        {canRead(notice) && !notice.readAt && (
          <Button
            variant="ghost"
            aria-label={`Đánh dấu đã đọc #${notice.id}`}
            title="Đánh dấu đã đọc"
            pending={mark.isPending && mark.variables === notice.id}
            disabled={mark.isPending}
            onClick={() =>
              mark.mutate(notice.id, {
                onSuccess: () => notify('Đã đánh dấu đã đọc.'),
                onError: (error) => notify(errorMessage(error)),
              })
            }
          >
            <Check size={17} />
          </Button>
        )}
        {isAdmin && (
          <Button
            variant="ghost"
            className="delete-action"
            aria-label={`Thu hồi thông báo #${notice.id}`}
            title="Thu hồi thông báo"
            onClick={() => confirmRevoke(notice)}
          >
            <Trash2 size={16} />
          </Button>
        )}
      </>
    );
  }
  return (
    <div className="notification-page">
      <PageHeader
        title="Thông báo"
        description="Theo dõi tiến độ xe, nhắc bảo dưỡng định kỳ và thông báo từ ban quản trị gara."
        actions={
          <>
            <Button
              aria-label="Làm mới thông báo"
              title="Làm mới"
              pending={query.isFetching}
              onClick={() => {
                void query.refetch();
              }}
            >
              <RefreshCw size={16} />
            </Button>
            {mode === 'inbox' && hasUnread && (
              <Button pending={markAll.isPending} onClick={readAll}>
                <CheckCheck size={16} />
                Đọc tất cả
              </Button>
            )}
            {isAdmin && (
              <Button variant="primary" onClick={() => setBroadcastOpen(true)}>
                <Plus size={17} />
                Phát thông báo
              </Button>
            )}
          </>
        }
      />
      <section className="panel table-panel notification-panel" aria-label="Hộp thư thông báo">
        <div className="notification-toolbar">
          <div className="tabs" role="group" aria-label="Lọc thông báo">
            <button
              type="button"
              aria-pressed={activeTab === 'all'}
              onClick={() => chooseTab('all')}
            >
              Tất cả
            </button>
            <button
              type="button"
              aria-pressed={activeTab === 'unread'}
              onClick={() => chooseTab('unread')}
            >
              Chưa đọc
              {hasUnread && <span className="tab-count">{unreadCount.data}</span>}
            </button>
            {NOTIFICATION_GROUPS.map((item) => (
              <button
                key={item.key}
                type="button"
                aria-pressed={activeTab === item.key}
                onClick={() => chooseTab(item.key)}
              >
                {item.label}
              </button>
            ))}
            {isAdmin && (
              <button
                type="button"
                aria-pressed={activeTab === 'admin'}
                onClick={() => chooseTab('admin')}
              >
                Toàn hệ thống
              </button>
            )}
          </div>
          <span className="notification-total">{query.data?.totalElements ?? '—'} thông báo</span>
        </div>
        {query.isPending ? (
          <Loading />
        ) : query.error ? (
          <QueryError
            error={query.error}
            retry={() => {
              void query.refetch();
            }}
          />
        ) : !cards.length ? (
          <Empty
            title={
              unread
                ? 'Bạn đã đọc hết thông báo'
                : group
                  ? `Không có thông báo ${group.label} trong trang này`
                  : 'Chưa có thông báo'
            }
            description="Thông báo mới sẽ xuất hiện tại đây."
          />
        ) : (
          <div className="notification-feed" aria-busy={query.isFetching}>
            {group && (
              <p className="notification-filter-note">
                Đang lọc nhóm “{group.label}” trong trang hiện tại.
              </p>
            )}
            {sections.map((section) => (
              <section key={section.key} aria-label={timeGroupLabels[section.key]}>
                <h3 className="notification-group-title">{timeGroupLabels[section.key]}</h3>
                <div className="notification-card-list">
                  {section.items.map(({ notice, parsed }) => (
                    <NotificationCard
                      key={notice.id}
                      parsed={parsed}
                      onOpen={() => setSelected({ notice, owned: canRead(notice) })}
                      actions={cardActions(notice)}
                      recipient={
                        mode === 'admin' ? (
                          <span className="notification-channel">
                            {notice.userId ? `Người nhận #${notice.userId}` : 'Chưa xác định'} ·{' '}
                            {notice.channel}
                          </span>
                        ) : undefined
                      }
                    />
                  ))}
                </div>
              </section>
            ))}
          </div>
        )}
        <Pagination
          page={table.page}
          size={table.size}
          data={query.data}
          policy={policy}
          disabled={query.isFetching}
          onChange={(page, size) => {
            setSelected(null);
            table.update({ page, size });
          }}
        />
      </section>
      {selected && (
        <Sheet
          open
          placement="center"
          size="md"
          title="Chi tiết thông báo"
          description={
            selected.owned
              ? 'Thông báo trong hộp thư của bạn.'
              : 'Xem thông báo của người nhận. Trạng thái đọc do người nhận cập nhật.'
          }
          onClose={() => setSelected(null)}
        >
          <div className="notification-reader-content">
            {selected.owned ? (
              <NoticeDetails
                key={selected.notice.id}
                id={selected.notice.id}
                zone={policy.garageTimeZone}
              />
            ) : (
              <NoticeContent notice={selected.notice} zone={policy.garageTimeZone} management />
            )}
          </div>
        </Sheet>
      )}
      {isAdmin && broadcastOpen && <BroadcastModal onClose={() => setBroadcastOpen(false)} />}
      <ConfirmDialog
        open={!!revoke}
        title="Thu hồi thông báo?"
        description="Thông báo này sẽ được xóa khỏi hộp thư người nhận. Thao tác được ghi vào nhật ký."
        pending={remove.isPending}
        danger
        confirmLabel="Thu hồi thông báo"
        error={remove.error ? errorMessage(remove.error) : undefined}
        onCancel={() => {
          if (!remove.isPending) setRevoke(null);
        }}
        onConfirm={() => {
          if (!revoke || remove.isPending) return;
          remove.mutate(revoke.id, {
            onSuccess: () => {
              notify('Đã thu hồi thông báo.');
              if (selected?.notice.id === revoke.id) setSelected(null);
              setRevoke(null);
            },
          });
        }}
      />
    </div>
  );
}
export function NotificationsPage() {
  const policy = useUiPolicy();
  if (policy.isPending) return <Loading />;
  if (policy.error)
    return (
      <QueryError
        error={policy.error}
        retry={() => {
          void policy.refetch();
        }}
      />
    );
  return <ReadyInbox policy={policy.data} />;
}
export { notificationMessage };

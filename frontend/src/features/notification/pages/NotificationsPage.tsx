import { useMemo, useState } from 'react';
import type { ColumnDef } from '@tanstack/react-table';
import { Check, Plus, RefreshCw, Trash2 } from 'lucide-react';
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
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import { dateTime } from '../../../shared/utils/format';
import type { Notice, UiPolicy } from '../../../shared/types/contracts';
import {
  useAdminNotificationsQuery,
  useDeleteNotificationMutation,
  useInboxQuery,
  useMarkReadMutation,
} from '../hooks/useNotifications';
import { BroadcastModal } from '../components/BroadcastModal';
import { NoticeContent, NoticeDetails } from '../components/NoticeDetails';
import { notificationMessage } from '../utils/message';

function ReadyInbox({ policy }: { policy: UiPolicy }) {
  const { user } = useAuth();
  const notify = useToast();
  const isAdmin = user?.role === 'ADMIN';
  const table = useTableSearchParams(policy);
  const mode = table.params.get('mode') === 'admin' && isAdmin ? 'admin' : 'inbox';
  const unread = mode === 'inbox' && table.params.get('unread') === 'true';
  const inbox = useInboxQuery({ unread, page: table.page, size: table.size }, mode === 'inbox');
  const adminInbox = useAdminNotificationsQuery({
    page: table.page,
    size: table.size,
    enabled: mode === 'admin',
  });
  const mark = useMarkReadMutation();
  const remove = useDeleteNotificationMutation();
  const [selected, setSelected] = useState<{ notice: Notice; owned: boolean } | null>(null);
  const [broadcastOpen, setBroadcastOpen] = useState(false);
  const [revoke, setRevoke] = useState<Notice | null>(null);
  const query = mode === 'admin' ? adminInbox : inbox;
  const rows = useMemo(() => query.data?.content ?? [], [query.data]);
  const canRead = (notice: Notice) =>
    notice.channel === 'WEB' && (mode === 'inbox' || notice.userId === user?.id);
  function chooseTab(next: 'all' | 'unread' | 'admin') {
    setSelected(null);
    table.update({
      mode: next === 'admin' ? 'admin' : undefined,
      unread: next === 'unread' ? 'true' : undefined,
      page: 0,
    });
  }
  function confirmRevoke(notice: Notice) {
    remove.reset();
    setRevoke(notice);
  }
  const columns: ColumnDef<Notice>[] = [
    {
      accessorKey: 'subject',
      header: 'Thông báo',
      cell: ({ row }) => (
        <button
          type="button"
          className={`notification-title-button ${row.original.readAt ? '' : 'is-unread'}`}
          onClick={() => setSelected({ notice: row.original, owned: canRead(row.original) })}
        >
          <span className="notification-subject">
            {!row.original.readAt && <span className="unread-dot" aria-label="Chưa đọc" />}
            {row.original.subject || 'Thông báo từ gara'}
          </span>
          <span className="notification-preview">
            {notificationMessage(row.original.payloadJson)}
          </span>
        </button>
      ),
    },
    ...(mode === 'admin'
      ? [
          {
            id: 'recipient',
            header: 'Người nhận',
            cell: ({ row }: { row: { original: Notice } }) => (
              <span className="muted">
                {row.original.userId ? `#${row.original.userId}` : 'Chưa xác định'}
                <small className="notification-channel">{row.original.channel}</small>
              </span>
            ),
          },
        ]
      : []),
    {
      id: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => (
        <span className="notification-status">
          {row.original.channel !== 'WEB'
            ? row.original.status
            : row.original.readAt
              ? 'Đã đọc'
              : 'Chưa đọc'}
        </span>
      ),
    },
    {
      accessorKey: 'createdAt',
      header: 'Thời gian',
      cell: ({ row }) => (
        <time className="notification-time" dateTime={row.original.createdAt}>
          {dateTime(row.original.createdAt, policy.garageTimeZone)}
        </time>
      ),
    },
    {
      id: 'actions',
      header: 'Thao tác',
      cell: ({ row }) => (
        <div className="row-actions">
          {canRead(row.original) && !row.original.readAt && (
            <Button
              variant="ghost"
              aria-label={`Đánh dấu đã đọc #${row.original.id}`}
              title="Đánh dấu đã đọc"
              pending={mark.isPending && mark.variables === row.original.id}
              disabled={mark.isPending}
              onClick={() =>
                mark.mutate(row.original.id, {
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
              aria-label={`Thu hồi thông báo #${row.original.id}`}
              title="Thu hồi thông báo"
              onClick={() => confirmRevoke(row.original)}
            >
              <Trash2 size={16} />
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <div className="notification-page">
      <PageHeader
        title="Thông báo"
        description="Cập nhật và nhắc lịch từ gara."
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
              aria-pressed={mode === 'inbox' && !unread}
              onClick={() => chooseTab('all')}
            >
              Hộp thư của tôi
            </button>
            <button type="button" aria-pressed={unread} onClick={() => chooseTab('unread')}>
              Chưa đọc
            </button>
            {isAdmin && (
              <button
                type="button"
                aria-pressed={mode === 'admin'}
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
        ) : !rows.length ? (
          <Empty
            title={unread ? 'Bạn đã đọc hết thông báo' : 'Chưa có thông báo'}
            description="Thông báo mới sẽ xuất hiện tại đây."
          />
        ) : (
          <DataTable
            caption="Danh sách thông báo"
            columns={columns}
            data={rows}
            busy={query.isFetching}
            hideOnMobile={['createdAt']}
          />
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

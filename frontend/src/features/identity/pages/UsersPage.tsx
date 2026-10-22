import { useCallback, useMemo, useRef, useState } from 'react';
import { LockKeyhole, LockOpen, Plus, ShieldCheck, SquarePen, Trash2 } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useToast } from '../../../app/providers/ToastProvider';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Button,
  ConfirmDialog,
  Loading,
  PageHeader,
  QueryError,
  RoleBadge,
  StatusBadge,
} from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { dateTime, initials } from '../../../shared/utils/format';
import { errorMessage } from '../../../shared/api/errors';
import type { UiPolicy, User } from '../../../shared/types/contracts';
import { useUsersQuery, useDeleteUserMutation, useUpdateUserMutation } from '../hooks/useUsers';
import { UserEditor } from '../components/UserEditor';
import { UserFilters } from '../components/UserFilters';
import { UserCards } from '../components/UserCards';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';
const sortFields = ['id', 'username', 'fullName', 'role', 'active', 'createdAt'];
function ReadyUsers({ policy }: { policy: UiPolicy }) {
  const compact = useMediaQuery('(max-width: 640px)');
  const auth = useAuth();
  const notify = useToast();
  const table = useTableSearchParams(policy);
  const [editing, setEditing] = useState<string | null>(null);
  const [action, setAction] = useState<{ kind: 'lock' | 'delete'; user: User } | null>(null);
  const lastTrigger = useRef<HTMLElement | null>(null);
  const createTrigger = useRef<HTMLButtonElement>(null);
  const keyword = table.params.get('keyword')?.slice(0, 254) ?? '';
  const debounced = useDebouncedValue(keyword);
  const active =
    table.params.get('active') === 'true'
      ? true
      : table.params.get('active') === 'false'
        ? false
        : undefined;
  const sortBy = sortFields.includes(table.params.get('sortBy') ?? '')
    ? table.params.get('sortBy')!
    : 'id';
  const sortDir = table.params.get('sortDir') === 'desc' ? 'desc' : 'asc';
  const users = useUsersQuery({
    keyword: debounced.trim() || undefined,
    active,
    page: table.page,
    size: table.size,
    sortBy,
    sortDir,
  });
  const remove = useDeleteUserMutation();
  const lock = useUpdateUserMutation();
  const closeEditor = useCallback(() => setEditing(null), []);
  const openEditor = useCallback((id: string, trigger: HTMLElement) => {
    lastTrigger.current = trigger;
    setEditing(id);
  }, []);
  const { reset: resetRemove } = remove;
  const { reset: resetLock } = lock;
  const openAction = useCallback(
    (kind: 'lock' | 'delete', user: User) => {
      resetRemove();
      resetLock();
      setAction({ kind, user });
    },
    [resetRemove, resetLock],
  );
  const columns = useMemo<ColumnDef<User>[]>(
    () => [
      {
        id: 'account',
        header: 'Tài khoản',
        cell: ({ row }) => (
          <div className="account-cell">
            <span className="avatar">{initials(row.original.fullName)}</span>
            <div>
              <strong>{row.original.fullName}</strong>
              <span>
                {row.original.username}
                {row.original.id === auth.user?.id && <span className="you-tag">Bạn</span>}
              </span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'email',
        header: 'Email',
        cell: ({ row }) => (
          <span className="email-cell">{row.original.email || 'Chưa cung cấp'}</span>
        ),
      },
      {
        accessorKey: 'role',
        header: 'Vai trò',
        cell: ({ row }) => <RoleBadge role={row.original.role} />,
      },
      {
        accessorKey: 'active',
        header: 'Trạng thái',
        cell: ({ row }) => <StatusBadge active={row.original.active} />,
      },
      {
        accessorKey: 'createdAt',
        header: 'Ngày tạo',
        cell: ({ row }) => (
          <span className="muted nowrap">
            {dateTime(row.original.createdAt, policy.garageTimeZone)}
          </span>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button
              variant="ghost"
              aria-label={`Sửa tài khoản ${row.original.fullName}`}
              onClick={(event) => openEditor(row.original.id, event.currentTarget)}
            >
              <SquarePen size={16} />
              <span>Sửa</span>
            </Button>
            {
              <Button
                variant="ghost"
                aria-label={`${row.original.active ? 'Khóa' : 'Mở khóa'} tài khoản ${row.original.fullName}`}
                onClick={() => openAction('lock', row.original)}
              >
                {row.original.active ? <LockKeyhole size={16} /> : <LockOpen size={16} />}
                <span>{row.original.active ? 'Khóa' : 'Mở khóa'}</span>
              </Button>
            }
            <Button
              variant="ghost"
              className="delete-action"
              aria-label={`Xóa tài khoản ${row.original.fullName}`}
              onClick={() => openAction('delete', row.original)}
            >
              <Trash2 size={16} />
              <span>Xóa</span>
            </Button>
          </div>
        ),
      },
    ],
    [auth.user?.id, policy.garageTimeZone, openEditor, openAction],
  );
  const submitAction = async () => {
    if (!action || remove.isPending || lock.isPending) return;
    try {
      const target = action.user;
      if (action.kind === 'delete')
        await remove.mutateAsync({ id: target.id, version: target.version });
      else
        await lock.mutateAsync({
          id: target.id,
          values: {
            fullName: target.fullName,
            email: target.email,
            role: target.role,
            active: !target.active,
            version: target.version,
          },
        });
      if (target.id === auth.user?.id) {
        auth.clearSession(
          action.kind === 'delete'
            ? 'Tài khoản của bạn đã được xóa.'
            : 'Tài khoản của bạn đã được khóa.',
        );
        return;
      }
      notify(
        action.kind === 'delete'
          ? 'Đã xóa tài khoản khỏi danh sách quản trị.'
          : target.active
            ? 'Đã khóa tài khoản. Nhấn Mở khóa để kích hoạt lại.'
            : 'Đã mở khóa tài khoản.',
      );
      setAction(null);
    } catch {
      /* Error remains in the confirmation dialog. */
    }
  };
  const ready = keyword === debounced;
  return (
    <>
      <PageHeader
        eyebrow="QUẢN TRỊ TRUY CẬP"
        title="Tài khoản"
        description="Quản lý thành viên, vai trò và quyền truy cập vào gara."
        actions={
          <Button
            variant="primary"
            ref={createTrigger}
            onClick={(event) => openEditor('new', event.currentTarget)}
          >
            <Plus size={18} />
            Thêm tài khoản
          </Button>
        }
      />
      <div className="info-strip">
        <ShieldCheck size={18} />
        <span>
          Thêm, sửa, khóa và xóa tài khoản theo phân quyền. Tài khoản đã khóa có thể mở lại.
        </span>
      </div>
      <section className="panel table-panel">
        <UserFilters
          keyword={keyword}
          active={active}
          sortBy={sortBy}
          sortDir={sortDir}
          onChange={table.update}
        />
        {users.isPending || !ready ? (
          <Loading />
        ) : users.error ? (
          <QueryError
            error={users.error}
            retry={() => {
              void users.refetch();
            }}
          />
        ) : (
          <>
            {compact ? (
              <UserCards
                users={users.data.content}
                currentId={auth.user?.id}
                onEdit={openEditor}
                onLock={(user) => openAction('lock', user)}
                onDelete={(user) => openAction('delete', user)}
              />
            ) : (
              <DataTable
                caption="Danh sách tài khoản"
                columns={columns}
                data={users.data.content}
                busy={users.isFetching}
                hideOnMobile={['email', 'createdAt']}
              />
            )}
            <Pagination
              page={table.page}
              size={table.size}
              data={users.data}
              policy={policy}
              disabled={users.isFetching}
              onChange={(page, size) => table.update({ page, size })}
            />
          </>
        )}
      </section>
      {editing && (
        <UserEditor
          key={editing}
          id={editing}
          policy={policy}
          onClose={closeEditor}
          onRestoreFocus={() => {
            if (lastTrigger.current?.isConnected) lastTrigger.current.focus();
            else createTrigger.current?.focus();
          }}
        />
      )}
      <ConfirmDialog
        open={!!action}
        title={
          action?.kind === 'delete'
            ? 'Xóa tài khoản?'
            : action?.user.active
              ? 'Khóa tài khoản?'
              : 'Mở khóa tài khoản?'
        }
        description={`${action?.kind === 'delete' ? `Xóa tài khoản ${action.user.fullName} khỏi danh sách quản trị và thu hồi phiên đăng nhập. Lịch sử được giữ để đối soát; không thể mở lại tài khoản này bằng mục Sửa.` : action?.user.active ? `Khóa tài khoản ${action.user.fullName}: tài khoản sẽ không thể đăng nhập. Nhấn Mở khóa trên cùng dòng để kích hoạt lại.` : `Mở khóa tài khoản ${action?.user.fullName ?? ''}: người dùng có thể đăng nhập lại với quyền hiện tại.`}${action?.user.id === auth.user?.id ? ' Đây là tài khoản của bạn; bạn sẽ được đăng xuất.' : ''}`}
        pending={remove.isPending || lock.isPending}
        error={remove.error || lock.error ? errorMessage(remove.error || lock.error) : undefined}
        onCancel={() => setAction(null)}
        onConfirm={() => {
          void submitAction();
        }}
        confirmLabel={
          action?.kind === 'delete'
            ? 'Xóa tài khoản'
            : action?.user.active
              ? 'Khóa tài khoản'
              : 'Mở khóa tài khoản'
        }
        danger={action?.kind === 'delete' || !!action?.user.active}
      />
    </>
  );
}
export function UsersPage() {
  const policy = useUiPolicy();
  return policy.isPending ? (
    <Loading label="Đang tải chính sách hệ thống…" />
  ) : policy.error ? (
    <QueryError
      error={policy.error}
      retry={() => {
        void policy.refetch();
      }}
    />
  ) : (
    <ReadyUsers policy={policy.data} />
  );
}

import { useMemo, useState } from 'react';
import type { ColumnDef } from '@tanstack/react-table';
import { Eye, ShieldCheck } from 'lucide-react';
import { parse, stringify } from 'lossless-json';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Button,
  Field,
  Loading,
  PageHeader,
  QueryError,
  ReadOnlyField,
  Sheet,
} from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { dateTime } from '../../../shared/utils/format';
import type { AuditEvent, UiPolicy } from '../../../shared/types/contracts';
import { useAuditEventsQuery } from '../hooks/useAuditEvents';
const actionLabels: Record<string, string> = {
  USER_DELETED: 'Xóa tài khoản',
  AUTH_LOGIN: 'Đăng nhập',
  AUTH_LOGIN_FAILED: 'Đăng nhập thất bại',
  AUTH_LOGOUT_ALL: 'Đăng xuất',
  ADMIN_BOOTSTRAPPED: 'Khởi tạo quản trị viên',
  USER_CREATED: 'Tạo tài khoản',
  USER_UPDATED: 'Cập nhật tài khoản',
  USER_PASSWORD_CHANGED: 'Đổi mật khẩu',
  NOTIFICATION_ENQUEUED: 'Tạo thông báo',
  NOTIFICATION_READ: 'Đọc thông báo',
};
export function safeAuditDetails(raw: string | null) {
  if (!raw) return 'Không có metadata bổ sung.';
  try {
    return (
      stringify(
        parse(raw, (key, value: unknown) =>
          /password|token|secret|hash|credential/i.test(key) ? '[Đã ẩn]' : value,
        ),
        null,
        2,
      ) ?? 'Không có metadata.'
    );
  } catch {
    return 'Không thể đọc metadata.';
  }
}
function ReadyAudit({ policy }: { policy: UiPolicy }) {
  const table = useTableSearchParams(policy);
  const actor = table.params.get('actorId') ?? '';
  const action = table.params.get('action')?.slice(0, 100) ?? '';
  const debouncedActor = useDebouncedValue(actor);
  const debouncedAction = useDebouncedValue(action);
  const [selected, setSelected] = useState<AuditEvent | null>(null);
  const validActor =
    !actor || (/^[1-9]\d{0,18}$/.test(actor) && BigInt(actor) <= 9223372036854775807n);
  const ready = actor === debouncedActor && action === debouncedAction;
  const events = useAuditEventsQuery(
    {
      actorId: debouncedActor || undefined,
      action: debouncedAction.trim() || undefined,
      page: table.page,
      size: table.size,
    },
    validActor && ready,
  );
  const columns = useMemo<ColumnDef<AuditEvent>[]>(
    () => [
      {
        accessorKey: 'createdAt',
        header: 'Thời gian',
        cell: ({ row }) => (
          <span className="nowrap muted">
            {dateTime(row.original.createdAt, policy.garageTimeZone)}
          </span>
        ),
      },
      {
        accessorKey: 'action',
        header: 'Hành động',
        cell: ({ row }) => (
          <div className="two-lines">
            <strong>{actionLabels[row.original.action] ?? row.original.action}</strong>
            <span>{row.original.action}</span>
          </div>
        ),
      },
      {
        accessorKey: 'actorUserId',
        header: 'Người thực hiện',
        cell: ({ row }) =>
          row.original.actorUserId
            ? `Tài khoản #${row.original.actorUserId}`
            : 'Không xác định / hệ thống',
      },
      {
        accessorKey: 'entityType',
        header: 'Đối tượng',
        cell: ({ row }) => (
          <span>
            {row.original.entityType} {row.original.entityId && `#${row.original.entityId}`}
          </span>
        ),
      },
      {
        id: 'details',
        header: 'Chi tiết',
        cell: ({ row }) => (
          <Button
            variant="ghost"
            onClick={() => setSelected(row.original)}
            aria-label={`Xem sự kiện ${row.original.id}`}
          >
            <Eye size={16} />
            Xem
          </Button>
        ),
      },
    ],
    [policy.garageTimeZone],
  );
  return (
    <>
      <PageHeader
        eyebrow="KIỂM SOÁT VÀ TRUY VẾT"
        title="Nhật ký hoạt động"
        description="Theo dõi những thay đổi quan trọng trong hệ thống."
      />
      <div className="info-strip">
        <ShieldCheck size={18} />
        <span>Nhật ký chỉ đọc. Chỉ hiển thị thông tin kiểm toán được phép truy cập.</span>
      </div>
      <section className="panel table-panel">
        <div className="filter-bar audit-filters">
          <Field
            label="Mã người thực hiện"
            error={!validActor ? 'Nhập ID dương hợp lệ.' : undefined}
          >
            <input
              className="input"
              inputMode="numeric"
              value={actor}
              maxLength={19}
              placeholder="Ví dụ: 12"
              onChange={(event) => table.update({ actorId: event.target.value, page: 0 })}
            />
          </Field>
          <Field label="Hành động">
            <input
              className="input"
              value={action}
              maxLength={100}
              list="audit-actions"
              placeholder="Tất cả hành động"
              onChange={(event) => table.update({ action: event.target.value, page: 0 })}
            />
          </Field>
          <datalist id="audit-actions">
            {Object.entries(actionLabels).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </datalist>
          {(actor || action) && (
            <Button
              variant="ghost"
              onClick={() => table.update({ actorId: undefined, action: undefined, page: 0 })}
            >
              Đặt lại
            </Button>
          )}
        </div>
        {!validActor ? (
          <p className="filter-help">Sửa mã người thực hiện để tìm kiếm.</p>
        ) : !ready || events.isPending ? (
          <Loading />
        ) : events.error ? (
          <QueryError
            error={events.error}
            retry={() => {
              void events.refetch();
            }}
          />
        ) : (
          <>
            <DataTable
              caption="Nhật ký hệ thống"
              columns={columns}
              data={events.data.content}
              hideOnMobile={['entityType']}
            />
            <Pagination
              page={table.page}
              size={table.size}
              data={events.data}
              policy={policy}
              disabled={events.isFetching}
              onChange={(page, size) => table.update({ page, size })}
            />
          </>
        )}
      </section>
      <Sheet
        open={!!selected}
        title="Chi tiết sự kiện"
        description={selected ? `Sự kiện #${selected.id}` : 'Thông tin kiểm toán'}
        placement="center"
        size="md"
        onClose={() => setSelected(null)}
      >
        <div className="editor-form">
          {selected && (
            <>
              <dl className="detail-list">
                <ReadOnlyField label="Hành động">
                  {actionLabels[selected.action] ?? selected.action}
                </ReadOnlyField>
                <ReadOnlyField label="Thời gian">
                  {dateTime(selected.createdAt, policy.garageTimeZone)}
                </ReadOnlyField>
                <ReadOnlyField label="Người thực hiện">
                  {selected.actorUserId
                    ? `Tài khoản #${selected.actorUserId}`
                    : 'Không xác định / hệ thống'}
                </ReadOnlyField>
                <ReadOnlyField label="Đối tượng">
                  {selected.entityType} {selected.entityId && `#${selected.entityId}`}
                </ReadOnlyField>
              </dl>
              <h3>Metadata</h3>
              <pre className="metadata">{safeAuditDetails(selected.afterJson)}</pre>
            </>
          )}
        </div>
      </Sheet>
    </>
  );
}
export function AuditPage() {
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
    <ReadyAudit policy={policy.data} />
  );
}

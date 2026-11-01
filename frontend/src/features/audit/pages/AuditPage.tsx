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
const actionInfo: Record<string, { label: string; description: string }> = {
  AUTH_LOGIN: { label: 'Đăng nhập', description: 'Đăng nhập vào hệ thống thành công.' },
  AUTH_LOGIN_FAILED: {
    label: 'Đăng nhập thất bại',
    description: 'Nhập sai thông tin hoặc tài khoản bị khóa.',
  },
  AUTH_LOGOUT_ALL: {
    label: 'Đăng xuất',
    description: 'Đăng xuất và thu hồi mọi phiên đăng nhập trên các thiết bị.',
  },
  ADMIN_BOOTSTRAPPED: {
    label: 'Khởi tạo quản trị viên',
    description: 'Tạo tài khoản quản trị đầu tiên của hệ thống.',
  },
  CUSTOMER_JIT_PROVISIONED: {
    label: 'Tự tạo hồ sơ khách hàng',
    description: 'Hệ thống tự tạo tài khoản khách hàng ở lần đăng nhập đầu tiên.',
  },
  USER_CREATED: { label: 'Tạo tài khoản', description: 'Thêm một tài khoản người dùng mới.' },
  USER_UPDATED: {
    label: 'Cập nhật tài khoản',
    description: 'Sửa thông tin, vai trò hoặc trạng thái của tài khoản.',
  },
  USER_DELETED: { label: 'Xóa tài khoản', description: 'Xóa một tài khoản người dùng.' },
  USER_PASSWORD_CHANGED: {
    label: 'Đổi mật khẩu',
    description: 'Mật khẩu của tài khoản đã được thay đổi.',
  },
  NOTIFICATION_ENQUEUED: {
    label: 'Tạo thông báo',
    description: 'Gửi một thông báo tới người dùng.',
  },
  NOTIFICATION_BROADCAST: {
    label: 'Gửi thông báo hàng loạt',
    description: 'Gửi một thông báo tới nhiều người dùng cùng lúc.',
  },
  NOTIFICATION_READ: { label: 'Đọc thông báo', description: 'Người dùng đã đọc một thông báo.' },
  NOTIFICATION_DELETED: { label: 'Xóa thông báo', description: 'Xóa một thông báo.' },
  CREATE_EMPLOYEE: { label: 'Thêm nhân viên', description: 'Thêm một nhân viên mới vào xưởng.' },
  UPDATE_EMPLOYEE: { label: 'Cập nhật nhân viên', description: 'Sửa thông tin nhân viên.' },
  DEACTIVATE_EMPLOYEE: {
    label: 'Ngừng hoạt động nhân viên',
    description: 'Nhân viên không còn được xếp ca làm việc.',
  },
  CREATE_SHIFT: { label: 'Tạo ca làm việc', description: 'Xếp một ca làm việc cho nhân viên.' },
  CANCEL_SHIFT: { label: 'Hủy ca làm việc', description: 'Hủy một ca làm việc đã xếp.' },
  CREATE_BAY: { label: 'Thêm khoang sửa chữa', description: 'Thêm một khoang sửa chữa mới.' },
  UPDATE_BAY: { label: 'Cập nhật khoang sửa chữa', description: 'Sửa thông tin khoang sửa chữa.' },
  DEACTIVATE_BAY: {
    label: 'Ngừng sử dụng khoang',
    description: 'Khoang sửa chữa không còn nhận xe.',
  },
  SCHEDULE_MAINTENANCE: {
    label: 'Lên lịch bảo trì khoang',
    description: 'Đặt lịch bảo trì, khoang tạm ngưng nhận xe trong thời gian đó.',
  },
};
const entityLabels: Record<string, string> = {
  USER: 'Tài khoản',
  NOTIFICATION: 'Thông báo',
  Employee: 'Nhân viên',
  EmployeeShift: 'Ca làm việc',
  ServiceBay: 'Khoang sửa chữa',
  BayReservation: 'Lịch giữ khoang',
};
const actionLabel = (action: string) => actionInfo[action]?.label ?? action;
const entityText = (type: string, id: string | null) =>
  `${entityLabels[type] ?? type}${id ? ` #${id}` : ''}`;
const actionLabels = Object.fromEntries(
  Object.entries(actionInfo).map(([code, info]) => [code, info.label]),
);
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
            <strong>{actionLabel(row.original.action)}</strong>
            <span>{actionInfo[row.original.action]?.description ?? row.original.action}</span>
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
        cell: ({ row }) => <span>{entityText(row.original.entityType, row.original.entityId)}</span>,
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
                <ReadOnlyField label="Hành động">{actionLabel(selected.action)}</ReadOnlyField>
                <ReadOnlyField label="Mô tả">
                  {actionInfo[selected.action]?.description ?? 'Không có mô tả.'}
                </ReadOnlyField>
                <ReadOnlyField label="Mã kỹ thuật">{selected.action}</ReadOnlyField>
                <ReadOnlyField label="Thời gian">
                  {dateTime(selected.createdAt, policy.garageTimeZone)}
                </ReadOnlyField>
                <ReadOnlyField label="Người thực hiện">
                  {selected.actorUserId
                    ? `Tài khoản #${selected.actorUserId}`
                    : 'Không xác định / hệ thống'}
                </ReadOnlyField>
                <ReadOnlyField label="Đối tượng">
                  {entityText(selected.entityType, selected.entityId)}
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

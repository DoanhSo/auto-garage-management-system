import { useMemo, useState } from 'react';
import type { ColumnDef } from '@tanstack/react-table';
import { Eye, Search, ShieldCheck, X } from 'lucide-react';
import { parse, stringify } from 'lossless-json';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Badge,
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
import { roleLabels, type AuditEvent, type UiPolicy } from '../../../shared/types/contracts';
import { useAuditEventsQuery } from '../hooks/useAuditEvents';
import { useUsersQuery } from '../../identity/hooks/useUsers';
import {
  auditActionsOf,
  auditDomains,
  auditSeverityBadge,
  auditSeverityLabels,
  formatAuditEntity,
  getAuditActionMeta,
  type AuditDomain,
  auditActionCodesMatching,
} from '../constants/auditCatalog';

const ACTOR_OPTION_LIMIT = 100;
const isAuditDomain = (value: string): value is AuditDomain =>
  auditDomains.some((domain) => domain.key === value);

function ActionBadge({ action }: { action: string }) {
  const meta = getAuditActionMeta(action);
  return <Badge variant={auditSeverityBadge[meta.severity]}>{meta.domainLabel}</Badge>;
}
function ActorCell({ event }: { event: AuditEvent }) {
  if (!event.actorUserId) return <span className="muted">Hệ thống (tự động)</span>;
  if (!event.actorFullName) return <span>Tài khoản #{event.actorUserId}</span>;
  return (
    <div className="two-lines">
      <strong>{event.actorFullName}</strong>
      <span>
        @{event.actorUsername}
        {event.actorRole ? ` · ${roleLabels[event.actorRole]}` : ''}
      </span>
    </div>
  );
}
const actorText = (event: AuditEvent) =>
  !event.actorUserId
    ? 'Hệ thống (tự động)'
    : event.actorFullName
      ? `${event.actorFullName} (@${event.actorUsername}${event.actorRole ? ` · ${roleLabels[event.actorRole]}` : ''})`
      : `Tài khoản #${event.actorUserId}`;
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
/** Giới hạn độ dài từ khóa tìm, khớp AuditConstants.QUERY_MAX_LENGTH của máy chủ. */
const AUDIT_QUERY_MAX_LENGTH = 100;

function ReadyAudit({ policy }: { policy: UiPolicy }) {
  const table = useTableSearchParams(policy);
  const actor = table.params.get('actorId') ?? '';
  const action = table.params.get('action')?.slice(0, 100) ?? '';
  const rawDomain = table.params.get('domain') ?? '';
  const domain: AuditDomain | '' = isAuditDomain(rawDomain) ? rawDomain : '';
  const domainActions = auditActionsOf(domain);
  const debouncedActor = useDebouncedValue(actor);
  const debouncedAction = useDebouncedValue(action);
  const search = table.params.get('q')?.slice(0, AUDIT_QUERY_MAX_LENGTH) ?? '';
  const debouncedSearch = useDebouncedValue(search);
  const searchActions = useMemo(() => auditActionCodesMatching(debouncedSearch), [debouncedSearch]);
  const [selected, setSelected] = useState<AuditEvent | null>(null);
  const users = useUsersQuery({
    page: 0,
    size: ACTOR_OPTION_LIMIT,
    sortBy: 'fullName',
    sortDir: 'asc',
  });
  // actorId arrives from the URL, so an edited link must not reach the API with a bad id.
  const validActor =
    !actor || (/^[1-9]\d{0,18}$/.test(actor) && BigInt(actor) <= 9223372036854775807n);
  const ready =
    actor === debouncedActor && action === debouncedAction && search === debouncedSearch;
  const events = useAuditEventsQuery(
    {
      actorId: debouncedActor || undefined,
      action: debouncedAction.trim() || undefined,
      query: debouncedSearch.trim() || undefined,
      actions: debouncedSearch.trim() && searchActions.length > 0 ? searchActions : undefined,
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
        cell: ({ row }) => {
          const meta = getAuditActionMeta(row.original.action);
          return (
            <div className="two-lines audit-action-cell">
              <ActionBadge action={row.original.action} />
              <strong>{meta.label}</strong>
              <span>{meta.description}</span>
            </div>
          );
        },
      },
      {
        accessorKey: 'actorUserId',
        header: 'Người thực hiện',
        cell: ({ row }) => <ActorCell event={row.original} />,
      },
      {
        accessorKey: 'entityType',
        header: 'Đối tượng',
        cell: ({ row }) => (
          <span>
            {formatAuditEntity(
              row.original.entityType,
              row.original.entityId,
              row.original.entityLabel,
            )}
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
          <Field label="Tìm kiếm nhật ký">
            <div className="user-search">
              <Search size={17} aria-hidden />
              <input
                type="search"
                className="input"
                placeholder="Tìm theo hành động, người thực hiện, đối tượng…"
                value={search}
                maxLength={AUDIT_QUERY_MAX_LENGTH}
                onChange={(event) => table.update({ q: event.target.value || undefined, page: 0 })}
              />
              {search && (
                <button
                  type="button"
                  className="search-clear"
                  aria-label="Xóa từ khóa tìm kiếm"
                  onClick={() => table.update({ q: undefined, page: 0 })}
                >
                  <X size={15} />
                </button>
              )}
            </div>
          </Field>
          <Field label="Phân hệ nghiệp vụ">
            <select
              className="input"
              value={domain}
              onChange={(event) => {
                const next = event.target.value;
                const keepAction =
                  !next || auditActionsOf(next as AuditDomain).some(([code]) => code === action);
                table.update({
                  domain: next || undefined,
                  action: keepAction ? action || undefined : undefined,
                  page: 0,
                });
              }}
            >
              <option value="">Tất cả phân hệ</option>
              {auditDomains.map((item) => (
                <option key={item.key} value={item.key}>
                  {item.label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Hành động">
            <select
              className="input"
              value={action}
              onChange={(event) =>
                table.update({ action: event.target.value || undefined, page: 0 })
              }
            >
              <option value="">Tất cả hành động</option>
              {domainActions.map(([code, meta]) => (
                <option key={code} value={code}>
                  {meta.label}
                </option>
              ))}
              {action && !domainActions.some(([code]) => code === action) && (
                <option value={action}>{getAuditActionMeta(action).label}</option>
              )}
            </select>
          </Field>
          <Field label="Người thực hiện">
            <select
              className="input"
              value={actor}
              onChange={(event) =>
                table.update({ actorId: event.target.value || undefined, page: 0 })
              }
            >
              <option value="">Tất cả người dùng</option>
              {users.data?.content.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.fullName} (@{user.username} · {roleLabels[user.role]})
                </option>
              ))}
              {actor && !users.data?.content.some((user) => user.id === actor) && (
                <option value={actor}>Tài khoản #{actor}</option>
              )}
            </select>
          </Field>
          {(actor || action || domain || search) && (
            <Button
              variant="ghost"
              onClick={() =>
                table.update({
                  actorId: undefined,
                  action: undefined,
                  domain: undefined,
                  q: undefined,
                  page: 0,
                })
              }
            >
              Đặt lại
            </Button>
          )}
        </div>
        {domain && !action && (
          <p className="filter-help">
            Đang gợi ý {domainActions.length} hành động thuộc phân hệ này. Chọn một hành động để lọc
            nhật ký.
          </p>
        )}
        {!validActor ? (
          <p className="filter-help">
            Đường dẫn chứa mã người thực hiện không hợp lệ, hãy bấm Đặt lại bộ lọc.
          </p>
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
              <div className="audit-detail-head">
                <ActionBadge action={selected.action} />
                <Badge variant="outline">
                  {auditSeverityLabels[getAuditActionMeta(selected.action).severity]}
                </Badge>
              </div>
              <dl className="detail-list audit-detail-grid">
                <ReadOnlyField label="Hành động">
                  {getAuditActionMeta(selected.action).label}
                </ReadOnlyField>
                <ReadOnlyField label="Mã kỹ thuật">{selected.action}</ReadOnlyField>
                <ReadOnlyField label="Thời gian">
                  {dateTime(selected.createdAt, policy.garageTimeZone)}
                </ReadOnlyField>
                <ReadOnlyField label="Người thực hiện">{actorText(selected)}</ReadOnlyField>
                <ReadOnlyField label="Đối tượng">
                  {formatAuditEntity(selected.entityType, selected.entityId, selected.entityLabel)}
                </ReadOnlyField>
                <ReadOnlyField label="Mô tả">
                  {getAuditActionMeta(selected.action).description}
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

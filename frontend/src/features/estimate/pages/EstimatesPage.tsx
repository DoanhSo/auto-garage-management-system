import { useMemo, useState } from 'react';
import {
  Plus,
  Eye,
  RefreshCw,
  FileText,
  RotateCcw,
  SlidersHorizontal,
  Send,
  Clock,
  CheckCircle2,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { Badge, Button, PageHeader, Loading, QueryError } from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { useRepairOrdersQuery } from '../../repair/hooks/useRepair';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { useUiPolicy } from '../../../shared/config/policy';
import { money, formatDate, formatDateTime, formatPlate } from '../../../shared/utils/format';
import { useEstimatesQuery, useSendEstimateMutation } from '../hooks/useEstimate';
import { EstimateEditor } from '../components/EstimateEditor';
import { EstimateDecisionModal } from '../components/EstimateDecisionModal';
import type { Estimate, EstimateStatus } from '../types/estimate';
import { estimateStatusLabels } from '../types/estimate';

type BadgeVariant = 'primary' | 'secondary' | 'success' | 'warning' | 'danger';

const statusVariant: Record<string, BadgeVariant> = {
  APPROVED: 'success',
  PARTIALLY_APPROVED: 'primary',
  SENT: 'warning',
  REJECTED: 'danger',
};

function ReadyEstimates({
  policy,
}: {
  policy: NonNullable<ReturnType<typeof useUiPolicy>['data']>;
}) {
  const notify = useToast();
  const [status, setStatus] = useState<EstimateStatus | 'ALL'>('ALL');
  const [kind, setKind] = useState<'ALL' | 'INITIAL' | 'SUPPLEMENTAL'>('ALL');
  const [keyword, setKeyword] = useState('');
  const sendMutation = useSendEstimateMutation();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(policy.paginationDefaultSize);

  const [createOpen, setCreateOpen] = useState(false);
  const [selectedEstimate, setSelectedEstimate] = useState<Estimate | null>(null);

  const queryParams = useMemo(
    () => ({ status: status === 'ALL' ? undefined : status, page, size }),
    [status, page, size],
  );

  const { data, isLoading, isError, error, refetch, isFetching } = useEstimatesQuery(queryParams);
  const pageEstimates = useMemo(() => data?.content ?? [], [data?.content]);

  const { data: orders } = useRepairOrdersQuery({
    page: 0,
    size: 100,
    sortBy: 'id',
    sortDir: 'desc',
  });
  const { data: vehicles = [] } = useVehiclesQuery();
  const orderMap = useMemo(
    () => new Map((orders?.content ?? []).map((o) => [String(o.id), o] as const)),
    [orders?.content],
  );
  const vehicleMap = useMemo(
    () => new Map(vehicles.map((v) => [String(v.id), v] as const)),
    [vehicles],
  );

  const selectedOrder = selectedEstimate
    ? orderMap.get(String(selectedEstimate.repairOrderId))
    : undefined;
  const selectedOrderVehicle = selectedOrder
    ? vehicleMap.get(String(selectedOrder.vehicleId))
    : null;

  const estimates = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    return pageEstimates.filter((e) => {
      if (kind !== 'ALL' && e.estimateKind !== kind) return false;
      if (!term) return true;
      const order = orderMap.get(String(e.repairOrderId));
      const plate = order ? (vehicleMap.get(String(order.vehicleId))?.licensePlate ?? '') : '';
      return [e.estimateCode, order?.orderCode ?? '', plate].join(' ').toLowerCase().includes(term);
    });
  }, [pageEstimates, kind, keyword, orderMap, vehicleMap]);

  const draftCount = pageEstimates.filter((e) => e.status === 'DRAFT').length;
  const sentCount = pageEstimates.filter((e) => e.status === 'SENT').length;
  const approved = pageEstimates.filter(
    (e) => e.status === 'APPROVED' || e.status === 'PARTIALLY_APPROVED',
  );
  const approvedTotal = approved.reduce((sum, e) => sum + Number(e.totalAmount), 0);

  const sendQuick = async (id: string) => {
    try {
      await sendMutation.mutateAsync(id);
      notify('Đã gửi báo giá cho khách hàng');
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const columns = useMemo<ColumnDef<Estimate>[]>(
    () => [
      {
        accessorKey: 'estimateCode',
        header: 'Mã báo giá',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>#{row.original.estimateCode}</strong>
              <span>
                v{row.original.versionNo} ·{' '}
                {row.original.estimateKind === 'INITIAL'
                  ? 'Ban đầu'
                  : `Phát sinh${row.original.parentEstimateId ? ` (từ báo giá #${row.original.parentEstimateId})` : ''}`}
              </span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => (
          <Badge variant={statusVariant[row.original.status] ?? 'secondary'}>
            {estimateStatusLabels[row.original.status] ?? row.original.status}
          </Badge>
        ),
      },
      {
        accessorKey: 'repairOrderId',
        header: 'Phiếu sửa chữa',
        cell: ({ row }) => {
          const order = orderMap.get(String(row.original.repairOrderId));
          const v = order ? vehicleMap.get(String(order.vehicleId)) : undefined;
          return (
            <div className="account-cell">
              <div>
                <strong>#{order?.orderCode ?? row.original.repairOrderId}</strong>
                <span>
                  {v ? `${formatPlate(v.licensePlate)} · ${v.brand} ${v.model}` : ''}
                  {v?.customerName && ` · ${v.customerName}`}
                </span>
              </div>
            </div>
          );
        },
      },
      {
        id: 'structure',
        header: 'Cơ cấu hạng mục',
        cell: ({ row }) => {
          const items = row.original.items ?? [];
          const services = items.filter((i) => i.itemType === 'SERVICE').length;
          const decided = items.filter((i) => i.approvalStatus !== 'PENDING').length;
          return (
            <div className="account-cell">
              <div>
                <strong>
                  {services} dịch vụ · {items.length - services} phụ tùng
                </strong>
                <span>
                  {row.original.status === 'DRAFT'
                    ? 'Chưa gửi khách'
                    : `Đã quyết định ${decided}/${items.length} dòng`}
                </span>
              </div>
            </div>
          );
        },
      },
      {
        accessorKey: 'totalAmount',
        header: 'Tổng tiền dự toán',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>{money(row.original.totalAmount)}</strong>
              {Number(row.original.discountAmount) > 0 && (
                <span>
                  Tạm tính {money(row.original.subtotal)} − giảm{' '}
                  {money(row.original.discountAmount)}
                </span>
              )}
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'createdAt',
        header: 'Ngày lập',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>{formatDateTime(row.original.createdAt)}</strong>
              <span>
                {row.original.validUntil
                  ? `Hiệu lực đến ${formatDate(row.original.validUntil)}`
                  : 'Không giới hạn hiệu lực'}
              </span>
            </div>
          </div>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            {row.original.status === 'DRAFT' && (
              <Button variant="ghost" onClick={() => sendQuick(row.original.id)}>
                <Send size={14} /> Gửi khách
              </Button>
            )}
            <Button variant="ghost" onClick={() => setSelectedEstimate(row.original)}>
              <Eye size={14} /> Xem & duyệt dòng
            </Button>
          </div>
        ),
      },
    ],
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [orderMap, vehicleMap],
  );

  return (
    <>
      <PageHeader
        eyebrow="DỊCH VỤ & XƯỞNG"
        title="Báo giá dịch vụ & phụ tùng"
        description="Quản lý các bản dự toán chi phí sửa chữa, gửi khách hàng và phê duyệt từng hạng mục."
        actions={
          <>
            <Button variant="secondary" onClick={() => refetch()} pending={isFetching}>
              <RefreshCw size={15} /> Làm mới
            </Button>
            <Button variant="primary" onClick={() => setCreateOpen(true)}>
              <Plus size={18} /> Lập báo giá mới
            </Button>
          </>
        }
      />

      <MetricGrid
        label="Tổng quan báo giá"
        items={[
          { label: 'Tổng báo giá', value: data?.totalElements ?? 0, icon: <FileText size={18} /> },
          {
            label: 'Bản nháp / chờ gửi',
            value: draftCount,
            tone: 'warning',
            icon: <Clock size={18} />,
          },
          { label: 'Chờ khách duyệt', value: sentCount, tone: 'info', icon: <Send size={18} /> },
          {
            label: 'Đã duyệt',
            value: approved.length,
            hint: money(approvedTotal),
            tone: 'success',
            icon: <CheckCircle2 size={18} />,
          },
        ]}
      />

      <div className="info-strip">
        <FileText size={18} />
        <span>
          Quy trình: lập báo giá (nháp) → gửi khách → khách duyệt từng dòng → hệ thống tạo việc sửa
          chữa và giữ phụ tùng cho các dòng được duyệt.
        </span>
      </div>

      <section className="panel table-panel">
        <div className="user-filters" role="search" aria-label="Bộ lọc báo giá">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc báo giá</h2>
            </div>
            <Button
              variant="ghost"
              className="filter-reset"
              disabled={status === 'ALL' && kind === 'ALL' && !keyword}
              onClick={() => {
                setStatus('ALL');
                setKind('ALL');
                setKeyword('');
                setPage(0);
              }}
            >
              <RotateCcw size={14} />
              Đặt lại
            </Button>
          </div>
          <div className="user-filter-grid">
            <div className="user-filter-field">
              <label htmlFor="estimate-keyword">Tìm kiếm</label>
              <input
                id="estimate-keyword"
                className="input"
                placeholder="Mã báo giá, mã lệnh, biển số…"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </div>
            <div className="user-filter-field">
              <label htmlFor="estimate-kind-filter">Loại báo giá</label>
              <select
                id="estimate-kind-filter"
                className="select"
                value={kind}
                onChange={(e) => setKind(e.target.value as 'ALL' | 'INITIAL' | 'SUPPLEMENTAL')}
              >
                <option value="ALL">Tất cả loại</option>
                <option value="INITIAL">Ban đầu</option>
                <option value="SUPPLEMENTAL">Phát sinh</option>
              </select>
            </div>
            <div className="user-filter-field">
              <label htmlFor="estimate-status-filter">Trạng thái</label>
              <select
                id="estimate-status-filter"
                className="select"
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value as EstimateStatus | 'ALL');
                  setPage(0);
                }}
              >
                <option value="ALL">Tất cả trạng thái</option>
                {Object.entries(estimateStatusLabels).map(([val, label]) => (
                  <option key={val} value={val}>
                    {label}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>

        {isLoading ? (
          <Loading label="Đang tải danh sách báo giá…" />
        ) : isError ? (
          <QueryError error={error} retry={() => refetch()} />
        ) : (
          <>
            <DataTable<Estimate>
              caption="Danh sách báo giá"
              columns={columns}
              data={estimates}
              busy={isFetching}
            />
            <Pagination
              page={page}
              size={size}
              data={data}
              policy={policy}
              disabled={isFetching}
              onChange={(nextPage, nextSize) => {
                setPage(nextPage);
                setSize(nextSize);
              }}
            />
          </>
        )}
      </section>

      <EstimateEditor
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSaved={() => refetch()}
      />

      <EstimateDecisionModal
        estimate={selectedEstimate}
        repairOrder={selectedEstimate ? orderMap.get(String(selectedEstimate.repairOrderId)) : null}
        vehicle={selectedOrderVehicle}
        open={!!selectedEstimate}
        onClose={() => setSelectedEstimate(null)}
      />
    </>
  );
}

export function EstimatesPage() {
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
    <ReadyEstimates policy={policy.data} />
  );
}

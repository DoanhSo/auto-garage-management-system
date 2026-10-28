import { useCallback, useMemo, useState } from 'react';
import { Eye, Info, RefreshCw } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { Button, Loading, PageHeader, QueryError } from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { useUiPolicy } from '../../../shared/config/policy';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import { formatDateTime } from '../../../shared/utils/format';
import type { UiPolicy } from '../../../shared/types/contracts';
import type { RepairOrder, RepairOrderStatus } from '../types/repairTypes';
import { useRepairOrdersQuery } from '../hooks/useRepair';
import { RepairOrderCards, RepairOrderStatusBadge } from '../components/RepairOrderCards';
import { REPAIR_STATUS_LABELS, RepairOrderFilters } from '../components/RepairOrderFilters';
import { RepairOrderDetailModal } from '../components/RepairOrderDetailModal';
import { WorkshopBayGrid } from '../components/WorkshopBayGrid';

const sortFields = ['id', 'orderCode', 'receivedAt', 'status', 'createdAt'];
type ViewMode = 'list' | 'bays';

function ReadyRepairOrders({ policy }: { policy: UiPolicy }) {
  const compact = useMediaQuery('(max-width: 640px)');
  const table = useTableSearchParams(policy);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);

  const view: ViewMode = table.params.get('view') === 'bays' ? 'bays' : 'list';
  const keyword = table.params.get('keyword')?.slice(0, 254) ?? '';
  const debounced = useDebouncedValue(keyword);
  const rawStatus = table.params.get('status');
  const status =
    rawStatus && rawStatus in REPAIR_STATUS_LABELS ? (rawStatus as RepairOrderStatus) : undefined;
  const sortBy = sortFields.includes(table.params.get('sortBy') ?? '')
    ? table.params.get('sortBy')!
    : 'createdAt';
  const sortDir = table.params.get('sortDir') === 'asc' ? 'asc' : 'desc';

  const orders = useRepairOrdersQuery({
    keyword: debounced.trim() || undefined,
    status,
    page: table.page,
    size: table.size,
    sortBy,
    sortDir,
  });

  const openDetail = useCallback((order: RepairOrder) => setSelectedOrderId(order.id), []);

  const columns = useMemo<ColumnDef<RepairOrder>[]>(
    () => [
      {
        accessorKey: 'orderCode',
        header: 'Mã lệnh',
        cell: ({ row }) => <span className="code-cell">{row.original.orderCode}</span>,
      },
      {
        accessorKey: 'vehicleId',
        header: 'Phương tiện',
        cell: ({ row }) => <span>Xe #{row.original.vehicleId}</span>,
      },
      {
        accessorKey: 'customerComplaint',
        header: 'Triệu chứng',
        cell: ({ row }) => (
          <span className="email-cell">{row.original.customerComplaint || 'Chưa ghi nhận'}</span>
        ),
      },
      {
        accessorKey: 'odometerIn',
        header: 'Số km vào',
        cell: ({ row }) => (
          <span className="nowrap">{row.original.odometerIn.toLocaleString('vi-VN')} km</span>
        ),
      },
      {
        accessorKey: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => <RepairOrderStatusBadge status={row.original.status} />,
      },
      {
        accessorKey: 'receivedAt',
        header: 'Nhận xe lúc',
        cell: ({ row }) => (
          <span className="muted nowrap">{formatDateTime(row.original.receivedAt)}</span>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button
              variant="ghost"
              aria-label={`Xem lệnh sửa chữa ${row.original.orderCode}`}
              onClick={() => openDetail(row.original)}
            >
              <Eye size={16} />
              <span>Chi tiết</span>
            </Button>
          </div>
        ),
      },
    ],
    [openDetail],
  );

  const ready = keyword === debounced;
  return (
    <>
      <PageHeader
        eyebrow="ĐIỀU HÀNH XƯỞNG"
        title="Lệnh sửa chữa"
        description="Theo dõi tiến độ sửa chữa, phân bổ khoang làm việc và bàn giao xe cho khách."
        actions={
          <Button
            variant="secondary"
            disabled={orders.isFetching}
            onClick={() => {
              void orders.refetch();
            }}
          >
            <RefreshCw size={16} />
            Làm mới
          </Button>
        }
      />
      <div className="info-strip">
        <Info size={18} />
        <span>
          Lệnh sửa chữa được tạo khi tiếp nhận xe. Mở chi tiết để chẩn đoán, lập báo giá, phân công
          và bàn giao.
        </span>
      </div>
      <section className="panel table-panel">
        <div className="tabs" role="group" aria-label="Chế độ hiển thị">
          <button
            type="button"
            aria-pressed={view === 'list'}
            onClick={() => table.update({ view: undefined })}
          >
            Danh sách lệnh
          </button>
          <button
            type="button"
            aria-pressed={view === 'bays'}
            onClick={() => table.update({ view: 'bays' })}
          >
            Sơ đồ khoang xưởng
          </button>
        </div>
        <RepairOrderFilters
          keyword={keyword}
          status={status}
          sortBy={sortBy}
          sortDir={sortDir}
          onChange={table.update}
        />
        {orders.isPending || !ready ? (
          <Loading />
        ) : orders.error ? (
          <QueryError
            error={orders.error}
            retry={() => {
              void orders.refetch();
            }}
          />
        ) : view === 'bays' ? (
          <WorkshopBayGrid orders={orders.data.content} onOpenDetail={openDetail} />
        ) : (
          <>
            {compact ? (
              <RepairOrderCards orders={orders.data.content} onOpenDetail={openDetail} />
            ) : (
              <DataTable
                caption="Danh sách lệnh sửa chữa"
                columns={columns}
                data={orders.data.content}
                busy={orders.isFetching}
                hideOnMobile={['customerComplaint', 'receivedAt']}
              />
            )}
            <Pagination
              page={table.page}
              size={table.size}
              data={orders.data}
              policy={policy}
              disabled={orders.isFetching}
              onChange={(page, size) => table.update({ page, size })}
            />
          </>
        )}
      </section>
      <RepairOrderDetailModal
        orderId={selectedOrderId}
        open={!!selectedOrderId}
        onClose={() => setSelectedOrderId(null)}
      />
    </>
  );
}

export function RepairOrdersPage() {
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
    <ReadyRepairOrders policy={policy.data} />
  );
}

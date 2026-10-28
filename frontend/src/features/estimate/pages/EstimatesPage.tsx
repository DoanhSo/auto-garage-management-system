import { useMemo, useState } from 'react';
import { Plus, Eye, RefreshCw } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { Button, PageHeader, Loading, QueryError } from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { money, formatDateTime } from '../../../shared/utils/format';
import { useEstimatesQuery } from '../hooks/useEstimate';
import { EstimateEditor } from '../components/EstimateEditor';
import { EstimateDecisionModal } from '../components/EstimateDecisionModal';
import type { Estimate, EstimateStatus } from '../types/estimate';
import { estimateStatusLabels } from '../types/estimate';

export function EstimatesPage() {
  const [status, setStatus] = useState<EstimateStatus | 'ALL'>('ALL');
  const [page, setPage] = useState(0);
  const [size] = useState(20);

  const [createOpen, setCreateOpen] = useState(false);
  const [selectedEstimate, setSelectedEstimate] = useState<Estimate | null>(null);

  const queryParams = useMemo(() => ({
    status: status === 'ALL' ? undefined : status,
    page,
    size,
  }), [status, page, size]);

  const { data, isLoading, isError, error, refetch, isFetching } = useEstimatesQuery(queryParams);

  const estimates = useMemo(() => data?.content ?? [], [data?.content]);

  const columns = useMemo<ColumnDef<Estimate>[]>(() => [
    {
      accessorKey: 'estimateCode',
      header: 'Mã báo giá',
      cell: ({ row }) => (
        <div>
          <div className="font-semibold text-primary">#{row.original.estimateCode}</div>
          <div className="text-xs text-muted-foreground">Bản v{row.original.versionNo} ({row.original.estimateKind})</div>
        </div>
      ),
    },
    {
      accessorKey: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => {
        const s = row.original.status;
        const colorClass =
          s === 'APPROVED'
            ? 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300'
            : s === 'PARTIALLY_APPROVED'
            ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-300'
            : s === 'SENT'
            ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
            : s === 'REJECTED'
            ? 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300'
            : 'bg-muted text-muted-foreground';
        return (
          <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${colorClass}`}>
            {estimateStatusLabels[s] || s}
          </span>
        );
      },
    },
    {
      accessorKey: 'repairOrderId',
      header: 'Phiếu sửa chữa',
      cell: ({ row }) => <span>#{row.original.repairOrderId}</span>,
    },
    {
      accessorKey: 'totalAmount',
      header: 'Tổng tiền dự toán',
      cell: ({ row }) => <span className="font-semibold">{money(row.original.totalAmount)}</span>,
    },
    {
      accessorKey: 'createdAt',
      header: 'Ngày lập',
      cell: ({ row }) => <span className="text-xs">{formatDateTime(row.original.createdAt)}</span>,
    },
    {
      id: 'actions',
      header: 'Thao tác',
      cell: ({ row }) => (
        <Button
          variant="secondary"
          onClick={() => setSelectedEstimate(row.original)}
        >
          <Eye size={14} /> Xem & Duyệt dòng
        </Button>
      ),
    },
  ], []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Báo giá dịch vụ & phụ tùng"
        description="Quản lý các bản dự toán chi phí sửa chữa, gửi khách hàng và phê duyệt từng hạng mục"
        actions={
          <div className="flex items-center gap-2">
            <Button variant="secondary" onClick={() => refetch()} pending={isFetching}>
              <RefreshCw size={15} /> Làm mới
            </Button>
            <Button variant="primary" onClick={() => setCreateOpen(true)}>
              <Plus size={16} /> Lập Báo giá mới
            </Button>
          </div>
        }
      />

      {/* Bộ lọc */}
      <div className="p-4 bg-card rounded-lg border flex items-center justify-between gap-4">
        <div className="w-[240px]">
          <select
            className="select"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value as EstimateStatus | 'ALL');
              setPage(0);
            }}
          >
            <option value="ALL">-- Tất cả trạng thái --</option>
            {Object.entries(estimateStatusLabels).map(([val, label]) => (
              <option key={val} value={val}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {isLoading ? (
        <Loading label="Đang tải danh sách báo giá…" />
      ) : isError ? (
        <QueryError error={error} retry={() => refetch()} />
      ) : (
        <DataTable<Estimate>
          caption="Danh sách báo giá"
          columns={columns}
          data={estimates}
          busy={isFetching}
        />
      )}

      {/* Modal lập báo giá */}
      <EstimateEditor
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        onSaved={() => refetch()}
      />

      {/* Modal duyệt từng dòng */}
      <EstimateDecisionModal
        estimate={selectedEstimate}
        open={!!selectedEstimate}
        onClose={() => setSelectedEstimate(null)}
      />
    </div>
  );
}

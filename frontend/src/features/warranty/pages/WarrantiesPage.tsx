import { useMemo, useState } from 'react';
import { ShieldCheck, RefreshCw, AlertCircle, FileText } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { Button, PageHeader, Loading, QueryError } from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { formatDateTime } from '../../../shared/utils/format';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import {
  useVehicleWarrantiesQuery,
  useWarrantyClaimsQuery,
} from '../hooks/useWarranty';
import { OpenClaimModal } from '../components/OpenClaimModal';
import { DecideClaimModal } from '../components/DecideClaimModal';
import type { Warranty, WarrantyClaim } from '../types/warranty';
import { warrantyStatusLabels, claimStatusLabels } from '../types/warranty';

export function WarrantiesPage() {
  const [selectedVehicleId, setSelectedVehicleId] = useState<string>('');
  const [activeTab, setActiveTab] = useState<'warranties' | 'claims'>('warranties');

  const [openClaimWarranty, setOpenClaimWarranty] = useState<Warranty | null>(null);
  const [decidingClaim, setDecidingClaim] = useState<WarrantyClaim | null>(null);

  const { data: vehicles = [] } = useVehiclesQuery({ isActive: true });

  // Auto select first vehicle if not selected
  const activeVehicleId = selectedVehicleId || (vehicles[0]?.id ? String(vehicles[0].id) : '');

  const {
    data: warrantiesData,
    isLoading: isLoadingWarranties,
    isError: isErrorWarranties,
    error: errorWarranties,
    refetch: refetchWarranties,
    isFetching: isFetchingWarranties,
  } = useVehicleWarrantiesQuery(activeVehicleId);

  const warranties = useMemo(() => warrantiesData?.content ?? [], [warrantiesData?.content]);

  // Selected warranty for claims
  const firstWarrantyId = warranties[0]?.id;
  const {
    data: claimsData,
    isLoading: isLoadingClaims,
    refetch: refetchClaims,
    isFetching: isFetchingClaims,
  } = useWarrantyClaimsQuery(firstWarrantyId);

  const claims = useMemo(() => claimsData?.content ?? [], [claimsData?.content]);

  // Warranty Columns
  const warrantyColumns = useMemo<ColumnDef<Warranty>[]>(() => [
    {
      accessorKey: 'warrantyCode',
      header: 'Mã bảo hành',
      cell: ({ row }) => (
        <span className="font-semibold text-primary">#{row.original.warrantyCode}</span>
      ),
    },
    {
      accessorKey: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => (
        <span
          className={`text-xs px-2.5 py-1 rounded-full font-medium ${
            row.original.status === 'ACTIVE'
              ? 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300'
              : 'bg-muted text-muted-foreground'
          }`}
        >
          {warrantyStatusLabels[row.original.status] || row.original.status}
        </span>
      ),
    },
    {
      accessorKey: 'warrantyDays',
      header: 'Thời hạn (Ngày)',
      cell: ({ row }) => <span>{row.original.warrantyDays} ngày</span>,
    },
    {
      accessorKey: 'endOdometer',
      header: 'Hạn ODO bảo hành',
      cell: ({ row }) => <span>{row.original.endOdometer.toLocaleString('vi-VN')} km</span>,
    },
    {
      accessorKey: 'expiresAt',
      header: 'Ngày hết hạn',
      cell: ({ row }) => <span className="text-xs">{formatDateTime(row.original.expiresAt)}</span>,
    },
    {
      id: 'actions',
      header: 'Thao tác',
      cell: ({ row }) => (
        row.original.status === 'ACTIVE' && (
          <Button
            variant="secondary"
            onClick={() => setOpenClaimWarranty(row.original)}
          >
            <AlertCircle size={13} /> Mở khiếu nại bảo hành
          </Button>
        )
      ),
    },
  ], []);

  // Claim Columns
  const claimColumns = useMemo<ColumnDef<WarrantyClaim>[]>(() => [
    {
      accessorKey: 'claimCode',
      header: 'Mã khiếu nại',
      cell: ({ row }) => (
        <span className="font-semibold text-primary">#{row.original.claimCode}</span>
      ),
    },
    {
      accessorKey: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => {
        const s = row.original.status;
        const color =
          s === 'APPROVED'
            ? 'bg-green-100 text-green-800'
            : s === 'REJECTED'
            ? 'bg-red-100 text-red-800'
            : 'bg-amber-100 text-amber-800';
        return (
          <span className={`text-xs px-2.5 py-1 rounded-full font-medium ${color}`}>
            {claimStatusLabels[s] || s}
          </span>
        );
      },
    },
    {
      accessorKey: 'currentOdometer',
      header: 'ODO khiếu nại',
      cell: ({ row }) => <span>{row.original.currentOdometer.toLocaleString('vi-VN')} km</span>,
    },
    {
      accessorKey: 'description',
      header: 'Mô tả hư hỏng',
      cell: ({ row }) => (
        <span className="text-xs max-w-[200px] truncate block" title={row.original.description}>
          {row.original.description}
        </span>
      ),
    },
    {
      accessorKey: 'requestedAt',
      header: 'Ngày mở',
      cell: ({ row }) => <span className="text-xs">{formatDateTime(row.original.requestedAt)}</span>,
    },
    {
      id: 'actions',
      header: 'Thao tác',
      cell: ({ row }) => (
        row.original.status === 'OPEN' && (
          <Button
            variant="primary"
            onClick={() => setDecidingClaim(row.original)}
          >
            Duyệt khiếu nại
          </Button>
        )
      ),
    },
  ], []);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Sổ bảo hành & Khiếu nại dịch vụ"
        description="Tra cứu sổ bảo hành điện tử theo phương tiện, tiếp nhận và thẩm định hồ sơ khiếu nại bảo hành"
        actions={
          <Button
            variant="secondary"
            onClick={() => {
              refetchWarranties();
              refetchClaims();
            }}
            pending={isFetchingWarranties}
          >
            <RefreshCw size={15} /> Làm mới
          </Button>
        }
      />

      {/* Bộ lọc chọn phương tiện */}
      <div className="p-4 bg-card rounded-lg border flex flex-wrap items-center gap-4">
        <label className="text-sm font-semibold text-muted-foreground">Chọn phương tiện tra cứu:</label>
        <select
          className="select w-[300px]"
          value={activeVehicleId}
          onChange={(e) => setSelectedVehicleId(e.target.value)}
        >
          {vehicles.map((v) => (
            <option key={v.id} value={v.id}>
              {v.licensePlate} ({v.brand} {v.model})
            </option>
          ))}
        </select>
      </div>

      {/* Tabs */}
      <div className="flex border-b gap-6">
        <button
          type="button"
          className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'warranties'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground'
          }`}
          onClick={() => setActiveTab('warranties')}
        >
          <ShieldCheck size={16} /> Chính sách bảo hành áp dụng ({warranties.length})
        </button>
        <button
          type="button"
          className={`pb-3 text-sm font-semibold border-b-2 flex items-center gap-2 transition-colors ${
            activeTab === 'claims'
              ? 'border-primary text-primary'
              : 'border-transparent text-muted-foreground'
          }`}
          onClick={() => setActiveTab('claims')}
        >
          <FileText size={16} /> Hồ sơ khiếu nại bảo hành ({claims.length})
        </button>
      </div>

      {activeTab === 'warranties' && (
        isLoadingWarranties ? (
          <Loading label="Đang tra cứu sổ bảo hành…" />
        ) : isErrorWarranties ? (
          <QueryError error={errorWarranties} retry={() => refetchWarranties()} />
        ) : warranties.length === 0 ? (
          <div className="text-center py-10 text-muted-foreground text-sm border rounded-lg">
            Phương tiện này hiện chưa có chính sách bảo hành phát sinh từ hóa đơn dịch vụ.
          </div>
        ) : (
          <DataTable<Warranty>
            caption="Danh sách sổ bảo hành"
            columns={warrantyColumns}
            data={warranties}
            busy={isFetchingWarranties}
          />
        )
      )}

      {activeTab === 'claims' && (
        isLoadingClaims ? (
          <Loading label="Đang tải danh sách khiếu nại…" />
        ) : claims.length === 0 ? (
          <div className="text-center py-10 text-muted-foreground text-sm border rounded-lg">
            Chưa có hồ sơ khiếu nại bảo hành nào cho chính sách này.
          </div>
        ) : (
          <DataTable<WarrantyClaim>
            caption="Danh sách khiếu nại bảo hành"
            columns={claimColumns}
            data={claims}
            busy={isFetchingClaims}
          />
        )
      )}

      {/* Modal mở claim */}
      <OpenClaimModal
        warranty={openClaimWarranty}
        open={!!openClaimWarranty}
        onClose={() => setOpenClaimWarranty(null)}
        onSaved={() => {
          refetchWarranties();
          refetchClaims();
        }}
      />

      {/* Modal duyệt claim */}
      <DecideClaimModal
        claim={decidingClaim}
        open={!!decidingClaim}
        onClose={() => setDecidingClaim(null)}
        onSaved={() => {
          refetchWarranties();
          refetchClaims();
        }}
      />
    </div>
  );
}

import { useMemo, useState } from 'react';
import { useQueries } from '@tanstack/react-query';
import { ShieldCheck, RefreshCw, AlertCircle, FileText, Clock, ShieldAlert } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { Badge, Button, PageHeader, Loading, QueryError } from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import { formatDate, formatDateTime } from '../../../shared/utils/format';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useVehiclesQuery } from '../../vehicle/hooks/useVehicles';
import { useCustomersQuery } from '../../customer/hooks/useCustomers';
import { warrantyApi } from '../api/warrantyApi';
import { useVehicleWarrantiesQuery, useCustomerWarrantiesQuery } from '../hooks/useWarranty';
import { OpenClaimModal } from '../components/OpenClaimModal';
import { DecideClaimModal } from '../components/DecideClaimModal';
import type { Warranty, WarrantyClaim, ClaimStatus } from '../types/warranty';
import { warrantyStatusLabels, claimStatusLabels } from '../types/warranty';

type LookupMode = 'vehicle' | 'customer';
const ALL_BOOKS = 'ALL';

const claimVariant = (status: ClaimStatus) =>
  status === 'APPROVED' || status === 'CLOSED'
    ? 'success'
    : status === 'REJECTED'
      ? 'danger'
      : 'warning';

const daysLeft = (expiresAt: string) =>
  Math.ceil((new Date(expiresAt).getTime() - Date.now()) / 86_400_000);

export function WarrantiesPage() {
  const { user } = useAuth();
  // Chỉ lễ tân/admin xem được danh mục khách hàng và mở khiếu nại; thẩm định dành riêng admin.
  const canLookupCustomers = user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST';
  const canOpenClaim = canLookupCustomers;
  const canDecideClaim = user?.role === 'ADMIN';
  const [mode, setMode] = useState<LookupMode>('vehicle');
  const [selectedVehicleId, setSelectedVehicleId] = useState('');
  const [selectedCustomerId, setSelectedCustomerId] = useState('');
  const [selectedWarrantyId, setSelectedWarrantyId] = useState(ALL_BOOKS);
  const [keyword, setKeyword] = useState('');
  const [activeTab, setActiveTab] = useState<'warranties' | 'claims'>('warranties');

  const [openClaimWarranty, setOpenClaimWarranty] = useState<Warranty | null>(null);
  const [decidingClaim, setDecidingClaim] = useState<WarrantyClaim | null>(null);

  const { data: vehicles = [] } = useVehiclesQuery({ isActive: true });
  const { data: customers = [] } = useCustomersQuery({ isActive: true }, canLookupCustomers);

  const activeVehicleId = selectedVehicleId || (vehicles[0]?.id ? String(vehicles[0].id) : '');
  const activeCustomerId = selectedCustomerId || (customers[0]?.id ? String(customers[0].id) : '');

  const vehicleQuery = useVehicleWarrantiesQuery(mode === 'vehicle' ? activeVehicleId : '');
  const customerQuery = useCustomerWarrantiesQuery(mode === 'customer' ? activeCustomerId : '');
  const warrantiesQuery = mode === 'vehicle' ? vehicleQuery : customerQuery;

  const vehicleMap = useMemo(() => new Map(vehicles.map((v) => [String(v.id), v])), [vehicles]);
  const customerMap = useMemo(() => new Map(customers.map((c) => [String(c.id), c] as const)), [customers]);

  const allWarranties = useMemo(
    () => warrantiesQuery.data?.content ?? [],
    [warrantiesQuery.data?.content],
  );

  const warranties = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    if (!term) return allWarranties;
    return allWarranties.filter((w) => {
      const plate = vehicleMap.get(String(w.vehicleId))?.licensePlate ?? '';
      return [w.warrantyCode, plate, w.termsSnapshot ?? '']
        .join(' ')
        .toLowerCase()
        .includes(term);
    });
  }, [allWarranties, keyword, vehicleMap]);

  const warrantyById = useMemo(() => new Map(allWarranties.map((w) => [String(w.id), w])), [allWarranties]);

  // Khiếu nại của một sổ cụ thể hoặc gộp tất cả sổ đang tra cứu.
  const claimWarrantyIds = useMemo(
    () =>
      selectedWarrantyId === ALL_BOOKS || !warrantyById.has(selectedWarrantyId)
        ? allWarranties.map((w) => String(w.id))
        : [selectedWarrantyId],
    [selectedWarrantyId, warrantyById, allWarranties],
  );

  const claimQueries = useQueries({
    queries: claimWarrantyIds.map((id) => ({
      queryKey: ['warranty-claims', id],
      queryFn: () => warrantyApi.getClaimsByWarranty(id),
    })),
  });
  const claimsLoading = claimQueries.some((q) => q.isLoading);
  const claimsError = claimQueries.find((q) => q.isError);
  const claimsFetching = claimQueries.some((q) => q.isFetching);
  const claims = useMemo(
    () =>
      claimQueries
        .flatMap((q) => q.data?.content ?? [])
        .sort((a, b) => b.requestedAt.localeCompare(a.requestedAt)),
    [claimQueries],
  );

  const activeCount = allWarranties.filter((w) => w.status === 'ACTIVE').length;
  const expiredCount = allWarranties.filter((w) => w.status === 'EXPIRED').length;
  const openClaimCount = claims.filter(
    (c) => c.status === 'OPEN' || c.status === 'IN_PROGRESS' || c.status === 'IN_REVIEW',
  ).length;

  const warrantyColumns = useMemo<ColumnDef<Warranty>[]>(
    () => [
      {
        accessorKey: 'warrantyCode',
        header: 'Mã sổ bảo hành',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>#{row.original.warrantyCode}</strong>
              <span>Cấp {formatDate(row.original.createdAt)}</span>
            </div>
          </div>
        ),
      },
      {
        id: 'vehicle',
        header: 'Phương tiện & chủ xe',
        cell: ({ row }) => {
          const v = vehicleMap.get(String(row.original.vehicleId));
          const c = customerMap.get(String(row.original.customerId));
          return (
            <div className="account-cell">
              <div>
                <strong>{v?.licensePlate ?? `Xe #${row.original.vehicleId}`}</strong>
                <span>
                  {v ? `${v.brand} ${v.model}` : ''}
                  {(v?.customerName ?? c?.fullName) && ` · ${v?.customerName ?? c?.fullName}`}
                </span>
              </div>
            </div>
          );
        },
      },
      {
        id: 'time',
        header: 'Hiệu lực thời gian',
        cell: ({ row }) => {
          const left = daysLeft(row.original.expiresAt);
          return (
            <div className="account-cell">
              <div>
                <strong>
                  {formatDate(row.original.startsAt)} → {formatDate(row.original.expiresAt)}
                </strong>
                <span>
                  {row.original.warrantyDays} ngày ·{' '}
                  {row.original.status !== 'ACTIVE'
                    ? warrantyStatusLabels[row.original.status]
                    : left > 0
                      ? `còn ${left} ngày`
                      : 'đã quá hạn'}
                </span>
              </div>
            </div>
          );
        },
      },
      {
        id: 'odo',
        header: 'Định mức ODO',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>
                {row.original.startOdometer.toLocaleString('vi-VN')} →{' '}
                {row.original.endOdometer.toLocaleString('vi-VN')} km
              </strong>
              <span>+{row.original.warrantyKm.toLocaleString('vi-VN')} km</span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'termsSnapshot',
        header: 'Điều khoản',
        cell: ({ row }) => (
          <span className="muted clamp-2">
            {row.original.termsSnapshot || 'Theo chính sách bảo hành chuẩn của xưởng'}
          </span>
        ),
      },
      {
        accessorKey: 'status',
        header: 'Trạng thái',
        cell: ({ row }) => (
          <Badge variant={row.original.status === 'ACTIVE' ? 'success' : 'secondary'}>
            {warrantyStatusLabels[row.original.status] || row.original.status}
          </Badge>
        ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button
              variant="ghost"
              onClick={() => {
                setSelectedWarrantyId(String(row.original.id));
                setActiveTab('claims');
              }}
            >
              <FileText size={14} /> Xem khiếu nại
            </Button>
            {canOpenClaim && row.original.status === 'ACTIVE' && (
              <Button variant="ghost" onClick={() => setOpenClaimWarranty(row.original)}>
                <AlertCircle size={14} /> Mở khiếu nại
              </Button>
            )}
          </div>
        ),
      },
    ],
    [vehicleMap, customerMap, canOpenClaim],
  );

  const claimColumns = useMemo<ColumnDef<WarrantyClaim>[]>(
    () => [
      {
        accessorKey: 'claimCode',
        header: 'Mã khiếu nại',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>#{row.original.claimCode}</strong>
              <span>
                Sổ #{warrantyById.get(String(row.original.warrantyId))?.warrantyCode ?? row.original.warrantyId}
              </span>
            </div>
          </div>
        ),
      },
      {
        id: 'opened',
        header: 'Ngày mở & ODO',
        cell: ({ row }) => {
          const w = warrantyById.get(String(row.original.warrantyId));
          return (
            <div className="account-cell">
              <div>
                <strong>{formatDateTime(row.original.requestedAt)}</strong>
                <span>
                  {row.original.currentOdometer.toLocaleString('vi-VN')} km
                  {w && ` / hạn ${w.endOdometer.toLocaleString('vi-VN')} km`}
                </span>
              </div>
            </div>
          );
        },
      },
      {
        accessorKey: 'description',
        header: 'Hiện tượng hư hỏng',
        cell: ({ row }) => <span className="muted clamp-2">{row.original.description}</span>,
      },
      {
        accessorKey: 'status',
        header: 'Trạng thái xử lý',
        cell: ({ row }) => (
          <Badge variant={claimVariant(row.original.status)}>
            {claimStatusLabels[row.original.status] || row.original.status}
          </Badge>
        ),
      },
      {
        id: 'decision',
        header: 'Kết quả thẩm định',
        cell: ({ row }) =>
          row.original.decisionReason ? (
            <div className="account-cell">
              <div>
                <strong className="clamp-2">{row.original.decisionReason}</strong>
                <span>{row.original.decidedAt ? formatDateTime(row.original.decidedAt) : ''}</span>
              </div>
            </div>
          ) : (
            <span className="muted">Chưa thẩm định</span>
          ),
      },
      {
        accessorKey: 'handlingOrderId',
        header: 'Lệnh sửa chữa BH',
        cell: ({ row }) =>
          row.original.handlingOrderId ? (
            <strong>#{row.original.handlingOrderId}</strong>
          ) : (
            <span className="muted">—</span>
          ),
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) =>
          canDecideClaim &&
          row.original.status === 'OPEN' && (
            <div className="row-actions">
              <Button variant="primary" onClick={() => setDecidingClaim(row.original)}>
                Thẩm định & duyệt
              </Button>
            </div>
          ),
      },
    ],
    [warrantyById, canDecideClaim],
  );

  const refreshAll = () => {
    void warrantiesQuery.refetch();
    claimQueries.forEach((q) => void q.refetch());
  };

  return (
    <>
      <PageHeader
        eyebrow="DỊCH VỤ & XƯỞNG"
        title="Sổ bảo hành & khiếu nại dịch vụ"
        description="Tra cứu sổ bảo hành điện tử theo phương tiện hoặc khách hàng, tiếp nhận và thẩm định hồ sơ khiếu nại."
        actions={
          <Button variant="secondary" onClick={refreshAll} pending={warrantiesQuery.isFetching}>
            <RefreshCw size={15} /> Làm mới
          </Button>
        }
      />

      <MetricGrid
        label="Tổng quan bảo hành"
        items={[
          {
            label: 'Sổ đang tra cứu',
            value: allWarranties.length,
            icon: <ShieldCheck size={18} />,
          },
          { label: 'Còn hiệu lực', value: activeCount, tone: 'success', icon: <ShieldCheck size={18} /> },
          { label: 'Đã hết hạn', value: expiredCount, tone: 'danger', icon: <Clock size={18} /> },
          {
            label: 'Khiếu nại đang xử lý',
            value: openClaimCount,
            tone: 'warning',
            icon: <ShieldAlert size={18} />,
          },
        ]}
      />

      <section className="panel table-panel">
        <div className="user-filters" role="search" aria-label="Tra cứu bảo hành">
          <div className="user-filter-grid">
            {canLookupCustomers && (
              <div className="user-filter-field">
                <label htmlFor="warranty-mode">Tra cứu theo</label>
                <select
                  id="warranty-mode"
                  className="select"
                  value={mode}
                  onChange={(e) => {
                    setMode(e.target.value as LookupMode);
                    setSelectedWarrantyId(ALL_BOOKS);
                  }}
                >
                  <option value="vehicle">Phương tiện</option>
                  <option value="customer">Khách hàng</option>
                </select>
              </div>
            )}
            {mode === 'vehicle' ? (
              <div className="user-filter-field">
                <label htmlFor="warranty-vehicle">Phương tiện</label>
                <select
                  id="warranty-vehicle"
                  className="select"
                  value={activeVehicleId}
                  onChange={(e) => {
                    setSelectedVehicleId(e.target.value);
                    setSelectedWarrantyId(ALL_BOOKS);
                  }}
                >
                  {vehicles.map((v) => (
                    <option key={v.id} value={v.id}>
                      {v.licensePlate} ({v.brand} {v.model})
                    </option>
                  ))}
                </select>
              </div>
            ) : (
              <div className="user-filter-field">
                <label htmlFor="warranty-customer">Khách hàng</label>
                <select
                  id="warranty-customer"
                  className="select"
                  value={activeCustomerId}
                  onChange={(e) => {
                    setSelectedCustomerId(e.target.value);
                    setSelectedWarrantyId(ALL_BOOKS);
                  }}
                >
                  {customers.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.fullName} · {c.phone}
                    </option>
                  ))}
                </select>
              </div>
            )}
            <div className="user-filter-field">
              <label htmlFor="warranty-book">Xem khiếu nại của</label>
              <select
                id="warranty-book"
                className="select"
                value={selectedWarrantyId}
                disabled={allWarranties.length === 0}
                onChange={(e) => setSelectedWarrantyId(e.target.value)}
              >
                <option value={ALL_BOOKS}>Tất cả sổ bảo hành đang tra cứu</option>
                {allWarranties.map((w) => (
                  <option key={w.id} value={w.id}>
                    #{w.warrantyCode} · {warrantyStatusLabels[w.status] ?? w.status}
                  </option>
                ))}
              </select>
            </div>
            <div className="user-filter-field">
              <label htmlFor="warranty-keyword">Tìm nhanh</label>
              <input
                id="warranty-keyword"
                className="input"
                placeholder="Mã bảo hành, biển số, điều khoản…"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="tabs" role="group" aria-label="Chọn nội dung hiển thị">
          <button
            type="button"
            aria-pressed={activeTab === 'warranties'}
            onClick={() => setActiveTab('warranties')}
          >
            <ShieldCheck size={15} aria-hidden /> Sổ bảo hành ({warranties.length})
          </button>
          <button
            type="button"
            aria-pressed={activeTab === 'claims'}
            onClick={() => setActiveTab('claims')}
          >
            <FileText size={15} aria-hidden /> Hồ sơ khiếu nại ({claims.length})
          </button>
        </div>

        {activeTab === 'warranties' ? (
          warrantiesQuery.isLoading ? (
            <Loading label="Đang tra cứu sổ bảo hành…" />
          ) : warrantiesQuery.isError ? (
            <QueryError error={warrantiesQuery.error} retry={() => warrantiesQuery.refetch()} />
          ) : warranties.length === 0 ? (
            <p className="empty-state">
              Không có sổ bảo hành phù hợp. Sổ được cấp tự động từ hóa đơn dịch vụ.
            </p>
          ) : (
            <DataTable<Warranty>
              caption="Danh sách sổ bảo hành"
              columns={warrantyColumns}
              data={warranties}
              busy={warrantiesQuery.isFetching}
            />
          )
        ) : allWarranties.length === 0 ? (
          <p className="empty-state">Chưa có sổ bảo hành nào để xem khiếu nại.</p>
        ) : claimsLoading ? (
          <Loading label="Đang tải danh sách khiếu nại…" />
        ) : claimsError ? (
          <QueryError error={claimsError.error} retry={refreshAll} />
        ) : claims.length === 0 ? (
          <p className="empty-state">Chưa có hồ sơ khiếu nại nào.</p>
        ) : (
          <DataTable<WarrantyClaim>
            caption="Danh sách khiếu nại bảo hành"
            columns={claimColumns}
            data={claims}
            busy={claimsFetching}
          />
        )}
      </section>

      <OpenClaimModal
        warranty={openClaimWarranty}
        open={!!openClaimWarranty}
        onClose={() => setOpenClaimWarranty(null)}
        onSaved={refreshAll}
      />

      <DecideClaimModal
        claim={decidingClaim}
        open={!!decidingClaim}
        onClose={() => setDecidingClaim(null)}
        onSaved={refreshAll}
      />
    </>
  );
}

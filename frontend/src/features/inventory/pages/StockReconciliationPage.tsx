import { useMemo, useState } from 'react';
import {
  AlertTriangle,
  CheckCircle2,
  Package,
  RefreshCw,
  RotateCcw,
  Scale,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import {
  Badge,
  Button,
  Loading,
  PageHeader,
} from '../../../shared/components/ui';
import { DataTable } from '../../../shared/components/DataTable';
import { useMediaQuery } from '../../../shared/hooks/useMediaQuery';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { useReconciliationQuery } from '../hooks/useInventory';
import type { StockReconciliation } from '../types/inventory';

type StockReconciliationRow = StockReconciliation & { id: string };

export function StockReconciliationPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [filterDiscrepancyOnly, setFilterDiscrepancyOnly] = useState(false);

  const { data, isLoading, refetch, isFetching } = useReconciliationQuery();

  const reconciliations = useMemo(() => data ?? [], [data]);
  const matchedCount = reconciliations.filter((r) => r.matched).length;
  const discrepancyCount = reconciliations.filter((r) => !r.matched).length;

  const filteredList = useMemo(() => {
    return reconciliations.filter((item) => {
      if (filterDiscrepancyOnly && item.matched) return false;
      if (debouncedQuery.trim()) {
        const q = debouncedQuery.toLowerCase().trim();
        return (
          item.sku.toLowerCase().includes(q) || item.partName.toLowerCase().includes(q)
        );
      }
      return true;
    });
  }, [reconciliations, filterDiscrepancyOnly, debouncedQuery]);

  const filteredListWithId: StockReconciliationRow[] = useMemo(() => {
    return filteredList.map((item) => ({ ...item, id: item.partId }));
  }, [filteredList]);

  const filterCount = (query.trim() ? 1 : 0) + (filterDiscrepancyOnly ? 1 : 0);
  const isFiltered = query.length > 0 || filterDiscrepancyOnly;

  const handleResetFilters = () => {
    setQuery('');
    setFilterDiscrepancyOnly(false);
  };

  const columns = useMemo<ColumnDef<StockReconciliationRow>[]>(
    () => [
      {
        id: 'part',
        header: 'Phụ tùng',
        cell: ({ row }) => (
          <div className="account-cell">
            <span
              className="avatar"
              style={{
                background: row.original.matched ? '#edf3ff' : '#fff1f2',
                color: row.original.matched ? '#245bdb' : '#b42335',
              }}
            >
              <Package size={16} />
            </span>
            <div>
              <strong style={{ color: '#172338' }}>{row.original.partName}</strong>
              <span className="muted font-mono" style={{ fontSize: '12px' }}>
                {row.original.sku}
              </span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'stockQuantity',
        header: 'Tồn kho hiện tại',
        cell: ({ row }) => (
          <span style={{ fontWeight: 650, color: '#172338' }}>
            {row.original.stockQuantity}
          </span>
        ),
      },
      {
        accessorKey: 'ledgerQuantity',
        header: 'Lũy kế sổ kho',
        cell: ({ row }) => (
          <span style={{ fontWeight: 650, color: '#36485f' }}>
            {row.original.ledgerQuantity}
          </span>
        ),
      },
      {
        id: 'discrepancy',
        header: 'Chênh lệch',
        cell: ({ row }) => {
          const diff = Number(row.original.discrepancy || 0);
          if (diff === 0) {
            return <span className="muted font-mono">0</span>;
          }
          return (
            <span
              style={{
                fontWeight: 700,
                color: '#b42335',
                fontFamily: 'monospace',
              }}
            >
              {diff > 0 ? `+${diff}` : diff}
            </span>
          );
        },
      },
      {
        id: 'availability',
        header: 'Giữ chỗ & Khả dụng',
        cell: ({ row }) => (
          <div style={{ fontSize: '12px' }}>
            <div>
              Khả dụng: <strong>{row.original.availableQuantity}</strong>
            </div>
            <div className="muted" style={{ fontSize: '11px' }}>
              Đang giữ: {row.original.reservedQuantity}
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'matched',
        header: 'Kết quả đối soát',
        cell: ({ row }) =>
          row.original.matched ? (
            <Badge variant="success" className="inline-flex items-center gap-1">
              <CheckCircle2 size={12} /> Khớp 100%
            </Badge>
          ) : (
            <Badge variant="danger" className="inline-flex items-center gap-1">
              <AlertTriangle size={12} /> Lệch số liệu
            </Badge>
          ),
      },
    ],
    [],
  );

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ KHO & VẬT TƯ"
        title="Đối soát kho & Sổ cái"
        description="Kiểm tra và đối soát tính toàn vẹn giữa số tồn thực tế (parts) và tổng giao dịch trong sổ kho (inventory_transactions)."
        actions={
          <Button
            variant="secondary"
            onClick={() => refetch()}
            pending={isFetching}
          >
            <RefreshCw size={16} className={isFetching ? 'spin' : ''} />
            <span>Đối soát lại toàn bộ</span>
          </Button>
        }
      />

      <div className="info-strip">
        <Scale size={18} />
        <span>
          Cơ chế đối soát tự động so sánh số tồn kho hiện hành với tổng phát sinh chứng từ bất biến. Khi có sai lệch, cần tiến hành kiểm kê thực tế hoặc lập phiếu điều chỉnh (ADJUSTMENT).
        </span>
      </div>

      {/* KPI Cards thống kê */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '16px',
        }}
      >
        <div
          style={{
            background: 'white',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '18px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
          }}
        >
          <div
            style={{
              padding: '10px',
              borderRadius: '8px',
              background: '#edf3ff',
              color: '#245bdb',
            }}
          >
            <Scale size={22} />
          </div>
          <div>
            <div style={{ fontSize: '12px', color: '#526278', fontWeight: 550 }}>
              Tổng phụ tùng đối soát
            </div>
            <div style={{ fontSize: '24px', fontWeight: 700, color: '#172338' }}>
              {reconciliations.length}
            </div>
          </div>
        </div>

        <div
          style={{
            background: 'white',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '18px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
          }}
        >
          <div
            style={{
              padding: '10px',
              borderRadius: '8px',
              background: '#ecfdf5',
              color: '#059669',
            }}
          >
            <CheckCircle2 size={22} />
          </div>
          <div>
            <div style={{ fontSize: '12px', color: '#526278', fontWeight: 550 }}>
              Khớp số liệu (100% cân bằng)
            </div>
            <div style={{ fontSize: '24px', fontWeight: 700, color: '#059669' }}>
              {matchedCount}
            </div>
          </div>
        </div>

        <div
          onClick={() => setFilterDiscrepancyOnly(!filterDiscrepancyOnly)}
          style={{
            background: 'white',
            border: filterDiscrepancyOnly ? '2px solid #b42335' : '1px solid var(--border)',
            borderRadius: '12px',
            padding: '18px 20px',
            display: 'flex',
            alignItems: 'center',
            gap: '14px',
            cursor: 'pointer',
            transition: 'all 0.15s ease',
          }}
        >
          <div
            style={{
              padding: '10px',
              borderRadius: '8px',
              background: discrepancyCount > 0 ? '#fff1f2' : '#f8fafc',
              color: discrepancyCount > 0 ? '#b42335' : '#64748b',
            }}
          >
            <AlertTriangle size={22} />
          </div>
          <div>
            <div style={{ fontSize: '12px', color: '#526278', fontWeight: 550 }}>
              Có chênh lệch (Cần kiểm kê)
            </div>
            <div
              style={{
                fontSize: '24px',
                fontWeight: 700,
                color: discrepancyCount > 0 ? '#b42335' : '#172338',
              }}
            >
              {discrepancyCount}
            </div>
          </div>
        </div>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc */}
        <div className="user-filters" role="search" aria-label="Bộ lọc đối soát kho">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc đối soát kho</h2>
              {filterCount > 0 && <span className="filter-count">{filterCount} đang áp dụng</span>}
            </div>
            <Button
              variant="ghost"
              className="filter-reset"
              disabled={!isFiltered}
              onClick={handleResetFilters}
            >
              <RotateCcw size={14} />
              Đặt lại
            </Button>
          </div>

          <div
            className="user-filter-grid"
            style={{
              gridTemplateColumns: 'minmax(240px, 1.8fr) minmax(280px, 1.2fr)',
            }}
          >
            {/* Ô tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="reconcile-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="reconcile-search-input"
                  className="input"
                  placeholder="Nhập tên phụ tùng hoặc mã SKU..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
                {query && (
                  <button
                    type="button"
                    className="search-clear"
                    aria-label="Xóa từ khóa tìm kiếm"
                    onClick={() => setQuery('')}
                  >
                    <X size={15} />
                  </button>
                )}
              </div>
            </div>

            {/* Chế độ hiển thị Segments */}
            <fieldset className="user-filter-field status-filter">
              <legend>Phạm vi đối soát</legend>
              <div className="status-segments">
                <button
                  type="button"
                  className={!filterDiscrepancyOnly ? 'selected' : ''}
                  onClick={() => setFilterDiscrepancyOnly(false)}
                >
                  Tất cả phụ tùng
                </button>
                <button
                  type="button"
                  className={filterDiscrepancyOnly ? 'selected' : ''}
                  onClick={() => setFilterDiscrepancyOnly(true)}
                  style={filterDiscrepancyOnly ? { background: '#fff1f2', color: '#b42335' } : {}}
                >
                  Chỉ xem lệch ({discrepancyCount})
                </button>
              </div>
            </fieldset>
          </div>
        </div>

        {/* Nội dung đối soát */}
        {isLoading ? (
          <Loading label="Đang kiểm tra và đối soát sổ kho…" />
        ) : filteredList.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '48px 16px' }}>
            <CheckCircle2 size={44} style={{ color: '#059669', margin: '0 auto 8px' }} />
            <h3 style={{ fontSize: '16px', fontWeight: 650, color: '#172338' }}>
              Tất cả dữ liệu sổ kho đều hoàn toàn khớp 100%!
            </h3>
            <p className="muted" style={{ fontSize: '13px', marginTop: '4px' }}>
              Không phát hiện bất kỳ sai lệch nào giữa số tồn thực tế và lũy kế các giao dịch kho.
            </p>
          </div>
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {filteredList.map((item) => (
              <div
                key={item.partId}
                style={{
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '14px',
                  background: 'white',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '8px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <strong style={{ fontSize: '15px' }}>{item.partName}</strong>
                    <div className="muted font-mono" style={{ fontSize: '12px' }}>{item.sku}</div>
                  </div>
                  {item.matched ? (
                    <Badge variant="success">Khớp 100%</Badge>
                  ) : (
                    <Badge variant="danger">Lệch: {item.discrepancy}</Badge>
                  )}
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                  <span>Tồn hiện tại: <strong>{item.stockQuantity}</strong></span>
                  <span>Sổ kho: <strong>{item.ledgerQuantity}</strong></span>
                </div>
                <div style={{ fontSize: '12px', color: '#526278', paddingTop: '4px', borderTop: '1px solid #f0f2f5' }}>
                  Khả dụng: <strong>{item.availableQuantity}</strong> (Đang giữ: {item.reservedQuantity})
                </div>
              </div>
            ))}
          </div>
        ) : (
          <DataTable
            caption="Kết quả đối soát tồn kho và sổ cái"
            columns={columns}
            data={filteredListWithId}
            busy={isFetching}
          />
        )}
      </section>
    </>
  );
}

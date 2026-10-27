import { useMemo, useState } from 'react';
import {
  AlertTriangle,
  Building2,
  CheckCircle2,
  CreditCard,
  RotateCcw,
  Search,
  SlidersHorizontal,
  TrendingDown,
  TrendingUp,
  Users,
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
import { useCustomerDebtsQuery, useSupplierDebtsQuery } from '../hooks/useBilling';
import type { CustomerDebtBalance, SupplierDebtBalance } from '../types/billing';

type CustomerDebtRow = CustomerDebtBalance & { id: string };
type SupplierDebtRow = SupplierDebtBalance & { id: string };

export function DebtLedgerPage() {
  const compact = useMediaQuery('(max-width: 640px)');
  const [activeTab, setActiveTab] = useState<'customer' | 'supplier'>('customer');
  const [hasBalanceOnly, setHasBalanceOnly] = useState(false);
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);

  const { data: customerDebtsData, isLoading: isCustLoading, isFetching: isCustFetching } =
    useCustomerDebtsQuery({
      hasBalance: hasBalanceOnly ? true : undefined,
      size: 100,
    });

  const { data: supplierDebtsData, isLoading: isSuppLoading, isFetching: isSuppFetching } =
    useSupplierDebtsQuery({
      hasBalance: hasBalanceOnly ? true : undefined,
      size: 100,
    });

  const customerDebts = customerDebtsData?.content || [];
  const supplierDebts = supplierDebtsData?.content || [];

  const totalReceivables = customerDebts.reduce(
    (sum, c) => sum + Number(c.balanceAmount || 0),
    0,
  );
  const totalPayables = supplierDebts.reduce(
    (sum, s) => sum + Number(s.balanceAmount || 0),
    0,
  );

  const filterCount = (query.trim() ? 1 : 0) + (hasBalanceOnly ? 1 : 0);
  const isFiltered = query.length > 0 || hasBalanceOnly;

  const handleResetFilters = () => {
    setQuery('');
    setHasBalanceOnly(false);
  };

  const filteredCustomerDebts = useMemo(() => {
    if (!debouncedQuery.trim()) return customerDebts;
    const q = debouncedQuery.toLowerCase().trim();
    return customerDebts.filter(
      (c) =>
        c.customerName?.toLowerCase().includes(q) ||
        c.customerCode?.toLowerCase().includes(q) ||
        c.phone?.includes(q),
    );
  }, [customerDebts, debouncedQuery]);

  const filteredCustomerDebtsWithId: CustomerDebtRow[] = useMemo(() => {
    return filteredCustomerDebts.map((c) => ({ ...c, id: c.customerId }));
  }, [filteredCustomerDebts]);

  const filteredSupplierDebts = useMemo(() => {
    if (!debouncedQuery.trim()) return supplierDebts;
    const q = debouncedQuery.toLowerCase().trim();
    return supplierDebts.filter(
      (s) =>
        s.supplierName?.toLowerCase().includes(q) ||
        s.supplierCode?.toLowerCase().includes(q) ||
        s.phone?.includes(q),
    );
  }, [supplierDebts, debouncedQuery]);

  const filteredSupplierDebtsWithId: SupplierDebtRow[] = useMemo(() => {
    return filteredSupplierDebts.map((s) => ({ ...s, id: s.supplierId }));
  }, [filteredSupplierDebts]);

  const customerColumns = useMemo<ColumnDef<CustomerDebtRow>[]>(
    () => [
      {
        id: 'customer',
        header: 'Khách hàng',
        cell: ({ row }) => (
          <div className="account-cell">
            <span
              className="avatar"
              style={{
                background: '#edf3ff',
                color: '#245bdb',
                fontWeight: 700,
              }}
            >
              <Users size={16} />
            </span>
            <div>
              <strong style={{ color: '#172338' }}>{row.original.customerName}</strong>
              <div className="muted font-mono" style={{ fontSize: '12px' }}>
                {row.original.customerCode || `#${row.original.customerId}`}
              </div>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'phone',
        header: 'Số điện thoại',
        cell: ({ row }) => (
          <span className="muted font-mono" style={{ fontSize: '13px' }}>
            {row.original.phone || '—'}
          </span>
        ),
      },
      {
        accessorKey: 'totalInvoiced',
        header: 'Tổng phát sinh (đ)',
        cell: ({ row }) => (
          <span style={{ fontWeight: 600 }}>
            {Number(row.original.totalInvoiced || 0).toLocaleString('vi-VN')} đ
          </span>
        ),
      },
      {
        accessorKey: 'totalPaid',
        header: 'Đã thanh toán (đ)',
        cell: ({ row }) => (
          <span style={{ fontWeight: 600, color: '#059669' }}>
            {Number(row.original.totalPaid || 0).toLocaleString('vi-VN')} đ
          </span>
        ),
      },
      {
        accessorKey: 'balanceAmount',
        header: 'Dư nợ hiện tại',
        cell: ({ row }) => {
          const bal = Number(row.original.balanceAmount || 0);
          return (
            <span
              style={{
                fontSize: '14px',
                fontWeight: 700,
                color: bal > 0 ? '#b42335' : '#526278',
              }}
            >
              {bal.toLocaleString('vi-VN')} đ
            </span>
          );
        },
      },
      {
        accessorKey: 'creditLimit',
        header: 'Hạn mức nợ',
        cell: ({ row }) => {
          const limit = Number(row.original.creditLimit || 0);
          return (
            <span className="muted" style={{ fontSize: '13px' }}>
              {limit > 0 ? `${limit.toLocaleString('vi-VN')} đ` : 'Không giới hạn'}
            </span>
          );
        },
      },
      {
        id: 'creditStatus',
        header: 'Tình trạng tín dụng',
        cell: ({ row }) => {
          const bal = Number(row.original.balanceAmount || 0);
          const limit = Number(row.original.creditLimit || 0);
          const isOver = limit > 0 && bal > limit;

          return isOver ? (
            <Badge variant="danger" className="inline-flex items-center gap-1">
              <AlertTriangle size={12} /> Vượt hạn mức
            </Badge>
          ) : (
            <Badge variant="success" className="inline-flex items-center gap-1">
              <CheckCircle2 size={12} /> Trong an toàn
            </Badge>
          );
        },
      },
    ],
    [],
  );

  const supplierColumns = useMemo<ColumnDef<SupplierDebtRow>[]>(
    () => [
      {
        id: 'supplier',
        header: 'Nhà cung cấp',
        cell: ({ row }) => (
          <div className="account-cell">
            <span
              className="avatar"
              style={{
                background: '#fff7ed',
                color: '#c2410c',
                fontWeight: 700,
              }}
            >
              <Building2 size={16} />
            </span>
            <div>
              <strong style={{ color: '#172338' }}>{row.original.supplierName}</strong>
              <div className="muted font-mono" style={{ fontSize: '12px' }}>
                {row.original.supplierCode}
              </div>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'phone',
        header: 'Số điện thoại',
        cell: ({ row }) => (
          <span className="muted font-mono" style={{ fontSize: '13px' }}>
            {row.original.phone || '—'}
          </span>
        ),
      },
      {
        accessorKey: 'totalPurchased',
        header: 'Tổng hàng nhập (đ)',
        cell: ({ row }) => (
          <span style={{ fontWeight: 600 }}>
            {Number(row.original.totalPurchased || 0).toLocaleString('vi-VN')} đ
          </span>
        ),
      },
      {
        accessorKey: 'totalPaid',
        header: 'Gara đã trả (đ)',
        cell: ({ row }) => (
          <span style={{ fontWeight: 600, color: '#059669' }}>
            {Number(row.original.totalPaid || 0).toLocaleString('vi-VN')} đ
          </span>
        ),
      },
      {
        accessorKey: 'balanceAmount',
        header: 'Gara còn nợ',
        cell: ({ row }) => {
          const bal = Number(row.original.balanceAmount || 0);
          return (
            <span
              style={{
                fontSize: '14px',
                fontWeight: 700,
                color: bal > 0 ? '#b42335' : '#526278',
              }}
            >
              {bal.toLocaleString('vi-VN')} đ
            </span>
          );
        },
      },
      {
        accessorKey: 'paymentTermDays',
        header: 'Thời hạn công nợ',
        cell: ({ row }) => (
          <Badge variant="outline">{row.original.paymentTermDays} ngày</Badge>
        ),
      },
    ],
    [],
  );

  const isLoading = activeTab === 'customer' ? isCustLoading : isSuppLoading;
  const isFetching = activeTab === 'customer' ? isCustFetching : isSuppFetching;

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ TÀI CHÍNH & THANH TOÁN"
        title="Sổ nợ & Tín dụng"
        description="Theo dõi hạn mức tín dụng khách hàng (phải thu) và công nợ nhà cung cấp phụ tùng (phải trả)."
      />

      <div className="info-strip">
        <CreditCard size={18} />
        <span>
          Hạn mức tín dụng khách hàng được kiểm tra tự động khi bàn giao xe (canCloseOrder). Chỉ khi công nợ nằm trong hạn mức an toàn thì xe mới được phép bàn giao ra khỏi xưởng.
        </span>
      </div>

      {/* KPI Cards Thống kê công nợ */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
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
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                padding: '10px',
                borderRadius: '8px',
                background: '#fff1f2',
                color: '#b42335',
              }}
            >
              <TrendingUp size={22} />
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#526278', fontWeight: 550 }}>
                Tổng nợ phải thu (Khách nợ gara)
              </div>
              <div style={{ fontSize: '22px', fontWeight: 700, color: '#b42335' }}>
                {totalReceivables.toLocaleString('vi-VN')} đ
              </div>
            </div>
          </div>
          <Badge variant="danger">
            {customerDebts.filter((c) => Number(c.balanceAmount) > 0).length} khách còn nợ
          </Badge>
        </div>

        <div
          style={{
            background: 'white',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            padding: '18px 20px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                padding: '10px',
                borderRadius: '8px',
                background: '#fff7ed',
                color: '#c2410c',
              }}
            >
              <TrendingDown size={22} />
            </div>
            <div>
              <div style={{ fontSize: '12px', color: '#526278', fontWeight: 550 }}>
                Tổng nợ phải trả (Gara nợ NCC)
              </div>
              <div style={{ fontSize: '22px', fontWeight: 700, color: '#c2410c' }}>
                {totalPayables.toLocaleString('vi-VN')} đ
              </div>
            </div>
          </div>
          <Badge variant="warning">
            {supplierDebts.filter((s) => Number(s.balanceAmount) > 0).length} NCC còn nợ
          </Badge>
        </div>
      </div>

      <section className="panel table-panel">
        {/* Bộ lọc công nợ */}
        <div className="user-filters" role="search" aria-label="Bộ lọc công nợ">
          <div className="user-filters-heading">
            <div>
              <SlidersHorizontal size={16} aria-hidden />
              <h2>Bộ lọc công nợ</h2>
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
              gridTemplateColumns: 'minmax(220px, 1.4fr) minmax(280px, 1.3fr) minmax(220px, 1fr)',
            }}
          >
            {/* Ô tìm kiếm */}
            <div className="user-filter-field">
              <label htmlFor="debt-search-input">Tìm kiếm</label>
              <div className="user-search">
                <Search size={17} aria-hidden />
                <input
                  id="debt-search-input"
                  className="input"
                  placeholder="Nhập tên, mã đối tác hoặc SĐT..."
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

            {/* Phân loại đối tượng Tabs */}
            <fieldset className="user-filter-field status-filter">
              <legend>Phân loại đối tượng</legend>
              <div className="status-segments">
                <button
                  type="button"
                  className={activeTab === 'customer' ? 'selected' : ''}
                  onClick={() => setActiveTab('customer')}
                >
                  Khách hàng ({customerDebts.length})
                </button>
                <button
                  type="button"
                  className={activeTab === 'supplier' ? 'selected' : ''}
                  onClick={() => setActiveTab('supplier')}
                >
                  Nhà cung cấp ({supplierDebts.length})
                </button>
              </div>
            </fieldset>

            {/* Lọc số dư */}
            <fieldset className="user-filter-field status-filter">
              <legend>Lọc số dư nợ</legend>
              <div className="status-segments">
                <button
                  type="button"
                  className={!hasBalanceOnly ? 'selected' : ''}
                  onClick={() => setHasBalanceOnly(false)}
                >
                  Tất cả
                </button>
                <button
                  type="button"
                  className={hasBalanceOnly ? 'selected' : ''}
                  onClick={() => setHasBalanceOnly(true)}
                  style={hasBalanceOnly ? { background: '#fff1f2', color: '#b42335' } : {}}
                >
                  Chỉ còn nợ
                </button>
              </div>
            </fieldset>
          </div>
        </div>

        {/* Nội dung bảng */}
        {isLoading ? (
          <Loading label="Đang tải dữ liệu sổ nợ & tín dụng…" />
        ) : activeTab === 'customer' ? (
          compact ? (
            <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {filteredCustomerDebts.map((c) => {
                const bal = Number(c.balanceAmount || 0);
                const limit = Number(c.creditLimit || 0);
                const isOver = limit > 0 && bal > limit;
                return (
                  <div
                    key={c.customerId}
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
                        <strong style={{ fontSize: '15px' }}>{c.customerName}</strong>
                        <div className="muted font-mono" style={{ fontSize: '12px' }}>{c.customerCode} · {c.phone}</div>
                      </div>
                      {isOver ? (
                        <Badge variant="danger">Vượt hạn mức</Badge>
                      ) : (
                        <Badge variant="success">An toàn</Badge>
                      )}
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                      <span>Dư nợ: <strong style={{ color: bal > 0 ? '#b42335' : '#172338' }}>{bal.toLocaleString('vi-VN')} đ</strong></span>
                      <span>Hạn mức: <strong>{limit > 0 ? `${limit.toLocaleString('vi-VN')} đ` : 'Không giới hạn'}</strong></span>
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <DataTable
              caption="Sổ nợ khách hàng (Phải thu)"
              columns={customerColumns}
              data={filteredCustomerDebtsWithId}
              busy={isFetching}
            />
          )
        ) : compact ? (
          <div style={{ padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {filteredSupplierDebts.map((s) => {
              const bal = Number(s.balanceAmount || 0);
              return (
                <div
                  key={s.supplierId}
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
                      <strong style={{ fontSize: '15px' }}>{s.supplierName}</strong>
                      <div className="muted font-mono" style={{ fontSize: '12px' }}>{s.supplierCode} · {s.phone}</div>
                    </div>
                    <Badge variant="outline">{s.paymentTermDays} ngày</Badge>
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px' }}>
                    <span>Gara còn nợ: <strong style={{ color: bal > 0 ? '#b42335' : '#172338' }}>{bal.toLocaleString('vi-VN')} đ</strong></span>
                    <span>Đã trả: <strong>{Number(s.totalPaid).toLocaleString('vi-VN')} đ</strong></span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <DataTable
            caption="Sổ nợ nhà cung cấp (Phải trả)"
            columns={supplierColumns}
            data={filteredSupplierDebtsWithId}
            busy={isFetching}
          />
        )}
      </section>
    </>
  );
}

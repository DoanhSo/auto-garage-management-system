import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertTriangle,
  Building2,
  CheckCircle2,
  CreditCard,
  Download,
  Eye,
  RefreshCw,
  RotateCcw,
  Scale,
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
  Modal,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { useAuth } from '../../../app/providers/AuthProvider';
import { DataTable } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import { useDebouncedValue } from '../../../shared/hooks/useDebouncedValue';
import { useUrlTab } from '../../../shared/hooks/useUrlTab';
import { formatDate, money } from '../../../shared/utils/format';
import {
  csvDate,
  csvMoney,
  csvSections,
  downloadCsv,
  type CsvRow,
} from '../../../shared/utils/csvExport';
import { useCustomerDebtsQuery, useSupplierDebtsQuery } from '../hooks/useBilling';
import type { CustomerDebtBalance, SupplierDebtBalance } from '../types/billing';

type CustomerDebtRow = CustomerDebtBalance & { id: string };
type SupplierDebtRow = SupplierDebtBalance & { id: string };
type CreditState = 'OVERDUE' | 'OVER_LIMIT' | 'NEAR_LIMIT' | 'OK';
type SortKey = 'balance-desc' | 'balance-asc' | 'invoiced-desc' | 'name-asc';
type CreditFilter = 'ALL' | 'ALERT' | 'SAFE';

const sortLabels: Record<SortKey, string> = {
  'balance-desc': 'Dư nợ cao → thấp',
  'balance-asc': 'Dư nợ thấp → cao',
  'invoiced-desc': 'Tổng phát sinh cao → thấp',
  'name-asc': 'Tên đối tác A → Z',
};

/**
 * Khớp BillingServiceImpl.requireHandoverAllowed: hạn mức 0 nghĩa là không cấp nợ
 * (mọi dư nợ đều bị chặn bàn giao), không phải "không giới hạn".
 */
function creditState(row: CustomerDebtBalance): CreditState {
  const balance = Number(row.balanceAmount || 0);
  const limit = Number(row.creditLimit || 0);
  if (row.overdueCount > 0) return 'OVERDUE';
  if (balance > limit) return 'OVER_LIMIT';
  if (limit > 0 && balance >= limit * 0.8) return 'NEAR_LIMIT';
  return 'OK';
}

const creditStateLabels: Record<CreditState, string> = {
  OVERDUE: 'Quá hạn thanh toán',
  OVER_LIMIT: 'Vượt hạn mức nợ',
  NEAR_LIMIT: 'Sắp chạm hạn mức',
  OK: 'Đủ điều kiện tín dụng',
};

function CreditBadge({ state }: { state: CreditState }) {
  if (state === 'OK')
    return (
      <Badge variant="success">
        <CheckCircle2 size={12} /> {creditStateLabels.OK}
      </Badge>
    );
  return (
    <Badge variant={state === 'NEAR_LIMIT' ? 'warning' : 'danger'}>
      <AlertTriangle size={12} /> {creditStateLabels[state]}
    </Badge>
  );
}

const limitText = (limit: string) =>
  Number(limit || 0) > 0 ? money(limit) : '0 đ · Thanh toán ngay';

const DEBT_TABS = ['customer', 'supplier'] as const;

export function DebtLedgerPage() {
  const { user } = useAuth();
  // Thủ kho chỉ quản lý công nợ nhà cung cấp nên mở sẵn tab đó.
  const [activeTab, setActiveTab] = useUrlTab(
    'tab',
    DEBT_TABS,
    user?.role === 'WAREHOUSE_STAFF' ? 'supplier' : 'customer',
  );
  const [hasBalanceOnly, setHasBalanceOnly] = useState(false);
  const [creditFilter, setCreditFilter] = useState<CreditFilter>('ALL');
  const [sortKey, setSortKey] = useState<SortKey>('balance-desc');
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query);
  const [detailCustomer, setDetailCustomer] = useState<CustomerDebtBalance | null>(null);
  const [detailSupplier, setDetailSupplier] = useState<SupplierDebtBalance | null>(null);

  const customerQuery = useCustomerDebtsQuery({ size: 100 });
  const supplierQuery = useSupplierDebtsQuery({ size: 100 });

  const customerDebts = useMemo(
    () => customerQuery.data?.content ?? [],
    [customerQuery.data?.content],
  );
  const supplierDebts = useMemo(
    () => supplierQuery.data?.content ?? [],
    [supplierQuery.data?.content],
  );

  const totalReceivables = customerDebts.reduce((sum, c) => sum + Number(c.balanceAmount || 0), 0);
  const totalPayables = supplierDebts.reduce((sum, s) => sum + Number(s.balanceAmount || 0), 0);
  const totalInvoiced = customerDebts.reduce((sum, c) => sum + Number(c.totalInvoiced || 0), 0);
  const totalCollected = customerDebts.reduce((sum, c) => sum + Number(c.totalPaid || 0), 0);
  const alertCount = customerDebts.filter((c) => creditState(c) !== 'OK').length;
  const collectRate = totalInvoiced > 0 ? Math.round((totalCollected / totalInvoiced) * 100) : 0;

  const filterCount =
    (query.trim() ? 1 : 0) + (hasBalanceOnly ? 1 : 0) + (creditFilter !== 'ALL' ? 1 : 0);

  const resetFilters = () => {
    setQuery('');
    setHasBalanceOnly(false);
    setCreditFilter('ALL');
  };

  const term = debouncedQuery.trim().toLowerCase();

  const customerRows: CustomerDebtRow[] = useMemo(() => {
    const rows = customerDebts
      .filter((c) => !hasBalanceOnly || Number(c.balanceAmount) > 0)
      .filter((c) => {
        if (creditFilter === 'ALL') return true;
        return (creditState(c) !== 'OK') === (creditFilter === 'ALERT');
      })
      .filter(
        (c) =>
          !term ||
          c.customerName?.toLowerCase().includes(term) ||
          c.customerCode?.toLowerCase().includes(term) ||
          c.phone?.includes(term),
      );
    rows.sort((a, b) => {
      if (sortKey === 'name-asc') return a.customerName.localeCompare(b.customerName, 'vi');
      if (sortKey === 'invoiced-desc') return Number(b.totalInvoiced) - Number(a.totalInvoiced);
      const diff = Number(a.balanceAmount) - Number(b.balanceAmount);
      return sortKey === 'balance-asc' ? diff : -diff;
    });
    return rows.map((c) => ({ ...c, id: c.customerId }));
  }, [customerDebts, hasBalanceOnly, creditFilter, term, sortKey]);

  const supplierRows: SupplierDebtRow[] = useMemo(() => {
    const rows = supplierDebts
      .filter((s) => !hasBalanceOnly || Number(s.balanceAmount) > 0)
      .filter(
        (s) =>
          !term ||
          s.supplierName?.toLowerCase().includes(term) ||
          s.supplierCode?.toLowerCase().includes(term) ||
          s.phone?.includes(term),
      );
    rows.sort((a, b) => {
      if (sortKey === 'name-asc') return a.supplierName.localeCompare(b.supplierName, 'vi');
      if (sortKey === 'invoiced-desc') return Number(b.totalPurchased) - Number(a.totalPurchased);
      const diff = Number(a.balanceAmount) - Number(b.balanceAmount);
      return sortKey === 'balance-asc' ? diff : -diff;
    });
    return rows.map((s) => ({ ...s, id: s.supplierId }));
  }, [supplierDebts, hasBalanceOnly, term, sortKey]);

  const customerColumns = useMemo<ColumnDef<CustomerDebtRow>[]>(
    () => [
      {
        id: 'customer',
        header: 'Khách hàng',
        cell: ({ row }) => (
          <div className="account-cell">
            <span className="avatar" style={{ background: '#edf3ff', color: '#245bdb' }}>
              <Users size={16} />
            </span>
            <div>
              <strong>{row.original.customerName}</strong>
              <span>
                {row.original.customerCode} · {row.original.phone || '—'}
              </span>
            </div>
          </div>
        ),
      },
      {
        id: 'invoices',
        header: 'HĐ nợ & hạn thanh toán',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>{row.original.invoiceCount} hóa đơn nợ</strong>
              <span style={row.original.overdueCount > 0 ? { color: '#b42335' } : undefined}>
                {row.original.overdueCount > 0
                  ? `Quá hạn ${row.original.overdueCount} HĐ`
                  : row.original.nearestDueDate
                    ? `Hạn gần nhất: ${formatDate(row.original.nearestDueDate)}`
                    : 'Không có hạn chờ'}
              </span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'totalInvoiced',
        header: 'Tổng phát sinh',
        cell: ({ row }) => <strong>{money(row.original.totalInvoiced)}</strong>,
      },
      {
        accessorKey: 'totalPaid',
        header: 'Đã thanh toán',
        cell: ({ row }) => {
          const invoiced = Number(row.original.totalInvoiced || 0);
          const pct =
            invoiced > 0 ? Math.round((Number(row.original.totalPaid) / invoiced) * 100) : 0;
          return (
            <div className="account-cell">
              <div>
                <strong style={{ color: '#059669' }}>{money(row.original.totalPaid)}</strong>
                <span>{pct}% tổng phát sinh</span>
              </div>
            </div>
          );
        },
      },
      {
        accessorKey: 'balanceAmount',
        header: 'Dư nợ hiện tại',
        cell: ({ row }) => {
          const bal = Number(row.original.balanceAmount || 0);
          return <strong style={{ color: bal > 0 ? '#b42335' : '#526278' }}>{money(bal)}</strong>;
        },
      },
      {
        id: 'policy',
        header: 'Chính sách tín dụng',
        cell: ({ row }) => (
          <div className="account-cell">
            <div>
              <strong>{limitText(row.original.creditLimit)}</strong>
              <span>Kỳ hạn: {row.original.paymentTermDays} ngày</span>
            </div>
          </div>
        ),
      },
      {
        id: 'creditStatus',
        header: 'Trạng thái tín dụng',
        cell: ({ row }) => <CreditBadge state={creditState(row.original)} />,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button variant="ghost" onClick={() => setDetailCustomer(row.original)}>
              <Eye size={14} /> Sổ chi tiết
            </Button>
          </div>
        ),
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
            <span className="avatar" style={{ background: '#fff7ed', color: '#c2410c' }}>
              <Building2 size={16} />
            </span>
            <div>
              <strong>{row.original.supplierName}</strong>
              <span>{row.original.supplierCode}</span>
            </div>
          </div>
        ),
      },
      {
        accessorKey: 'phone',
        header: 'Liên hệ',
        cell: ({ row }) => <span className="muted">{row.original.phone || '—'}</span>,
      },
      {
        accessorKey: 'totalPurchased',
        header: 'Tổng hàng nhập',
        cell: ({ row }) => <strong>{money(row.original.totalPurchased)}</strong>,
      },
      {
        accessorKey: 'totalPaid',
        header: 'Gara đã trả',
        cell: ({ row }) => (
          <strong style={{ color: '#059669' }}>{money(row.original.totalPaid)}</strong>
        ),
      },
      {
        accessorKey: 'balanceAmount',
        header: 'Gara còn nợ',
        cell: ({ row }) => {
          const bal = Number(row.original.balanceAmount || 0);
          return <strong style={{ color: bal > 0 ? '#b42335' : '#526278' }}>{money(bal)}</strong>;
        },
      },
      {
        accessorKey: 'paymentTermDays',
        header: 'Kỳ hạn công nợ',
        cell: ({ row }) => <Badge variant="outline">{row.original.paymentTermDays} ngày</Badge>,
      },
      {
        id: 'actions',
        header: 'Thao tác',
        cell: ({ row }) => (
          <div className="row-actions">
            <Button variant="ghost" onClick={() => setDetailSupplier(row.original)}>
              <Eye size={14} /> Chi tiết
            </Button>
          </div>
        ),
      },
    ],
    [],
  );

  const exportCsv = () => {
    const customerCsv: CsvRow[] = customerRows.map((c) => [
      c.customerCode,
      c.customerName,
      c.phone,
      c.invoiceCount,
      c.overdueCount,
      csvDate(c.nearestDueDate),
      csvMoney(c.totalInvoiced),
      csvMoney(c.totalPaid),
      csvMoney(c.balanceAmount),
      csvMoney(c.creditLimit),
      c.paymentTermDays,
      creditStateLabels[creditState(c)],
    ]);
    const supplierCsv: CsvRow[] = supplierRows.map((s) => [
      s.supplierCode,
      s.supplierName,
      s.phone,
      csvMoney(s.totalPurchased),
      csvMoney(s.totalPaid),
      csvMoney(s.balanceAmount),
      s.paymentTermDays,
    ]);
    downloadCsv(
      `so-no-${new Date().toISOString().slice(0, 10)}`,
      csvSections([
        {
          title: 'I. SỔ NỢ KHÁCH HÀNG (PHẢI THU)',
          headers: [
            'Mã KH',
            'Khách hàng',
            'SĐT',
            'Số HĐ nợ',
            'Số HĐ quá hạn',
            'Hạn gần nhất',
            'Tổng phát sinh',
            'Đã thanh toán',
            'Dư nợ',
            'Hạn mức',
            'Kỳ hạn (ngày)',
            'Trạng thái tín dụng',
          ],
          rows: customerCsv,
        },
        {
          title: 'II. SỔ NỢ NHÀ CUNG CẤP (PHẢI TRẢ)',
          headers: [
            'Mã NCC',
            'Nhà cung cấp',
            'SĐT',
            'Tổng hàng nhập',
            'Đã trả',
            'Còn nợ',
            'Kỳ hạn (ngày)',
          ],
          rows: supplierCsv,
        },
      ]),
    );
  };

  const current = activeTab === 'customer' ? customerQuery : supplierQuery;

  return (
    <>
      <PageHeader
        eyebrow="QUẢN LÝ TÀI CHÍNH & THANH TOÁN"
        title="Sổ nợ & Tín dụng"
        description="Theo dõi hạn mức tín dụng khách hàng (phải thu) và công nợ nhà cung cấp phụ tùng (phải trả)."
        actions={
          <div className="row-actions">
            <Button
              variant="secondary"
              onClick={() => {
                void customerQuery.refetch();
                void supplierQuery.refetch();
              }}
              pending={current.isFetching}
            >
              <RefreshCw size={15} /> Làm mới
            </Button>
            <Button variant="secondary" onClick={exportCsv}>
              <Download size={15} /> Xuất CSV sổ nợ
            </Button>
          </div>
        }
      />

      <div className="info-strip">
        <CreditCard size={18} />
        <span>
          Xe chỉ được bàn giao khi dư nợ trong hạn mức và không có hóa đơn quá hạn. Hạn mức 0 đ
          nghĩa là khách phải thanh toán ngay, không được ghi nợ.
        </span>
      </div>

      <MetricGrid
        label="Tổng quan công nợ"
        items={[
          {
            label: 'Nợ phải thu từ khách',
            value: money(totalReceivables),
            hint: `${customerDebts.filter((c) => Number(c.balanceAmount) > 0).length} khách còn nợ`,
            tone: 'danger',
            icon: <TrendingUp size={18} />,
          },
          {
            label: 'Nợ phải trả nhà cung cấp',
            value: money(totalPayables),
            hint: `${supplierDebts.filter((s) => Number(s.balanceAmount) > 0).length} nhà cung cấp còn nợ`,
            tone: 'warning',
            icon: <TrendingDown size={18} />,
          },
          {
            label: 'Cảnh báo tín dụng & quá hạn',
            value: alertCount,
            hint: 'Khách vượt hạn mức hoặc có hóa đơn quá hạn',
            tone: alertCount > 0 ? 'danger' : 'success',
            icon: <AlertTriangle size={18} />,
          },
          {
            label: 'Công nợ ròng (thu − trả)',
            value: money(totalReceivables - totalPayables),
            hint: `Đã thu ${collectRate}% tổng phát sinh`,
            tone: 'info',
            icon: <Scale size={18} />,
          },
        ]}
      />

      <section className="panel table-panel">
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
              disabled={filterCount === 0}
              onClick={resetFilters}
            >
              <RotateCcw size={14} />
              Đặt lại
            </Button>
          </div>

          <div className="user-filter-grid">
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

            <fieldset className="user-filter-field status-filter">
              <legend>Phân loại đối tượng</legend>
              <div className="status-segments">
                <button
                  type="button"
                  className={activeTab === 'customer' ? 'selected' : ''}
                  aria-pressed={activeTab === 'customer'}
                  onClick={() => setActiveTab('customer')}
                >
                  Khách hàng ({customerDebts.length})
                </button>
                <button
                  type="button"
                  className={activeTab === 'supplier' ? 'selected' : ''}
                  aria-pressed={activeTab === 'supplier'}
                  onClick={() => setActiveTab('supplier')}
                >
                  Nhà cung cấp ({supplierDebts.length})
                </button>
              </div>
            </fieldset>

            <fieldset className="user-filter-field status-filter">
              <legend>Lọc số dư nợ</legend>
              <div className="status-segments">
                <button
                  type="button"
                  className={!hasBalanceOnly ? 'selected' : ''}
                  aria-pressed={!hasBalanceOnly}
                  onClick={() => setHasBalanceOnly(false)}
                >
                  Tất cả
                </button>
                <button
                  type="button"
                  className={hasBalanceOnly ? 'selected' : ''}
                  aria-pressed={hasBalanceOnly}
                  onClick={() => setHasBalanceOnly(true)}
                >
                  Chỉ còn nợ
                </button>
              </div>
            </fieldset>

            <div className="user-filter-field">
              <label htmlFor="debt-sort">Sắp xếp</label>
              <select
                id="debt-sort"
                className="select"
                value={sortKey}
                onChange={(e) => setSortKey(e.target.value as SortKey)}
              >
                {Object.entries(sortLabels).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            {activeTab === 'customer' && (
              <div className="user-filter-field">
                <label htmlFor="debt-credit">Tình trạng tín dụng</label>
                <select
                  id="debt-credit"
                  className="select"
                  value={creditFilter}
                  onChange={(e) => setCreditFilter(e.target.value as CreditFilter)}
                >
                  <option value="ALL">Tất cả</option>
                  <option value="ALERT">Có cảnh báo / quá hạn</option>
                  <option value="SAFE">An toàn</option>
                </select>
              </div>
            )}
          </div>
        </div>

        {current.isLoading ? (
          <Loading label="Đang tải dữ liệu sổ nợ & tín dụng…" />
        ) : current.isError ? (
          <QueryError error={current.error} retry={() => current.refetch()} />
        ) : activeTab === 'customer' ? (
          <DataTable
            caption="Sổ nợ khách hàng (Phải thu)"
            columns={customerColumns}
            data={customerRows}
            busy={customerQuery.isFetching}
          />
        ) : (
          <DataTable
            caption="Sổ nợ nhà cung cấp (Phải trả)"
            columns={supplierColumns}
            data={supplierRows}
            busy={supplierQuery.isFetching}
          />
        )}
      </section>

      <Modal
        open={!!detailCustomer}
        onOpenChange={(open) => !open && setDetailCustomer(null)}
        size="lg"
        title={`Sổ chi tiết · ${detailCustomer?.customerName ?? ''}`}
        description={
          detailCustomer
            ? `${detailCustomer.customerCode} · Hạn mức: ${limitText(detailCustomer.creditLimit)} · Kỳ hạn ${detailCustomer.paymentTermDays} ngày`
            : undefined
        }
      >
        {detailCustomer && (
          <>
            <p>
              <CreditBadge state={creditState(detailCustomer)} /> Dư nợ{' '}
              <strong>{money(detailCustomer.balanceAmount)}</strong>
              {detailCustomer.overdueCount > 0 &&
                ` · quá hạn ${detailCustomer.overdueCount} HĐ (${money(detailCustomer.overdueAmount)})`}
            </p>
            {detailCustomer.invoices.length === 0 ? (
              <p className="empty-state">Khách hàng chưa có hóa đơn nào.</p>
            ) : (
              <div className="table-scroll">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Hóa đơn</th>
                      <th>Phát hành</th>
                      <th>Hạn thanh toán</th>
                      <th>Tổng tiền</th>
                      <th>Đã thu</th>
                      <th>Còn nợ</th>
                    </tr>
                  </thead>
                  <tbody>
                    {detailCustomer.invoices.map((inv) => {
                      const overdue =
                        Number(inv.balanceAmount) > 0 &&
                        !!inv.dueDate &&
                        inv.dueDate.slice(0, 10) < new Date().toISOString().slice(0, 10);
                      return (
                        <tr key={inv.invoiceId}>
                          <td>
                            <strong>#{inv.invoiceCode}</strong>
                          </td>
                          <td>{formatDate(inv.issuedAt)}</td>
                          <td style={overdue ? { color: '#b42335' } : undefined}>
                            {inv.dueDate ? formatDate(inv.dueDate) : '—'}
                            {overdue && ' (quá hạn)'}
                          </td>
                          <td>{money(inv.totalAmount)}</td>
                          <td>{money(inv.paidAmount)}</td>
                          <td>
                            <strong>{money(inv.balanceAmount)}</strong>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
            <div className="modal-form-actions">
              <Button variant="secondary" onClick={() => setDetailCustomer(null)}>
                Đóng
              </Button>
              <Link className="button button-primary" to="/billing/payments">
                Thu nợ tại Sổ quỹ
              </Link>
            </div>
          </>
        )}
      </Modal>

      <Modal
        open={!!detailSupplier}
        onOpenChange={(open) => !open && setDetailSupplier(null)}
        title={`Công nợ · ${detailSupplier?.supplierName ?? ''}`}
        description={detailSupplier?.supplierCode}
      >
        {detailSupplier && (
          <>
            <dl className="detail-list">
              <div>
                <dt>Tổng hàng nhập</dt>
                <dd>{money(detailSupplier.totalPurchased)}</dd>
              </div>
              <div>
                <dt>Gara đã trả</dt>
                <dd>{money(detailSupplier.totalPaid)}</dd>
              </div>
              <div>
                <dt>Gara còn nợ</dt>
                <dd>
                  <strong>{money(detailSupplier.balanceAmount)}</strong>
                </dd>
              </div>
              <div>
                <dt>Kỳ hạn công nợ</dt>
                <dd>{detailSupplier.paymentTermDays} ngày</dd>
              </div>
              <div>
                <dt>Liên hệ</dt>
                <dd>{detailSupplier.phone || '—'}</dd>
              </div>
            </dl>
            <p className="muted">
              Việc chi trả được ghi theo từng phiếu nhập kho đã ghi sổ tại màn Sổ quỹ.
            </p>
            <div className="modal-form-actions">
              <Button variant="secondary" onClick={() => setDetailSupplier(null)}>
                Đóng
              </Button>
              <Link className="button button-primary" to="/billing/payments">
                Chi trả tại Sổ quỹ
              </Link>
            </div>
          </>
        )}
      </Modal>
    </>
  );
}

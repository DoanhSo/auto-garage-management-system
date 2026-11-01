import { useMemo, useState } from 'react';
import { Temporal } from '@js-temporal/polyfill';
import {
  ArrowDownLeft,
  ArrowUpRight,
  CalendarDays,
  Clock3,
  Download,
  Package,
  RefreshCw,
  Scale,
  TrendingUp,
  Truck,
  Users,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Badge,
  Button,
  Field,
  FormError,
  Loading,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import {
  dateRange,
  initialReportDates,
  today,
  type ReportRange,
} from '../../../shared/utils/dates';
import { dateTime, formatDate, hours, money } from '../../../shared/utils/format';
import {
  csvDate,
  csvDateTime,
  csvMoney,
  csvSections,
  csvTable,
  downloadCsv,
  type CsvRow,
} from '../../../shared/utils/csvExport';
import type { UiPolicy } from '../../../shared/types/contracts';
import { useDashboardQuery } from '../hooks/useReports';
import { useCustomerDebtsQuery, usePaymentsQuery, useSupplierDebtsQuery } from '../../billing/hooks/useBilling';
import { usePartsQuery } from '../../inventory/hooks/useInventory';
import type { InvoiceBalanceItem, Payment, SupplierDebtBalance } from '../../billing/types/billing';
import type { Part } from '../../inventory/types/inventory';
import { useToast } from '../../../app/providers/ToastProvider';

type TabKey = 'receivables' | 'payables' | 'cashflow' | 'inventory';

interface ReceivableRow extends InvoiceBalanceItem {
  id: string;
  customerName: string;
  customerCode: string;
  phone: string | null;
  overdueDays: number;
}

interface InventoryRow extends Part {
  stockValue: number;
  stockState: string;
}

const methodLabels: Record<string, string> = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  VIETQR: 'VietQR',
  CARD: 'Thẻ',
};

const tabLabels: Record<TabKey, string> = {
  receivables: 'Hóa đơn còn phải thu',
  payables: 'Công nợ nhà cung cấp',
  cashflow: 'Giao dịch thu / chi trong kỳ',
  inventory: 'Tồn kho & giá trị vật tư',
};

function parseRange(
  from: string,
  through: string,
  policy: UiPolicy,
): { range: ReportRange | null; error?: string } {
  try {
    return {
      range: dateRange(from, through, policy.garageTimeZone, policy.reportMaxPeriodSeconds),
    };
  } catch (error) {
    return {
      range: null,
      error:
        error instanceof Error && error.message.startsWith('Khoảng')
          ? error.message
          : 'Chọn khoảng ngày hợp lệ, ngày kết thúc bằng hoặc sau ngày bắt đầu.',
    };
  }
}

function presetDates(key: string, zone: string): { from: string; through: string } {
  const end = Temporal.PlainDate.from(today(zone));
  switch (key) {
    case 'today':
      return { from: end.toString(), through: end.toString() };
    case '7d':
      return { from: end.subtract({ days: 6 }).toString(), through: end.toString() };
    case '30d':
      return { from: end.subtract({ days: 29 }).toString(), through: end.toString() };
    case 'month':
      return { from: end.with({ day: 1 }).toString(), through: end.toString() };
    default: {
      const prev = end.with({ day: 1 }).subtract({ months: 1 });
      return { from: prev.toString(), through: prev.with({ day: prev.daysInMonth }).toString() };
    }
  }
}

const presets = [
  { key: 'today', label: 'Hôm nay' },
  { key: '7d', label: '7 ngày qua' },
  { key: '30d', label: '30 ngày qua' },
  { key: 'month', label: 'Tháng này' },
  { key: 'prev', label: 'Tháng trước' },
];

function ReadyReports({ policy }: { policy: UiPolicy }) {
  const notify = useToast();
  const initial = useMemo(
    () => initialReportDates(policy.garageTimeZone, policy.reportMaxPeriodSeconds),
    [policy],
  );
  const [dates, setDates] = useState(initial);
  const [applied, setApplied] = useState(
    () => parseRange(initial.from, initial.through, policy).range,
  );
  const [tab, setTab] = useState<TabKey>('receivables');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(policy.paginationDefaultSize);
  const checked = parseRange(dates.from, dates.through, policy);

  const dashboard = useDashboardQuery(applied);
  const customerDebts = useCustomerDebtsQuery({ size: 100 });
  const supplierDebts = useSupplierDebtsQuery({ size: 100 });
  const payments = usePaymentsQuery({ page: 0, size: 100, sortBy: 'paidAt', sortDir: 'desc' });
  const parts = usePartsQuery({ page: 0, size: 100, active: true, sortBy: 'sku', sortDir: 'asc' });

  const receivables: ReceivableRow[] = useMemo(() => {
    const now = Date.now();
    return (customerDebts.data?.content ?? [])
      .flatMap((c) =>
        c.invoices
          .filter((i) => Number(i.balanceAmount) > 0)
          .map((i) => ({
            ...i,
            id: i.invoiceId,
            customerName: c.customerName,
            customerCode: c.customerCode,
            phone: c.phone,
            overdueDays: i.dueDate
              ? Math.max(0, Math.floor((now - new Date(i.dueDate).getTime()) / 86_400_000))
              : 0,
          })),
      )
      .sort((a, b) => Number(b.balanceAmount) - Number(a.balanceAmount));
  }, [customerDebts.data?.content]);

  const payables: (SupplierDebtBalance & { id: string })[] = useMemo(
    () =>
      (supplierDebts.data?.content ?? [])
        .filter((s) => Number(s.balanceAmount) > 0 || Number(s.totalPurchased) > 0)
        .map((s) => ({ ...s, id: s.supplierId })),
    [supplierDebts.data?.content],
  );

  const periodPayments: Payment[] = useMemo(() => {
    if (!applied) return [];
    const from = new Date(applied.from).getTime();
    const until = new Date(applied.until).getTime();
    return (payments.data?.content ?? []).filter((p) => {
      const t = new Date(p.paidAt).getTime();
      return t >= from && t < until;
    });
  }, [payments.data?.content, applied]);

  const inventory: InventoryRow[] = useMemo(
    () =>
      (parts.data?.content ?? []).map((p) => {
        const stock = Number(p.stockQuantity);
        return {
          ...p,
          stockValue: stock * Number(p.purchasePrice),
          stockState:
            stock <= 0
              ? 'Hết hàng'
              : Number(p.availableQuantity) <= Number(p.reorderLevel)
                ? 'Cảnh báo sắp hết'
                : 'Đủ tồn',
        };
      }),
    [parts.data?.content],
  );

  const receivableColumns = useMemo<ColumnDef<ReceivableRow>[]>(
    () => [
      {
        accessorKey: 'invoiceCode',
        header: 'Hóa đơn',
        cell: ({ row }) => <strong className="code-cell">#{row.original.invoiceCode}</strong>,
      },
      {
        id: 'customer',
        header: 'Khách hàng',
        cell: ({ row }) => (
          <div className="account-cell">
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
        accessorKey: 'repairOrderId',
        header: 'Lệnh sửa chữa',
        cell: ({ row }) => (row.original.repairOrderId ? `#${row.original.repairOrderId}` : '—'),
      },
      {
        accessorKey: 'issuedAt',
        header: 'Ngày phát hành',
        cell: ({ row }) => formatDate(row.original.issuedAt),
      },
      {
        id: 'due',
        header: 'Hạn thanh toán',
        cell: ({ row }) =>
          row.original.overdueDays > 0 ? (
            <Badge variant="danger">Quá hạn {row.original.overdueDays} ngày</Badge>
          ) : (
            <span>{row.original.dueDate ? formatDate(row.original.dueDate) : '—'}</span>
          ),
      },
      {
        accessorKey: 'totalAmount',
        header: 'Tổng tiền',
        cell: ({ row }) => <span className="money">{money(row.original.totalAmount)}</span>,
      },
      {
        accessorKey: 'paidAmount',
        header: 'Đã thu',
        cell: ({ row }) => <span className="money muted">{money(row.original.paidAmount)}</span>,
      },
      {
        accessorKey: 'balanceAmount',
        header: 'Còn phải thu',
        cell: ({ row }) => <strong className="money">{money(row.original.balanceAmount)}</strong>,
      },
    ],
    [],
  );

  const payableColumns = useMemo<ColumnDef<SupplierDebtBalance & { id: string }>[]>(
    () => [
      {
        accessorKey: 'supplierCode',
        header: 'Mã NCC',
        cell: ({ row }) => <strong className="code-cell">{row.original.supplierCode}</strong>,
      },
      { accessorKey: 'supplierName', header: 'Nhà cung cấp' },
      {
        accessorKey: 'phone',
        header: 'Số điện thoại',
        cell: ({ row }) => row.original.phone || '—',
      },
      {
        accessorKey: 'paymentTermDays',
        header: 'Kỳ hạn nợ',
        cell: ({ row }) => `${row.original.paymentTermDays} ngày`,
      },
      {
        accessorKey: 'totalPurchased',
        header: 'Tổng giá trị nhập',
        cell: ({ row }) => <span className="money">{money(row.original.totalPurchased)}</span>,
      },
      {
        accessorKey: 'totalPaid',
        header: 'Đã thanh toán',
        cell: ({ row }) => <span className="money muted">{money(row.original.totalPaid)}</span>,
      },
      {
        accessorKey: 'balanceAmount',
        header: 'Còn phải trả',
        cell: ({ row }) => <strong className="money">{money(row.original.balanceAmount)}</strong>,
      },
    ],
    [],
  );

  const cashColumns = useMemo<ColumnDef<Payment>[]>(
    () => [
      {
        accessorKey: 'transactionCode',
        header: 'Mã giao dịch',
        cell: ({ row }) => <strong className="code-cell">{row.original.transactionCode}</strong>,
      },
      {
        accessorKey: 'direction',
        header: 'Loại',
        cell: ({ row }) =>
          row.original.direction === 'IN' ? (
            <Badge variant="success">Phiếu thu</Badge>
          ) : (
            <Badge variant="warning">Phiếu chi</Badge>
          ),
      },
      {
        id: 'source',
        header: 'Chứng từ gốc',
        cell: ({ row }) =>
          row.original.invoiceCode
            ? `#${row.original.invoiceCode}`
            : row.original.documentCode
              ? `#${row.original.documentCode}`
              : '—',
      },
      {
        accessorKey: 'paymentMethod',
        header: 'Phương thức',
        cell: ({ row }) => methodLabels[row.original.paymentMethod] ?? row.original.paymentMethod,
      },
      {
        accessorKey: 'paidAt',
        header: 'Thời gian',
        cell: ({ row }) => dateTime(row.original.paidAt, policy.garageTimeZone),
      },
      {
        accessorKey: 'recordedByName',
        header: 'Người ghi nhận',
        cell: ({ row }) => row.original.recordedByName || '—',
      },
      {
        accessorKey: 'amount',
        header: 'Số tiền',
        cell: ({ row }) => <strong className="money">{money(row.original.amount)}</strong>,
      },
    ],
    [policy.garageTimeZone],
  );

  const inventoryColumns = useMemo<ColumnDef<InventoryRow>[]>(
    () => [
      {
        accessorKey: 'sku',
        header: 'Mã SKU',
        cell: ({ row }) => <strong className="code-cell">{row.original.sku}</strong>,
      },
      { accessorKey: 'partName', header: 'Tên phụ tùng' },
      { accessorKey: 'unit', header: 'ĐVT' },
      { accessorKey: 'stockQuantity', header: 'Tồn kho' },
      { accessorKey: 'reservedQuantity', header: 'Đang giữ chỗ' },
      { accessorKey: 'availableQuantity', header: 'Khả dụng' },
      {
        accessorKey: 'purchasePrice',
        header: 'Đơn giá nhập',
        cell: ({ row }) => <span className="money">{money(row.original.purchasePrice)}</span>,
      },
      {
        accessorKey: 'stockValue',
        header: 'Giá trị tồn',
        cell: ({ row }) => <strong className="money">{money(row.original.stockValue)}</strong>,
      },
      {
        accessorKey: 'stockState',
        header: 'Định mức',
        cell: ({ row }) => (
          <Badge
            variant={
              row.original.stockState === 'Đủ tồn'
                ? 'success'
                : row.original.stockState === 'Hết hàng'
                  ? 'danger'
                  : 'warning'
            }
          >
            {row.original.stockState}
          </Badge>
        ),
      },
    ],
    [],
  );

  const sections = {
    receivables: {
      title: 'III. CHI TIẾT HÓA ĐƠN CÒN PHẢI THU KHÁCH HÀNG',
      file: 'cong-no-khach-hang',
      headers: [
        'Hóa đơn',
        'Mã KH',
        'Khách hàng',
        'SĐT',
        'Lệnh sửa chữa',
        'Ngày phát hành',
        'Hạn thanh toán',
        'Số ngày quá hạn',
        'Tổng tiền',
        'Đã thu',
        'Còn phải thu',
      ],
      rows: receivables.map<CsvRow>((r) => [
        r.invoiceCode,
        r.customerCode,
        r.customerName,
        r.phone,
        r.repairOrderId,
        csvDate(r.issuedAt),
        csvDate(r.dueDate),
        r.overdueDays,
        csvMoney(r.totalAmount),
        csvMoney(r.paidAmount),
        csvMoney(r.balanceAmount),
      ]),
    },
    payables: {
      title: 'IV. CHI TIẾT CÔNG NỢ PHẢI TRẢ NHÀ CUNG CẤP',
      file: 'cong-no-nha-cung-cap',
      headers: ['Mã NCC', 'Nhà cung cấp', 'SĐT', 'Kỳ hạn (ngày)', 'Tổng nhập', 'Đã trả', 'Còn nợ'],
      rows: payables.map<CsvRow>((s) => [
        s.supplierCode,
        s.supplierName,
        s.phone,
        s.paymentTermDays,
        csvMoney(s.totalPurchased),
        csvMoney(s.totalPaid),
        csvMoney(s.balanceAmount),
      ]),
    },
    cashflow: {
      title: 'V. CHI TIẾT GIAO DỊCH THU - CHI TRONG KỲ',
      file: 'so-quy-thu-chi',
      headers: [
        'Mã giao dịch',
        'Loại',
        'Chứng từ gốc',
        'Phương thức',
        'Thời gian',
        'Người ghi nhận',
        'Số tiền',
      ],
      rows: periodPayments.map<CsvRow>((p) => [
        p.transactionCode,
        p.direction === 'IN' ? 'Phiếu thu' : 'Phiếu chi',
        p.invoiceCode ?? p.documentCode,
        methodLabels[p.paymentMethod] ?? p.paymentMethod,
        csvDateTime(p.paidAt),
        p.recordedByName,
        csvMoney(p.amount),
      ]),
    },
    inventory: {
      title: 'VI. BÁO CÁO TỒN KHO & GIÁ TRỊ VẬT TƯ PHỤ TÙNG',
      file: 'bao-cao-ton-kho',
      headers: [
        'Mã SKU',
        'Tên phụ tùng',
        'ĐVT',
        'Tồn kho',
        'Đang giữ chỗ',
        'Khả dụng',
        'Đơn giá nhập',
        'Giá trị tồn',
        'Định mức',
      ],
      rows: inventory.map<CsvRow>((p) => [
        p.sku,
        p.partName,
        p.unit,
        Number(p.stockQuantity),
        Number(p.reservedQuantity),
        Number(p.availableQuantity),
        csvMoney(p.purchasePrice),
        p.stockValue,
        p.stockState,
      ]),
    },
  } satisfies Record<TabKey, { title: string; file: string; headers: string[]; rows: CsvRow[] }>;

  const periodLabel = applied
    ? `${csvDate(applied.from)}_den_${csvDate(new Date(new Date(applied.until).getTime() - 1).toISOString())}`.replace(/\//g, '-')
    : '';

  const exportFull = () => {
    if (!applied || !dashboard.data) return;
    const d = dashboard.data;
    downloadCsv(
      `bao-cao-tong-hop-gara-${periodLabel}`,
      csvSections([
        {
          title: 'I. THÔNG TIN KỲ BÁO CÁO & THỜI ĐIỂM KẾT XUẤT',
          headers: ['Chỉ tiêu', 'Giá trị'],
          rows: [
            ['Từ', csvDateTime(d.from)],
            ['Đến (không bao gồm)', csvDateTime(d.until)],
            ['Múi giờ', policy.garageTimeZone],
            ['Thời điểm kết xuất', csvDateTime(new Date().toISOString())],
          ],
        },
        {
          title: 'II. TỔNG HỢP KẾT QUẢ HOẠT ĐỘNG TRONG KỲ & SỐ DƯ HIỆN TẠI',
          headers: ['Chỉ tiêu', 'Giá trị'],
          rows: [
            ['Doanh thu phát hành', csvMoney(d.revenue)],
            ['Tiền thực thu', csvMoney(d.cashIn)],
            ['Tiền thực chi', csvMoney(d.cashOut)],
            ['Dòng tiền thuần', csvMoney(d.cashIn) - csvMoney(d.cashOut)],
            ['Giờ công kỹ thuật', Number(d.laborHours)],
            ['Phải thu khách hàng', csvMoney(d.customerDebt)],
            ['Phải trả nhà cung cấp', csvMoney(d.supplierDebt)],
            ['Giá trị tồn kho', csvMoney(d.stockValue)],
          ],
        },
        ...(['receivables', 'payables', 'cashflow', 'inventory'] as const).map((k) => ({
          title: sections[k].title,
          headers: sections[k].headers,
          rows: sections[k].rows,
        })),
      ]),
    );
    notify('Đã tải báo cáo tổng hợp CSV.');
  };

  const exportTab = () => {
    const s = sections[tab];
    downloadCsv(s.file, csvTable(s.headers, s.rows));
  };

  const tabQuery =
    tab === 'receivables'
      ? customerDebts
      : tab === 'payables'
        ? supplierDebts
        : tab === 'cashflow'
          ? payments
          : parts;
  const tabRows: unknown[] =
    tab === 'receivables'
      ? receivables
      : tab === 'payables'
        ? payables
        : tab === 'cashflow'
          ? periodPayments
          : inventory;
  const slice = <T,>(rows: T[]) => rows.slice(page * size, page * size + size);
  const pageInfo = {
    totalElements: String(tabRows.length),
    totalPages: Math.max(1, Math.ceil(tabRows.length / size)),
  };

  const d = dashboard.data;
  const netCash = d ? csvMoney(d.cashIn) - csvMoney(d.cashOut) : 0;
  const collectRate = d && csvMoney(d.revenue) > 0 ? Math.round((csvMoney(d.cashIn) / csvMoney(d.revenue)) * 100) : 0;

  return (
    <>
      <PageHeader
        eyebrow="TỔNG QUAN VẬN HÀNH"
        title="Báo cáo"
        description="Theo dõi kết quả trong kỳ, số dư hiện tại và xuất báo cáo CSV cho kế toán."
        actions={
          <div className="row-actions">
            <Button
              variant="primary"
              disabled={!applied || !d}
              onClick={exportFull}
            >
              <Download size={17} />
              Xuất báo cáo CSV đầy đủ
            </Button>
          </div>
        }
      />
      <section className="panel report-filter">
        <form
          className="date-filter"
          onSubmit={(event) => {
            event.preventDefault();
            if (checked.range) setApplied(checked.range);
          }}
        >
          <div className="filter-intro">
            <CalendarDays size={20} />
            <div>
              <strong>Kỳ báo cáo</strong>
              <span>Múi giờ {policy.garageTimeZone}</span>
            </div>
          </div>
          <Field label="Từ ngày" required>
            <input
              className="input"
              type="date"
              value={dates.from}
              onChange={(event) => setDates({ ...dates, from: event.target.value })}
            />
          </Field>
          <Field label="Đến hết ngày" required>
            <input
              className="input"
              type="date"
              value={dates.through}
              onChange={(event) => setDates({ ...dates, through: event.target.value })}
            />
          </Field>
          <Button
            type="submit"
            variant="primary"
            disabled={!checked.range}
            pending={dashboard.isFetching}
          >
            Áp dụng
          </Button>
          <Button
            aria-label="Làm mới báo cáo"
            disabled={!applied}
            onClick={() => {
              void dashboard.refetch();
              void customerDebts.refetch();
              void supplierDebts.refetch();
              void payments.refetch();
              void parts.refetch();
            }}
          >
            <RefreshCw size={17} />
          </Button>
        </form>
        <div className="report-presets">
          <span>Chọn nhanh:</span>
          {presets.map((p) => {
            const range = presetDates(p.key, policy.garageTimeZone);
            return (
              <Button
                key={p.key}
                variant="ghost"
                disabled={!parseRange(range.from, range.through, policy).range}
                onClick={() => setDates(range)}
              >
                {p.label}
              </Button>
            );
          })}
          <Button
            variant="ghost"
            disabled={!parseRange(initial.from, initial.through, policy).range}
            onClick={() => setDates(initial)}
          >
            Kỳ mặc định
          </Button>
        </div>
        {checked.error && <FormError message={checked.error} />}
        <p className="field-hint">
          Ngày kết thúc bao gồm trọn ngày. Giới hạn kỳ được lấy từ cấu hình hệ thống.
        </p>
      </section>
      {!applied ? (
        <div className="alert" role="status">
          Chính sách hiện tại không cho phép khoảng ngày đã chọn. Hãy chọn lại kỳ báo cáo.
        </div>
      ) : dashboard.isPending ? (
        <Loading />
      ) : dashboard.error ? (
        <QueryError
          error={dashboard.error}
          retry={() => {
            void dashboard.refetch();
          }}
        />
      ) : (
        <>
          <div className="section-heading">
            <div>
              <h2>Kết quả trong kỳ</h2>
              <p>
                {dateTime(dashboard.data.from, policy.garageTimeZone)} → trước{' '}
                {dateTime(dashboard.data.until, policy.garageTimeZone)}
              </p>
            </div>
            <span className="subtle-label">VND · Dữ liệu đã ghi nhận</span>
          </div>
          <MetricGrid
            label="Kết quả trong kỳ"
            items={[
              {
                label: 'Doanh thu',
                value: money(dashboard.data.revenue),
                hint: 'Doanh thu ghi nhận trong kỳ',
                icon: <TrendingUp size={18} />,
              },
              {
                label: 'Tiền thu',
                value: money(dashboard.data.cashIn),
                hint: `${collectRate}% doanh thu đã thực thu`,
                tone: 'success',
                icon: <ArrowDownLeft size={18} />,
              },
              {
                label: 'Tiền chi',
                value: money(dashboard.data.cashOut),
                hint: 'Dòng tiền ra trong kỳ',
                tone: 'danger',
                icon: <ArrowUpRight size={18} />,
              },
              {
                label: 'Dòng tiền thuần',
                value: money(netCash),
                hint: netCash >= 0 ? 'Thu nhiều hơn chi' : 'Chi nhiều hơn thu',
                tone: netCash >= 0 ? 'success' : 'danger',
                icon: <Scale size={18} />,
              },
              {
                label: 'Giờ công',
                value: hours(dashboard.data.laborHours),
                hint: 'Tổng giờ công trong kỳ',
                tone: 'info',
                icon: <Clock3 size={18} />,
              },
            ]}
          />
          <div className="section-heading">
            <div>
              <h2>Số dư tại thời điểm hiện tại</h2>
              <p>
                Chụp lúc {dateTime(dashboard.data.snapshotAt, policy.garageTimeZone)}. Các số dư này
                không lọc theo kỳ phía trên.
              </p>
            </div>
          </div>
          <MetricGrid
            label="Số dư hiện tại"
            items={[
              {
                label: 'Khách hàng còn nợ',
                value: money(dashboard.data.customerDebt),
                hint: 'Tổng công nợ phải thu',
                tone: 'warning',
                icon: <Users size={18} />,
              },
              {
                label: 'Nợ nhà cung cấp',
                value: money(dashboard.data.supplierDebt),
                hint: 'Tổng công nợ phải trả',
                tone: 'danger',
                icon: <Truck size={18} />,
              },
              {
                label: 'Giá trị tồn kho',
                value: money(dashboard.data.stockValue),
                hint: 'Giá trị vật tư hiện có',
                icon: <Package size={18} />,
              },
              {
                label: 'Công nợ ròng',
                value: money(csvMoney(dashboard.data.customerDebt) - csvMoney(dashboard.data.supplierDebt)),
                hint: 'Phải thu − phải trả',
                tone: 'info',
                icon: <Scale size={18} />,
              },
            ]}
          />
        </>
      )}
      <section className="panel table-panel debt-panel">
        <div className="panel-heading">
          <div>
            <h2>{tabLabels[tab]}</h2>
            <p>
              {tab === 'cashflow'
                ? 'Giao dịch phát sinh trong kỳ báo cáo đã chọn.'
                : 'Dữ liệu hiện tại, độc lập với kỳ báo cáo.'}
            </p>
          </div>
          <Button variant="secondary" disabled={tabRows.length === 0} onClick={exportTab}>
            <Download size={15} /> Xuất CSV bảng này
          </Button>
        </div>
        <div className="tabs" role="group" aria-label="Chọn bảng báo cáo">
          {(Object.keys(tabLabels) as TabKey[]).map((key) => (
            <button
              key={key}
              type="button"
              aria-pressed={tab === key}
              onClick={() => {
                setTab(key);
                setPage(0);
              }}
            >
              {tabLabels[key]}
            </button>
          ))}
        </div>
        {tabQuery.isPending ? (
          <Loading />
        ) : tabQuery.error ? (
          <QueryError
            error={tabQuery.error}
            retry={() => {
              void tabQuery.refetch();
            }}
          />
        ) : tabRows.length === 0 ? (
          <p className="empty-state">Không có dữ liệu cho bảng này.</p>
        ) : (
          <>
            {tab === 'receivables' && (
              <DataTable
                caption="Hóa đơn còn phải thu"
                columns={receivableColumns}
                data={slice(receivables)}
              />
            )}
            {tab === 'payables' && (
              <DataTable
                caption="Công nợ nhà cung cấp"
                columns={payableColumns}
                data={slice(payables)}
              />
            )}
            {tab === 'cashflow' && (
              <DataTable caption="Giao dịch thu chi" columns={cashColumns} data={slice(periodPayments)} />
            )}
            {tab === 'inventory' && (
              <DataTable
                caption="Tồn kho và giá trị vật tư"
                columns={inventoryColumns}
                data={slice(inventory)}
              />
            )}
            <Pagination
              page={page}
              size={size}
              data={pageInfo}
              policy={policy}
              onChange={(nextPage, nextSize) => {
                setPage(nextPage);
                setSize(nextSize);
              }}
            />
          </>
        )}
      </section>
    </>
  );
}

export function ReportsPage() {
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
    <ReadyReports policy={policy.data} />
  );
}

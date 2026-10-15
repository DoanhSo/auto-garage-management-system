import { useMemo, useState } from 'react';
import { ArrowDownLeft, ArrowUpRight, CalendarDays, Download, RefreshCw } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useUiPolicy } from '../../../shared/config/policy';
import {
  Button,
  Field,
  FormError,
  Loading,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { DataTable, Pagination } from '../../../shared/components/DataTable';
import { useTableSearchParams } from '../../../shared/hooks/useTableSearchParams';
import {
  dateRange,
  initialReportDates,
  reportFileName,
  today,
  type ReportRange,
} from '../../../shared/utils/dates';
import { dateTime, hours, money } from '../../../shared/utils/format';
import { errorMessage } from '../../../shared/api/errors';
import type { InvoiceBalance, UiPolicy } from '../../../shared/types/contracts';
import { useDashboardQuery, useDebtsQuery, useExportMutation } from '../hooks/useReports';
import { useToast } from '../../../app/providers/ToastProvider';

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
function ReadyReports({ policy }: { policy: UiPolicy }) {
  const table = useTableSearchParams(policy);
  const notify = useToast();
  const initial = useMemo(
    () => initialReportDates(policy.garageTimeZone, policy.reportMaxPeriodSeconds),
    [policy],
  );
  const [dates, setDates] = useState(initial);
  const [applied, setApplied] = useState(
    () => parseRange(initial.from, initial.through, policy).range,
  );
  const checked = parseRange(dates.from, dates.through, policy);
  const dashboard = useDashboardQuery(applied);
  const debts = useDebtsQuery(table.page, table.size);
  const csv = useExportMutation();
  const columns = useMemo<ColumnDef<InvoiceBalance>[]>(
    () => [
      {
        accessorKey: 'invoiceCode',
        header: 'Hóa đơn',
        cell: ({ row }) => <strong className="code-cell">{row.original.invoiceCode}</strong>,
      },
      {
        accessorKey: 'customerId',
        header: 'Mã khách hàng',
        cell: ({ row }) => `#${row.original.customerId}`,
      },
      {
        accessorKey: 'totalAmount',
        header: 'Tổng tiền',
        cell: ({ row }) => <span className="money">{money(row.original.totalAmount)}</span>,
      },
      {
        accessorKey: 'paidAmount',
        header: 'Đã thanh toán',
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
  const exportCsv = async () => {
    if (!applied || csv.isPending) return;
    try {
      const blob = await csv.mutateAsync(applied);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = reportFileName(applied, policy.garageTimeZone);
      document.body.append(link);
      link.click();
      link.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      notify('Đã tải báo cáo CSV.');
    } catch {
      /* Display the safe error below the filters. */
    }
  };
  return (
    <>
      <PageHeader
        eyebrow="TỔNG QUAN VẬN HÀNH"
        title="Báo cáo"
        description="Theo dõi kết quả trong kỳ và số dư hiện tại của gara."
        actions={
          <Button
            pending={csv.isPending}
            disabled={!applied || dashboard.isPending || !!dashboard.error}
            onClick={() => {
              void exportCsv();
            }}
          >
            <Download size={17} />
            Xuất CSV
          </Button>
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
              void debts.refetch();
            }}
          >
            <RefreshCw size={17} />
          </Button>
        </form>
        <div className="report-presets">
          <span>Chọn nhanh:</span>
          <Button
            variant="ghost"
            disabled={
              !parseRange(today(policy.garageTimeZone), today(policy.garageTimeZone), policy).range
            }
            onClick={() =>
              setDates({
                from: today(policy.garageTimeZone),
                through: today(policy.garageTimeZone),
              })
            }
          >
            Hôm nay
          </Button>
          <Button
            variant="ghost"
            disabled={!parseRange(initial.from, initial.through, policy).range}
            onClick={() => setDates(initial)}
          >
            Kỳ gần nhất
          </Button>
        </div>
        {checked.error && <FormError message={checked.error} />}
        <p className="field-hint">
          Ngày kết thúc bao gồm trọn ngày. Giới hạn kỳ được lấy từ cấu hình hệ thống.
        </p>
        {csv.error && <FormError message={errorMessage(csv.error)} />}
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
          <div className="metric-grid period-metrics">
            <Metric
              label="Doanh thu"
              value={money(dashboard.data.revenue)}
              description="Doanh thu ghi nhận trong kỳ"
            />
            <Metric
              label="Tiền thu"
              value={money(dashboard.data.cashIn)}
              description="Dòng tiền vào trong kỳ"
              icon={<ArrowDownLeft size={18} />}
            />
            <Metric
              label="Tiền chi"
              value={money(dashboard.data.cashOut)}
              description="Dòng tiền ra trong kỳ"
              icon={<ArrowUpRight size={18} />}
            />
            <Metric
              label="Giờ công"
              value={hours(dashboard.data.laborHours)}
              description="Tổng giờ công trong kỳ"
            />
          </div>
          <div className="section-heading">
            <div>
              <h2>Số dư tại thời điểm hiện tại</h2>
              <p>
                Chụp lúc {dateTime(dashboard.data.snapshotAt, policy.garageTimeZone)}. Các số dư này
                không lọc theo kỳ phía trên.
              </p>
            </div>
          </div>
          <div className="metric-grid snapshot-metrics">
            <Metric
              label="Khách hàng còn nợ"
              value={money(dashboard.data.customerDebt)}
              description="Tổng công nợ phải thu"
            />
            <Metric
              label="Nợ nhà cung cấp"
              value={money(dashboard.data.supplierDebt)}
              description="Tổng công nợ phải trả"
            />
            <Metric
              label="Giá trị tồn kho"
              value={money(dashboard.data.stockValue)}
              description="Giá trị vật tư hiện có"
            />
          </div>
        </>
      )}
      <section className="panel table-panel debt-panel">
        <div className="panel-heading">
          <div>
            <h2>Hóa đơn còn phải thu</h2>
            <p>Danh sách công nợ hiện tại, độc lập với kỳ báo cáo.</p>
          </div>
          <span className="badge badge-neutral">Chỉ xem</span>
        </div>
        {debts.isPending ? (
          <Loading />
        ) : debts.error ? (
          <QueryError
            error={debts.error}
            retry={() => {
              void debts.refetch();
            }}
          />
        ) : (
          <>
            <DataTable
              caption="Hóa đơn còn phải thu"
              columns={columns}
              data={debts.data.content}
              hideOnMobile={['totalAmount', 'paidAmount']}
            />
            <Pagination
              page={table.page}
              size={table.size}
              data={debts.data}
              policy={policy}
              disabled={debts.isFetching}
              onChange={(page, size) => table.update({ page, size })}
            />
          </>
        )}
      </section>
    </>
  );
}
function Metric({
  label,
  value,
  description,
  icon,
}: {
  label: string;
  value: string;
  description: string;
  icon?: React.ReactNode;
}) {
  return (
    <article className="metric-card">
      <div className="metric-label">
        {label}
        {icon && <span className="metric-icon">{icon}</span>}
      </div>
      <strong className="metric-value">{value}</strong>
      <p>{description}</p>
    </article>
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

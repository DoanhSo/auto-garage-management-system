import { useMemo, useState } from 'react';
import {
  Calculator,
  ChevronLeft,
  ChevronRight,
  Download,
  FilePlus2,
  RefreshCw,
  Trophy,
  Truck,
  Wallet,
  CalendarCheck,
} from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { DataTable } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import {
  Badge,
  Button,
  ConfirmDialog,
  Loading,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { csvMoney, csvSections, downloadCsv } from '../../../shared/utils/csvExport';
import { formatDate, money } from '../../../shared/utils/format';
import { useSchedulingEmployeesQuery } from '../../scheduling/hooks/useScheduling';
import { SKILL_LABELS, parseSkills } from '../../scheduling/types/scheduling';
import {
  useApprovePayrollMutation,
  useCalculatePayrollMutation,
  useCreatePayrollPeriodMutation,
  useDisbursePayrollMutation,
  usePayrollPeriodByMonthQuery,
} from '../hooks/usePayroll';
import {
  payrollItemStatusLabels,
  payrollStatusLabels,
  periodStatusVariant,
  type PayrollItem,
} from '../types/payroll';
import { EmployeeWorkSummaryModal } from './EmployeeWorkSummaryModal';
import { PayrollAdjustModal } from './PayrollAdjustModal';

type ItemRow = PayrollItem & { id: string };

const hoursText = (value: string | number) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

const pad = (n: number) => String(n).padStart(2, '0');
const monthKey = (year: number, month: number) => `${year}-${pad(month)}`;

export function PayrollManagerView({ canApprove }: { canApprove: boolean }) {
  const notify = useToast();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [keyword, setKeyword] = useState('');
  const [skill, setSkill] = useState('ALL');
  const [confirm, setConfirm] = useState<'approve' | 'disburse' | null>(null);
  const [summaryItem, setSummaryItem] = useState<PayrollItem | null>(null);
  const [editing, setEditing] = useState<PayrollItem | null>(null);

  const key = monthKey(year, month);
  const query = usePayrollPeriodByMonthQuery(key);
  const employees = useSchedulingEmployeesQuery({ active: true });
  const createMutation = useCreatePayrollPeriodMutation();
  const calculate = useCalculatePayrollMutation();
  const approve = useApprovePayrollMutation();
  const disburse = useDisbursePayrollMutation();

  const period = query.data?.period;
  const editable = period?.status === 'DRAFT' || period?.status === 'CALCULATED';
  const busy =
    createMutation.isPending || calculate.isPending || approve.isPending || disburse.isPending;

  const skillsByEmployee = useMemo(
    () =>
      new Map(
        (employees.data ?? []).map((e) => [String(e.id), parseSkills(e.skillsJson)] as const),
      ),
    [employees.data],
  );
  const canQcByEmployee = useMemo(
    () => new Map((employees.data ?? []).map((e) => [String(e.id), !!e.canQc] as const)),
    [employees.data],
  );

  const coefficientByEmployee = useMemo(
    () =>
      new Map(
        (employees.data ?? []).map(
          (e) => [String(e.id), Number(e.salaryCoefficient ?? 1)] as const,
        ),
      ),
    [employees.data],
  );

  const bankByEmployee = useMemo(
    () =>
      new Map(
        (employees.data ?? []).map(
          (e) =>
            [
              String(e.id),
              e.bankAccountNumber ? `${e.bankName ?? 'Ngân hàng'} · ${e.bankAccountNumber}` : null,
            ] as const,
        ),
      ),
    [employees.data],
  );

  const items = useMemo(() => query.data?.items ?? [], [query.data?.items]);
  const rows: ItemRow[] = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    return items.filter((item) => {
      if (skill !== 'ALL') {
        const skills = skillsByEmployee.get(String(item.employeeId)) ?? [];
        const qc = skill === 'QC' && canQcByEmployee.get(String(item.employeeId));
        if (!skills.includes(skill) && !qc) return false;
      }
      return (
        !term ||
        `${item.employeeName ?? ''} ${item.employeeCode ?? ''}`.toLowerCase().includes(term)
      );
    });
  }, [items, keyword, skill, skillsByEmployee, canQcByEmployee]);

  const totalShifts = items.reduce((sum, item) => sum + item.shiftCount, 0);
  const best = items.reduce<PayrollItem | null>(
    (top, item) => (item.completedOrderCount > (top?.completedOrderCount ?? 0) ? item : top),
    null,
  );

  const shiftMonth = (delta: number) => {
    const next = new Date(year, month - 1 + delta, 1);
    setYear(next.getFullYear());
    setMonth(next.getMonth() + 1);
  };
  const isCurrentMonth = year === now.getFullYear() && month === now.getMonth() + 1;
  const years = [
    now.getFullYear() + 1,
    now.getFullYear(),
    now.getFullYear() - 1,
    now.getFullYear() - 2,
  ];

  const run = async (action: () => Promise<unknown>, success: string) => {
    try {
      await action();
      notify(success);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const exportCsv = () => {
    if (!period) return;
    downloadCsv(
      `bang-luong-${period.periodCode}`,
      csvSections([
        {
          title: `Bảng lương ${period.periodCode} (${formatDate(period.fromDate)} - ${formatDate(period.toDate)})`,
          headers: [
            'Mã NV',
            'Họ tên',
            'Giờ thường',
            'Giờ tăng ca',
            'Số ca',
            'Số đơn',
            'Lương cứng',
            'Lương giờ',
            'Thưởng đơn & hoa hồng',
            'Phụ cấp',
            'Giảm trừ',
            'Thực nhận',
          ],
          rows: items.map((item) => [
            item.employeeCode,
            item.employeeName,
            Number(item.regularHours),
            Number(item.overtimeHours),
            item.shiftCount,
            item.completedOrderCount,
            csvMoney(item.baseSalarySnapshot),
            csvMoney(item.hourlyPayAmount),
            csvMoney(item.commissionAmount),
            csvMoney(item.allowanceAmount),
            csvMoney(item.deductionAmount),
            csvMoney(item.netPay),
          ]),
        },
      ]),
    );
  };

  const columns: ColumnDef<ItemRow>[] = [
    {
      id: 'employee',
      header: 'Nhân viên',
      cell: ({ row }) => {
        const id = String(row.original.employeeId);
        const skills = skillsByEmployee.get(id) ?? [];
        return (
          <div>
            <strong>{row.original.employeeName ?? '—'}</strong>
            <div className="description">
              {row.original.employeeCode}
              {skills.length > 0 && ` · ${skills.map((s) => SKILL_LABELS[s] ?? s).join(', ')}`}
              {canQcByEmployee.get(id) ? ' · QC' : ''}
            </div>
            {bankByEmployee.get(id) && (
              <div className="description">TK: {bankByEmployee.get(id)}</div>
            )}
            <div className="description">Hệ số lương {coefficientByEmployee.get(id) ?? 1}x</div>
          </div>
        );
      },
    },
    {
      id: 'shifts',
      header: 'Ca trực',
      cell: ({ row }) => <span className="tabular">{row.original.shiftCount}</span>,
    },
    {
      id: 'orders',
      header: 'Đơn sửa xong',
      cell: ({ row }) => (
        <Badge variant={row.original.completedOrderCount > 0 ? 'success' : 'secondary'}>
          {row.original.completedOrderCount} xe
        </Badge>
      ),
    },
    {
      id: 'hours',
      header: 'Giờ công thực tế',
      cell: ({ row }) => (
        <span className="tabular">
          {hoursText(Number(row.original.regularHours) + Number(row.original.overtimeHours))}
          {Number(row.original.overtimeHours) > 0 &&
            ` (TC ${hoursText(row.original.overtimeHours)})`}
        </span>
      ),
    },
    {
      id: 'base',
      header: 'Lương cơ bản',
      cell: ({ row }) => <span className="tabular">{money(row.original.baseSalarySnapshot)}</span>,
    },
    {
      id: 'hourly',
      header: 'Lương giờ công',
      cell: ({ row }) => <span className="tabular">{money(row.original.hourlyPayAmount)}</span>,
    },
    {
      id: 'bonus',
      header: 'Thưởng đơn & hoa hồng',
      cell: ({ row }) => <span className="tabular">{money(row.original.commissionAmount)}</span>,
    },
    {
      id: 'adjust',
      header: 'Phụ cấp / Giảm trừ',
      cell: ({ row }) => (
        <span className="tabular">
          +{money(row.original.allowanceAmount)} / −{money(row.original.deductionAmount)}
        </span>
      ),
    },
    {
      id: 'net',
      header: 'Thực nhận',
      cell: ({ row }) => <strong className="tabular net-pay">{money(row.original.netPay)}</strong>,
    },
    {
      id: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => <Badge>{payrollItemStatusLabels[row.original.status]}</Badge>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="row-actions">
          <Button variant="ghost" onClick={() => setSummaryItem(row.original)}>
            Xe & ca
          </Button>
          {editable && (
            <Button variant="secondary" onClick={() => setEditing(row.original)}>
              Điều chỉnh
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="page">
      <PageHeader
        eyebrow="Tài chính & công nợ"
        title="Bảng lương & Năng suất nhân viên"
        description="Theo dõi ca trực, năng suất sửa chữa và thu nhập chi tiết của từng nhân viên theo tháng."
      />

      <section className="panel" aria-label="Chọn kỳ lương">
        <div className="row-actions">
          <Button variant="secondary" aria-label="Tháng trước" onClick={() => shiftMonth(-1)}>
            <ChevronLeft size={16} />
          </Button>
          <select
            className="select"
            aria-label="Tháng"
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>
                Tháng {pad(m)}
              </option>
            ))}
          </select>
          <select
            className="select"
            aria-label="Năm"
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
          >
            {years.map((y) => (
              <option key={y} value={y}>
                Năm {y}
              </option>
            ))}
          </select>
          <Button variant="secondary" aria-label="Tháng sau" onClick={() => shiftMonth(1)}>
            <ChevronRight size={16} />
          </Button>
          <Button
            variant="ghost"
            disabled={isCurrentMonth}
            onClick={() => {
              setYear(now.getFullYear());
              setMonth(now.getMonth() + 1);
            }}
          >
            Tháng hiện tại
          </Button>
          <span className="grow" />
          {period && (
            <Badge variant={periodStatusVariant[period.status]}>
              {payrollStatusLabels[period.status]}
            </Badge>
          )}
          {period && editable && (
            <Button
              variant="secondary"
              pending={calculate.isPending}
              disabled={busy}
              onClick={() =>
                void run(
                  () => calculate.mutateAsync(period.id),
                  `Đã tính lương ${pad(month)}/${year}`,
                )
              }
            >
              <RefreshCw size={16} /> {period.status === 'DRAFT' ? 'Tính lương' : 'Tính lại lương'}
            </Button>
          )}
          {period && canApprove && period.status === 'CALCULATED' && (
            <Button variant="secondary" disabled={busy} onClick={() => setConfirm('approve')}>
              <Calculator size={16} /> Phê duyệt
            </Button>
          )}
          {period && canApprove && period.status === 'APPROVED' && (
            <Button variant="primary" disabled={busy} onClick={() => setConfirm('disburse')}>
              <Wallet size={16} /> Chi trả lương
            </Button>
          )}
          {period && (
            <Button variant="secondary" disabled={items.length === 0} onClick={exportCsv}>
              <Download size={16} /> Xuất CSV
            </Button>
          )}
        </div>
      </section>

      {query.isLoading && <Loading />}
      {query.isError && <QueryError error={query.error} retry={() => void query.refetch()} />}

      {query.isSuccess && !period && (
        <section className="panel stack" aria-label="Chưa có kỳ lương">
          <p className="description">
            Chưa có kỳ tính lương cho tháng {pad(month)}/{year}. Khởi tạo kỳ để hệ thống tổng hợp ca
            trực, giờ công và thưởng đơn của tất cả nhân viên.
          </p>
          <div>
            <Button
              variant="primary"
              pending={createMutation.isPending}
              onClick={() =>
                void run(
                  () => createMutation.mutateAsync({ periodMonth: key }),
                  `Đã khởi tạo kỳ lương ${pad(month)}/${year}`,
                )
              }
            >
              <FilePlus2 size={16} /> Khởi tạo kỳ tính lương Tháng {pad(month)}/{year}
            </Button>
          </div>
        </section>
      )}

      {period && (
        <>
          <MetricGrid
            label={`Thống kê năng suất tháng ${pad(month)}/${year}`}
            items={[
              {
                label: 'Tổng quỹ lương tháng',
                value: money(period.totalNetPay),
                icon: <Wallet size={16} />,
                tone: 'success',
              },
              {
                label: 'Tổng ca trực khoang',
                value: `${totalShifts} ca`,
                icon: <CalendarCheck size={16} />,
              },
              {
                label: 'Đơn xe sửa xong',
                value: `${period.completedOrderCount} xe`,
                icon: <Truck size={16} />,
              },
              {
                label: 'Thợ xuất sắc nhất tháng',
                value: best ? (best.employeeName ?? best.employeeCode) : '—',
                hint: best ? `${best.completedOrderCount} xe hoàn thành` : 'Chưa có đơn hoàn thành',
                icon: <Trophy size={16} />,
              },
            ]}
          />

          <section className="panel table-panel">
            <div className="row-actions" role="search" aria-label="Lọc nhân viên">
              <input
                className="input"
                aria-label="Tìm nhân viên"
                placeholder="Tìm theo tên hoặc mã nhân viên…"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
              <select
                className="select"
                aria-label="Lọc theo chuyên môn"
                value={skill}
                onChange={(e) => setSkill(e.target.value)}
              >
                <option value="ALL">Tất cả chuyên môn</option>
                {Object.entries(SKILL_LABELS).map(([k, label]) => (
                  <option key={k} value={k}>
                    {label}
                  </option>
                ))}
                <option value="QC">QC</option>
              </select>
              <span className="description">
                Kỳ công {formatDate(period.fromDate)} – {formatDate(period.toDate)}
              </span>
            </div>
            <DataTable
              caption="Bảng lương và năng suất từng nhân viên"
              columns={columns}
              data={rows}
              busy={query.isFetching}
              hideOnMobile={['shifts', 'hours', 'base', 'hourly', 'bonus', 'adjust', 'status']}
            />
          </section>
        </>
      )}

      <EmployeeWorkSummaryModal
        employeeId={summaryItem?.employeeId ?? null}
        employeeName={summaryItem?.employeeName}
        month={period?.periodMonth ?? ''}
        onClose={() => setSummaryItem(null)}
      />
      <PayrollAdjustModal item={editing} onClose={() => setEditing(null)} />

      <ConfirmDialog
        open={confirm !== null}
        title={confirm === 'approve' ? 'Phê duyệt bảng lương?' : 'Chi trả lương và ghi sổ quỹ?'}
        description={
          confirm === 'approve'
            ? 'Sau khi phê duyệt, bảng lương không thể tính lại hoặc chỉnh sửa.'
            : `Hệ thống sẽ ghi phiếu chi ${period ? money(period.totalNetPay) : ''} vào Sổ quỹ Thu / Chi và đánh dấu kỳ lương đã chi trả.`
        }
        confirmLabel={confirm === 'approve' ? 'Phê duyệt' : 'Chi trả'}
        pending={approve.isPending || disburse.isPending}
        onCancel={() => setConfirm(null)}
        onConfirm={() => {
          if (!period) return;
          const action = confirm;
          void run(
            () =>
              action === 'approve'
                ? approve.mutateAsync(period.id)
                : disburse.mutateAsync(period.id),
            action === 'approve' ? 'Đã phê duyệt bảng lương' : 'Đã chi trả lương và ghi sổ quỹ',
          ).then(() => setConfirm(null));
        }}
      />
    </div>
  );
}

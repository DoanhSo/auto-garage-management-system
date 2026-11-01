import { useMemo, useState } from 'react';
import { Banknote, Clock, Eye, FilePlus2, Truck, Wallet } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { DataTable } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import {
  Badge,
  Button,
  Field,
  FormError,
  Loading,
  Modal,
  PageHeader,
  QueryError,
} from '../../../shared/components/ui';
import { formatDate, money } from '../../../shared/utils/format';
import { MyPayslipModal } from '../components/MyPayslipModal';
import { PayrollDetailModal, periodStatusVariant } from '../components/PayrollDetailModal';
import {
  useCreatePayrollPeriodMutation,
  useMyPayslipsQuery,
  usePayrollPeriodsQuery,
} from '../hooks/usePayroll';
import { payrollStatusLabels, type PayrollPeriod, type Payslip } from '../types/payroll';

type PeriodRow = PayrollPeriod & { id: string };
type PayslipRow = Payslip & { id: string };

const hoursText = (value: string | number) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

function currentMonth(): string {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
}

export function PayrollPage() {
  const { user } = useAuth();
  const manager = user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST';
  return manager ? <PayrollManagerView canApprove={user?.role === 'ADMIN'} /> : <MyPayslipsView />;
}

function PayrollManagerView({ canApprove }: { canApprove: boolean }) {
  const notify = useToast();
  const thisYear = new Date().getFullYear();
  const [year, setYear] = useState(thisYear);
  const [detailId, setDetailId] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [month, setMonth] = useState(currentMonth());
  const [notes, setNotes] = useState('');

  const periods = usePayrollPeriodsQuery({ year, size: 24 });
  const createMutation = useCreatePayrollPeriodMutation();
  const rows: PeriodRow[] = useMemo(() => periods.data?.content ?? [], [periods.data?.content]);
  const latest = rows[0];

  const columns: ColumnDef<PeriodRow>[] = [
    { id: 'code', header: 'Mã kỳ', cell: ({ row }) => <strong>{row.original.periodCode}</strong> },
    { id: 'month', header: 'Tháng', cell: ({ row }) => row.original.periodMonth },
    {
      id: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => (
        <Badge variant={periodStatusVariant[row.original.status]}>
          {payrollStatusLabels[row.original.status]}
        </Badge>
      ),
    },
    { id: 'employees', header: 'Số NV', cell: ({ row }) => row.original.totalEmployees },
    {
      id: 'net',
      header: 'Tổng thực chi',
      cell: ({ row }) => <strong className="tabular">{money(row.original.totalNetPay)}</strong>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button variant="secondary" onClick={() => setDetailId(row.original.id)}>
          <Eye size={16} /> Chi tiết
        </Button>
      ),
    },
  ];

  const create = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) {
      notify('Chọn tháng tính lương hợp lệ');
      return;
    }
    try {
      const created = await createMutation.mutateAsync({
        periodMonth: month,
        notes: notes.trim() || undefined,
      });
      notify(`Đã khởi tạo kỳ lương ${created.periodCode}`);
      setCreating(false);
      setNotes('');
      setDetailId(created.id);
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  return (
    <div className="page">
      <PageHeader
        eyebrow="Tài chính & công nợ"
        title="Bảng lương & Năng suất nhân viên"
        description="Tổng hợp giờ công, thưởng theo đơn hoàn thành và chi trả lương theo kỳ."
        actions={
          <>
            <select
              className="select"
              aria-label="Năm"
              value={year}
              onChange={(e) => setYear(Number(e.target.value))}
            >
              {[thisYear, thisYear - 1, thisYear - 2, thisYear - 3].map((y) => (
                <option key={y} value={y}>
                  Năm {y}
                </option>
              ))}
            </select>
            <Button variant="primary" onClick={() => setCreating(true)}>
              <FilePlus2 size={16} /> Khởi tạo kỳ tính lương
            </Button>
          </>
        }
      />

      {latest && (
        <MetricGrid
          label={`Tổng hợp kỳ ${latest.periodCode}`}
          items={[
            {
              label: 'Tổng giờ công thực tế',
              value: hoursText(
                Number(latest.totalRegularHours) + Number(latest.totalOvertimeHours),
              ),
              icon: <Clock size={16} />,
            },
            {
              label: 'Đơn hoàn thành',
              value: `${latest.completedOrderCount} xe`,
              icon: <Truck size={16} />,
            },
            {
              label: 'Hoa hồng & thưởng',
              value: money(latest.totalCommissions),
              icon: <Banknote size={16} />,
            },
            {
              label: 'Ngân sách lương thực chi',
              value: money(latest.totalNetPay),
              icon: <Wallet size={16} />,
              tone: 'success',
            },
          ]}
        />
      )}

      {periods.isLoading && <Loading />}
      {periods.isError && <QueryError error={periods.error} retry={() => void periods.refetch()} />}
      {periods.data && (
        <DataTable
          caption="Danh sách kỳ tính lương"
          columns={columns}
          data={rows}
          busy={periods.isFetching}
          hideOnMobile={['month', 'employees']}
        />
      )}

      <PayrollDetailModal
        periodId={detailId}
        canApprove={canApprove}
        onClose={() => setDetailId(null)}
      />

      <Modal
        open={creating}
        title="Khởi tạo kỳ tính lương"
        description="Kỳ công mặc định từ ngày đầu đến ngày cuối của tháng đã chọn."
        onOpenChange={(open) => !open && !createMutation.isPending && setCreating(false)}
      >
        <form onSubmit={create} noValidate className="editor-form account-editor-form">
          <FormError
            message={createMutation.error ? errorMessage(createMutation.error) : undefined}
          />
          <Field label="Tháng tính lương" required>
            <input
              type="month"
              className="input"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
              disabled={createMutation.isPending}
            />
          </Field>
          <div className="full-width-field">
            <Field label="Ghi chú">
              <input
                className="input"
                maxLength={2000}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                disabled={createMutation.isPending}
              />
            </Field>
          </div>
          <div className="form-actions account-editor-actions">
            <Button onClick={() => setCreating(false)} disabled={createMutation.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={createMutation.isPending}>
              Khởi tạo kỳ lương
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

function MyPayslipsView() {
  const payslips = useMyPayslipsQuery();
  const [selected, setSelected] = useState<Payslip | null>(null);
  const rows: PayslipRow[] = useMemo(
    () => (payslips.data?.content ?? []).map((slip) => ({ ...slip, id: slip.item.id })),
    [payslips.data?.content],
  );

  const columns: ColumnDef<PayslipRow>[] = [
    {
      id: 'period',
      header: 'Kỳ lương',
      cell: ({ row }) => <strong>{row.original.periodCode}</strong>,
    },
    {
      id: 'range',
      header: 'Kỳ công',
      cell: ({ row }) =>
        `${formatDate(row.original.fromDate)} – ${formatDate(row.original.toDate)}`,
    },
    {
      id: 'hours',
      header: 'Giờ công',
      cell: ({ row }) => (
        <span className="tabular">
          {hoursText(
            Number(row.original.item.regularHours) + Number(row.original.item.overtimeHours),
          )}
        </span>
      ),
    },
    {
      id: 'status',
      header: 'Trạng thái',
      cell: ({ row }) => (
        <Badge variant={periodStatusVariant[row.original.periodStatus]}>
          {payrollStatusLabels[row.original.periodStatus]}
        </Badge>
      ),
    },
    {
      id: 'net',
      header: 'Thực lĩnh',
      cell: ({ row }) => <strong className="tabular">{money(row.original.item.netPay)}</strong>,
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button variant="secondary" onClick={() => setSelected(row.original)}>
          <Eye size={16} /> Xem phiếu
        </Button>
      ),
    },
  ];

  return (
    <div className="page">
      <PageHeader
        eyebrow="Cá nhân"
        title="Phiếu lương của tôi"
        description="Thu nhập từ giờ công, thưởng theo đơn hoàn thành và các khoản điều chỉnh."
      />
      {payslips.isLoading && <Loading />}
      {payslips.isError && (
        <QueryError error={payslips.error} retry={() => void payslips.refetch()} />
      )}
      {payslips.data && (
        <DataTable
          caption="Danh sách phiếu lương"
          columns={columns}
          data={rows}
          busy={payslips.isFetching}
          hideOnMobile={['range', 'hours']}
        />
      )}
      <MyPayslipModal payslip={selected} onClose={() => setSelected(null)} />
    </div>
  );
}

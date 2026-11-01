import { useMemo, useState } from 'react';
import { Eye } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useAuth } from '../../../app/providers/AuthProvider';
import { DataTable } from '../../../shared/components/DataTable';
import { Badge, Button, Loading, PageHeader, QueryError } from '../../../shared/components/ui';
import { formatDate, money } from '../../../shared/utils/format';
import { MyPayslipModal } from '../components/MyPayslipModal';
import { PayrollManagerView } from '../components/PayrollManagerView';
import { useMyPayslipsQuery } from '../hooks/usePayroll';
import { payrollStatusLabels, periodStatusVariant, type Payslip } from '../types/payroll';

type PayslipRow = Payslip & { id: string };

const hoursText = (value: string | number) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

export function PayrollPage() {
  const { user } = useAuth();
  const manager = user?.role === 'ADMIN' || user?.role === 'RECEPTIONIST';
  return manager ? <PayrollManagerView canApprove={user?.role === 'ADMIN'} /> : <MyPayslipsView />;
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

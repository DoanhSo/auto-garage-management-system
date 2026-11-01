import { useMemo, useState } from 'react';
import { Calculator, Download, RefreshCw } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { DataTable } from '../../../shared/components/DataTable';
import { MetricGrid } from '../../../shared/components/MetricGrid';
import {
  Badge,
  Button,
  ConfirmDialog,
  Field,
  FormError,
  Loading,
  Modal,
  QueryError,
} from '../../../shared/components/ui';
import { csvMoney, csvSections, downloadCsv } from '../../../shared/utils/csvExport';
import { formatDate, money } from '../../../shared/utils/format';
import { EmployeeWorkSummaryModal } from './EmployeeWorkSummaryModal';
import { useSchedulingEmployeesQuery } from '../../scheduling/hooks/useScheduling';
import { parseSkills, SKILL_LABELS } from '../../scheduling/types/scheduling';
import {
  useAdjustPayrollItemMutation,
  useApprovePayrollMutation,
  useCalculatePayrollMutation,
  useDisbursePayrollMutation,
  usePayrollPeriodQuery,
} from '../hooks/usePayroll';
import {
  payrollItemStatusLabels,
  payrollStatusLabels,
  type PayrollItem,
  type PayrollStatus,
} from '../types/payroll';

export const periodStatusVariant: Record<
  PayrollStatus,
  'secondary' | 'warning' | 'primary' | 'success'
> = {
  DRAFT: 'secondary',
  CALCULATED: 'warning',
  APPROVED: 'primary',
  PAID: 'success',
};

type ItemRow = PayrollItem & { id: string };
type ConfirmAction = 'approve' | 'disburse' | null;

const hoursText = (value: string | number) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

export function PayrollDetailModal({
  periodId,
  canApprove,
  onClose,
}: {
  periodId: string | null;
  canApprove: boolean;
  onClose: () => void;
}) {
  const notify = useToast();
  const query = usePayrollPeriodQuery(periodId);
  const calculate = useCalculatePayrollMutation();
  const approve = useApprovePayrollMutation();
  const disburse = useDisbursePayrollMutation();
  const adjust = useAdjustPayrollItemMutation();
  const employees = useSchedulingEmployeesQuery({ active: true });

  const [roleFilter, setRoleFilter] = useState('ALL');
  const [confirm, setConfirm] = useState<ConfirmAction>(null);
  const [summaryItem, setSummaryItem] = useState<PayrollItem | null>(null);
  const [editing, setEditing] = useState<PayrollItem | null>(null);
  const [allowance, setAllowance] = useState('0');
  const [deduction, setDeduction] = useState('0');
  const [note, setNote] = useState('');

  const period = query.data?.period;
  const editable = period?.status === 'DRAFT' || period?.status === 'CALCULATED';

  const skillsByEmployee = useMemo(
    () =>
      new Map(
        (employees.data ?? []).map((e) => [String(e.id), parseSkills(e.skillsJson)] as const),
      ),
    [employees.data],
  );

  const rows: ItemRow[] = useMemo(
    () =>
      (query.data?.items ?? []).filter(
        (item) =>
          roleFilter === 'ALL' ||
          (skillsByEmployee.get(String(item.employeeId)) ?? []).includes(roleFilter),
      ),
    [query.data?.items, roleFilter, skillsByEmployee],
  );

  const columns: ColumnDef<ItemRow>[] = [
    {
      id: 'employee',
      header: 'Nhân viên',
      cell: ({ row }) => (
        <div>
          <strong>{row.original.employeeName ?? '—'}</strong>
          <div className="description">{row.original.employeeCode}</div>
        </div>
      ),
    },
    {
      id: 'hours',
      header: 'Giờ công',
      cell: ({ row }) => (
        <span className="tabular">
          {hoursText(row.original.regularHours)}
          {Number(row.original.overtimeHours) > 0 &&
            ` + ${hoursText(row.original.overtimeHours)} TC`}
        </span>
      ),
    },
    {
      id: 'hourly',
      header: 'Lương giờ',
      cell: ({ row }) => <span className="tabular">{money(row.original.hourlyPayAmount)}</span>,
    },
    {
      id: 'commission',
      header: 'Thưởng đơn',
      cell: ({ row }) => <span className="tabular">{money(row.original.commissionAmount)}</span>,
    },
    {
      id: 'allowance',
      header: 'Phụ cấp',
      cell: ({ row }) => <span className="tabular">{money(row.original.allowanceAmount)}</span>,
    },
    {
      id: 'deduction',
      header: 'Giảm trừ',
      cell: ({ row }) => <span className="tabular">{money(row.original.deductionAmount)}</span>,
    },
    {
      id: 'net',
      header: 'Thực lĩnh',
      cell: ({ row }) => <strong className="tabular">{money(row.original.netPay)}</strong>,
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
            <Button
              variant="secondary"
              onClick={() => {
                setEditing(row.original);
                setAllowance(String(row.original.allowanceAmount));
                setDeduction(String(row.original.deductionAmount));
                setNote(row.original.notes ?? '');
              }}
            >
              Điều chỉnh
            </Button>
          )}
        </div>
      ),
    },
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
    if (!period || !query.data) return;
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
            'Thưởng đơn',
            'Phụ cấp',
            'Giảm trừ',
            'Thực lĩnh',
          ],
          rows: query.data.items.map((item) => [
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

  const saveAdjustment = async () => {
    if (!editing || !periodId) return;
    const allowanceValue = Number(allowance);
    const deductionValue = Number(deduction);
    if (
      !Number.isFinite(allowanceValue) ||
      allowanceValue < 0 ||
      !Number.isFinite(deductionValue) ||
      deductionValue < 0
    ) {
      notify('Phụ cấp và giảm trừ phải là số không âm');
      return;
    }
    await run(
      () =>
        adjust.mutateAsync({
          periodId,
          itemId: editing.id,
          data: {
            allowanceAmount: allowanceValue,
            deductionAmount: deductionValue,
            notes: note.trim(),
          },
        }),
      'Đã cập nhật phiếu lương',
    );
    setEditing(null);
  };

  const busy = calculate.isPending || approve.isPending || disburse.isPending;

  return (
    <>
      <Modal
        open={!!periodId}
        size="xl"
        title={period ? `Bảng lương ${period.periodCode}` : 'Bảng lương'}
        description={
          period
            ? `Kỳ công ${formatDate(period.fromDate)} – ${formatDate(period.toDate)}`
            : 'Đang tải chi tiết kỳ lương'
        }
        onOpenChange={(open) => !open && !busy && onClose()}
      >
        {query.isLoading && <Loading />}
        {query.isError && <QueryError error={query.error} retry={() => void query.refetch()} />}
        {period && query.data && (
          <div className="stack">
            <div className="row-actions">
              <Badge variant={periodStatusVariant[period.status]}>
                {payrollStatusLabels[period.status]}
              </Badge>
              <select
                className="select"
                aria-label="Lọc theo chuyên môn"
                value={roleFilter}
                onChange={(e) => setRoleFilter(e.target.value)}
              >
                <option value="ALL">Tất cả chuyên môn</option>
                {Object.entries(SKILL_LABELS).map(([key, label]) => (
                  <option key={key} value={key}>
                    {label}
                  </option>
                ))}
              </select>
              <span className="grow" />
              {editable && (
                <Button
                  variant="secondary"
                  pending={calculate.isPending}
                  disabled={busy}
                  onClick={() =>
                    void run(
                      () => calculate.mutateAsync(period.id),
                      'Đã tính lại toàn bộ bảng lương',
                    )
                  }
                >
                  <RefreshCw size={16} />{' '}
                  {period.status === 'DRAFT' ? 'Tính lương tự động' : 'Tính lại toàn bộ'}
                </Button>
              )}
              <Button variant="secondary" onClick={exportCsv} disabled={!query.data.items.length}>
                <Download size={16} /> Xuất CSV
              </Button>
              {canApprove && period.status === 'CALCULATED' && (
                <Button variant="primary" disabled={busy} onClick={() => setConfirm('approve')}>
                  <Calculator size={16} /> Phê duyệt bảng lương
                </Button>
              )}
              {canApprove && period.status === 'APPROVED' && (
                <Button variant="primary" disabled={busy} onClick={() => setConfirm('disburse')}>
                  Phê duyệt & Hạch toán chi quỹ
                </Button>
              )}
            </div>

            <MetricGrid
              label="Tổng hợp kỳ lương"
              items={[
                { label: 'Nhân viên', value: period.totalEmployees },
                {
                  label: 'Giờ công',
                  value: hoursText(period.totalRegularHours),
                  hint: `${hoursText(period.totalOvertimeHours)} tăng ca`,
                },
                { label: 'Hoa hồng & thưởng', value: money(period.totalCommissions) },
                { label: 'Thực chi', value: money(period.totalNetPay), tone: 'success' },
              ]}
            />

            <DataTable
              caption="Phiếu lương từng nhân viên"
              columns={columns}
              data={rows}
              hideOnMobile={['hourly', 'allowance', 'deduction', 'status']}
            />
          </div>
        )}
      </Modal>

      <EmployeeWorkSummaryModal
        employeeId={summaryItem?.employeeId ?? null}
        employeeName={summaryItem?.employeeName}
        month={period?.periodMonth ?? ''}
        onClose={() => setSummaryItem(null)}
      />

      <Modal
        open={!!editing}
        title="Điều chỉnh phiếu lương"
        description={editing ? `${editing.employeeName ?? ''} · ${editing.employeeCode ?? ''}` : ''}
        onOpenChange={(open) => !open && !adjust.isPending && setEditing(null)}
      >
        <form
          className="editor-form account-editor-form"
          noValidate
          onSubmit={(e) => {
            e.preventDefault();
            void saveAdjustment();
          }}
        >
          <FormError message={adjust.error ? errorMessage(adjust.error) : undefined} />
          <Field label="Phụ cấp (đ)">
            <input
              type="number"
              min="0"
              step="10000"
              className="input"
              value={allowance}
              onChange={(e) => setAllowance(e.target.value)}
            />
          </Field>
          <Field label="Giảm trừ / tạm ứng (đ)">
            <input
              type="number"
              min="0"
              step="10000"
              className="input"
              value={deduction}
              onChange={(e) => setDeduction(e.target.value)}
            />
          </Field>
          <div className="full-width-field">
            <Field label="Ghi chú">
              <input
                className="input"
                maxLength={2000}
                value={note}
                onChange={(e) => setNote(e.target.value)}
              />
            </Field>
          </div>
          <div className="form-actions account-editor-actions">
            <Button onClick={() => setEditing(null)} disabled={adjust.isPending}>
              Hủy
            </Button>
            <Button variant="primary" type="submit" pending={adjust.isPending}>
              Lưu điều chỉnh
            </Button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        open={confirm !== null}
        title={confirm === 'approve' ? 'Phê duyệt bảng lương?' : 'Hạch toán chi lương vào sổ quỹ?'}
        description={
          confirm === 'approve'
            ? 'Sau khi phê duyệt, bảng lương không thể tính lại hoặc chỉnh sửa.'
            : `Hệ thống sẽ ghi một phiếu chi ${period ? money(period.totalNetPay) : ''} vào Sổ quỹ Thu / Chi và đánh dấu kỳ lương đã chi trả.`
        }
        pending={approve.isPending || disburse.isPending}
        confirmLabel={confirm === 'approve' ? 'Phê duyệt' : 'Hạch toán chi'}
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
    </>
  );
}

import { useMemo, useState } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { DataTable } from '../../../shared/components/DataTable';
import { Badge, Button, Loading, QueryError } from '../../../shared/components/ui';
import { formatDate, money } from '../../../shared/utils/format';
import { useSchedulingEmployeesQuery } from '../../scheduling/hooks/useScheduling';
import { SKILL_LABELS, parseSkills, type Employee } from '../../scheduling/types/scheduling';
import { STANDARD_SHIFT_HOURS } from '../types/payroll';
import { SalaryCoefficientModal } from './SalaryCoefficientModal';

const DEFAULT_OVERTIME_MULTIPLIER = 1.5;

export function SalaryCoefficientManager() {
  const employeesQuery = useSchedulingEmployeesQuery({ active: true });
  const [keyword, setKeyword] = useState('');
  const [editing, setEditing] = useState<Employee | null>(null);

  const rows = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    return (employeesQuery.data ?? []).filter(
      (e) => !term || `${e.fullName ?? ''} ${e.employeeCode}`.toLowerCase().includes(term),
    );
  }, [employeesQuery.data, keyword]);

  const rateOf = (e: Employee) => Number(e.hourlyCost ?? 0);

  const columns: ColumnDef<Employee>[] = [
    {
      id: 'employee',
      header: 'Nhân viên',
      cell: ({ row }) => {
        const skills = parseSkills(row.original.skillsJson);
        return (
          <div>
            <strong>{row.original.fullName ?? '—'}</strong>
            <div className="description">
              {row.original.employeeCode}
              {skills.length > 0 && ` · ${skills.map((s) => SKILL_LABELS[s] ?? s).join(', ')}`}
            </div>
            {row.original.joinedAt && (
              <div className="description">Vào làm {formatDate(row.original.joinedAt)}</div>
            )}
          </div>
        );
      },
    },
    {
      id: 'coefficient',
      header: 'Hệ số lương',
      cell: ({ row }) => (
        <Badge variant="primary">{Number(row.original.salaryCoefficient ?? 1).toFixed(2)}x</Badge>
      ),
    },
    {
      id: 'rate',
      header: 'Đơn giá giờ',
      cell: ({ row }) => <span className="tabular">{money(rateOf(row.original))}/giờ</span>,
    },
    {
      id: 'overtime',
      header: 'Giờ tăng ca',
      cell: ({ row }) => {
        const multiplier = Number(row.original.overtimeMultiplier ?? DEFAULT_OVERTIME_MULTIPLIER);
        return (
          <span className="tabular">
            {money(Math.round(rateOf(row.original) * multiplier))}/giờ ({multiplier}x)
          </span>
        );
      },
    },
    {
      id: 'shift',
      header: `1 ca chuẩn (${STANDARD_SHIFT_HOURS}h)`,
      cell: ({ row }) => (
        <strong className="tabular">{money(rateOf(row.original) * STANDARD_SHIFT_HOURS)}</strong>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <Button variant="secondary" onClick={() => setEditing(row.original)}>
          <SlidersHorizontal size={14} /> Điều chỉnh hệ số
        </Button>
      ),
    },
  ];

  return (
    <section className="panel table-panel" aria-label="Hệ số và đơn giá lương">
      <div className="payroll-filter-row" role="search" aria-label="Lọc nhân viên">
        <input
          className="input"
          aria-label="Tìm nhân viên"
          placeholder="Tìm theo tên hoặc mã nhân viên…"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <span className="description toolbar-end">
          Đơn giá giờ là mức áp vào giờ công khi tính lương; hệ số dùng để gợi ý đơn giá.
        </span>
      </div>
      {employeesQuery.isLoading && <Loading />}
      {employeesQuery.isError && (
        <QueryError error={employeesQuery.error} retry={() => void employeesQuery.refetch()} />
      )}
      {employeesQuery.data && (
        <DataTable
          caption="Hệ số và đơn giá lương từng nhân viên"
          columns={columns}
          data={rows}
          busy={employeesQuery.isFetching}
          hideOnMobile={['overtime', 'shift']}
          emptyTitle="Không có nhân viên đang làm việc"
        />
      )}
      <SalaryCoefficientModal employee={editing} onClose={() => setEditing(null)} />
    </section>
  );
}

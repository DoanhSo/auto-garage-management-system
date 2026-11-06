import { useMemo, useState } from 'react';
import { Plus, SquarePen } from 'lucide-react';
import type { ColumnDef } from '@tanstack/react-table';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { DataTable } from '../../../shared/components/DataTable';
import { Badge, Button, Loading, QueryError } from '../../../shared/components/ui';
import { formatDate, money } from '../../../shared/utils/format';
import { EmployeeEditor } from '../../scheduling/components/EmployeeEditor';
import { todayKey } from '../../../shared/utils/weekCalendar';
import { useStaffEmployee } from '../hooks/useStaffEmployee';
import { EmployeeDetailSheet } from './EmployeeDetailSheet';
import {
  useSchedulingEmployeesQuery,
  useShiftsQuery,
  useUpdateEmployeeMutation,
} from '../../scheduling/hooks/useScheduling';
import { SKILL_LABELS, parseSkills, type Employee } from '../../scheduling/types/scheduling';

type StatusFilter = 'ACTIVE' | 'INACTIVE' | 'ALL';

const statusFilters: Record<StatusFilter, string> = {
  ACTIVE: 'Đang làm việc',
  INACTIVE: 'Tạm nghỉ',
  ALL: 'Tất cả',
};

/** "2 năm 3 tháng" since the joining date; empty when it is unknown or in the future. */
function seniorityText(joinedAt?: string | null): string {
  if (!joinedAt) return '';
  const start = new Date(joinedAt);
  const now = new Date();
  const months = (now.getFullYear() - start.getFullYear()) * 12 + now.getMonth() - start.getMonth();
  if (!Number.isFinite(months) || months < 0) return '';
  if (months < 1) return 'Dưới 1 tháng';
  const years = Math.floor(months / 12);
  const rest = months % 12;
  return [years > 0 ? `${years} năm` : '', rest > 0 ? `${rest} tháng` : '']
    .filter(Boolean)
    .join(' ');
}

export function EmployeeDirectory({ canCreate }: { canCreate: boolean }) {
  const notify = useToast();
  const [keyword, setKeyword] = useState('');
  const [skill, setSkill] = useState('ALL');
  const [status, setStatus] = useState<StatusFilter>('ACTIVE');
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<Employee | null>(null);
  const [selectedId, selectEmployee] = useStaffEmployee();
  const employeesQuery = useSchedulingEmployeesQuery();
  const activeShifts = useShiftsQuery({ status: 'ACTIVE', from: todayKey(), size: 200 });
  const shiftCounts = useMemo(() => {
    const counts = new Map<string, number>();
    (activeShifts.data?.content ?? [])
      .filter((shift) => new Date(shift.endsAt).getTime() >= Date.now())
      .forEach((shift) =>
        counts.set(String(shift.employeeId), (counts.get(String(shift.employeeId)) ?? 0) + 1),
      );
    return counts;
  }, [activeShifts.data]);
  const updateEmployee = useUpdateEmployeeMutation();

  const employees = useMemo(() => employeesQuery.data ?? [], [employeesQuery.data]);
  const takenUserIds = useMemo(() => new Set(employees.map((e) => String(e.userId))), [employees]);
  const rows = useMemo(() => {
    const term = keyword.trim().toLowerCase();
    return employees.filter((e) => {
      if (status === 'ACTIVE' && !e.isActive) return false;
      if (status === 'INACTIVE' && e.isActive) return false;
      if (skill === 'QC' && !e.canQc) return false;
      if (skill !== 'ALL' && skill !== 'QC' && !parseSkills(e.skillsJson).includes(skill))
        return false;
      return (
        !term ||
        `${e.fullName ?? ''} ${e.employeeCode} ${e.bankAccountNumber ?? ''}`
          .toLowerCase()
          .includes(term)
      );
    });
  }, [employees, keyword, skill, status]);

  const openEditor = (employee: Employee | null) => {
    setEditing(employee);
    setEditorOpen(true);
  };

  const toggleActive = async (employee: Employee) => {
    try {
      await updateEmployee.mutateAsync({
        id: employee.id,
        data: { isActive: !employee.isActive },
      });
      notify(employee.isActive ? 'Đã tạm ngưng nhân viên' : 'Đã kích hoạt lại nhân viên');
    } catch (err: unknown) {
      notify(errorMessage(err));
    }
  };

  const columns: ColumnDef<Employee>[] = [
    {
      id: 'employee',
      header: 'Mã & Họ tên',
      cell: ({ row }) => (
        <div>
          <button
            type="button"
            className="link-button"
            onClick={() => selectEmployee(row.original.id)}
          >
            {row.original.fullName ?? '—'}
          </button>
          <div className="description">
            {row.original.employeeCode} · {row.original.isActive ? 'Đang làm việc' : 'Tạm nghỉ'}
          </div>
        </div>
      ),
    },
    {
      id: 'skills',
      header: 'Chuyên môn & QC',
      cell: ({ row }) => {
        const skills = parseSkills(row.original.skillsJson);
        return (
          <div className="row-actions">
            {skills.map((s) => (
              <Badge key={s} variant="outline">
                {SKILL_LABELS[s] ?? s}
              </Badge>
            ))}
            {row.original.canQc && <Badge variant="primary">Quyền QC</Badge>}
            {skills.length === 0 && !row.original.canQc && <span className="muted">—</span>}
          </div>
        );
      },
    },
    {
      id: 'joinedAt',
      header: 'Ngày vào công ty',
      cell: ({ row }) =>
        row.original.joinedAt ? (
          <div>
            <span>{formatDate(row.original.joinedAt)}</span>
            <div className="description">{seniorityText(row.original.joinedAt)}</div>
          </div>
        ) : (
          <span className="muted">Chưa cập nhật</span>
        ),
    },
    {
      id: 'shifts',
      header: 'Ca hiệu lực',
      cell: ({ row }) => <span>{shiftCounts.get(String(row.original.id)) ?? 0} ca</span>,
    },
    {
      id: 'bank',
      header: 'Tài khoản nhận lương',
      cell: ({ row }) =>
        row.original.bankAccountNumber ? (
          <div>
            <span>
              {row.original.bankName ?? 'Ngân hàng'} · {row.original.bankAccountNumber}
            </span>
            {row.original.bankAccountHolder && (
              <div className="description">{row.original.bankAccountHolder}</div>
            )}
          </div>
        ) : (
          <Badge variant="warning">Chưa cập nhật STK</Badge>
        ),
    },
    {
      id: 'rate',
      header: 'Đơn giá & Hệ số',
      cell: ({ row }) => (
        <span className="tabular">
          {row.original.hourlyCost != null ? `${money(row.original.hourlyCost)}/h` : '—'} · Hệ số{' '}
          {Number(row.original.salaryCoefficient ?? 1).toFixed(2)}x
        </span>
      ),
    },
    {
      id: 'actions',
      header: '',
      cell: ({ row }) => (
        <div className="row-actions">
          <Button variant="ghost" onClick={() => openEditor(row.original)}>
            <SquarePen size={14} /> Sửa hồ sơ
          </Button>
          <Button
            variant="ghost"
            className={row.original.isActive ? 'delete-action' : undefined}
            disabled={updateEmployee.isPending}
            onClick={() => void toggleActive(row.original)}
          >
            {row.original.isActive ? 'Tạm ngưng' : 'Kích hoạt'}
          </Button>
        </div>
      ),
    },
  ];

  return (
    <section className="panel table-panel" aria-label="Hồ sơ nhân viên">
      <div className="payroll-filter-row" role="search" aria-label="Lọc nhân viên">
        <input
          className="input"
          aria-label="Tìm nhân viên"
          placeholder="Tìm theo tên, mã nhân viên hoặc STK…"
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
        <select
          className="select"
          aria-label="Lọc theo trạng thái"
          value={status}
          onChange={(e) => setStatus(e.target.value as StatusFilter)}
        >
          {(Object.keys(statusFilters) as StatusFilter[]).map((k) => (
            <option key={k} value={k}>
              {statusFilters[k]}
            </option>
          ))}
        </select>
        {canCreate && (
          <Button variant="primary" className="toolbar-end" onClick={() => openEditor(null)}>
            <Plus size={16} /> Thêm nhân viên
          </Button>
        )}
      </div>
      {employeesQuery.isLoading && <Loading />}
      {employeesQuery.isError && (
        <QueryError error={employeesQuery.error} retry={() => void employeesQuery.refetch()} />
      )}
      {employeesQuery.data && (
        <DataTable
          caption="Danh sách hồ sơ nhân viên"
          columns={columns}
          data={rows}
          busy={employeesQuery.isFetching}
          hideOnMobile={['skills', 'joinedAt', 'rate']}
          emptyTitle="Không có nhân viên phù hợp"
          emptyDescription="Thử đổi bộ lọc trạng thái hoặc chuyên môn."
        />
      )}
      <EmployeeDetailSheet
        employee={employees.find((e) => String(e.id) === selectedId) ?? null}
        onClose={() => selectEmployee(null)}
        onEdit={(employee) => openEditor(employee)}
      />
      <EmployeeEditor
        employee={editing}
        takenUserIds={takenUserIds}
        open={editorOpen}
        onClose={() => setEditorOpen(false)}
        onSaved={() => setEditorOpen(false)}
      />
    </section>
  );
}

import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { CalendarClock, SquarePen } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { Badge, Button, Loading, Sheet } from '../../../shared/components/ui';
import { formatDate, formatDateTime, money } from '../../../shared/utils/format';
import { AccountStatusBadge } from '../../../shared/components/AccountStatusBadge';
import { todayKey } from '../../../shared/utils/weekCalendar';
import { ShiftEditor } from '../../scheduling/components/ShiftEditor';
import { useShiftsQuery } from '../../scheduling/hooks/useScheduling';
import {
  shiftTypeLabels,
  SKILL_LABELS,
  parseSkills,
  type Employee,
} from '../../scheduling/types/scheduling';
import { employeeShiftsHref, employeeWorkHref } from '../utils/links';

const UPCOMING_SHIFTS = 5;

/** One employee at a glance, with the ways into the screens that depend on them. */
export function EmployeeDetailSheet({
  employee,
  onClose,
  onEdit,
}: {
  employee: Employee | null;
  onClose: () => void;
  onEdit: (employee: Employee) => void;
}) {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';
  const [shiftOpen, setShiftOpen] = useState(false);
  const shifts = useShiftsQuery(
    { employeeId: Number(employee?.id), status: 'ACTIVE', from: todayKey(), size: 30 },
    !!employee,
  );

  const upcoming = useMemo(() => {
    const now = Date.now();
    return (shifts.data?.content ?? [])
      .filter((shift) => new Date(shift.endsAt).getTime() >= now)
      .sort((a, b) => new Date(a.startsAt).getTime() - new Date(b.startsAt).getTime())
      .slice(0, UPCOMING_SHIFTS);
  }, [shifts.data]);

  if (!employee) return null;
  const skills = parseSkills(employee.skillsJson);

  return (
    <>
      <Sheet
        open
        placement="center"
        size="md"
        onClose={onClose}
        title={employee.fullName ?? employee.employeeCode}
        description={`${employee.employeeCode} · ${employee.isActive ? 'Đang làm việc' : 'Tạm nghỉ'}`}
      >
        <div className="employee-detail">
          <section aria-label="Hồ sơ">
            <h4>Hồ sơ</h4>
            <dl>
              <dt>Tài khoản đăng nhập</dt>
              <dd className="account-status">
                <span>{employee.account?.username ?? employee.username ?? '—'}</span>
                <AccountStatusBadge account={employee.account} />
              </dd>
              <dt>Chuyên môn</dt>
              <dd className="row-actions">
                {skills.map((s) => (
                  <Badge key={s} variant="outline">
                    {SKILL_LABELS[s] ?? s}
                  </Badge>
                ))}
                {employee.canQc && <Badge variant="primary">Quyền QC</Badge>}
                {skills.length === 0 && !employee.canQc && <span className="muted">—</span>}
              </dd>
              <dt>Ngày vào làm</dt>
              <dd>{employee.joinedAt ? formatDate(employee.joinedAt) : 'Chưa cập nhật'}</dd>
              {employee.hourlyCost != null && (
                <>
                  <dt>Đơn giá & hệ số</dt>
                  <dd>
                    {money(employee.hourlyCost)}/h · Hệ số{' '}
                    {Number(employee.salaryCoefficient ?? 1).toFixed(2)}x
                  </dd>
                  <dt>Nhận lương</dt>
                  <dd>
                    {employee.bankAccountNumber
                      ? `${employee.bankName ?? 'Ngân hàng'} · ${employee.bankAccountNumber}`
                      : 'Chưa cập nhật STK'}
                  </dd>
                </>
              )}
            </dl>
          </section>

          <section aria-label="Ca sắp tới">
            <h4>Ca sắp tới</h4>
            {shifts.isLoading && <Loading />}
            {shifts.data && upcoming.length === 0 && (
              <p className="muted">Chưa có ca nào sắp tới.</p>
            )}
            {upcoming.length > 0 && (
              <ul>
                {upcoming.map((shift) => (
                  <li key={shift.id}>
                    {formatDateTime(shift.startsAt)} – {formatDateTime(shift.endsAt)}
                    {shift.shiftType
                      ? ` · ${shiftTypeLabels[shift.shiftType] ?? shift.shiftType}`
                      : ''}
                  </li>
                ))}
              </ul>
            )}
          </section>

          <div className="employee-detail-actions">
            <Link
              className="button button-secondary"
              to={employeeShiftsHref(employee.id)}
              onClick={onClose}
            >
              Xem lịch ca
            </Link>
            <Link
              className="button button-secondary"
              to={employeeWorkHref(employee.id)}
              onClick={onClose}
            >
              Công & lương tháng
            </Link>
            {isAdmin && (
              <>
                <Button
                  variant="secondary"
                  disabled={!employee.isActive}
                  onClick={() => setShiftOpen(true)}
                >
                  <CalendarClock size={15} /> Xếp ca
                </Button>
                <Button variant="secondary" onClick={() => onEdit(employee)}>
                  <SquarePen size={15} /> Sửa hồ sơ
                </Button>
              </>
            )}
          </div>
        </div>
      </Sheet>
      {shiftOpen && (
        <ShiftEditor
          open
          initialEmployeeId={employee.id}
          onClose={() => setShiftOpen(false)}
          onSaved={() => void shifts.refetch()}
        />
      )}
    </>
  );
}

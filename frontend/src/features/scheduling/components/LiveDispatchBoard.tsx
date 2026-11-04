import { Link } from 'react-router-dom';
import { Clock, Hammer, LogIn, LogOut, UserCheck, Wrench } from 'lucide-react';
import { useAuth } from '../../../app/providers/AuthProvider';
import { useToast } from '../../../app/providers/ToastProvider';
import { errorMessage } from '../../../shared/api/errors';
import { FRONT_DESK_ROLES } from '../../../app/access';
import { Badge, Button, Empty, Loading, QueryError } from '../../../shared/components/ui';
import { PendingShiftRegistrations } from './PendingShiftRegistrations';
import {
  useOnShiftTechniciansQuery,
  useSchedulingEmployeesQuery,
  useShiftAttendanceMutation,
  useShiftReconciliationQuery,
} from '../hooks/useScheduling';
import {
  SKILL_LABELS,
  attendanceLabels,
  availabilityLabels,
  shiftTypeLabels,
  bayTypeLabels,
  type AttendanceStatus,
  type BayReservation,
  type OnShiftTechnician,
  type ServiceBay,
  type TechnicianAvailability,
} from '../types/scheduling';

const hhmm = (iso: string) =>
  new Date(iso).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });

const availabilityVariant: Record<TechnicianAvailability, 'success' | 'warning' | 'danger'> = {
  AVAILABLE: 'success',
  BUSY: 'warning',
  OVERLOADED: 'danger',
};

const attendanceVariant: Record<AttendanceStatus, 'secondary' | 'success' | 'warning' | 'danger'> =
  {
    SCHEDULED: 'secondary',
    CHECKED_IN: 'success',
    LATE: 'warning',
    CHECKED_OUT: 'secondary',
    ABSENT: 'danger',
  };

const bonusText = (value: number | string) =>
  `+${new Intl.NumberFormat('vi-VN').format(Number(value))} đ`;

const hoursText = (value: number | string) => `${Number(value).toFixed(1).replace('.', ',')} giờ`;

function todayLocal(): string {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
}

function bayState(reservation?: BayReservation): {
  label: string;
  variant: 'success' | 'primary' | 'warning' | 'danger';
} {
  if (!reservation) return { label: 'SẴN SÀNG', variant: 'success' };
  if (reservation.reservationType === 'REPAIR') return { label: 'ĐANG SỬA', variant: 'primary' };
  if (reservation.reservationType === 'BOOKING') return { label: 'GIỮ CHỖ', variant: 'warning' };
  return { label: 'BẢO TRÌ', variant: 'danger' };
}

function TechnicianChip({ tech, compact = false }: { tech: OnShiftTechnician; compact?: boolean }) {
  const attendance = tech.attendanceStatus ?? 'SCHEDULED';
  if (compact) {
    return (
      <div className="dispatch-tech-compact">
        <div className="dispatch-tech">
          <span>Thợ trực:</span>
          <strong>{tech.fullName}</strong>
          <span className="muted">({tech.employeeCode})</span>
          <Badge variant={availabilityVariant[tech.availabilityStatus]}>
            {tech.hasActiveTimer && <Clock size={12} />}{' '}
            {availabilityLabels[tech.availabilityStatus]}
          </Badge>
        </div>
        <small className="muted">
          {attendanceLabels[attendance]} · Ca này: {tech.completedOrdersInShift} đơn ·{' '}
          {bonusText(tech.currentShiftBonus)}
        </small>
      </div>
    );
  }
  return (
    <div className="dispatch-tech">
      <strong>{tech.fullName}</strong>
      <span className="muted">{tech.employeeCode}</span>
      <Badge variant={availabilityVariant[tech.availabilityStatus]}>
        {tech.hasActiveTimer && <Clock size={12} />} {availabilityLabels[tech.availabilityStatus]}
      </Badge>
      <Badge variant={attendanceVariant[attendance]}>{attendanceLabels[attendance]}</Badge>
      <Badge variant={tech.completedOrdersInShift > 0 ? 'success' : 'outline'}>
        Đã hoàn thành ca này: {tech.completedOrdersInShift} đơn · Thưởng tạm tính:{' '}
        {bonusText(tech.currentShiftBonus)}
      </Badge>
    </div>
  );
}

export function LiveDispatchBoard({
  bays,
  currentByBay,
  onAssignShift,
}: {
  bays: ServiceBay[];
  currentByBay: Map<string, BayReservation>;
  onAssignShift: (bay: ServiceBay) => void;
}) {
  const notify = useToast();
  const { user } = useAuth();
  const roster = useOnShiftTechniciansQuery();
  const attendance = useShiftAttendanceMutation();
  const isFrontDesk = !!user && FRONT_DESK_ROLES.includes(user.role);
  const employees = useSchedulingEmployeesQuery({ active: true });
  const ownEmployeeId = (employees.data ?? []).find(
    (e) => String(e.userId) === String(user?.id),
  )?.id;
  const today = todayLocal();
  const reconciliation = useShiftReconciliationQuery(today, isFrontDesk);

  const punch = (tech: OnShiftTechnician, action: 'check-in' | 'check-out') =>
    attendance.mutate(
      { id: tech.shiftId, action },
      {
        onSuccess: () =>
          notify(action === 'check-in' ? 'Đã chấm công vào ca' : 'Đã chấm công ra ca'),
        onError: (err: unknown) => notify(errorMessage(err)),
      },
    );
  const canPunch = (tech: OnShiftTechnician) =>
    isFrontDesk || String(tech.employeeId) === String(ownEmployeeId);

  if (roster.isLoading) return <Loading label="Đang tải ca trực hiện tại…" />;
  if (roster.isError)
    return <QueryError error={roster.error} retry={() => void roster.refetch()} />;

  const technicians = roster.data ?? [];
  const byBay = new Map<string, OnShiftTechnician[]>();
  technicians.forEach((tech) => {
    if (!tech.serviceBayId) return;
    const key = String(tech.serviceBayId);
    byBay.set(key, [...(byBay.get(key) ?? []), tech]);
  });
  const floating = technicians.filter((tech) => !tech.serviceBayId);

  return (
    <div className="dispatch-board-panel stack">
      {isFrontDesk && <PendingShiftRegistrations canReview={user?.role === 'ADMIN'} />}

      <section aria-label="Kỹ thuật viên đang trực ca">
        <h2 className="section-title">
          <UserCheck size={16} aria-hidden /> Đang trực ca ({technicians.length})
        </h2>
        {technicians.length === 0 ? (
          <Empty
            title="Hiện chưa có kỹ thuật viên nào trong ca trực"
            description="Xếp ca ở tab Lịch ca làm việc để kỹ thuật viên xuất hiện tại đây."
          />
        ) : (
          <div className="dispatch-roster">
            {technicians.map((tech) => (
              <article key={tech.employeeId} className="metric-card">
                <TechnicianChip tech={tech} />
                <p className="muted">
                  {tech.skills.map((skill) => SKILL_LABELS[skill] ?? skill).join(', ') || 'Đa năng'}
                  {tech.canQc ? ' · QC' : ''}
                </p>
                <p className="muted">
                  {tech.shiftType ? `${shiftTypeLabels[tech.shiftType]} · ` : ''}Ca{' '}
                  {hhmm(tech.shiftStartsAt)}–{hhmm(tech.shiftEndsAt)}
                  {tech.bayCode ? ` · ${tech.bayCode}` : ''} · {tech.activeOrdersCount} xe phụ trách
                </p>
                {canPunch(tech) && (
                  <div className="row-actions">
                    {(tech.attendanceStatus ?? 'SCHEDULED') === 'SCHEDULED' && (
                      <Button
                        variant="secondary"
                        pending={attendance.isPending}
                        onClick={() => punch(tech, 'check-in')}
                      >
                        <LogIn size={14} /> Vào ca
                      </Button>
                    )}
                    {(tech.attendanceStatus === 'CHECKED_IN' ||
                      tech.attendanceStatus === 'LATE') && (
                      <Button
                        variant="secondary"
                        pending={attendance.isPending}
                        onClick={() => punch(tech, 'check-out')}
                      >
                        <LogOut size={14} /> Ra ca
                      </Button>
                    )}
                  </div>
                )}
              </article>
            ))}
          </div>
        )}
      </section>

      {isFrontDesk && (reconciliation.data?.length ?? 0) > 0 && (
        <section aria-label="Đối soát giờ trực ca hôm nay">
          <h2 className="section-title">
            <Clock size={16} aria-hidden /> Đối soát ca trực hôm nay
          </h2>
          <div className="table-scroll">
            <table className="data-table">
              <caption className="sr-only">Giờ trực ca so với giờ bấm giờ thi công</caption>
              <thead>
                <tr>
                  <th scope="col">Kỹ thuật viên</th>
                  <th scope="col">Ca</th>
                  <th scope="col">Chấm công</th>
                  <th scope="col">Giờ trực</th>
                  <th scope="col">Giờ thi công</th>
                  <th scope="col">Giờ rảnh</th>
                  <th scope="col">Vượt ca</th>
                  <th scope="col">Hiệu suất</th>
                </tr>
              </thead>
              <tbody>
                {reconciliation.data?.map((row) => (
                  <tr key={row.shiftId}>
                    <td>
                      <strong>{row.employeeName ?? row.employeeCode}</strong>
                    </td>
                    <td>
                      {row.shiftType ? shiftTypeLabels[row.shiftType] : '—'} · {hhmm(row.startsAt)}–
                      {hhmm(row.endsAt)}
                    </td>
                    <td>
                      <Badge variant={attendanceVariant[row.attendanceStatus]}>
                        {attendanceLabels[row.attendanceStatus]}
                      </Badge>
                    </td>
                    <td className="tabular">{hoursText(row.scheduledHours)}</td>
                    <td className="tabular">{hoursText(row.laborHours)}</td>
                    <td className="tabular">{hoursText(row.idleHours)}</td>
                    <td className="tabular">{hoursText(row.overShiftHours)}</td>
                    <td className="tabular">{row.utilizationPercent}%</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}

      <section aria-label="Khoang xưởng">
        <h2 className="section-title">
          <Wrench size={16} aria-hidden /> Khoang xưởng ({bays.length})
        </h2>
        <div className="dispatch-grid">
          {bays.map((bay) => {
            const reservation = currentByBay.get(String(bay.id));
            const state = bayState(reservation);
            const crew = byBay.get(String(bay.id)) ?? [];
            return (
              <article key={bay.id} className="metric-card dispatch-bay">
                <header className="row-actions">
                  <strong>{bay.bayCode}</strong>
                  <span className="muted">{bayTypeLabels[bay.bayType] ?? bay.bayType}</span>
                  <span className="grow" />
                  <Badge variant={bay.isActive ? state.variant : 'secondary'}>
                    {bay.isActive ? state.label : 'NGƯNG'}
                  </Badge>
                </header>
                <div className="dispatch-bay-body">
                  {reservation?.repairOrderId ? (
                    <p>
                      <Hammer size={14} aria-hidden /> Lệnh sửa chữa{' '}
                      <strong>#{reservation.repairOrderId}</strong> · {hhmm(reservation.startsAt)}–
                      {hhmm(reservation.endsAt)}
                    </p>
                  ) : reservation ? (
                    <p className="muted">
                      {reservation.reservationType === 'BOOKING' && reservation.bookingId
                        ? `Lịch hẹn #${reservation.bookingId}`
                        : 'Đang bảo trì'}{' '}
                      · {hhmm(reservation.startsAt)}–{hhmm(reservation.endsAt)}
                    </p>
                  ) : (
                    <p className="muted">Sẵn sàng nhận xe</p>
                  )}
                  {crew.length > 0 ? (
                    crew.map((tech) => <TechnicianChip key={tech.employeeId} tech={tech} compact />)
                  ) : (
                    <p className="muted">Chưa có thợ trực khoang này</p>
                  )}
                </div>
                <footer className="row-actions">
                  <Button
                    variant="secondary"
                    onClick={() => onAssignShift(bay)}
                    disabled={!bay.isActive}
                  >
                    {crew.length > 0 ? 'Đổi thợ trực' : 'Chỉ định thợ trực'}
                  </Button>
                  {reservation?.repairOrderId && (
                    <Link className="btn btn-ghost" to="/repair-orders">
                      Xem tiến độ
                    </Link>
                  )}
                </footer>
              </article>
            );
          })}
        </div>
        {floating.length > 0 && (
          <p className="muted dispatch-floating-note">
            {floating.length} kỹ thuật viên đang trực chưa gán khoang cố định:{' '}
            {floating.map((tech) => tech.fullName).join(', ')}.
          </p>
        )}
      </section>
    </div>
  );
}

import { Link } from 'react-router-dom';
import { Clock, Hammer, UserCheck, Wrench } from 'lucide-react';
import { Badge, Button, Empty, Loading, QueryError } from '../../../shared/components/ui';
import { useOnShiftTechniciansQuery } from '../hooks/useScheduling';
import {
  SKILL_LABELS,
  availabilityLabels,
  bayTypeLabels,
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

function bayState(reservation?: BayReservation): {
  label: string;
  variant: 'success' | 'primary' | 'warning' | 'danger';
} {
  if (!reservation) return { label: 'TRỐNG', variant: 'success' };
  if (reservation.reservationType === 'REPAIR') return { label: 'ĐANG SỬA', variant: 'primary' };
  if (reservation.reservationType === 'BOOKING') return { label: 'GIỮ CHỖ', variant: 'warning' };
  return { label: 'BẢO TRÌ', variant: 'danger' };
}

function TechnicianChip({ tech }: { tech: OnShiftTechnician }) {
  return (
    <div className="dispatch-tech">
      <strong>{tech.fullName}</strong>
      <span className="muted">{tech.employeeCode}</span>
      <Badge variant={availabilityVariant[tech.availabilityStatus]}>
        {tech.hasActiveTimer && <Clock size={12} />} {availabilityLabels[tech.availabilityStatus]}
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
  const roster = useOnShiftTechniciansQuery();

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
    <div className="stack">
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
                  Ca {hhmm(tech.shiftStartsAt)}–{hhmm(tech.shiftEndsAt)}
                  {tech.bayCode ? ` · ${tech.bayCode}` : ''} · {tech.activeOrdersCount} xe phụ trách
                </p>
              </article>
            ))}
          </div>
        )}
      </section>

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
                    crew.map((tech) => <TechnicianChip key={tech.employeeId} tech={tech} />)
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
                    Chỉ định thợ trực
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
          <p className="muted">
            {floating.length} kỹ thuật viên đang trực chưa gán khoang cố định:{' '}
            {floating.map((tech) => tech.fullName).join(', ')}.
          </p>
        )}
      </section>
    </div>
  );
}

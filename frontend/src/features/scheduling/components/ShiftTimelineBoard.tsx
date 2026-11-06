import { useMemo, useState } from 'react';
import { ChevronLeft, ChevronRight, Plus, X, XCircle } from 'lucide-react';
import { Badge, Button } from '../../../shared/components/ui';
import {
  WEEKDAY_LABELS,
  localDate,
  localDateTime,
  shiftDate,
  todayKey,
  weekStart,
} from '../../../shared/utils/weekCalendar';
import type { Employee, ServiceBay, Shift } from '../types/scheduling';
import { attendanceLabels, shiftTypeLabels } from '../types/scheduling';
import {
  SHIFT_CHIP_LABELS,
  cellKey,
  groupShiftsByCell,
  shiftChipState,
  type ShiftChipState,
} from '../utils/shiftTimeline';

export interface ShiftTimelineBoardProps {
  readonly shifts: readonly Shift[];
  readonly employees: readonly Employee[];
  readonly employeeName: (id: string | number) => string;
  readonly bayOf: (id?: string | number | null) => ServiceBay | undefined;
  /** Ngày bất kỳ trong tuần đang xem (YYYY-MM-DD). */
  readonly selectedDate: string;
  readonly onDateChange: (date: string) => void;
  readonly canManage: boolean;
  /** Đang lọc theo từ khóa: chỉ giữ kỹ thuật viên có ca khớp trong tuần. */
  readonly searching: boolean;
  readonly onAddShift: (employeeId: string | number, day: string) => void;
  readonly onCancelShift: (shift: Shift) => void;
}

const dayMonth = (dateKey: string) => `${dateKey.slice(8, 10)}/${dateKey.slice(5, 7)}`;
const hhmm = (iso: string) => localDateTime(iso).slice(11, 16);

const STATE_BADGE: Record<
  ShiftChipState,
  'primary' | 'success' | 'secondary' | 'danger' | 'warning'
> = {
  upcoming: 'primary',
  live: 'success',
  done: 'secondary',
  cancelled: 'danger',
  pending: 'warning',
};

export function ShiftTimelineBoard({
  shifts,
  employees,
  employeeName,
  bayOf,
  selectedDate,
  onDateChange,
  canManage,
  searching,
  onAddShift,
  onCancelShift,
}: ShiftTimelineBoardProps) {
  const today = todayKey();
  const firstDay = weekStart(selectedDate || today);
  const days = useMemo(
    () => Array.from({ length: 7 }, (_, i) => shiftDate(firstDay, i)),
    [firstDay],
  );
  const [focusedId, setFocusedId] = useState<string | null>(null);
  const now = Date.now();

  const cells = useMemo(() => groupShiftsByCell(shifts, days), [shifts, days]);
  const weekShifts = useMemo(() => [...cells.values()].flat(), [cells]);
  const countState = (state: ShiftChipState) =>
    weekShifts.filter((shift) => shiftChipState(shift, now) === state).length;
  const summary = [
    { label: 'Tổng ca trong tuần', value: weekShifts.length },
    { label: 'Đang trực', value: countState('live') },
    { label: 'Sắp tới', value: countState('upcoming') },
    { label: 'Đã hủy', value: countState('cancelled') },
  ];

  const rows = useMemo(() => {
    const withShift = new Set(weekShifts.map((shift) => String(shift.employeeId)));
    return employees
      .filter((emp) =>
        searching ? withShift.has(String(emp.id)) : emp.isActive || withShift.has(String(emp.id)),
      )
      .slice()
      .sort((a, b) => employeeName(a.id).localeCompare(employeeName(b.id), 'vi'));
  }, [employees, weekShifts, searching, employeeName]);

  const focused = weekShifts.find((shift) => String(shift.id) === focusedId) ?? null;

  function detail(shift: Shift) {
    const state = shiftChipState(shift, now);
    const hours =
      (new Date(shift.endsAt).getTime() - new Date(shift.startsAt).getTime()) / 3_600_000;
    const bay = bayOf(shift.serviceBayId);
    const cancellable =
      canManage && shift.status !== 'CANCELLED' && (state === 'upcoming' || state === 'live');
    return (
      <aside
        className="timeline-detail"
        aria-label={`Chi tiết ca của ${employeeName(shift.employeeId)}`}
      >
        <header>
          <strong>
            {dayMonth(localDate(shift.startsAt))} · {hhmm(shift.startsAt)} – {hhmm(shift.endsAt)}
          </strong>
          <Badge variant={STATE_BADGE[state]}>{SHIFT_CHIP_LABELS[state]}</Badge>
          <Button variant="ghost" aria-label="Đóng chi tiết" onClick={() => setFocusedId(null)}>
            <X size={14} />
          </Button>
        </header>
        <p>
          <strong>{employeeName(shift.employeeId)}</strong>
          {shift.shiftType ? ` · ${shiftTypeLabels[shift.shiftType]}` : ''}
          {` · ${hours.toFixed(1).replace('.', ',')} giờ`}
        </p>
        <p className="muted">
          {bay ? `Khoang ${bay.bayCode} · ${bay.bayName}` : 'Không cố định khoang'}
          {shift.attendanceStatus ? ` · ${attendanceLabels[shift.attendanceStatus]}` : ''}
        </p>
        {shift.notes && <p className="muted">Ghi chú: {shift.notes}</p>}
        {cancellable && (
          <div className="row-actions">
            <Button variant="ghost" className="delete-action" onClick={() => onCancelShift(shift)}>
              <XCircle size={14} /> Hủy ca
            </Button>
          </div>
        )}
      </aside>
    );
  }

  return (
    <div className="booking-timeline">
      <div className="booking-timeline-header">
        <div className="booking-timeline-nav">
          <Button
            variant="ghost"
            aria-label="Tuần trước"
            onClick={() => onDateChange(shiftDate(firstDay, -7))}
          >
            <ChevronLeft size={16} />
            Tuần trước
          </Button>
          <strong className="booking-timeline-range">
            {dayMonth(days[0])} – {dayMonth(days[6])}/{days[6].slice(0, 4)}
          </strong>
          <Button
            variant="ghost"
            aria-label="Tuần sau"
            onClick={() => onDateChange(shiftDate(firstDay, 7))}
          >
            Tuần sau
            <ChevronRight size={16} />
          </Button>
          <Button variant="ghost" onClick={() => onDateChange(today)}>
            Tuần này
          </Button>
          <input
            type="date"
            className="input"
            aria-label="Chọn ngày để xem tuần chứa ngày đó"
            value={selectedDate}
            onChange={(event) => onDateChange(event.target.value || today)}
          />
        </div>
        <dl className="booking-timeline-summary">
          {summary.map((item) => (
            <div key={item.label}>
              <dt>{item.label}</dt>
              <dd>{item.value}</dd>
            </div>
          ))}
        </dl>
      </div>

      {rows.length === 0 ? (
        <p className="timeline-hint">
          {searching
            ? 'Không có ca nào khớp với từ khóa trong tuần này.'
            : 'Chưa có kỹ thuật viên nào.'}
        </p>
      ) : (
        <div className="timeline-week-scroll">
          <div
            className="timeline-week timeline-week-staff"
            role="grid"
            aria-label="Sơ đồ ca làm việc theo tuần"
          >
            <div className="timeline-week-row timeline-week-head" role="row">
              <div className="timeline-corner" role="columnheader">
                Kỹ thuật viên
              </div>
              {days.map((day, index) => (
                <div
                  key={day}
                  role="columnheader"
                  className={`timeline-day-head ${day === today ? 'is-today' : ''}`.trim()}
                >
                  <strong>{WEEKDAY_LABELS[index]}</strong>
                  <span>{dayMonth(day)}</span>
                </div>
              ))}
            </div>
            {rows.map((employee) => (
              <div key={employee.id} className="timeline-week-row" role="row">
                <div className="timeline-staff-head" role="rowheader">
                  <strong>{employeeName(employee.id)}</strong>
                  <span>
                    {employee.employeeCode}
                    {employee.canQc ? ' · QC' : ''}
                  </span>
                </div>
                {days.map((day) => {
                  const list = cells.get(cellKey(employee.id, day)) ?? [];
                  return (
                    <div
                      key={day}
                      role="gridcell"
                      className={`timeline-cell ${day === today ? 'is-today' : ''}`.trim()}
                    >
                      {list.map((shift) => {
                        const state = shiftChipState(shift, now);
                        const bay = bayOf(shift.serviceBayId);
                        return (
                          <button
                            key={shift.id}
                            type="button"
                            className={`timeline-chip timeline-chip-${state} ${
                              String(shift.id) === focusedId ? 'is-focused' : ''
                            }`.trim()}
                            aria-label={`Ca ${employeeName(shift.employeeId)} ${hhmm(shift.startsAt)} đến ${hhmm(
                              shift.endsAt,
                            )}, ${SHIFT_CHIP_LABELS[state]}`}
                            onClick={() => setFocusedId(String(shift.id))}
                          >
                            <strong>
                              {hhmm(shift.startsAt)}–{hhmm(shift.endsAt)}
                            </strong>
                            <span>
                              {shift.shiftType ? shiftTypeLabels[shift.shiftType] : 'Ca làm việc'}
                            </span>
                            {bay && <small>{bay.bayCode}</small>}
                          </button>
                        );
                      })}
                      {canManage && (
                        <button
                          type="button"
                          className="timeline-add"
                          aria-label={`Xếp ca cho ${employeeName(employee.id)} ngày ${dayMonth(day)}`}
                          title={`Xếp ca cho ${employeeName(employee.id)} ngày ${dayMonth(day)}`}
                          onClick={() => onAddShift(employee.id, day)}
                        >
                          <Plus size={12} aria-hidden />
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            ))}
          </div>
        </div>
      )}

      {focused ? (
        detail(focused)
      ) : (
        <p className="timeline-hint">
          Bấm vào một ca để xem chi tiết
          {canManage
            ? ' và hủy ca, hoặc bấm dấu + trong ô trống để xếp ca cho đúng người và ngày đó'
            : ''}
          .
        </p>
      )}
    </div>
  );
}

import { formatPlate } from '../../../shared/utils/format';
import { useMemo, useState } from 'react';
import {
  CalendarClock,
  CalendarX,
  CheckCircle,
  ChevronLeft,
  ChevronRight,
  LogIn,
  Plus,
  UserX,
  X,
} from 'lucide-react';
import { Button } from '../../../shared/components/ui';
import type { Booking } from '../../../shared/types/contracts';
import { BookingStatusBadge } from './BookingCards';
import {
  MAX_BAYS_PER_HOUR,
  SLOT_LOAD_LABELS,
  WEEKDAY_LABELS,
  WORKSHOP_HOUR_SLOTS,
  isActiveBooking,
  localDate,
  localDateTime,
  shiftDate,
  slotLoad,
  todayKey,
  weekStart,
} from '../constants/bookingSlots';

export interface BookingTimelineBoardProps {
  readonly bookings: readonly Booking[];
  readonly selectedDate: string; // YYYY-MM-DD, ngày bất kỳ trong tuần đang xem
  readonly onDateChange: (date: string) => void;
  readonly canManage: boolean;
  readonly onQuickBookSlot: (isoDateTimeLocal: string) => void;
  readonly onConfirm: (booking: Booking) => void;
  readonly onCheckIn: (booking: Booking) => void;
  readonly onReschedule: (booking: Booking) => void;
  readonly onCancel: (booking: Booking) => void;
  readonly onNoShow: (booking: Booking) => void;
}

const OUTSIDE_HOURS = -1;
const dayMonth = (dateKey: string) => `${dateKey.slice(8, 10)}/${dateKey.slice(5, 7)}`;

export function BookingTimelineBoard({
  bookings,
  selectedDate,
  onDateChange,
  canManage,
  onQuickBookSlot,
  onConfirm,
  onCheckIn,
  onReschedule,
  onCancel,
  onNoShow,
}: BookingTimelineBoardProps) {
  const today = todayKey();
  const firstDay = weekStart(selectedDate || today);
  const days = useMemo(
    () => Array.from({ length: 7 }, (_, i) => shiftDate(firstDay, i)),
    [firstDay],
  );
  const [focusedId, setFocusedId] = useState<string | null>(null);

  // Gom lịch hẹn theo ô: khóa "ngày|giờ"
  const cells = useMemo(() => {
    const map = new Map<string, Booking[]>();
    for (const booking of bookings) {
      const local = localDateTime(booking.appointmentAt);
      const day = local.slice(0, 10);
      if (!days.includes(day)) continue;
      const hour = Number(local.slice(11, 13));
      const row = WORKSHOP_HOUR_SLOTS.includes(hour) ? hour : OUTSIDE_HOURS;
      const key = `${day}|${row}`;
      map.set(key, [...(map.get(key) ?? []), booking]);
    }
    for (const list of map.values())
      list.sort((a, b) => a.appointmentAt.localeCompare(b.appointmentAt));
    return map;
  }, [bookings, days]);

  const weekBookings = useMemo(() => [...cells.values()].flat(), [cells]);
  const countStatus = (status: Booking['status']) =>
    weekBookings.filter((booking) => booking.status === status).length;
  const summary = [
    { label: 'Tổng lịch hẹn trong tuần', value: weekBookings.length },
    { label: 'Chờ xác nhận', value: countStatus('PENDING') },
    { label: 'Đã xác nhận', value: countStatus('CONFIRMED') },
    { label: 'Đã tiếp nhận', value: countStatus('RECEIVED') },
  ];
  const hasOutside = days.some((day) => cells.has(`${day}|${OUTSIDE_HOURS}`));
  const rows = hasOutside ? [...WORKSHOP_HOUR_SLOTS, OUTSIDE_HOURS] : WORKSHOP_HOUR_SLOTS;
  const focused = weekBookings.find((booking) => booking.id === focusedId) ?? null;

  function detail(booking: Booking) {
    const open = booking.status === 'PENDING' || booking.status === 'CONFIRMED';
    return (
      <aside className="timeline-detail" aria-label={`Chi tiết lịch hẹn ${booking.bookingCode}`}>
        <header>
          <strong>
            {dayMonth(localDate(booking.appointmentAt))} ·{' '}
            {localDateTime(booking.appointmentAt).slice(11, 16)}
          </strong>
          <span className="code-tag">#{booking.bookingCode}</span>
          <BookingStatusBadge status={booking.status} />
          <Button variant="ghost" aria-label="Đóng chi tiết" onClick={() => setFocusedId(null)}>
            <X size={14} />
          </Button>
        </header>
        <div>
          <span className="plate-badge">{formatPlate(booking.licensePlate)}</span>
          {booking.vehicleModel && <span className="muted"> {booking.vehicleModel}</span>}
        </div>
        <p>
          {booking.customerName} · {booking.customerPhone}
        </p>
        <p className="muted">
          {booking.serviceName || 'Khám tổng quát'}
          {booking.requestDescription ? ` — ${booking.requestDescription}` : ''}
        </p>
        <div className="row-actions">
          {canManage && booking.status === 'PENDING' && (
            <Button
              variant="primary"
              aria-label={`Xác nhận lịch hẹn ${booking.bookingCode}`}
              onClick={() => onConfirm(booking)}
            >
              <CheckCircle size={14} />
              Xác nhận
            </Button>
          )}
          {canManage && booking.status === 'CONFIRMED' && (
            <Button
              variant="primary"
              aria-label={`Tiếp nhận xe ${formatPlate(booking.licensePlate)} vào xưởng`}
              onClick={() => onCheckIn(booking)}
            >
              <LogIn size={14} />
              Tiếp nhận
            </Button>
          )}
          {open && (
            <>
              <Button
                variant="ghost"
                aria-label={`Dời lịch hẹn ${booking.bookingCode}`}
                onClick={() => onReschedule(booking)}
              >
                <CalendarClock size={14} />
                Dời lịch
              </Button>
              {canManage && booking.status === 'CONFIRMED' && (
                <Button
                  variant="ghost"
                  aria-label={`Đánh dấu vắng mặt lịch hẹn ${booking.bookingCode}`}
                  onClick={() => onNoShow(booking)}
                >
                  <UserX size={14} />
                  Vắng mặt
                </Button>
              )}
              <Button
                variant="ghost"
                className="delete-action"
                aria-label={`Hủy lịch hẹn ${booking.bookingCode}`}
                onClick={() => onCancel(booking)}
              >
                <CalendarX size={14} />
                Hủy
              </Button>
            </>
          )}
        </div>
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

      <div className="timeline-week-scroll">
        <div className="timeline-week" role="grid" aria-label="Sơ đồ lịch hẹn theo tuần">
          <div className="timeline-week-row timeline-week-head" role="row">
            <div className="timeline-corner" role="columnheader">
              Giờ
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
          {rows.map((hour) => (
            <div key={hour} className="timeline-week-row" role="row">
              <div className="timeline-hour-head" role="rowheader">
                {hour === OUTSIDE_HOURS ? 'Khác' : `${String(hour).padStart(2, '0')}:00`}
              </div>
              {days.map((day) => {
                const list = cells.get(`${day}|${hour}`) ?? [];
                const active = list.filter(isActiveBooking).length;
                const load = slotLoad(active);
                const hh = String(hour).padStart(2, '0');
                return (
                  <div
                    key={day}
                    role="gridcell"
                    className={`timeline-cell timeline-cell-${load} ${
                      day === today ? 'is-today' : ''
                    }`.trim()}
                  >
                    {list.length > 0 && (
                      <span
                        className="timeline-cell-load"
                        title={`${active}/${MAX_BAYS_PER_HOUR} khoang · ${SLOT_LOAD_LABELS[load]}`}
                      >
                        {active}/{MAX_BAYS_PER_HOUR}
                      </span>
                    )}
                    {list.map((booking) => (
                      <button
                        key={booking.id}
                        type="button"
                        className={`timeline-chip timeline-chip-${booking.status.toLowerCase()} ${
                          booking.id === focusedId ? 'is-focused' : ''
                        }`.trim()}
                        aria-label={`Lịch hẹn ${booking.bookingCode} lúc ${localDateTime(
                          booking.appointmentAt,
                        ).slice(11, 16)}, xe ${formatPlate(booking.licensePlate)}`}
                        onClick={() => setFocusedId(booking.id)}
                      >
                        <strong>{localDateTime(booking.appointmentAt).slice(11, 16)}</strong>
                        <span>{formatPlate(booking.licensePlate)}</span>
                        <small>{booking.customerName}</small>
                      </button>
                    ))}
                    {hour !== OUTSIDE_HOURS && (
                      <button
                        type="button"
                        className="timeline-add"
                        aria-label={`Đặt lịch ${dayMonth(day)} lúc ${hh}:00`}
                        title={`Đặt lịch ${dayMonth(day)} lúc ${hh}:00`}
                        onClick={() => onQuickBookSlot(`${day}T${hh}:00`)}
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

      {focused ? (
        detail(focused)
      ) : (
        <p className="timeline-hint">
          Bấm vào một lịch hẹn để xem chi tiết và thao tác, hoặc bấm dấu + trong ô trống để đặt lịch
          đúng ngày giờ đó.
        </p>
      )}
    </div>
  );
}

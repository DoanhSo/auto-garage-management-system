import { useMemo } from 'react';
import type { Booking } from '../../../shared/types/contracts';
import { useBookingsQuery } from '../hooks/useReception';
import {
  PICKER_TIME_SLOTS,
  isActiveBooking,
  isSlotInPast,
  localDateTime,
  todayKey,
} from '../constants/bookingSlots';

export interface BookingTimeSlotPickerProps {
  readonly value: string; // YYYY-MM-DDTHH:mm
  readonly onChange: (nextDateTimeLocal: string) => void;
  readonly bookings?: readonly Booking[];
  readonly excludeBookingId?: string;
  readonly disabled?: boolean;
}

function slotEnd(slot: string): string {
  const [hour, minute] = slot.split(':').map(Number);
  const total = hour * 60 + minute + 30;
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
}

export function BookingTimeSlotPicker({
  value,
  onChange,
  bookings,
  excludeBookingId,
  disabled,
}: BookingTimeSlotPickerProps) {
  const fetched = useBookingsQuery(undefined);
  const source = bookings ?? fetched.data;
  const day = value ? value.slice(0, 10) : todayKey();
  const selectedTime = value && value.includes('T') ? value.slice(11, 16) : '';

  const countBySlot = useMemo(() => {
    const counts = new Map<string, number>();
    for (const booking of source ?? []) {
      if (!isActiveBooking(booking) || booking.id === excludeBookingId) continue;
      const local = localDateTime(booking.appointmentAt);
      if (local.slice(0, 10) !== day) continue;
      const time = local.slice(11, 16);
      const slot = PICKER_TIME_SLOTS.find((start) => time >= start && time < slotEnd(start));
      if (slot) counts.set(slot, (counts.get(slot) ?? 0) + 1);
    }
    return counts;
  }, [source, day, excludeBookingId]);

  return (
    <div className="timeslot-picker" role="group" aria-label={`Khung giờ ngày ${day}`}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
        <p className="timeslot-picker-title">Khung giờ tiếp nhận (30 phút / lượt)</p>
        <span style={{ fontSize: '11px', color: 'var(--muted, #64748b)' }}>
          08:00–11:30 &bull; 13:30–17:30
        </span>
      </div>
      <div className="timeslot-picker-grid">
        {PICKER_TIME_SLOTS.map((slot) => {
          const count = countBySlot.get(slot) ?? 0;
          const isPast = isSlotInPast(day, slot);
          const isFull = count >= 5;
          const selected = slot === selectedTime && !isPast;
          const isSlotDisabled = disabled || isPast || isFull;

          return (
            <button
              key={slot}
              type="button"
              disabled={isSlotDisabled}
              aria-pressed={selected}
              title={
                isPast
                  ? 'Khung giờ này đã qua'
                  : isFull
                    ? 'Khung giờ đã kín lịch'
                    : `Chọn khung giờ ${slot}`
              }
              className={`timeslot-picker-slot ${selected ? 'is-selected' : ''} ${
                isPast ? 'is-past' : isFull ? 'is-busy' : count >= 3 ? 'is-busy' : ''
              }`.trim()}
              onClick={() => onChange(`${day}T${slot}`)}
            >
              <strong>{slot}</strong>
              <span>
                {selected
                  ? 'Đang chọn'
                  : isPast
                    ? 'Đã qua'
                    : isFull
                      ? 'Đầy'
                      : count === 0
                        ? 'Trống'
                        : `${count} xe`}
              </span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

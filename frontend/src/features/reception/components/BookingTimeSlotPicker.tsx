import { useMemo } from 'react';
import type { Booking } from '../../../shared/types/contracts';
import { useBookingsQuery } from '../hooks/useReception';
import {
  PICKER_TIME_SLOTS,
  isActiveBooking,
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
  const selectedTime = value ? value.slice(11, 16) : '';

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
    <div className="timeslot-picker" role="group" aria-label={`Chọn nhanh khung giờ ngày ${day}`}>
      <p className="timeslot-picker-title">Chọn nhanh khung giờ trong ngày</p>
      <div className="timeslot-picker-grid">
        {PICKER_TIME_SLOTS.map((slot) => {
          const count = countBySlot.get(slot) ?? 0;
          const selected = slot === selectedTime;
          return (
            <button
              key={slot}
              type="button"
              disabled={disabled}
              aria-pressed={selected}
              className={`timeslot-picker-slot ${selected ? 'is-selected' : ''} ${
                count >= 3 ? 'is-busy' : ''
              }`.trim()}
              onClick={() => onChange(`${day}T${slot}`)}
            >
              <strong>{slot}</strong>
              <span>{selected ? 'Đang chọn' : count === 0 ? 'Trống' : `${count} xe đã đặt`}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}

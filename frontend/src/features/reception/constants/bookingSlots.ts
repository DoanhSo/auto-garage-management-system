import { dateTimeInput } from '../../../shared/utils/dates';
import type { Booking } from '../../../shared/types/contracts';

/** Số khoang kỹ thuật của xưởng, dùng làm sức chứa tối đa mỗi khung giờ. */
export const MAX_BAYS_PER_HOUR = 6;

/** Khung giờ chuẩn của xưởng: ca sáng 08–11h, ca chiều 13–17h. */
export const WORKSHOP_HOUR_SLOTS: readonly number[] = [8, 9, 10, 11, 13, 14, 15, 16, 17];

/** Các mốc giờ chọn nhanh trong form đặt lịch / dời lịch. */
export const PICKER_TIME_SLOTS: readonly string[] = [
  '08:00',
  '08:30',
  '09:00',
  '09:30',
  '10:00',
  '10:30',
  '11:00',
  '13:30',
  '14:00',
  '14:30',
  '15:00',
  '15:30',
  '16:00',
  '16:30',
  '17:00',
];

export type SlotLoad = 'light' | 'busy' | 'full';

export const SLOT_LOAD_LABELS: Record<SlotLoad, string> = {
  light: 'Thông thoáng',
  busy: 'Khá đông',
  full: 'Sắp kín',
};

export function slotLoad(count: number): SlotLoad {
  if (count >= 5) return 'full';
  if (count >= 3) return 'busy';
  return 'light';
}

/** Lịch hẹn còn hiệu lực (chiếm chỗ trong khung giờ). */
export function isActiveBooking(booking: Booking): boolean {
  return booking.status !== 'CANCELLED' && booking.status !== 'NO_SHOW';
}

/** Chuỗi `YYYY-MM-DDTHH:mm` theo giờ địa phương. */
export function localDateTime(iso: string): string {
  return dateTimeInput(iso);
}

/** Ngày `YYYY-MM-DD` theo giờ địa phương. */
export function localDate(iso: string): string {
  return localDateTime(iso).slice(0, 10);
}

export function todayKey(): string {
  return localDate(new Date().toISOString());
}

/** Ngày thứ Hai của tuần chứa `dateKey`. */
export function weekStart(dateKey: string): string {
  const date = new Date(`${dateKey || todayKey()}T00:00`);
  const offset = (date.getDay() + 6) % 7;
  return shiftDate(localDate(date.toISOString()), -offset);
}

export const WEEKDAY_LABELS: readonly string[] = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

export function shiftDate(dateKey: string, days: number): string {
  const date = new Date(`${dateKey || todayKey()}T00:00`);
  date.setDate(date.getDate() + days);
  return localDate(date.toISOString());
}

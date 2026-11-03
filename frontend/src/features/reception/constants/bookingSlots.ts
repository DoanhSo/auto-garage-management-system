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

/** Kiểm tra một khung giờ trên một ngày cụ thể có nằm trong quá khứ so với hiện tại hay không. */
export function isSlotInPast(day: string, slot: string): boolean {
  if (!day || !slot) return false;
  const [year, month, d] = day.split('-').map(Number);
  const [hour, minute] = slot.split(':').map(Number);
  if (isNaN(year) || isNaN(month) || isNaN(d) || isNaN(hour) || isNaN(minute)) return false;
  const slotDate = new Date(year, month - 1, d, hour, minute, 0, 0);
  return slotDate.getTime() <= Date.now();
}

/**
 * Trả về khung giờ mặc định hợp lệ ở tương lai.
 * Nếu initial đã chọn hợp lệ trong tương lai thì giữ nguyên.
 * Nếu không thì chọn khung giờ khả dụng kế tiếp trong ngày hôm nay, hoặc 08:30 ngày mai.
 */
export function getInitialAppointmentSlot(initial?: string): string {
  if (initial && initial.includes('T')) {
    const d = new Date(initial);
    if (!isNaN(d.getTime()) && d.getTime() > Date.now()) {
      return initial;
    }
  }
  const now = new Date();
  const today = todayKey();
  // Tìm khung giờ hôm nay còn cách hiện tại ít nhất 10 phút
  const upcomingToday = PICKER_TIME_SLOTS.find((slot) => {
    const [h, m] = slot.split(':').map(Number);
    const slotDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), h, m, 0, 0);
    return slotDate.getTime() > now.getTime() + 10 * 60 * 1000;
  });

  if (upcomingToday) {
    return `${today}T${upcomingToday}`;
  }

  // Nếu hôm nay đã hết giờ làm việc (sau 17h), mặc định sang sáng mai 08:30
  const tomorrow = new Date(now);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const tomorrowKey = localDate(tomorrow.toISOString());
  return `${tomorrowKey}T08:30`;
}

/** Định dạng hiển thị lịch hẹn thân thiện: `dd/mm/yyyy lúc HH:mm` */
export function formatAppointmentDisplay(isoLocal?: string): string {
  if (!isoLocal || !isoLocal.includes('T')) return 'Chưa chọn';
  const [datePart, timePart] = isoLocal.split('T');
  const [year, month, day] = datePart.split('-');
  if (!year || !month || !day || !timePart) return 'Chưa chọn';
  return `${day}/${month}/${year} lúc ${timePart}`;
}

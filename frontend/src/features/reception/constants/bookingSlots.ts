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

// Các hàm tuần/ngày dùng chung với sơ đồ lịch ca nên nằm ở shared; giữ lại đường dẫn cũ cho màn đặt lịch.
export {
  WEEKDAY_LABELS,
  localDate,
  localDateTime,
  shiftDate,
  todayKey,
  weekStart,
} from '../../../shared/utils/weekCalendar';

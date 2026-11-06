import { dateTimeInput } from './dates';

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

export function shiftDate(dateKey: string, days: number): string {
  const date = new Date(`${dateKey || todayKey()}T00:00`);
  date.setDate(date.getDate() + days);
  return localDate(date.toISOString());
}

/** Ngày thứ Hai của tuần chứa `dateKey`. */
export function weekStart(dateKey: string): string {
  const date = new Date(`${dateKey || todayKey()}T00:00`);
  const offset = (date.getDay() + 6) % 7;
  return shiftDate(localDate(date.toISOString()), -offset);
}

export const WEEKDAY_LABELS: readonly string[] = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

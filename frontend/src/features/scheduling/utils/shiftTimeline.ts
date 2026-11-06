import { localDate } from '../../../shared/utils/weekCalendar';
import { matchesSearch } from '../../../shared/utils/text';
import type { Employee, ServiceBay, Shift } from '../types/scheduling';
import {
  attendanceLabels,
  parseSkills,
  shiftStatusLabels,
  shiftTypeLabels,
  SKILL_LABELS,
} from '../types/scheduling';

export type ShiftChipState = 'upcoming' | 'live' | 'done' | 'cancelled' | 'pending';

export const SHIFT_CHIP_LABELS: Record<ShiftChipState, string> = {
  upcoming: 'Sắp tới',
  live: 'Đang trực',
  done: 'Đã xong',
  cancelled: 'Đã hủy',
  pending: 'Chờ duyệt',
};

/** Tình trạng hiển thị của một ca tại thời điểm `now` (mili giây). */
export function shiftChipState(shift: Shift, now: number): ShiftChipState {
  if (shift.status === 'CANCELLED' || shift.status === 'REJECTED') return 'cancelled';
  if (shift.status === 'PENDING') return 'pending';
  const start = new Date(shift.startsAt).getTime();
  const end = new Date(shift.endsAt).getTime();
  if (now < start) return 'upcoming';
  if (now <= end) return 'live';
  return 'done';
}

export const cellKey = (employeeId: string | number, day: string) => `${employeeId}|${day}`;

/** Gom ca vào ô `kỹ thuật viên|ngày`; ca qua nửa đêm nằm ở ngày bắt đầu. Ca ngoài tuần bị bỏ. */
export function groupShiftsByCell(
  shifts: readonly Shift[],
  days: readonly string[],
): Map<string, Shift[]> {
  const map = new Map<string, Shift[]>();
  for (const shift of shifts) {
    const day = localDate(shift.startsAt);
    if (!days.includes(day)) continue;
    const key = cellKey(shift.employeeId, day);
    map.set(key, [...(map.get(key) ?? []), shift]);
  }
  for (const list of map.values()) list.sort((a, b) => a.startsAt.localeCompare(b.startsAt));
  return map;
}

export interface ShiftSearchContext {
  employeeOf: (id: string | number) => Employee | undefined;
  nameOf: (id: string | number) => string;
  bayOf: (id?: string | number | null) => ServiceBay | undefined;
}

/**
 * Lọc ca theo chữ gõ (không phân biệt hoa thường và dấu): tên, mã, tài khoản, chuyên môn,
 * loại ca, tình trạng, mã và tên khoang, ghi chú.
 */
export function filterShifts(
  shifts: readonly Shift[],
  term: string,
  ctx: ShiftSearchContext,
): Shift[] {
  if (!term.trim()) return [...shifts];
  return shifts.filter((shift) => {
    const employee = ctx.employeeOf(shift.employeeId);
    const bay = ctx.bayOf(shift.serviceBayId);
    return matchesSearch(
      term,
      ctx.nameOf(shift.employeeId),
      employee?.employeeCode,
      employee?.username,
      parseSkills(employee?.skillsJson)
        .map((skill) => SKILL_LABELS[skill] ?? skill)
        .join(' '),
      employee?.canQc ? 'QC kiểm định' : undefined,
      shift.shiftType ? shiftTypeLabels[shift.shiftType] : undefined,
      shiftStatusLabels[shift.status],
      shift.attendanceStatus ? attendanceLabels[shift.attendanceStatus] : undefined,
      bay?.bayCode,
      bay?.bayName,
      shift.notes,
    );
  });
}

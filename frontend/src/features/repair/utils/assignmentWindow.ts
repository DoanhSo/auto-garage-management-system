/** Mặc định một phân công kéo dài 2 giờ nhưng không vượt quá hết ca của thợ. */
export const DEFAULT_ASSIGNMENT_HOURS = 2;

/** Định dạng `datetime-local` theo giờ máy người dùng. */
export function toLocalInput(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

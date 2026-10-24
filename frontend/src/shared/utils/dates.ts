import { Temporal } from '@js-temporal/polyfill';
export function dateTimeInput(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const twoDigits = (number: number) => String(number).padStart(2, '0');
  return `${date.getFullYear()}-${twoDigits(date.getMonth() + 1)}-${twoDigits(date.getDate())}T${twoDigits(date.getHours())}:${twoDigits(date.getMinutes())}`;
}
export interface ReportRange {
  from: string;
  until: string;
}
export function reportFileName(range: ReportRange, zone: string) {
  const date = Temporal.Instant.from(range.from).toZonedDateTimeISO(zone).toPlainDate().toString();
  return `bao-cao-gara-${date}.csv`;
}
export function today(zone: string) {
  return Temporal.Now.plainDateISO(zone).toString();
}
export function dateRange(
  fromDate: string,
  throughDate: string,
  zone: string,
  maxSeconds: number,
): ReportRange {
  const from = Temporal.PlainDate.from(fromDate)
    .toZonedDateTime({ timeZone: zone, plainTime: '00:00' })
    .toInstant();
  const until = Temporal.PlainDate.from(throughDate)
    .add({ days: 1 })
    .toZonedDateTime({ timeZone: zone, plainTime: '00:00' })
    .toInstant();
  const seconds = Number(until.epochNanoseconds - from.epochNanoseconds) / 1e9;
  if (seconds <= 0) throw new Error('Ngày kết thúc phải bằng hoặc sau ngày bắt đầu.');
  if (seconds > maxSeconds) throw new Error('Khoảng ngày vượt giới hạn báo cáo của hệ thống.');
  return { from: from.toString(), until: until.toString() };
}
export function initialReportDates(zone: string, maxSeconds: number) {
  const end = Temporal.PlainDate.from(today(zone));
  const days = Math.max(1, Math.min(7, Math.floor(maxSeconds / 86400)));
  return {
    from: end.subtract({ days: days - 1 }).toString(),
    through: end.toString(),
  };
}

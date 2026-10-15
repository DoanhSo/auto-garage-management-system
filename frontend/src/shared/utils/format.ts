import Decimal from 'decimal.js';
export function money(value: string): string {
  const decimal = new Decimal(value);
  const [whole, fraction] = decimal.abs().toFixed(2).split('.');
  return `${decimal.isNegative() ? '-' : ''}${new Intl.NumberFormat('vi-VN').format(BigInt(whole))}${fraction === '00' ? '' : ',' + fraction} ₫`;
}
export function hours(value: string): string {
  return new Decimal(value).toFixed(2).replace('.', ',') + ' giờ';
}
export function dateTime(value: string, zone: string): string {
  const date = new Date(value);
  return Number.isNaN(date.valueOf())
    ? 'Không xác định'
    : new Intl.DateTimeFormat('vi-VN', {
        dateStyle: 'short',
        timeStyle: 'short',
        timeZone: zone,
      }).format(date);
}
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(-2)
    .map((part) => Array.from(part)[0])
    .join('')
    .toLocaleUpperCase('vi');
}
function hasUnsafeCharacters(value: string) {
  return Array.from(value).some((char) => char === '\\' || char.charCodeAt(0) < 32);
}
export function safeReturnTo(value: unknown, fallback: string): string {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/') ||
    value.startsWith('//') ||
    hasUnsafeCharacters(value)
  )
    return fallback;
  try {
    const decoded = decodeURIComponent(value);
    if (
      decoded.startsWith('//') ||
      hasUnsafeCharacters(decoded) ||
      value.split('?')[0] === '/login'
    )
      return fallback;
  } catch {
    return fallback;
  }
  return value;
}

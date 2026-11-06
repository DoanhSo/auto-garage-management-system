import Decimal from 'decimal.js';

export function money(value: string | number): string {
  const decimal = new Decimal(value.toString());
  const [whole, fraction] = decimal.abs().toFixed(2).split('.');
  return `${decimal.isNegative() ? '-' : ''}${new Intl.NumberFormat('vi-VN').format(BigInt(whole))}${fraction === '00' ? '' : ',' + fraction} ₫`;
}

export function hours(value: string): string {
  return new Decimal(value).toFixed(2).replace('.', ',') + ' giờ';
}

export function formatPlate(value?: string | null): string {
  if (!value) return '';
  const clean = value.trim().toUpperCase().replace(/\s+/g, '');
  const car5Match = clean.match(/^(\d{2}[A-Z]{1,2})[-.]?(\d{3})[-.]?(\d{2})$/);
  if (car5Match) {
    return car5Match[1] + '-' + car5Match[2] + '.' + car5Match[3];
  }
  const car4Match = clean.match(/^(\d{2}[A-Z]{1,2})[-.]?(\d{4})$/);
  if (car4Match) {
    return car4Match[1] + '-' + car4Match[2];
  }
  const moto5Match = clean.match(/^(\d{2}[A-Z]\d|\d{2}-[A-Z]\d)[-.]?(\d{3})[-.]?(\d{2})$/);
  if (moto5Match) {
    const prefix = moto5Match[1].replace('-', '');
    return prefix + '-' + moto5Match[2] + '.' + moto5Match[3];
  }
  return clean;
}
export function dateTime(value: string, zone?: string): string {
  const date = new Date(value);
  return Number.isNaN(date.valueOf())
    ? 'Không xác định'
    : new Intl.DateTimeFormat('vi-VN', {
        dateStyle: 'short',
        timeStyle: 'short',
        ...(zone ? { timeZone: zone } : {}),
      }).format(date);
}

export function formatDate(value?: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.valueOf())
    ? ''
    : new Intl.DateTimeFormat('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
      }).format(date);
}

export function formatDateTime(value?: string | null): string {
  if (!value) return '';
  const date = new Date(value);
  return Number.isNaN(date.valueOf())
    ? ''
    : new Intl.DateTimeFormat('vi-VN', {
        dateStyle: 'short',
        timeStyle: 'short',
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

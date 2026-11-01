export type CsvCell = string | number | boolean | null | undefined;
export type CsvRow = CsvCell[];

const BOM = '﻿';

/** Escape theo RFC 4180: bọc trong ngoặc kép nếu có dấu phẩy, ngoặc kép hoặc xuống dòng. */
export function csvCell(value: CsvCell): string {
  if (value === null || value === undefined) return '';
  let text = String(value);
  // Chặn CSV/formula injection khi mở bằng Excel.
  if (/^[=+\-@\t\r]/.test(text) && Number.isNaN(Number(text))) text = `'${text}`;
  return /[",\r\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

export function csvLine(row: CsvRow): string {
  return row.map(csvCell).join(',');
}

/** Một bảng CSV: dòng tiêu đề cột + các dòng dữ liệu. */
export function csvTable(headers: string[], rows: CsvRow[]): string {
  return [csvLine(headers), ...rows.map(csvLine)].join('\r\n');
}

/** Nhiều phần, mỗi phần có tiêu đề riêng, ngăn cách bằng dòng trống. */
export function csvSections(sections: { title: string; headers: string[]; rows: CsvRow[] }[]) {
  return sections
    .map((section) => `${csvLine([section.title])}\r\n${csvTable(section.headers, section.rows)}`)
    .join('\r\n\r\n');
}

/** Tải file UTF-8 có BOM để Excel (Windows/macOS) hiển thị đúng tiếng Việt. */
export function downloadCsv(filename: string, content: string): void {
  const blob = new Blob([BOM, content], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename.endsWith('.csv') ? filename : `${filename}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

/** Số tiền xuất dạng số thuần (không dấu phân cách) để Excel tính toán được. */
export const csvMoney = (value: string | number | null | undefined) => Number(value ?? 0);

export function csvDate(value?: string | null): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

export function csvDateTime(value?: string | null): string {
  if (!value) return '';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return String(value);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${csvDate(value)} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

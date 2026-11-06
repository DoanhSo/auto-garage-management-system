/**
 * Chuẩn hóa chuỗi để tìm kiếm: bỏ dấu tiếng Việt, hạ chữ thường, gộp khoảng trắng.
 * Dùng cho cả hai phía (từ khóa và dữ liệu) để "Dang nhap" khớp "Đăng nhập".
 */
export function normalizeSearchText(value: string | null | undefined): string {
  return (value ?? '')
    .normalize('NFD')
    .replace(/\p{M}/gu, '')
    .replace(/đ/gi, 'd')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, ' ');
}

/** Từ khóa có khớp một trong các trường văn bản không (từ khóa rỗng thì luôn khớp). */
export function matchesSearch(term: string, ...fields: Array<string | null | undefined>): boolean {
  const needle = normalizeSearchText(term);
  if (!needle) return true;
  return normalizeSearchText(fields.filter(Boolean).join(' ')).includes(needle);
}

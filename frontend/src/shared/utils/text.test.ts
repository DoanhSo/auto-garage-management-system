import { describe, expect, it } from 'vitest';
import { matchesSearch, normalizeSearchText } from './text';

describe('normalizeSearchText', () => {
  it('bỏ dấu tiếng Việt và hạ chữ thường', () => {
    expect(normalizeSearchText('Đăng Nhập  Hệ Thống')).toBe('dang nhap he thong');
  });

  it('xử lý null và chuỗi rỗng', () => {
    expect(normalizeSearchText(null)).toBe('');
    expect(normalizeSearchText(undefined)).toBe('');
    expect(normalizeSearchText('   ')).toBe('');
  });
});

describe('matchesSearch', () => {
  it('từ khóa rỗng luôn khớp', () => {
    expect(matchesSearch('', 'bất kỳ')).toBe(true);
    expect(matchesSearch('   ')).toBe(true);
  });

  it('khớp không phân biệt dấu và hoa thường trên nhiều trường', () => {
    expect(matchesSearch('ca sang', 'Nguyễn Văn An', 'Ca sáng')).toBe(true);
    expect(matchesSearch('NGUYEN', 'Nguyễn Văn An')).toBe(true);
  });

  it('bỏ qua trường rỗng và báo không khớp khi không có chữ nào chứa từ khóa', () => {
    expect(matchesSearch('b01', null, undefined, 'Khoang A1')).toBe(false);
  });
});

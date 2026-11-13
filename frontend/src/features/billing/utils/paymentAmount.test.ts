import { describe, expect, it } from 'vitest';
import { validatePaymentAmount } from './paymentAmount';

describe('validatePaymentAmount', () => {
  it('chấp nhận đúng số còn nợ, kể cả số không tròn nghìn (hóa đơn #10 còn 1.365.998)', () => {
    expect(validatePaymentAmount(1_365_998, 1_365_998)).toBeNull();
    expect(validatePaymentAmount(3_168_000, 3_168_000)).toBeNull();
  });

  it('chấp nhận mọi số nguyên từ 1 đến số còn nợ', () => {
    expect(validatePaymentAmount(1, 3_168_000)).toBeNull();
    expect(validatePaymentAmount(1_793_001, 3_168_000)).toBeNull();
    expect(validatePaymentAmount(1_793_000, 3_168_000)).toBeNull();
    expect(validatePaymentAmount(9_001, 3_168_000)).toBeNull();
  });

  it('từ chối số không dương hoặc không hợp lệ', () => {
    expect(validatePaymentAmount(0, 100)).toBe('Số tiền thu phải lớn hơn 0.');
    expect(validatePaymentAmount(-5, 100)).toBe('Số tiền thu phải lớn hơn 0.');
    expect(validatePaymentAmount(Number.NaN, 100)).toBe('Số tiền thu phải lớn hơn 0.');
  });

  it('từ chối số vượt số còn nợ dù chỉ hơn 1 đồng và nêu số còn nợ', () => {
    expect(validatePaymentAmount(1_365_999, 1_365_998)).toBe(
      `Số tiền thu vượt số còn nợ (${(1_365_998).toLocaleString('vi-VN')} đ).`,
    );
  });

  it('không có số còn nợ thì chỉ kiểm tra số dương', () => {
    expect(validatePaymentAmount(999_999_999)).toBeNull();
    expect(validatePaymentAmount(500, null)).toBeNull();
  });

  it('phiếu chi dùng chữ "chi" trong thông báo', () => {
    expect(validatePaymentAmount(0, 100, 'chi')).toBe('Số tiền chi phải lớn hơn 0.');
    expect(validatePaymentAmount(200, 100, 'chi')).toContain('Số tiền chi vượt số còn nợ');
  });
});

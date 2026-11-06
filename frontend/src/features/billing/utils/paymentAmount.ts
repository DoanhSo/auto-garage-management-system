/**
 * Kiểm tra số tiền thu/chi trước khi gửi lên máy chủ.
 * `balance` là số còn nợ cho phép tối đa (bỏ trống nếu chưa biết); trả `null` khi hợp lệ.
 * Không dùng thuộc tính `step` của ô nhập vì trình duyệt tính bước từ `min`, làm các số tròn bị chặn.
 */
export function validatePaymentAmount(
  amount: number,
  balance?: number | null,
  kind: 'thu' | 'chi' = 'thu',
): string | null {
  if (!Number.isFinite(amount) || amount <= 0) return `Số tiền ${kind} phải lớn hơn 0.`;
  if (balance != null && Number.isFinite(balance) && amount > balance) {
    return `Số tiền ${kind} vượt số còn nợ (${balance.toLocaleString('vi-VN')} đ).`;
  }
  return null;
}

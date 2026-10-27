import type { FieldValues, Path, UseFormSetError } from 'react-hook-form';
const messages: Record<number, string> = {
  2001: 'Kiểm tra lại thông tin đã nhập.',
  2002: 'Dữ liệu không còn tồn tại.',
  2003: 'Dữ liệu trùng hoặc xung đột với yêu cầu trước. Kiểm tra tên đăng nhập và email.',
  2004: 'Thông tin yêu cầu không hợp lệ.',
  2005: 'Tài khoản đã được cập nhật ở nơi khác. Tải bản mới và xem lại thay đổi.',
  2006: 'Không thể khóa hoặc hạ quyền quản trị viên hoạt động cuối cùng.',
  2007: 'Mật khẩu chưa đáp ứng chính sách hiện tại.',
  2010: 'Dữ liệu đang được xử lý. Vui lòng tải lại trước khi thao tác.',
  2013: 'Tệp hoặc yêu cầu vượt quá dung lượng cho phép. Hãy chọn tệp nhỏ hơn.',
  3001: 'Phiên đăng nhập không còn hợp lệ.',
  3002: 'Bạn không có quyền thực hiện thao tác này.',
  3003: 'Phiên đăng nhập đã hết hạn hoặc bị thu hồi.',
  3005: 'Tên đăng nhập/email hoặc mật khẩu không đúng.',
  4001: 'Không thể hoàn tất thao tác. Vui lòng thử lại sau.',
  4002: 'Dịch vụ xác thực tạm thời không sẵn sàng. Phiên của bạn vẫn được giữ.',
  4003: 'Dịch vụ dữ liệu tạm thời không sẵn sàng.',
  4004: 'Dịch vụ thông báo tạm thời không sẵn sàng.',
};
export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: number,
    public readonly fields: Record<string, string> = {},
    public readonly uncertain = false,
  ) {
    super(
      uncertain
        ? 'Chưa xác nhận được kết quả trên máy chủ. Kiểm tra trạng thái trước khi gửi lại.'
        : (messages[code] ??
            (status === 0
              ? 'Không kết nối được máy chủ. Kiểm tra mạng và thử lại.'
              : 'Không thể xử lý yêu cầu. Vui lòng thử lại.')),
    );
    this.name = 'ApiError';
  }
}
export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : 'Không thể xử lý dữ liệu. Vui lòng thử lại.';
}
export function applyFormError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  allowed: readonly string[],
): void {
  if (error instanceof ApiError && error.code === 2001) {
    Object.entries(error.fields).forEach(([field]) => {
      if (allowed.includes(field))
        setError(field as Path<T>, {
          type: 'server',
          message: 'Giá trị không hợp lệ. Kiểm tra nội dung và độ dài.',
        });
    });
  }
  setError('root.server', { message: errorMessage(error) });
}
export function retryQuery(failures: number, error: unknown): boolean {
  return failures < 1 && error instanceof ApiError && (error.status === 0 || error.status >= 500);
}

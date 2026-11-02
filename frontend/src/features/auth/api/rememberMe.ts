// Chỉ lưu định danh đăng nhập (không bí mật); mật khẩu và token không bao giờ vào storage.
const REMEMBER_KEY = 'autogarage_remember_identifier';

export const rememberMeStorage = {
  get(): string {
    try {
      return localStorage.getItem(REMEMBER_KEY) ?? '';
    } catch {
      return '';
    }
  },
  set(identifier: string) {
    try {
      localStorage.setItem(REMEMBER_KEY, identifier.trim());
    } catch {
      // Storage có thể bị chặn (chế độ riêng tư); bỏ qua.
    }
  },
  clear() {
    try {
      localStorage.removeItem(REMEMBER_KEY);
    } catch {
      // Bỏ qua.
    }
  },
};

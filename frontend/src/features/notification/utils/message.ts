export function notificationMessage(raw: string): string {
  try {
    const data: unknown = JSON.parse(raw);
    if (data && typeof data === 'object') {
      if ('message' in data && typeof data.message === 'string') return data.message;
      if ('text' in data && typeof data.text === 'string') return data.text;
    }
  } catch {
    // Keep malformed payloads out of the reader.
  }
  return 'Thông báo từ gara. Liên hệ nhân viên phụ trách nếu cần thêm thông tin.';
}

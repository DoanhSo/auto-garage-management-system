import type { Role } from '../../../shared/types/contracts';

export interface DemoAccount {
  role: Role;
  label: string;
  badge: string;
  identifier: string;
  note: string;
}

// Chỉ điền tên đăng nhập; mật khẩu không được lưu trong mã nguồn.
export const DEMO_ACCOUNTS: readonly DemoAccount[] = [
  {
    role: 'ADMIN',
    label: 'Quản trị viên',
    badge: 'Admin',
    identifier: 'admin',
    note: 'Báo cáo, hệ thống, duyệt quỹ',
  },
  {
    role: 'RECEPTIONIST',
    label: 'Lễ tân / Cố vấn',
    badge: 'Lễ tân',
    identifier: 'receptionist',
    note: 'Tiếp nhận xe, báo giá, xếp khoang',
  },
  {
    role: 'TECHNICIAN',
    label: 'Kỹ thuật viên',
    badge: 'KTV',
    identifier: 'technician',
    note: 'Lệnh sửa, bấm giờ, nghiệm thu',
  },
  {
    role: 'WAREHOUSE_STAFF',
    label: 'Thủ kho phụ tùng',
    badge: 'Kho',
    identifier: 'warehouse',
    note: 'Phụ tùng, chứng từ kho',
  },
  {
    role: 'CUSTOMER',
    label: 'Khách hàng',
    badge: 'Khách',
    identifier: 'customer',
    note: 'Tiến độ xe, lịch hẹn, bảo hành',
  },
];

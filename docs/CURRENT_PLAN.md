# Kế Hoạch Chuyển Thanh Navigation Sang Dạng Phẳng (Single Page Navigation)

> **Mã plan**: `flat-navigation`
> **Chế độ**: `PLAN:` & `CODE:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: Yêu cầu người dùng tối ưu hóa điều hướng sidebar và loại bỏ cấu trúc cây trùng lặp.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- Thanh sidebar chính trong [`frontend/src/app/layouts/AppShell.tsx`](file:///d:/auto-garage-management-system/frontend/src/app/layouts/AppShell.tsx) đang sử dụng cấu trúc cây lồng 2 cấp (`NavTreeItem` có `children: NavChildLink[]`) cho 5 module: Khoang & Lịch ca, Bảo hành, Sổ nợ & Tín dụng, Bảng lương & Năng suất, Báo cáo.
- Bên trong từng màn hình trên đều đã có sẵn hệ thống tab điều hướng nội bộ (`<div className="tabs">`) liên kết với query param `?tab=...` qua hook `useUrlTab`.
- Cấu trúc cây ở sidebar gây ra trùng lặp điều hướng (double navigation), tốn diện tích cuộn và bị vỡ trải nghiệm khi thu gọn thanh bên (collapsed sidebar mode).

### 1.2. Mục tiêu
- Chuyển đổi toàn bộ thanh sidebar về dạng phẳng 1 cấp (Single Page / Flat Link) sạch sẽ, tinh gọn theo đúng nguyên tắc Minimalism của `docs/UI_DESIGN_GUIDELINES.md`.
- Mỗi mục điều hướng trên sidebar trỏ trực tiếp đến route trang chính (ví dụ: `/scheduling`, `/warranties`, `/payroll`, `/admin/reports`, `/billing/debt-ledger`).
- Mọi điều hướng chi tiết giữa các phân mục/góc nhìn bên trong trang được đảm nhiệm hoàn toàn bởi thanh tab ngang nội bộ trang.
- Giữ nguyên toàn bộ phân quyền vai trò (`allowedRoles`), tên nhãn ngữ cảnh (`customerLabel`, `technicianLabel`) và huy hiệu thông báo số lượng (`nav-count`).
- Dọn dẹp các selector CSS không còn sử dụng (`.nav-tree-*`).

### 1.3. Ngoài phạm vi
- Không thay đổi đường dẫn URL hay cấu trúc routing trong `App.tsx`.
- Không can thiệp vào logic các tab nội bộ bên trong các trang tính năng.

---

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Thanh sidebar hiển thị dạng phẳng 1 cấp duy nhất cho tất cả các mục điều hướng thuộc 5 nhóm phân hệ (`DỊCH VỤ & XƯỞNG`, `KHO & VẬT TƯ`, `TÀI CHÍNH & CÔNG NỢ`, `QUẢN TRỊ HỆ THỐNG`, `CÁ NHÂN`), không còn nút mũi tên toggle hay danh sách nhánh con lùi dòng.
- **AC-2**: Bấm vào bất kỳ mục menu nào đều dẫn trực tiếp đến trang tương ứng, trang mở ra với tab mặc định đã được cấu hình sẵn trong trang đó.
- **AC-3**: Chế độ thu gọn sidebar (`sidebar-collapsed`) hiển thị icon căn giữa thẳng hàng, không bị ẩn hay vỡ layout do nhánh con.
- **AC-4**: Kiểm thử frontend (`npm run check` gồm `lint + test + build`) vượt qua 100% không có lỗi hoặc cảnh báo.

---

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `frontend/src/app/layouts/AppShell.tsx` | 65–85, 110–350, 360–480 | Bỏ `NavChildLink`, đơn giản hóa `NavTreeItem` thành `NavItem`, bỏ nhánh `children`, bỏ state `expandedGroups` và dọn dẹp hàm `nav()` |
| `frontend/src/styles.css` | 4050–4130 | Dọn dẹp các class `.nav-tree-*` không còn dùng |

---

## 4. Hợp Đồng Kỹ Thuật

- Kiểu dữ liệu `NavItem`:
  ```ts
  interface NavItem {
    to: string;
    label: string;
    customerLabel?: string;
    technicianLabel?: string;
    icon: typeof Bell;
    section: NavSection;
    allowedRoles: readonly Role[];
  }
  ```
- Component `NavLink` của React Router được sử dụng đồng nhất cho mọi mục menu, tự động áp dụng class `active` khi URL khớp.

---

## 5. Checklist Thực Thi

- [x] **Step 1: Tối giản hóa dữ liệu điều hướng và giao diện AppShell.tsx** _(AC-1, AC-2, AC-3)_
- [x] **Step 2: Dọn dẹp CSS nav-tree trong styles.css** _(AC-1, AC-3)_
- [x] **Step 3: Chạy kiểm thử frontend lint, vitest và build** _(AC-4)_
- [x] **Step 4: Cập nhật Docker container và xác minh hoạt động thực tế** _(tất cả AC)_

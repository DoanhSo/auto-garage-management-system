# KẾ HOẠCH THỰC THI HIỆN TẠI (CURRENT PLAN)

> **Cập nhật lần cuối:** 2026-11-01 — **Thiết kế lại trang Đăng nhập / Đăng ký (`/login`, `/register`) theo bố cục chuẩn mẫu tham chiếu (Header điều hướng giống Landing Page trên đỉnh + Nền xám sáng `#f5f7fb` + Card trắng nổi ở giữa tích hợp khung ảnh xưởng bên trái/phải & Form tối giản kiểu Agoda) & Tối ưu toàn bộ bảng dữ liệu (`DataTable`) hiển thị 100% các cột trên màn hình KHÔNG cuộn ngang và KHÔNG gộp cột**

---

## 1. Mục tiêu & Phạm vi

### 1.1. Phần A — Thiết kế trang Đăng nhập & Đăng ký (`/login`, `/register`) theo mẫu tham chiếu + đồng bộ Landing Page
Dựa trên ảnh mẫu tham chiếu người dùng cung cấp và yêu cầu *"ảnh để bên trái hoặc bên phải"*, giao diện `/login` và `/register` được cấu trúc thành **3 thành phần rõ ràng, đồng bộ 100% với Landing Page (`home.css`) và `UI_DESIGN_GUIDELINES.md`**:

1. **Thanh Header Điều hướng trên đỉnh trang (`auth-header` — Đồng bộ 100% với `HomeHeader` của Landing Page):**
   - Nền trắng `#ffffff` toàn chiều ngang (`border-bottom: 1px solid #e3e8f0`, chiều cao `64px`).
   - Bên trái: Logo **AutoGarage Tây Sơn** (biểu tượng cờ lê nền xanh `#245bdb`).
   - Ở giữa: Thanh menu điều hướng ngang dẫn thẳng về các mục của Landing Page (*Dịch vụ*, *Quy trình*, *Khoang kỹ thuật*, *Phụ tùng*, *Đặt lịch hẹn*) giống hệt thanh menu trên ảnh mẫu.
   - Bên phải: Hotline `0963 943 872` và nút quay về `"Trang chủ"`.

2. **Nền trang (`auth-canvas`) sáng phẳng, thoáng đãng (`#f5f7fb`):**
   - Nền xám trắng nhẹ nhàng (`#f5f7fb`), căn giữa khung Card chính giữa màn hình với khoảng thở rộng rãi hai bên giống ảnh mẫu.

3. **Khối Card Trung tâm (`auth-split-card` — Card trắng `#ffffff` đổ bóng nhẹ `0 6px 28px rgba(16, 24, 40, 0.08)`, chia 2 nửa: KhungForm kiểu Agoda + Khung Ảnh xưởng bên phải/trái):**
   - **Cột Trái — Khung Ảnh Xưởng thực tế (`auth-visual-side`, ~45% chiều rộng Card):**
     - Hiển thị ảnh thực tế khu vực sảnh cố vấn dịch vụ & phòng chờ khách hàng `/images/home/garage-lounge.jpg` (`object-fit: cover; height: 100%`).
     - Phủ lớp gradient nhẹ ở chân ảnh để hiển thị thông tin nhận diện **AutoGarage Tây Sơn**:
       - Huy hiệu: `"XƯỞNG DỊCH VỤ TIÊU CHUẨN HÃNG"`
       - Điểm nhấn dịch vụ: *"Chẩn đoán số hóa OBD-II · Cân chỉnh 3D Hunter · 100% Phụ tùng chính hãng"*
       - Địa chỉ: *Số 226 Phố Tây Sơn, P. Trung Liệt, Q. Đống Đa, Hà Nội* · Hotline: *0963 943 872*.
   - **Cột Phải — Khung Form Tối giản kiểu Agoda (`auth-form-side`, ~55% chiều rộng Card, nền `#ffffff`, padding `36px 40px`):**
     - **Tiêu đề & Mô tả (Giống mẫu Agoda):**
       - `<h1>` in đậm rõ nét (`22px–24px`, màu `#101828`): `"Đăng nhập"` (trên `/login`) và `"Đăng ký tài khoản"` (trên `/register`).
       - Mô tả ngắn ngay dưới tiêu đề (`13.5px`, màu `#526278`): *"Đăng ký miễn phí hoặc đăng nhập để đặt lịch hẹn ưu tiên, theo dõi tiến độ sửa chữa và nhận ưu đãi dịch vụ!"*.
     - **Các trường nhập liệu & Nút hành động (Bo góc mềm mại, thoáng, chuẩn mẫu):**
       - Ô input nền trắng `#ffffff`, viền `#d5dce7`, bo góc `8px`, chiều cao `44px`, hiệu ứng focus viền xanh `#245bdb`.
       - Trên trang **Đăng ký (`/register`)**: Sắp xếp các trường theo **lưới 2 cột bằng nhau trên desktop** (`Họ và tên` + `Tên đăng nhập`, `Email` trải ngang, `Mật khẩu` + `Xác nhận mật khẩu`) để form gọn trong khung nhìn, không phải cuộn dọc.
       - Nút Submit chính (`Đăng nhập` / `Đăng ký`): Nút full-width nổi bật màu xanh chủ đạo `#245bdb`, bo góc tròn trịa (`border-radius: 999px` hoặc `8px` hiện đại), chiều cao `46px`, chữ trắng đậm.
     - **Dải phân cách `"hoặc"` & Chuyển đổi nhanh (`auth-divider` & `auth-alt-action`):**
       - Đường kẻ ngang mảnh với chữ `"hoặc"` ở chính giữa (giống hệt ảnh mẫu).
       - Nút/khối chuyển đổi nhẹ nền xanh nhạt `#eef4ff` chữ `#245bdb` ở dưới (`"Tạo tài khoản khách hàng mới"` khi đang ở trang Đăng nhập, hoặc `"Đăng nhập bằng tài khoản đã có"` khi đang ở trang Đăng ký).
     - **Dòng điều khoản dưới chân Card (`auth-legal-note`):**
       - Căn giữa, cỡ chữ `12px` màu `#526278`, kèm link xanh `#245bdb`: *"Khi đăng nhập, tôi đồng ý với các Điều khoản sử dụng và Chính sách bảo mật của AutoGarage Tây Sơn."* (giống hệt bố cục chân card trên ảnh mẫu).

---

### 1.2. Phần B — Hiển thị trọn vẹn 100% các cột bảng dữ liệu trên màn hình (KHÔNG cuộn ngang & KHÔNG gộp cột)
- **Mục tiêu:** Giữ nguyên **100% số lượng cột độc lập** hiện tại trên tất cả các màn hình (Tiếp nhận & Lịch hẹn giữ đủ **7 cột**, Phương tiện giữ đủ **7 cột**, Dịch vụ giữ đủ **8 cột**, cùng toàn bộ các màn hình Khách hàng, Lệnh sửa chữa, Phụ tùng, Nhà cung cấp, Chứng từ kho, Đối soát, Hóa đơn, Sổ quỹ, Công nợ, Tài khoản, Nhật ký, Báo cáo, Thông báo) nhưng **không xuất hiện thanh cuộn ngang**.
- **Giải pháp kỹ thuật:**
  1. **Tối ưu CSS bảng toàn cục trong `AutoGarage/frontend/src/styles.css`:**
     - Đặt `.table-scroll { overflow-x: visible; width: 100%; }` trên desktop để bảng luôn khớp 100% chiều rộng khung `.panel`.
     - Giảm padding ngang của `.data-table th` xuống `10px 10px` và `.data-table td` xuống `11px 10px` (tiết kiệm ngay **160px–180px** chiều ngang bị lãng phí cho khoảng trắng).
     - Cho phép tiêu đề cột `.data-table th` và nội dung ô `.data-table td` tự động xuống dòng thông minh (`white-space: normal; overflow-wrap: break-word; min-width: 0`), đồng thời bỏ `min-width: 160px` cứng trên `.account-cell`.
     - Cho phép cụm nút `.row-actions` tự động co giãn (`flex-wrap: wrap; gap: 4px;`) với kích thước nút gọn (`min-height: 28px; padding: 4px 7px; font-size: 11px`).
  2. **Gỡ bỏ `.nowrap` gây tràn ngang trên các trường văn bản mô tả dài ở từng màn hình (Giữ nguyên tất cả các cột):**
     - `BookingsPage.tsx` (giữ đủ **7 cột**): bỏ `.nowrap` trên `vehicleModel` và `requestDescription` để mô tả dài tự xuống dòng trong ô.
     - `VehiclesPage.tsx` (giữ đủ **7 cột**): bỏ `.nowrap` trên `VIN` và thông số `Năm • Màu • Nhiên liệu`.
     - `ServicesPage.tsx` (giữ đủ **8 cột**): bỏ `.nowrap` trên `description` và `bayAndWarranty`.
     - `SuppliersPage.tsx` (giữ đủ **6 cột**): bỏ `whiteSpace: 'nowrap'` cứng ở địa chỉ nhà cung cấp để hiển thị gọn trong cột.

---

## 2. Danh sách file mục tiêu

| STT | Đường dẫn file | Vùng dòng cần sửa | Nội dung tác động |
|---|---|---|---|
| 1 | `AutoGarage/frontend/src/features/auth/components/AuthLayout.tsx` | Dòng 1–108 | Thiết kế lại khung `AuthLayout`: Topbar giống `HomeHeader` của Landing Page + Nền sáng `#f5f7fb` + Card trắng trung tâm 2 cột (Cột ảnh `/images/home/garage-lounge.jpg` + Cột form kiểu Agoda có dải `"hoặc"` và ghi chú Điều khoản dịch vụ). |
| 2 | `AutoGarage/frontend/src/features/auth/pages/LoginPage.tsx` | Dòng 86–171 | Tinh chỉnh bố cục form Đăng nhập theo mẫu Agoda (tiêu đề, mô tả, input, nút submit bo tròn nổi bật, vạch phân cách `"hoặc"`, nút chuyển sang Đăng ký), giữ nguyên tuyệt đối các `label` và button name cho `workflows.test.tsx`. |
| 3 | `AutoGarage/frontend/src/features/auth/pages/RegistrationPage.tsx` | Dòng 67–156 | Tinh chỉnh form Đăng ký (lưới 2 cột trên desktop, vạch `"hoặc"`, nút chuyển sang Đăng nhập), giữ nguyên heading `<h1>Đăng ký tài khoản</h1>` và nút `Đăng ký` cho `RegistrationPage.test.tsx`. |
| 4 | `AutoGarage/frontend/src/features/auth/auth.css` | Dòng 1–498 | Viết mới bộ CSS sáng đồng bộ Landing Page (`#f5f7fb`, `#ffffff`, `#101828`, `#245bdb`, `#e3e8f0`) cho Header, Split Card (Ảnh + Form), Divider `"hoặc"`, Pill Button và Legal Footer. |
| 5 | `AutoGarage/frontend/src/styles.css` | Dòng 624–689, 1902–1906 | Cập nhật `.table-scroll`, `.data-table`, `.data-table th`, `.data-table td`, `.account-cell`, `.email-cell`, `.row-actions` để hiển thị trọn vẹn mọi cột không cần cuộn ngang. |
| 6 | `AutoGarage/frontend/src/features/reception/pages/BookingsPage.tsx` | Dòng 135–181 | Giữ nguyên 7 cột; bỏ `.nowrap` trên `vehicleModel` và `requestDescription`. |
| 7 | `AutoGarage/frontend/src/features/vehicle/pages/VehiclesPage.tsx` | Dòng 105–137 | Giữ nguyên 7 cột; bỏ `.nowrap` trên `VIN` và thông số `Năm • Màu • Nhiên liệu`. |
| 8 | `AutoGarage/frontend/src/features/catalog/pages/ServicesPage.tsx` | Dòng 84–176 | Giữ nguyên 8 cột; bỏ `.nowrap` trên `description` và `bayAndWarranty`. |
| 9 | `AutoGarage/frontend/src/features/inventory/pages/SuppliersPage.tsx` | Dòng 200–220 | Giữ nguyên 6 cột; cho phép địa chỉ nhà cung cấp tự xuống dòng gọn trong cột `MST / Địa chỉ`. |

---

## 3. Hợp đồng kỹ thuật (Contract)

### 3.1. Hợp đồng Giao diện Đăng nhập / Đăng ký (`AuthLayout.tsx`, `LoginPage.tsx`, `RegistrationPage.tsx`, `auth.css`)
- **Cấu trúc JSX của `AuthLayout.tsx`:**
  ```tsx
  <div className="auth-page">
    {/* 1. Header giống hệt Landing Page + ảnh mẫu */}
    <header className="auth-header">
      <div className="auth-header-inner">
        <Link to="/" className="auth-brand">...</Link>
        <nav className="auth-nav" aria-label="Điều hướng trang chủ">
          <Link to="/#dich-vu">Dịch vụ</Link>
          <Link to="/#quy-trinh">Quy trình</Link>
          <Link to="/#khoang-xuong">Khoang kỹ thuật</Link>
          <Link to="/#phu-tung">Phụ tùng</Link>
          <Link to="/#dat-lich">Đặt lịch hẹn</Link>
        </nav>
        <div className="auth-header-right">
          <a href="tel:0963943872" className="auth-hotline">0963 943 872</a>
          <Link to="/" className="button">Về trang chủ</Link>
        </div>
      </div>
    </header>

    {/* 2. Vùng nền xám sáng #f5f7fb căn giữa Card */}
    <main className="auth-main" id="main-content" tabIndex={-1}>
      <div className={`auth-card auth-card-${variant}`}>
        {/* Cột ảnh xưởng bên trái (hoặc phải) */}
        <aside className="auth-card-media" aria-label="Hình ảnh xưởng AutoGarage Tây Sơn">
          <img src="/images/home/garage-lounge.jpg" alt="Khu vực tiếp nhận và phòng chờ khách hàng AutoGarage Tây Sơn" />
          <div className="auth-card-media-overlay">...</div>
        </aside>

        {/* Cột Form kiểu Agoda */}
        <section className="auth-card-form">
          <header className="auth-form-header">
            <h1>{title}</h1>
            <p>{description}</p>
          </header>
          {children}
          <p className="auth-legal-text">
            Khi tiếp tục, tôi đồng ý với các <a href="/#dich-vu">Điều khoản sử dụng</a> và{' '}
            <a href="/#lien-he">Chính sách bảo mật</a> của AutoGarage Tây Sơn.
          </p>
        </section>
      </div>
    </main>
  </div>
  ```
- **Ràng buộc kiểm thử bắt buộc giữ nguyên:**
  - Không render bất kỳ button nào có tên chứa `/Keycloak/i` trên `LoginPage`.
  - Giữ nguyên nhãn input `'Tên đăng nhập hoặc email'`, `'Mật khẩu'`, và button submit `'Đăng nhập'` trên `LoginPage`.
  - Giữ nguyên heading `<h1>` có nội dung chính xác `'Đăng ký tài khoản'` và button submit `'Đăng ký'` trên `RegistrationPage`.

### 3.2. Hợp đồng CSS Bảng dữ liệu Không cuộn ngang & Không gộp cột (`styles.css`)
- Giữ nguyên 100% các cột trên mọi màn hình.
- Giảm padding ngang của `.data-table th, .data-table td` về `10px`.
- Cho phép tiêu đề và nội dung dài tự xuống dòng (`white-space: normal; overflow-wrap: break-word`), `.account-cell { min-width: 0 }`, `.row-actions { flex-wrap: wrap; gap: 4px }`.

---

## 4. Checklist thực thi

- [x] **Step 1 (Thiết kế lại giao diện Đăng nhập & Đăng ký theo mẫu tham chiếu + ảnh xưởng bên cạnh):**
  - Cập nhật `AutoGarage/frontend/src/features/auth/components/AuthLayout.tsx`, `LoginPage.tsx`, `RegistrationPage.tsx` và `AutoGarage/frontend/src/features/auth/auth.css`.
- [x] **Step 2 (Cập nhật CSS bảng toàn cục chống tràn ngang mà không cần gộp cột):**
  - Cập nhật `.table-scroll`, `.data-table`, `.data-table th`, `.data-table td`, `.account-cell`, `.email-cell`, `.row-actions` trong `AutoGarage/frontend/src/styles.css`.
- [x] **Step 3 (Gỡ bỏ `.nowrap` gây tràn ngang trên các cột mô tả của từng màn hình, giữ nguyên 100% số cột):**
  - Cập nhật `BookingsPage.tsx` (giữ đủ 7 cột), `VehiclesPage.tsx` (giữ đủ 7 cột), `ServicesPage.tsx` (giữ đủ 8 cột), `SuppliersPage.tsx` (giữ đủ 6 cột).
- [x] **Step 4 (Kiểm thử & Xác nhận thực tế):**
  - Chạy `npm run check` (lint, typecheck, 91 unit tests, production build) và khởi động lại container `frontend` để nghiệm thu trực tiếp trên trình duyệt.

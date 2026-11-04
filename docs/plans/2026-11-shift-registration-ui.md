# Kế Hoạch Thiết Kế Lại Giao Diện Đăng Ký Ca Làm Việc Của Nhân Viên

> **Mã plan**: `shift-registration-ui`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
Qua khảo sát mã nguồn và hình ảnh thực tế của hộp thoại **Đăng ký ca làm việc**, hiện trạng đang gặp 5 vấn đề về bố cục và trải nghiệm người dùng:

1. **Vỡ bố cục khối "Ca của tôi gần đây" nằm dưới hàng nút hành động và dính sát mép trái** (`frontend/src/features/scheduling/components/ShiftRegistrationModal.tsx:148-180`):
   - Thẻ `<section aria-label="Ca đã đăng ký" className="stack">` được đặt **bên ngoài** `<form className="editor-form account-editor-form">`, là con trực tiếp của `.sheet.centered-dialog`.
   - Trong `frontend/src/styles.css:1138-1150`, padding `24px` chỉ được gán cho `.sheet-header` và `.account-editor-form`, không gán cho thẻ `<section>` bên ngoài. Hậu quả là tiêu đề **"Ca của tôi gần đây"** và dòng **"Bạn chưa có ca nào."** bị dính sát mép trái của modal (`padding: 0`) và nằm lơ lửng bên dưới hai nút `Đóng` / `Gửi đăng ký ca`, vi phạm quy chuẩn tại `docs/UI_DESIGN_GUIDELINES.md:97` (*"Hàng nút cuối form chiếm trọn hàng; Hủy ở trước, Tạo/Lưu thay đổi ở sau"*).
2. **Kích thước modal quá rộng và lạm dụng `.full-width-field` gây mất cân đối** (`ShiftRegistrationModal.tsx:78-137`):
   - Modal đang dùng `size="lg"` (`1000px`) cho một biểu mẫu chỉ có 4 trường nhập liệu.
   - Trường `Khoang mong muốn` (`ShiftRegistrationModal.tsx:113-126`) bị đặt trong `.full-width-field` kéo dài suốt chiều ngang 1000px thay vì xếp cân đối trên lưới 2 cột chuẩn `repeat(2, minmax(0, 1fr))` (`docs/UI_DESIGN_GUIDELINES.md:79-81`).
3. **Chọn ngày và khung ca đơn điệu, thiếu tóm tắt xác nhận ca trực** (`ShiftRegistrationModal.tsx:86-112`):
   - Chọn khung ca chỉ là thẻ `<select>` đơn thuần, người dùng phải mở dropdown mới biết có những ca nào và không thấy tổng số giờ công của ca.
   - Chưa có nút chọn nhanh ngày thường dùng (`Ngày mai`, `Ngày kia`, `Thứ Hai tới`).
   - Thiếu dải tóm tắt xác nhận (`info-strip`) và 안내 giới hạn giờ làm (tối đa 12 giờ/ngày, 60 giờ/tuần, nghỉ tối thiểu 8 giờ giữa 2 ca) như đã có ở modal phân ca của quản trị viên (`ShiftEditor.tsx:229-245`).
4. **Danh sách "Ca của tôi gần đây" thiếu thông tin nghiệp vụ** (`ShiftRegistrationModal.tsx:150-179`):
   - Cột `Thời gian` chỉ hiển thị `formatDateTime(shift.startsAt)`, thiếu giờ kết thúc ca (`endsAt`) nên nhân viên không biết ca kéo dài từ mấy giờ đến mấy giờ.
   - Thiếu cột hiển thị **Khoang trực** (mã khoang hoặc `Thợ cơ động`).
   - Trạng thái rỗng chỉ hiển thị thẻ `<p className="description">Bạn chưa có ca nào.</p>` đơn sơ.
5. **Thiếu nút "Đăng ký ca làm việc" trên Header trang Lịch ca làm việc cho Kỹ thuật viên** (`frontend/src/features/scheduling/pages/SchedulingPage.tsx:569-601`):
   - Trong `SchedulingPage.tsx`, biến `primaryAction` chưa phân nhánh theo role `TECHNICIAN`. Khi kỹ thuật viên mở tab `Lịch ca làm việc` (`shifts`), giao diện hiển thị nút `+ Xếp ca làm việc` của Admin (mở `ShiftEditor` gọi API yêu cầu quyền `ADMIN` gây lỗi 403), trong khi nút `Đăng ký ca làm` lại chỉ nằm khuất bên trong tab `Điều phối xưởng` (`LiveDispatchBoard.tsx:155-161`).

### 1.2. Mục tiêu
1. **Chuẩn hóa cấu trúc Dialog & Lưới 2 cột**:
   - Đổi kích thước hộp thoại về chuẩn cân đối, đưa toàn bộ nội dung (bao gồm cả khu vực "Ca của tôi gần đây") nằm gọn trong luồng bố cục có đệm lề chuẩn `24px`, và đặt hàng nút hành động (`Đóng` / `Gửi đăng ký ca`) ở vị trí **cuối cùng** của hộp thoại.
   - Xếp `Ngày làm việc` (cột 1) và `Khoang mong muốn` (cột 2) thẳng hàng trên cùng một dòng lưới 2 cột (`repeat(2, minmax(0, 1fr))`).
2. **Trực quan hóa thao tác chọn Ngày & Khung ca**:
   - Bổ sung nhóm chip chọn nhanh ngày (`Hôm nay`, `Ngày mai`, `Ngày kia`) kết hợp với ô chọn ngày `type="date"`.
   - Thiết kế bộ chọn **Khung ca mong muốn** dạng thẻ chọn trực quan (Shift Option Cards) cho 5 loại ca (`Ca sáng`, `Ca chiều`, `Cả ngày`, `Tăng ca`, `Ca đêm`), hiển thị rõ nhãn ca, khung giờ (`08:00–17:00`) và số giờ công (`9,0 giờ`), đồng thời giữ khả năng điều hướng bàn phím / ngữ nghĩa radio chuẩn accessibility.
3. **Bổ sung Dải tóm tắt ca đăng ký (`info-strip`)**:
   - Hiển thị tóm tắt trực tiếp: **Ngày làm việc** $\rightarrow$ **Khung ca & Giờ (`HH:mm–HH:mm` · số giờ)** $\rightarrow$ **Vị trí (`Mã khoang` hoặc `Thợ cơ động`)**, kèm ghi chú quy tắc giới hạn giờ làm việc của xưởng.
4. **Nâng cấp bảng "Ca của tôi gần đây"**:
   - Hiển thị tối đa 5 ca gần nhất của nhân viên với đầy đủ 4 cột: **Ngày & Khung giờ** (`dd/MM/yyyy · HH:mm–HH:mm`), **Loại ca**, **Vị trí khoang** (`Mã khoang` hoặc `Thợ cơ động`), và **Trạng thái** (`Chờ duyệt`, `Đang hoạt động`, `Bị từ chối` kèm lý do từ chối nổi bật).
   - Thiết kế trạng thái rỗng gọn gàng, đúng lề khi nhân viên chưa có lịch sử đăng ký ca.
5. **Đồng bộ nút "Đăng ký ca làm việc" trên `SchedulingPage`**:
   - Với tài khoản `TECHNICIAN`, hiển thị nút chính **"`+` Đăng ký ca làm việc"** ngay trên `PageHeader` của trang `/scheduling` (mở `ShiftRegistrationModal`), ẩn các nút tạo khoang/xếp ca/thêm nhân viên vốn chỉ dành cho `ADMIN`.

### 1.3. Ngoài phạm vi
- Không thay đổi cơ sở dữ liệu hay các endpoint Backend (`POST /api/v1/shifts/register`, `GET /api/v1/shifts/my`).
- Không thay đổi luồng phê duyệt/từ chối ca của Quản trị viên trong `PendingShiftRegistrations.tsx`.

---

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1 (Bố cục chuẩn & Hàng nút ở đáy modal)**: Khi mở hộp thoại "Đăng ký ca làm việc", trường `Ngày làm việc` và `Khoang mong muốn` nằm trên 2 cột cân đối (desktop), tự thu về 1 cột trên mobile. Khu vực `Ca của tôi gần đây` nằm **phía trên** hàng nút `Đóng` / `Gửi đăng ký ca`, căn lề thẳng hàng `24px` với các trường form, không bị tràn hoặc dính mép trái. _Kiểm chứng_: Unit test RTL & kiểm tra DOM/CSS.
- **AC-2 (Chọn nhanh ngày & Khung ca trực quan)**: Người dùng chọn được khung ca (`Ca sáng`, `Ca chiều`, `Cả ngày`, `Tăng ca`, `Ca đêm`) và thấy rõ khung giờ (`07:30–11:30`, `08:00–17:00`...) cùng số giờ công; có thể bấm nút chọn nhanh ngày (`Ngày mai`, `Ngày kia`). Khi bấm `Gửi đăng ký ca`, payload gửi đúng `startsAt`, `endsAt`, `shiftType`, `serviceBayId`, `notes`. _Kiểm chứng_: Unit test trong `ShiftRegistrationModal.test.tsx`.
- **AC-3 (Dải tóm tắt `info-strip` cập nhật tức thì)**: Khi người dùng đổi ngày, đổi khung ca hoặc chọn khoang sửa chữa, dải `info-strip` hiển thị chính xác khung giờ, thời lượng (ví dụ `9,0 giờ`) và tên khoang (hoặc `Thợ cơ động`). _Kiểm chứng_: Unit test trong `ShiftRegistrationModal.test.tsx`.
- **AC-4 (Bảng "Ca của tôi gần đây" đầy đủ cột & trạng thái)**: Khi có dữ liệu ca từ `useMyShiftsQuery`, bảng hiển thị đầy đủ: Ngày & khung giờ bắt đầu–kết thúc, Loại ca, Khoang trực (mã khoang hoặc `Thợ cơ động`), Badge trạng thái và lý do từ chối (nếu `status === 'REJECTED'`). Khi danh sách rỗng, hiển thị khối trạng thái rỗng rõ ràng. _Kiểm chứng_: Unit test trong `ShiftRegistrationModal.test.tsx`.
- **AC-5 (Phân quyền nút hành động trên `SchedulingPage`)**: Khi đăng nhập với vai trò `TECHNICIAN`, `PageHeader` của `SchedulingPage` hiển thị nút `Đăng ký ca làm việc` để mở `ShiftRegistrationModal`, và không hiển thị các nút quản trị (`Thêm khoang`, `Xếp ca làm việc`, `Thêm kỹ thuật viên`). _Kiểm chứng_: Unit test RTL / kiểm tra code `SchedulingPage.tsx`.
- **AC-6 (Không hồi quy test & build)**: Toàn bộ test suite Vitest và lệnh `npm run build` chạy thành công 100%. _Kiểm chứng_: `npm --prefix AutoGarage/frontend run test` và `npm --prefix AutoGarage/frontend run build`.

---

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `frontend/src/features/scheduling/components/ShiftRegistrationModal.tsx` | 1–183 | Tái cấu trúc bố cục form 2 cột, thêm chọn nhanh ngày, thẻ chọn khung ca kèm giờ & thời lượng, dải `info-strip` tóm tắt, đưa khối "Ca của tôi gần đây" vào trong form trước hàng nút cuối và bổ sung cột Khoang trực + khung giờ đầy đủ. |
| `frontend/src/features/scheduling/pages/SchedulingPage.tsx` | 85–125, 569–605, 700–718, 874–908 | Lấy `user` từ `useAuth()`, kiểm tra role `ADMIN` cho các nút thêm/sửa/xóa quản trị; hiển thị nút `Đăng ký ca làm việc` trên `PageHeader` cho `TECHNICIAN` và gắn `<ShiftRegistrationModal />`. |
| `frontend/src/features/scheduling/components/LiveDispatchBoard.tsx` | 153–164 | Giữ nút mở nhanh hoặc đồng bộ với modal đăng ký ca để tránh lặp nút dư thừa khi đã có trên `PageHeader`. |
| `frontend/src/styles.css` | Cuối file (hoặc vùng scheduling styles ~3600–3750) | Bổ sung class CSS chuẩn design token cho `.shift-type-selector`, `.shift-type-card`, `.shift-quick-dates`, `.shift-recent-panel`, `.shift-recent-empty`. |
| `frontend/src/features/scheduling/components/ShiftRegistrationModal.test.tsx` | File mới | Viết unit test kiểm chứng toàn diện AC-1, AC-2, AC-3, AC-4 (render bố cục, chọn khung ca, chọn khoang, gửi form, hiển thị danh sách ca gần đây và lý do từ chối). |

---

## 4. Hợp Đồng Kỹ Thuật

### 4.1. Bố cục trực quan của `ShiftRegistrationModal`

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ Đăng ký ca làm việc                                                      [×] │
│ Chọn ngày và khung ca mong muốn. Ca chỉ vào lịch chính thức sau khi duyệt.   │
├──────────────────────────────────────────────────────────────────────────────┤
│ Ngày làm việc *                         Khoang mong muốn                     │
│ [ 05/11/2026                       📅 ] [ Không chọn khoang (thợ cơ động) ▾] │
│ Chọn nhanh: [Hôm nay] [Ngày mai] [Ngày kia]  Để trống nếu làm thợ cơ động    │
│                                                                              │
│ Khung ca mong muốn *                                                         │
│ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────┐ │
│ │ Ca sáng      │ │ Ca chiều     │ │ ● Cả ngày    │ │ Tăng ca      │ │Ca đêm│ │
│ │ 07:30–11:30  │ │ 13:00–17:00  │ │ 08:00–17:00  │ │ 17:30–20:30  │ │20–24h│ │
│ │ 4,0 giờ      │ │ 4,0 giờ      │ │ 9,0 giờ      │ │ 3,0 giờ      │ │4,0 h │ │
│ └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘ └──────┘ │
│                                                                              │
│ ℹ️ Tóm tắt đăng ký: Thứ Năm, 05/11/2026 · Cả ngày (08:00–17:00 · 9,0 giờ)    │
│    → Vị trí: Thợ cơ động (hỗ trợ toàn xưởng)                                 │
│                                                                              │
│ Lý do / ghi chú                                                              │
│ [ Nhập ghi chú hoặc đề xuất ca trực (không bắt buộc)...                    ] │
│                                                                              │
│ ┌──────────────────────────────────────────────────────────────────────────┐ │
│ │ 🕒 Ca của tôi gần đây                                       (Tối đa 5 ca)│ │
│ │ ──────────────────────────────────────────────────────────────────────── │ │
│ │ Ngày & Giờ           Loại ca     Khoang trực      Trạng thái             │ │
│ │ 05/11/2026 08:00–17:00  Cả ngày  BAY-01           [Chờ duyệt]            │ │
│ │ 03/11/2026 07:30–11:30  Ca sáng  Thợ cơ động      [Bị từ chối]           │ │
│ │                                                   Lý do: Đủ quân số      │ │
│ └──────────────────────────────────────────────────────────────────────────┘ │
│ ──────────────────────────────────────────────────────────────────────────── │
│                                                    [ Đóng ] [ Gửi đăng ký ca]│
└──────────────────────────────────────────────────────────────────────────────┘
```

### 4.2. Chi tiết kỹ thuật trong `ShiftRegistrationModal.tsx`
- **Hàm tính thời lượng ca (`shiftDurationLabel`)**:
  ```ts
  function shiftDurationLabel(type: ShiftType): string {
    const { start, end } = shiftTypeWindows[type];
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    const hours = Math.round(((eh * 60 + em - (sh * 60 + sm)) / 60) * 10) / 10;
    return `${String(hours).replace('.', ',')} giờ`;
  }
  ```
- **Giữ khả năng tương thích ngược với test & accessibility**:
  - Bên cạnh hoặc tích hợp cùng bộ chọn khung ca, vẫn đảm bảo control có label `"Khung ca mong muốn"` (dùng `<select aria-label="Khung ca mong muốn">` hoặc `Field label="Khung ca mong muốn"` kết hợp các thẻ chọn nhanh) để cả thao tác chuột nhanh trên thẻ lẫn truy vấn `getByLabelText('Khung ca mong muốn')` đều hoạt động mượt mà.
- **Định dạng thời gian ca trong bảng "Ca của tôi gần đây"**:
  - Hiển thị ngày kèm khung giờ bắt đầu – kết thúc: `dd/MM/yyyy · HH:mm–HH:mm`.
  - Ánh xạ `serviceBayId` sang `bayCode` từ danh sách `bays.data?.content` (nếu không có `serviceBayId` thì hiển thị `'Thợ cơ động'`).
  - Giới hạn hiển thị 5 bản ghi gần nhất (`.slice(0, 5)`) để modal luôn gọn trong khung nhìn desktop, không gây thanh cuộn dài.

### 4.3. Phân quyền nút hành động trên `SchedulingPage.tsx`
- Sử dụng `const { user } = useAuth();`
- `const isAdmin = user?.role === 'ADMIN';`
- `const isTechnician = user?.role === 'TECHNICIAN';`
- Trên `PageHeader`:
  - Nếu `isTechnician`: hiển thị `<Button variant="primary" onClick={() => setRegisterModalOpen(true)}><CalendarClock size={16} /> Đăng ký ca làm việc</Button>`.
  - Nếu `isAdmin`: giữ nguyên các nút `Thêm khoang`, `Xếp ca làm việc`, `Thêm kỹ thuật viên` theo từng tab.

---

## 5. Checklist Thực Thi

- [x] **Step 1: Bổ sung CSS cho form đăng ký ca và bảng lịch sử ca gần đây trong `frontend/src/styles.css`** _(AC-1, AC-2, AC-4)_
  - Thêm style cho `.shift-quick-dates`, `.shift-type-grid`, `.shift-type-pill`, `.shift-recent-section`, `.shift-recent-empty` sử dụng đúng biến màu (`var(--primary)`, `var(--border)`, `var(--muted)`, `#f8fafc`) và responsive về 1 cột trên màn hình nhỏ.
- [x] **Step 2: Thiết kế lại `ShiftRegistrationModal.tsx`** _(AC-1, AC-2, AC-3, AC-4)_
  - Chuyển bố cục sang lưới 2 cột cân đối (`Ngày làm việc` cột 1, `Khoang mong muốn` cột 2).
  - Thêm nút chọn nhanh ngày (`Hôm nay`, `Ngày mai`, `Ngày kia`).
  - Thêm thẻ chọn nhanh 5 loại khung ca kèm khung giờ & tổng giờ công, đồng bộ với trường `Khung ca mong muốn`.
  - Thêm dải `info-strip` tóm tắt thông tin ca đăng ký và quy định giới hạn giờ làm.
  - Đưa khối `Ca của tôi gần đây` vào trong bố cục form (phía trên `.form-actions`), bổ sung cột `Khoang trực`, định dạng khung giờ `HH:mm–HH:mm`, và làm đẹp trạng thái rỗng.
- [x] **Step 3: Đồng bộ nút "Đăng ký ca làm việc" trên `SchedulingPage.tsx` và `LiveDispatchBoard.tsx`** _(AC-5)_
  - Cập nhật `SchedulingPage.tsx` để hiển thị nút `Đăng ký ca làm việc` trên `PageHeader` cho `TECHNICIAN` và chỉ hiển thị các nút quản trị (`Thêm khoang`, `Xếp ca làm việc`, `Thêm kỹ thuật viên`) khi người dùng có quyền phù hợp.
- [x] **Step 4: Viết Unit Test `ShiftRegistrationModal.test.tsx` & Xác minh toàn trình** _(AC-1, AC-2, AC-3, AC-4, AC-5, AC-6)_
  - Tạo file test kiểm chứng: hiển thị form, chọn nhanh ngày/khung ca, cập nhật dải tóm tắt, gửi request `POST /api/v1/shifts/register`, hiển thị danh sách ca gần đây (kể cả trạng thái rỗng và trạng thái bị từ chối kèm lý do).
  - Chạy `npm --prefix AutoGarage/frontend run test` và `npm --prefix AutoGarage/frontend run build`.
---

## 6. Kết Quả Hội Tụ (Converge)

- **Ngày**: 2026-11-04
- **Kết luận**: `converged` — mọi tiêu chí trong phạm vi đều `Đạt`.
- **Bằng chứng kiểm chứng**: `npm run test` (12 files / 105 tests pass), `npm run build` (exit 0), `npm run lint` (`eslint --max-warnings 0`, exit 0), `npm run typecheck` (exit 0).

| Mục | Trạng thái | Ghi chú / Bằng chứng |
|---|---|---|
| AC-1 — Bố cục 2 cột, khối ca gần đây nằm trong form trước hàng nút | Đạt | `ShiftRegistrationModal.tsx:139-160` (hai `Field` liền nhau trên lưới `.account-editor-form`), khối `shift-recent-section` đặt trong `<form>` trước `.form-actions` (`:259-311`). Test `keeps the recent shift panel inside the form ahead of the action row` khẳng định `section.closest('form') === form` và section đứng trước nút gửi. Mobile 1 cột nhờ `@media (max-width: 640px) .centered-dialog .account-editor-form { grid-template-columns: 1fr }`. |
| AC-2 — Chọn nhanh ngày + 5 thẻ khung ca kèm giờ & số giờ công, payload đúng | Đạt | `ShiftRegistrationModal.tsx:162-226` (`shift-quick-dates`, `shift-type-grid` với `role="radiogroup"`), `shiftDurationLabel()` (`:27-33`). Test `sends the chosen quick date, shift window, bay and notes` kiểm chứng `startsAt`/`endsAt` = `07:30`–`11:30` giờ địa phương, `shiftType: 'MORNING'`, `serviceBayId: 10`, `notes` đã trim. |
| AC-3 — `info-strip` cập nhật tức thì | Đạt | `ShiftRegistrationModal.tsx:228-238`. Test `updates the summary strip for date, shift type and bay` kiểm chứng `08:00–17:00 · 9,0 giờ`, đổi sang `07:30–11:30 · 4,0 giờ`, vị trí `Thợ cơ động` → `BAY-01`. |
| AC-4 — Bảng "Ca của tôi gần đây" đủ cột, trạng thái, lý do từ chối, trạng thái rỗng | Đạt | `ShiftRegistrationModal.tsx:259-302` (cột `Ngày & giờ`, `Loại ca`, `Khoang trực`, `Trạng thái`; `formatShiftRange()` → `dd/MM/yyyy · HH:mm–HH:mm`; giới hạn `.slice(0, 5)`). Test `lists recent shifts with full range, bay, status and rejection reason` và `shows a tidy empty state when there are no registrations`. |
| AC-5 — Nút `Đăng ký ca làm việc` cho `TECHNICIAN`, ẩn nút quản trị | Đạt | `SchedulingPage.tsx:89-91` (`useAuth`, `isAdmin`, `isTechnician`), `:575-579` (`isTechnician` → mở `ShiftRegistrationModal`, `!isAdmin → null` ẩn `Thêm khoang`/`Xếp ca làm việc`/`Thêm kỹ thuật viên`), `:891-895` gắn modal. Nút trùng trong `LiveDispatchBoard.tsx` đã bỏ để tránh 2 nút cùng chức năng. Kiểm chứng bằng `tsc -b` + đọc code (không có test RTL riêng cho `SchedulingPage`). |
| AC-6 — Không hồi quy test & build | Đạt | `npm run test` → 12 files / 105 tests passed; `npm run build` → `built in 4.01s`; `npm run lint` → exit 0. |
| Mục tiêu 1.2.2 — Chọn ngày & khung ca trực quan | Đạt | Thẻ chọn khung ca kèm giờ + thời lượng, chip ngày nhanh, `aria-pressed`/`role="radio"` cho điều hướng bàn phím. |
| Mục tiêu 1.2.3 — Giới hạn giờ làm | Đạt | Dòng mô tả giới hạn 12h/ngày, 60h/tuần, nghỉ 8h (`ShiftRegistrationModal.tsx:240-245`). |

### Sai lệch nhỏ đã chấp nhận

- Kích thước modal dùng `size="md"` thay vì `lg` để form 4 trường cân đối, đúng UI guideline (không còn 1000px trống).
- Tên class CSS thực tế theo Step 1: `.shift-quick-dates`, `.shift-type-grid`, `.shift-type-pill`, `.shift-recent-section`, `.shift-recent-empty` (bảng file ở mục 3 dùng tên nháp `.shift-type-selector`, `.shift-type-card`, `.shift-recent-panel`).
- Bảng ca gần đây trên mobile dùng `table-layout: fixed` + giảm padding (không tạo thanh cuộn ngang).
- `LiveDispatchBoard.tsx` bỏ hoàn toàn nút + modal đăng ký ca để tránh lặp với `PageHeader`.

### Còn lại ngoài phạm vi (không chặn hội tụ)

- `SchedulingPage` vẫn cho mọi role thấy tab `Kỹ thuật viên` với các nút thao tác `Xếp ca` / `Sửa` / `Xóa` trong bảng; backend trả 403 nếu không đủ quyền. Nếu muốn ẩn theo role thì cần plan riêng cho toàn bộ tab/table action.
- Chưa xác minh UI bằng trình duyệt (không có phiên đăng nhập trong môi trường này); đã kiểm chứng bằng RTL + build.

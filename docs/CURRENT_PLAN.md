# Kế Hoạch Bỏ Sơ Đồ Cây Thanh Điều Hướng Nhân Sự/Khoang/Ca Và Sửa Lỗi 2004/3002 Tại Ca Làm Việc & Chấm Công Vào Ca

> **Mã plan**: `fix-shift-endpoints-and-flat-sidebar-nav`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: _(không)_ — plan trước `fix-invoice-readiness-and-part-issue-flow` đã lưu tại `docs/plans/2026-11-fix-invoice-readiness-and-part-issue-flow.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
Khi thao tác tại các màn **Quản lý nhân viên** (`/staff`) và **Khoang & ca làm việc** (`/scheduling`), người dùng gặp 4 vấn đề:

1. **Thanh điều hướng bên trái hiển thị dạng sơ đồ cây (`children`)**:
   - Trong `frontend/src/app/layouts/navItems.ts` (dòng 146–179) và `frontend/src/app/layouts/AppShell.tsx` (dòng 78–100), các mục **Quản lý nhân viên** (`/staff`), **Khoang & ca làm việc** (`/scheduling`) và **Bảng lương** (`/payroll`) khai báo `children: [...]`, làm thanh điều hướng bên trái bung thêm các nhánh con `.nav-children` / `.nav-subitem` dạng sơ đồ cây, trùng lặp với thanh `.tabs` đã có sẵn bên trong từng trang.
   - Ngoài ra, tại `EmployeeDetailSheet.tsx` (`frontend/src/features/staff/components/EmployeeDetailSheet.tsx:52`), khung xem chi tiết nhân viên đang đặt `placement="side"` (drawer bên phải) thay vì dialog ở giữa màn hình theo `docs/UI_DESIGN_GUIDELINES.md`.
2. **Lỗi `400 / code: 2004` (`"Request fields or JSON are invalid"`) tại `GET /api/v1/employee-shifts`**:
   - Khi mở `/staff`, component `EmployeeDirectory.tsx` (`frontend/src/features/staff/components/EmployeeDirectory.tsx:52`) gọi:
     `useShiftsQuery({ status: 'ACTIVE', from: todayKey(), size: 200 })`
   - Tại backend, `PaginationPolicy` (`src/main/java/com/Group13_65CNTT/AutoGarage/common/util/PaginationPolicy.java:26–33`) giới hạn `size <= properties.getMaxSize()` (`100`). Do `size=200 > 100`, backend ném `BusinessException(ResponseCode.BAD_REQUEST)` -> HTTP 400 (`code: 2004`, `"Request fields or JSON are invalid"`).
3. **Lỗi `403 / code: 3002` (`"You do not have permission for this operation"`) tại `GET /api/v1/shifts/reconciliation`**:
   - Trong `frontend/src/features/scheduling/api/schedulingApi.ts` (dòng 104–105), hàm `getShiftReconciliation` gọi nhầm đường dẫn `/shifts/reconciliation` thay vì `/employee-shifts/reconciliation` (`ApiPaths.SHIFTS_BASE = "/api/v1/employee-shifts"`).
   - Vì `/api/v1/shifts/**` không nằm trong danh sách đường dẫn được cấp quyền ở `SecurityConfig`, Spring Security chặn bằng `.anyRequest().denyAll()` và trả về `403 FORBIDDEN` (`code: 3002`).
4. **Lỗi `403 / code: 3002` (`"You do not have permission for this operation"`) tại `POST /api/v1/shifts/15/check-in` — Admin không chấm công (check-in / check-out) vào ca được**:
   - Trong `frontend/src/features/scheduling/api/schedulingApi.ts` (dòng 98–102), `checkInShift` và `checkOutShift` gọi nhầm `/shifts/${id}/check-in` và `/shifts/${id}/check-out` thay vì `/employee-shifts/${id}/check-in` và `/employee-shifts/${id}/check-out`.
   - Trong `frontend/src/features/scheduling/hooks/useScheduling.ts` (dòng 88), `useShiftAttendanceMutation` invalidate nhầm queryKey `['shifts']` thay vì `['employee-shifts']`.
   - Đồng thời, trên giao diện **Lịch ca làm việc** (`ShiftTimelineBoard.tsx` và bảng danh sách ca trong `SchedulingPage.tsx`), Admin/Lễ tân mới chỉ có nút **Hủy ca** mà chưa có nút **Vào ca** / **Ra ca** trực tiếp trên từng ca trực (hiện mới chỉ có ở tab **Điều phối xưởng** khi ca đang diễn ra).

### 1.2. Mục tiêu
- **Bỏ sơ đồ cây ở thanh điều hướng bên trái**:
  - Xóa thuộc tính `children` khỏi các mục trong `frontend/src/app/layouts/navItems.ts` và bỏ khối render `.nav-children` trong `frontend/src/app/layouts/AppShell.tsx` để thanh điều hướng phẳng, đồng nhất trên toàn hệ thống.
  - Chuyển `EmployeeDetailSheet.tsx` từ `placement="side"` sang `placement="center"` đúng chuẩn dialog giữa màn hình.
- **Sửa dứt điểm lỗi `2004` tại `/api/v1/employee-shifts`**:
  - Trong `EmployeeDirectory.tsx`, đổi `size: 200` thành `size: 100` (đúng `paginationMaxSize`).
- **Sửa dứt điểm lỗi `3002` tại `/reconciliation`, `/check-in`, `/check-out` và hỗ trợ Admin chấm công vào/ra ca**:
  - Trong `schedulingApi.ts`, đổi cả 3 đường dẫn sang `/employee-shifts/${id}/check-in`, `/employee-shifts/${id}/check-out` và `/employee-shifts/reconciliation`.
  - Trong `useScheduling.ts`, invalidate đúng `['employee-shifts']`, `['on-shift-technicians']` và `['shift-reconciliation']` sau khi check-in/check-out.
  - Trong `ShiftTimelineBoard.tsx` (khung chi tiết ca) và `SchedulingPage.tsx` (cột thao tác bảng danh sách ca), bổ sung nút **Vào ca** (`check-in`) và **Ra ca** (`check-out`) cho Admin / Lễ tân (và chính kỹ thuật viên của ca đó) khi ca đang hoạt động (`status === 'ACTIVE'`).
  - Tại backend `ShiftAttendanceServiceImpl.java`, cho phép `ADMIN` / `RECEPTIONIST` xác nhận chấm công vào ca (`checkIn`) cho ca trực `ACTIVE` trong ngày của ca đó mà không bị chặn cứng bởi cửa sổ 30 phút của kỹ thuật viên tự bấm (hoặc khi `attendanceStatus` là `null` / `SCHEDULED` / `ABSENT`), đảm bảo Admin luôn chấp nhận/ghi nhận check-in vào ca cho nhân viên được thuận tiện.

### 1.3. Ngoài phạm vi
- Không thay đổi cấu trúc bảng cơ sở dữ liệu hay tạo migration Flyway mới.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Thanh điều hướng bên trái (`AppShell.tsx`, `navItems.ts`) hiển thị phẳng cho **Quản lý nhân viên**, **Khoang & ca làm việc** và **Bảng lương**, không còn nhánh con `.nav-children` / `.nav-subitem` dạng sơ đồ cây. _Kiểm chứng_: unit test `navItems.test.ts` và kiểm tra DOM.
- **AC-2**: Mở trang **Quản lý nhân viên** (`/staff`) gọi `GET /api/v1/employee-shifts?status=ACTIVE&from=...&page=0&size=100...` trả về HTTP 200, không còn lỗi `code: 2004`. _Kiểm chứng_: test RTL `StaffPage.test.tsx`.
- **AC-3**: Mở trang **Khoang & ca làm việc** (`/scheduling`) gọi `GET /api/v1/employee-shifts/reconciliation?date=...` trả về HTTP 200, không còn lỗi `code: 3002` tại `/api/v1/shifts/reconciliation`. _Kiểm chứng_: test RTL `SchedulingPage.test.tsx`.
- **AC-4**: Admin bấm **Vào ca** (`check-in`) hoặc **Ra ca** (`check-out`) trên cả tab **Điều phối xưởng** (`LiveDispatchBoard`) lẫn tab **Lịch ca làm việc** (`ShiftTimelineBoard` / bảng danh sách ca) gọi `POST /api/v1/employee-shifts/{id}/check-in` (hoặc `/check-out`) thành công HTTP 200 (`code: 1002`), không còn lỗi `code: 3002`. _Kiểm chứng_: test trong `SchedulingPage.test.tsx` và kiểm thử backend.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `frontend/src/app/layouts/navItems.ts` | 50–63, 146–180 | Bỏ khai báo `children` dạng cây khỏi `/staff`, `/scheduling`, `/payroll` |
| `frontend/src/app/layouts/AppShell.tsx` | 1–13, 60–103 | Bỏ khối render `.nav-children` / `.nav-subitem` dưới mục đang chọn |
| `frontend/src/app/layouts/navItems.test.ts` | 29–48 | Cập nhật test xác minh các mục điều hướng phẳng, không có `children` |
| `frontend/src/features/staff/components/EmployeeDirectory.tsx` | 50–55 | Đổi `size: 200` thành `size: 100` trong `useShiftsQuery` |
| `frontend/src/features/staff/components/EmployeeDetailSheet.tsx` | 50–58 | Đổi `placement="side"` thành `placement="center"` |
| `frontend/src/features/scheduling/api/schedulingApi.ts` | 98–106 | Đổi `/shifts/${id}/check-in`, `/shifts/${id}/check-out`, `/shifts/reconciliation` sang `/employee-shifts/...` |
| `frontend/src/features/scheduling/hooks/useScheduling.ts` | 81–92 | Trong `useShiftAttendanceMutation`, invalidate `['employee-shifts']` cùng `['on-shift-technicians']` và `['shift-reconciliation']` |
| `frontend/src/features/scheduling/components/ShiftTimelineBoard.tsx` | 23–36, 96–136 | Thêm callback `onAttendance?: (shift: Shift, action: 'check-in' \| 'check-out') => void`, `canPunch?: (shift: Shift) => boolean`, `attendancePending?: boolean` và hiển thị nút **Vào ca** / **Ra ca** trong khung chi tiết ca |
| `frontend/src/features/scheduling/pages/SchedulingPage.tsx` | 40–55, 130–145, 412–445, 780–805 | Kết nối `useShiftAttendanceMutation`, hiển thị trạng thái chấm công và nút **Vào ca** / **Ra ca** trong bảng danh sách ca và sơ đồ tuần |
| `src/main/java/com/Group13_65CNTT/AutoGarage/scheduling/service/impl/ShiftAttendanceServiceImpl.java` | 56–79 | Xử lý an toàn `attendanceStatus == null` và cho phép `ADMIN` / `RECEPTIONIST` check-in vào ca trực `ACTIVE` trong ngày của ca trực (kể cả trước cửa sổ 30 phút của KTV tự chấm công) |
| `frontend/src/features/scheduling/pages/SchedulingPage.test.tsx` | 60–104 | Thêm handler `/api/v1/employee-shifts/reconciliation` và test kiểm chứng Admin bấm **Vào ca** gọi đúng `POST /api/v1/employee-shifts/{id}/check-in` |

## 4. Hợp Đồng Kỹ Thuật

- **Endpoints chấm công & đối soát ca (`EmployeeShiftController` — `ApiPaths.SHIFTS_BASE = "/api/v1/employee-shifts"`)**:
  - `POST /api/v1/employee-shifts/{id}/check-in` (`@PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)` = `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`).
  - `POST /api/v1/employee-shifts/{id}/check-out` (`@PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)` = `ADMIN`, `RECEPTIONIST`, `TECHNICIAN`).
  - `GET /api/v1/employee-shifts/reconciliation?date=YYYY-MM-DD` (`@PreAuthorize(SecurityConstants.HAS_FRONT_DESK)` = `ADMIN`, `RECEPTIONIST`).
- **Quy tắc `ShiftAttendanceServiceImpl.checkIn`**:
  - Cho phép chấm công khi `shift.getAttendanceStatus() == null || shift.getAttendanceStatus() == AttendanceStatus.SCHEDULED || shift.getAttendanceStatus() == AttendanceStatus.ABSENT`.
  - Với `TECHNICIAN` tự chấm công: giữ nguyên giới hạn không sớm hơn `startsAt - 30 phút` và trước `endsAt`.
  - Với `ADMIN` / `RECEPTIONIST` chấm công / chấp nhận vào ca: cho phép xác nhận vào ca đối với mọi ca `ACTIVE` chưa kết thúc (`now.isBefore(shift.getEndsAt())`) trong cùng ngày làm việc hoặc trong khung chuẩn bị ca.

## 5. Checklist Thực Thi

- [x] **Step 1: Bỏ sơ đồ cây trên thanh điều hướng và chuẩn hóa dialog nhân viên** — Cập nhật `navItems.ts`, `AppShell.tsx`, `navItems.test.ts` và `EmployeeDetailSheet.tsx`. _(AC-1)_
- [x] **Step 2: Sửa lỗi `2004` tại `/api/v1/employee-shifts` và lỗi `3002` tại `/reconciliation`, `/check-in`, `/check-out` kèm nút chấm công cho Admin** — Cập nhật `EmployeeDirectory.tsx`, `schedulingApi.ts`, `useScheduling.ts`, `ShiftTimelineBoard.tsx`, `SchedulingPage.tsx` và `ShiftAttendanceServiceImpl.java`. _(AC-2, AC-3, AC-4)_
- [x] **Step 3: Cập nhật test, kiểm thử và build lại Docker backend + frontend** — Cập nhật `SchedulingPage.test.tsx`, chạy `npm --prefix frontend run check`, `./mvnw package -DskipTests` và `docker compose up -d --build backend frontend`. _(AC-1, AC-2, AC-3, AC-4)_

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

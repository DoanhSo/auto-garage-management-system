# Kế Hoạch Thiết Kế Lại Điều Hướng Nhân Sự: Quản Lý Nhân Viên Và Quản Lý Khoang & Ca Làm Việc

> **Mã plan**: `hr-navigation-hubs`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-workflow-gates-warranty-hr-nav.md` (Step 8: sidebar phẳng 7 mục, được thay bằng thiết kế này). Step 11–12 của plan đó vẫn còn mở, không thuộc plan này.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng (kết quả rà soát)

- **Hai danh sách nhân viên trùng nhau**: tab "Quản lý nhân viên" của Lương (`features/payroll/components/EmployeePayrollDirectory.tsx`) và tab "Đội ngũ kỹ thuật viên" của Lịch ca (`SchedulingPage.tsx:445-530`, `employeeColumns`). Cả hai dùng chung `useSchedulingEmployeesQuery` và `EmployeeEditor`, chỉ khác cột hiển thị. Người dùng không biết nên sửa hồ sơ ở đâu.
- **Các màn phụ thuộc nhau nhưng không dẫn sang nhau**: ca làm việc thuộc về nhân viên và khoang, lương tính từ giờ công và ca, hệ số lương nằm trên hồ sơ nhân viên. Tuy vậy:
  - dòng nhân viên không có lối sang "lịch ca của người này" hay "công và lương tháng" (`EmployeeWorkSummaryModal` chỉ mở từ bảng lương);
  - dòng ca và ô lịch tuần không có lối sang hồ sơ nhân viên;
  - dòng khoang không cho xem ca đang gán khoang đó;
  - hàng trong bảng lương không mở được hồ sơ nhân viên;
  - duy nhất `notificationParser.ts:53-54` có đường dẫn `/scheduling?tab=shifts` và `/payroll`.
- **Sidebar sau Step 8 trước** (`AppShell.tsx`) là 7 mục phẳng, mỗi mục là một tab của hai trang. Mục "Quản lý nhân viên" nằm ở trang Lương, "Đội kỹ thuật viên" nằm ở trang Lịch ca, nên người dùng vẫn phải đoán tab nằm ở trang nào. Hệ số & đơn giá lương và thưởng ca đặt cạnh mục nhân sự dù là cấu hình lương.
- Quyền hiện có: danh sách nhân viên đọc được bởi mọi nhân viên (`EmployeeController`, `HAS_ROLE_STAFF`), tạo/sửa chỉ ADMIN; trang Lương dành cho ADMIN/lễ tân, kỹ thuật viên chỉ thấy phiếu lương của mình.

### 1.2. Mục tiêu (thiết kế điều hướng)

Hai điểm đến chính cho công việc quản lý, cộng bảng lương, mỗi điểm đến là một "trung tâm" có thanh tab riêng và liên kết chéo:

```
NHÂN SỰ & LƯƠNG                    (sidebar, mở rộng theo trang đang xem)
├─ Quản lý nhân viên   /staff
│    ├─ Hồ sơ nhân viên          (gộp 2 danh sách trùng; chi tiết nhân viên mở trong khung bên)
│    └─ Hệ số & đơn giá lương    (chuyển từ trang Lương)
├─ Khoang & ca làm việc /scheduling
│    ├─ Điều phối xưởng
│    ├─ Khoang sửa chữa
│    └─ Lịch ca làm việc         (sơ đồ tuần / danh sách, ca chờ duyệt)
└─ Bảng lương           /payroll
     ├─ Bảng lương tháng
     └─ Chính sách thưởng ca
     (kỹ thuật viên: "Phiếu lương của tôi")
```

- Sidebar chỉ có **3 mục chính**; mục đang mở bung ra các tab con (dạng cây), bấm tab con đi thẳng tới tab đó.
- **Khung chi tiết nhân viên** (một Sheet) là điểm nối: hồ sơ, đơn giá và hệ số, 5 ca sắp tới, công và lương tháng này, với các lối đi: "Xem lịch ca" → `/scheduling?tab=shifts&employeeId=…`, "Xếp ca" mở `ShiftEditor` đã chọn sẵn nhân viên, "Công & lương tháng" → `/payroll?tab=monthly&employeeId=…` (mở `EmployeeWorkSummaryModal`).
- **Liên kết chéo ngược**: tên nhân viên trong dòng ca, ô lịch tuần, bảng lương, `LiveDispatchBoard` bấm được → `/staff?employeeId=…` mở khung chi tiết; dòng khoang có "Ca gán khoang này" → `/scheduling?tab=shifts&q=<mã khoang>`.
- Thanh "Liên quan" nhỏ dưới tiêu đề mỗi trang trung tâm nêu hai trang còn lại (ví dụ ở Quản lý nhân viên: "Lịch ca & khoang", "Bảng lương").

### 1.3. Ngoài phạm vi

- Không đổi API backend, không thêm bảng hay quyền mới (bộ lọc `employeeId` của API ca và dữ liệu nhân viên đã có).
- Không đổi nghiệp vụ xếp ca, duyệt đăng ký ca, tính lương, hoa hồng.
- Không đổi đường dẫn `/scheduling` và `/payroll` (giữ để thông báo và đường dẫn cũ còn chạy); chỉ thêm `/staff`.
- Không làm màn "Tài khoản" (`/admin/users`) vì thuộc quản trị hệ thống.

### 1.4. Quyết định thiết kế đã chọn (nêu để người dùng phản hồi)

1. Hồ sơ nhân viên gộp về **một** danh sách ở `/staff`. Tab "Đội ngũ kỹ thuật viên" của Lịch ca bị bỏ; đường dẫn cũ `/scheduling?tab=employees` và `/payroll?tab=employees|coefficients` tự chuyển hướng sang `/staff`.
2. Kỹ thuật viên **không** thấy mục "Quản lý nhân viên" (hồ sơ có số tài khoản và lương). Họ vẫn thấy "Khoang & ca làm việc" (đăng ký ca) và "Phiếu lương của tôi". Lễ tân thấy cả ba, chỉ ADMIN được tạo/sửa (nút bị ẩn với lễ tân, như hiện tại).
3. Cột chuyên môn/QC, số ca hiệu lực (từ danh sách của Lịch ca) và tài khoản nhận lương, đơn giá, hệ số (từ danh sách của Lương) đều có trong danh sách gộp; hai cột cuối ẩn với vai trò không phải quản lý.

---

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Sidebar nhóm "NHÂN SỰ & LƯƠNG" có đúng 3 mục chính cho quản lý (Quản lý nhân viên, Khoang & ca làm việc, Bảng lương) và 2 mục cho kỹ thuật viên (Khoang & ca làm việc, Phiếu lương của tôi). Mục đang mở bung các tab con; chỉ một mục chính và một tab con sáng. _Kiểm chứng_: test `navMatch`/`AppShell` theo vai trò, trình duyệt.
- **AC-2**: `/staff` có hai tab "Hồ sơ nhân viên" và "Hệ số & đơn giá lương", tab giữ trong URL (`?tab=`); danh sách hồ sơ gồm đủ cột chuyên môn & QC, ngày vào làm, số ca hiệu lực, STK, đơn giá & hệ số, trạng thái, bộ lọc tìm kiếm/chuyên môn/trạng thái, thêm/sửa/tạm ngưng cho ADMIN. _Kiểm chứng_: test RTL `StaffPage`.
- **AC-3**: Bấm một nhân viên (hoặc mở `/staff?employeeId=5`) mở khung chi tiết với hồ sơ, đơn giá & hệ số, tối đa 5 ca sắp tới và 3 nút: "Xem lịch ca", "Xếp ca", "Công & lương tháng". _Kiểm chứng_: test RTL khung chi tiết với dữ liệu giả (msw).
- **AC-4**: "Xem lịch ca" mở `/scheduling?tab=shifts&employeeId=5` với bộ lọc kỹ thuật viên đã chọn sẵn; dòng khoang có "Ca gán khoang này" mở tab ca với từ khóa là mã khoang. _Kiểm chứng_: test RTL `SchedulingPage` đọc tham số URL.
- **AC-5**: Tên nhân viên trong dòng ca, ô lịch tuần, bảng lương tháng và bảng điều phối là liên kết tới `/staff?employeeId=…` (không hiện với vai trò không có quyền vào `/staff`). _Kiểm chứng_: test RTL.
- **AC-6**: "Công & lương tháng" mở `/payroll?tab=monthly&employeeId=5` và tự mở `EmployeeWorkSummaryModal` của người đó. _Kiểm chứng_: test RTL `PayrollManagerView`.
- **AC-7**: Đường dẫn cũ vẫn dùng được: `/scheduling?tab=employees`, `/payroll?tab=employees`, `/payroll?tab=coefficients` chuyển hướng sang `/staff` đúng tab; thông báo cũ trỏ `/payroll` vẫn mở bảng lương tháng. _Kiểm chứng_: test router.
- **AC-8**: Mỗi trang trung tâm có thanh "Liên quan" nêu hai trang còn lại và chỉ hiện trang người dùng có quyền vào. _Kiểm chứng_: test RTL theo vai trò.
- **AC-9**: Tab "Đội ngũ kỹ thuật viên" không còn trong `SchedulingPage`, tab "Quản lý nhân viên" và "Hệ số & đơn giá lương" không còn trong `PayrollManagerView`; danh sách trùng `employeeColumns` bị xóa. _Kiểm chứng_: `tsc`, `eslint` (không còn import thừa), test.
- **AC-10**: `lint`, `tsc`, toàn bộ test frontend và build đạt; trình duyệt: đi qua cả ba mục và các liên kết chéo bằng vai trò ADMIN, lễ tân, kỹ thuật viên. _Kiểm chứng_: lệnh trong Step cuối, thao tác tay.

---

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `app/layouts/navMatch.ts` (+ test) | thêm mô hình cây: mục chính có `children` (tab con); hàm xác định mục chính và tab con đang sáng |
| `app/layouts/AppShell.tsx` | thay 7 mục phẳng bằng 3 mục chính có tab con, bung theo trang đang mở; ẩn theo vai trò |
| `app/access.ts` | thêm `STAFF_MANAGE_ROLES` (ADMIN, lễ tân) |
| `app/App.tsx` | thêm route `/staff` (bảo vệ theo vai trò như `/payroll`), chuyển hướng đường dẫn cũ |
| `features/staff/pages/StaffPage.tsx` (mới) | trang trung tâm: tiêu đề, thanh "Liên quan", tab Hồ sơ / Hệ số |
| `features/staff/components/EmployeeDirectory.tsx` (mới, thay `EmployeePayrollDirectory.tsx`) | danh sách gộp, mở khung chi tiết |
| `features/staff/components/EmployeeDetailSheet.tsx` (mới) | khung chi tiết nhân viên với liên kết chéo |
| `features/staff/hooks/useStaffEmployee.ts` (mới) | đọc `employeeId` từ URL, mở/đóng khung chi tiết |
| `features/payroll/components/SalaryCoefficientManager.tsx` | chuyển vào tab của `/staff` (giữ nguyên nội dung) |
| `features/payroll/components/PayrollManagerView.tsx` | bỏ tab nhân viên và hệ số, tab còn `monthly`, `bonus`; đọc `employeeId` để mở `EmployeeWorkSummaryModal`; tên nhân viên thành liên kết |
| `features/payroll/types/payroll.ts` | `PAYROLL_TABS` còn `monthly`, `bonus` |
| `features/scheduling/pages/SchedulingPage.tsx` | bỏ tab `employees` và `employeeColumns`; đọc `employeeId`, `q` từ URL làm bộ lọc ca; nút "Ca gán khoang này"; tên nhân viên thành liên kết; thanh "Liên quan" |
| `features/scheduling/components/LiveDispatchBoard.tsx`, `ShiftTimelineBoard.tsx` | tên nhân viên thành liên kết |
| `shared/components/RelatedLinks.tsx` (mới) | thanh liên kết "Liên quan" dùng chung |
| `features/notification/utils/notificationParser.ts` | cập nhật nhãn và đường dẫn đích (`/scheduling?tab=shifts`, `/payroll?tab=monthly`) |
| `docs/` (UI) | một dòng mô tả sơ đồ điều hướng nhân sự trong tài liệu giao diện nếu có |

Test mới: `app/layouts/navMatch.test.ts` (cập nhật), `features/staff/pages/StaffPage.test.tsx`, `features/staff/components/EmployeeDetailSheet.test.tsx`, `app/routes.redirects.test.tsx`.

---

## 4. Hợp Đồng Kỹ Thuật

**Cây điều hướng** (`navMatch.ts`):
```ts
interface NavNode extends NavTarget {
  label: string; icon; roles: readonly Role[];
  children?: ReadonlyArray<NavTarget & { label: string; roles?: readonly Role[] }>;
}
```
`activeNode(nodes, pathname, search)` trả mục chính (khớp theo `to`) và tab con (khớp `tab`, `alsoTabs`, mặc định khi thiếu tab). Sidebar chỉ vẽ `children` của mục chính đang mở (hoặc khi đang rê/focus), có `aria-expanded`, tab con là `NavLink` thụt vào.

**Route**: `/staff` (`ProtectedRoute` cho `STAFF_MANAGE_ROLES`), tab `profiles | salary`. Chuyển hướng: `/scheduling?tab=employees` → `/staff?tab=profiles`; `/payroll?tab=employees` → `/staff?tab=profiles`; `/payroll?tab=coefficients` → `/staff?tab=salary`. Thực hiện bằng một component `LegacyTabRedirect` đọc `?tab=` của trang cũ.

**Tham số URL**: `employeeId` (id nhân viên) dùng ở `/staff` (mở khung chi tiết), `/scheduling?tab=shifts` (chọn sẵn bộ lọc `shiftEmployee`), `/payroll?tab=monthly` (mở bảng công). `q` ở `/scheduling?tab=shifts` đặt sẵn `shiftKeyword`. Khung chi tiết đóng thì xóa `employeeId` khỏi URL, giữ các tham số khác.

**Khung chi tiết** (`EmployeeDetailSheet`): dùng `Sheet` phải, dữ liệu từ `useSchedulingEmployeesQuery` (đã có), 5 ca sắp tới từ API ca với `employeeId`, `from = hôm nay` (tham số có sẵn ở `EmployeeShiftController`), công tháng từ `payrollApi.getWorkSummary` chỉ khi người xem là quản lý. Nút "Xếp ca" chỉ với ADMIN, mở `ShiftEditor` với `initialEmployeeId`.

**Liên kết chéo**: dùng `Link` của `react-router-dom`; liên kết sang `/staff` chỉ hiện khi `STAFF_MANAGE_ROLES` gồm vai trò hiện tại, ngược lại hiện chữ thường. Một hàm `employeeHref(id)` ở `features/staff/utils/links.ts` để mọi nơi dùng chung.

**`RelatedLinks`**: nhận danh sách `{to, label, roles}`, lọc theo vai trò của `useAuth`, hiển thị dạng dải liên kết dưới `PageHeader`.

**Không đổi** hợp đồng API, `ApiPaths`, `ResponseCode`.

---

## 5. Checklist Thực Thi

- [x] **Step 1: Cây điều hướng và sidebar 3 mục** _(AC-1)_
  - `navMatch.ts` thêm mô hình cây và `activeNode`; `AppShell.tsx` vẽ 3 mục chính với tab con bung theo trang đang mở; `access.ts` thêm `STAFF_MANAGE_ROLES`; cập nhật `navMatch.test.ts` và thêm test hiển thị theo vai trò.

- [x] **Step 2: Trung tâm Quản lý nhân viên** _(AC-2, AC-9)_
  - Tạo `features/staff/` (`StaffPage`, `EmployeeDirectory` gộp cột của hai danh sách, tab Hệ số chứa `SalaryCoefficientManager`); thêm route `/staff` trong `App.tsx`; bỏ `EmployeePayrollDirectory.tsx`; test `StaffPage`.

- [x] **Step 3: Khung chi tiết nhân viên và liên kết** _(AC-3, AC-5, AC-6)_
  - `EmployeeDetailSheet`, `useStaffEmployee`, `links.ts`; liên kết "Xem lịch ca", "Xếp ca", "Công & lương tháng"; `PayrollManagerView` đọc `employeeId` mở bảng công; test khung chi tiết.

- [x] **Step 4: Trung tâm Khoang & ca làm việc** _(AC-4, AC-5, AC-8, AC-9)_
  - `SchedulingPage` bỏ tab nhân viên và `employeeColumns`, đọc `employeeId` và `q` từ URL, thêm nút "Ca gán khoang này", tên nhân viên thành liên kết ở dòng ca, `ShiftTimelineBoard`, `LiveDispatchBoard`; thêm `RelatedLinks` ở hai trang; test đọc tham số URL.

- [x] **Step 5: Bảng lương gọn lại và đường dẫn cũ** _(AC-7, AC-8, AC-9)_
  - `PAYROLL_TABS` còn `monthly`, `bonus`; `LegacyTabRedirect` cho ba đường dẫn cũ; cập nhật `notificationParser.ts`; test router chuyển hướng.

- [x] **Step 6: Kiểm thử & xác minh** _(tất cả AC)_
  - `npm --prefix frontend run lint`, `tsc`, `npm --prefix frontend test`, `npm --prefix frontend run build`; build lại container frontend; thử trên trình duyệt bằng ADMIN, lễ tân và kỹ thuật viên: sidebar bung tab, hồ sơ → khung chi tiết → lịch ca → quay lại hồ sơ, dòng khoang → ca gán khoang, bảng lương → hồ sơ, ba đường dẫn cũ.

- [x] **Step 7 (converge): Test liên kết tên nhân viên và thanh "Liên quan" theo vai trò** _(AC-5, AC-8)_
  - Thêm `features/staff/components/EmployeeLink.test.tsx`: ADMIN và lễ tân thấy liên kết tới `/staff?tab=profiles&employeeId=…`; kỹ thuật viên chỉ thấy chữ đậm không có liên kết (`EmployeeLink.tsx`).
  - Thêm `shared/components/RelatedLinks.test.tsx`: lọc liên kết theo vai trò, trả `null` khi không còn liên kết nào (`RelatedLinks.tsx`).

- [x] **Step 8 (converge): Test đọc tham số URL ở Lịch ca và Bảng lương** _(AC-4, AC-6)_
  - Thêm test cho `SchedulingPage` (msw): mở `/scheduling?tab=shifts&employeeId=5&q=BAY-01` thì bộ lọc kỹ thuật viên và ô tìm kiếm có sẵn giá trị; bấm "Ca gán khoang" ở tab Khoang chuyển sang tab ca với từ khóa là mã khoang (`SchedulingPage.tsx`, `shiftEmployee`, `shiftKeyword`, `bayColumns`).
  - Thêm test cho `PayrollManagerView`: `/payroll?tab=monthly&employeeId=5` mở `EmployeeWorkSummaryModal` của nhân viên đó và đóng thì xóa `employeeId` khỏi địa chỉ (`PayrollManagerView.tsx`, `linkedEmployeeId`).

## 6. Kết Quả Hội Tụ (Converge)

Ngày 2026-11-06. **Kết luận: partial** (mọi tiêu chí có code và test; còn mục chỉ xác minh được trên trình duyệt).

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 sidebar 3 mục, tab con bung theo trang | Đạt (test) | `navItems.test.ts`, `navMatch.test.ts`; chưa xem trên trình duyệt |
| AC-2 `/staff` hai tab, danh sách hồ sơ gộp | Đạt | `StaffPage.test.tsx` |
| AC-3 khung chi tiết nhân viên, 3 lối đi | Đạt | `StaffPage.test.tsx` |
| AC-4 lịch ca đọc `employeeId`, `q`; nút "Ca gán khoang" | Đạt | `SchedulingPage.test.tsx` |
| AC-5 tên nhân viên là liên kết theo quyền | Đạt | `EmployeeLink.test.tsx` |
| AC-6 bảng công mở theo `employeeId` | Đạt | `PayrollManagerView.test.tsx` |
| AC-7 địa chỉ cũ chuyển hướng | Đạt | `LegacyTabRedirect.test.tsx` |
| AC-8 dải "Liên quan" theo vai trò | Đạt | `RelatedLinks.test.tsx` |
| AC-9 bỏ tab và danh sách trùng | Đạt | `tsc -b`, `eslint` sạch, không còn `employeeColumns` |
| AC-10 lint, tsc, test, build; thử trình duyệt | Đạt (lệnh), chưa xác minh trình duyệt | 190/190 test; container frontend chưa build lại sau thay đổi cuối vì Docker Desktop đang tắt |

**Lỗi phát hiện và đã sửa**: hộp thoại bảng công mở từ link `?employeeId=` hiện tiêu đề thiếu tên khi kỳ lương chưa tải; nay chỉ mở sau khi tải xong (`PayrollManagerView.tsx`).

**Còn lại (ngoài phạm vi code)**: thử tay bằng ADMIN, lễ tân, kỹ thuật viên; build lại container frontend khi Docker chạy.

# Kế Hoạch Hiển Thị Trạng Thái Tài Khoản Đăng Nhập Của Nhân Viên Và Khách Hàng

> **Mã plan**: `account-status-visibility`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: _(không)_ — plan trước `review-7fd0865-fixes` đã hội tụ và lưu tại `docs/plans/2026-11-review-7fd0865-fixes.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
Lễ tân và quản trị viên hiện không biết ai đã có tài khoản đăng nhập.

- **Khách hàng:** `customers.user_id` cho phép trống (`V1__create_identity_and_catalogs.sql:22`). Khách lễ tân tạo tại quầy không có tài khoản; khách tự đăng ký, hoặc đăng ký bằng email đã xác thực trùng với hồ sơ có sẵn, được nối `user_id` (`AuthServiceImpl.linkExistingCustomerByVerifiedEmail`, `ensureCustomerProfileLinked`). `CustomerResponse` có `userId` nhưng màn **Khách hàng** (`CustomersPage.tsx`) không hiển thị.
- **Nhân viên:** `employees.user_id` bắt buộc (`V1…sql:96`), nên nhân viên nào cũng có dòng `users`. Nhưng có dòng `users` chưa có nghĩa là đăng nhập được: tài khoản có thể chưa nối với Keycloak (`users.identity_id` trống, chưa đăng nhập lần nào), bị khóa (`is_active = false`) hoặc đã xóa mềm (`deleted_at`). `EmployeeResponse` chỉ có `username`, `email`; màn **Quản lý nhân viên** không cho biết trạng thái này.
- **Người dùng (admin):** `UserResponse` có `identityId`, `active` nhưng không có `lastLoginAt`; `UsersPage.tsx` không cho biết tài khoản đã từng đăng nhập chưa.
- **`users.last_login_at` chỉ được ghi lần đầu** (khi gán `identity_id` hoặc tạo khách từ JWT, `AuthServiceImpl.me()`), nên không dùng được làm "đăng nhập gần nhất".

### 1.2. Mục tiêu
- Mỗi khách hàng, nhân viên và người dùng có một **trạng thái tài khoản** do máy chủ tính, cùng một cách hiểu ở mọi màn:

  | Trạng thái | Điều kiện | Nhãn hiển thị |
  |---|---|---|
  | `NONE` | Khách hàng chưa có `user_id` | Chưa có tài khoản (xám) |
  | `PENDING` | Có `users`, còn hoạt động, chưa từng đăng nhập (`identity_id` và `last_login_at` đều trống) | Chưa đăng nhập lần nào (vàng) |
  | `ACTIVE` | Có `users`, còn hoạt động, đã đăng nhập | Đang hoạt động (xanh) + "Đăng nhập gần nhất …" |
  | `LOCKED` | `is_active = false` hoặc `deleted_at` có giá trị | Bị khóa (đỏ) |

- **Màn Khách hàng:** cột "Tài khoản" và bộ lọc "Tài khoản: Tất cả / Đã có / Chưa có". Khách chưa có tài khoản hiện gợi ý: khách tự đăng ký bằng đúng email trong hồ sơ (đã xác thực) sẽ được nối tự động.
- **Màn Quản lý nhân viên:** cột "Tài khoản đăng nhập" (tên đăng nhập + trạng thái); khung chi tiết nhân viên hiện thêm lần đăng nhập gần nhất.
- **Màn Người dùng (admin):** cột "Đăng nhập gần nhất" và nhãn trạng thái.
- `last_login_at` được cập nhật ở mỗi phiên, giãn cách tối thiểu 1 giờ để không ghi cơ sở dữ liệu mỗi lần tải trang.

### 1.3. Ngoài phạm vi
- Lễ tân tạo hoặc mời tài khoản cho khách đang chưa có (gửi email mời, đặt mật khẩu tạm).
- Đọc trạng thái trực tiếp từ Keycloak (phiên đang mở, email đã xác thực chưa); chỉ dùng dữ liệu MySQL.
- Lịch sử đăng nhập chi tiết (đã có nhật ký kiểm toán cho đăng xuất).
- Lọc nhân viên theo trạng thái ở máy chủ (danh sách nhỏ, lọc phía client nếu cần).

## 2. Tiêu Chí Chấp Nhận

- **AC-1**: `GET /customers` trả mỗi khách một trường `account` gồm `status`, `username`, `lastLoginAt`; khách không có `user_id` có `status = NONE`. Dữ liệu được nạp theo lô, không truy vấn `users` từng dòng. _Kiểm chứng_: test service + test tích hợp đếm số câu truy vấn hoặc kiểm tra gọi `findAllById` một lần.
- **AC-2**: `GET /customers?hasAccount=true|false` lọc đúng theo `user_id`. _Kiểm chứng_: test tích hợp repository.
- **AC-3**: `GET /employees` và `GET /employees/{id}` trả `account` với trạng thái `PENDING`, `ACTIVE`, `LOCKED` đúng điều kiện ở bảng 1.2. _Kiểm chứng_: test service.
- **AC-4**: `UserResponse` có `lastLoginAt` và `accountStatus`. _Kiểm chứng_: test mapper/service.
- **AC-5**: `GET /auth/me` cập nhật `last_login_at` khi giá trị cũ trống hoặc đã quá 1 giờ, không cập nhật nếu chưa quá 1 giờ. _Kiểm chứng_: test `AuthServiceTest` với `Clock` cố định.
- **AC-6**: Màn Khách hàng có cột "Tài khoản" với 4 nhãn, bộ lọc 3 lựa chọn, gợi ý tự nối cho khách chưa có tài khoản có email. _Kiểm chứng_: test RTL; kiểm tra trên trình duyệt.
- **AC-7**: Màn Quản lý nhân viên và khung chi tiết hiện tên đăng nhập, trạng thái, lần đăng nhập gần nhất. _Kiểm chứng_: test RTL `StaffPage.test.tsx`.
- **AC-8**: Màn Người dùng có cột "Đăng nhập gần nhất" và nhãn trạng thái. _Kiểm chứng_: test RTL.
- **AC-9**: `./mvnw test`, `tsc -b`, `eslint`, vitest xanh.

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `src/main/java/.../shared/contracts/AccountDirectory.java` (mới) | `Map<Long, AccountSummary> summarize(Collection<Long> userIds)` |
| `src/main/java/.../shared/contracts/AccountSummary.java`, `AccountStatus.java` (mới) | record `(AccountStatus status, String username, Instant lastLoginAt)`; enum `NONE, PENDING, ACTIVE, LOCKED` |
| `src/main/java/.../identity/service/impl/AccountDirectoryImpl.java` (mới) | tính trạng thái từ `User`, một lần `findAllById` |
| `identity/dto/response/UserResponse.java`, `identity/mapper/UserMapper.java` | thêm `lastLoginAt`, `accountStatus` |
| `auth/service/impl/AuthServiceImpl.java` | `me()` làm mới `last_login_at` theo khoảng cách tối thiểu |
| `common/constant/SecurityConstants.java` | `LAST_LOGIN_REFRESH_INTERVAL = Duration.ofHours(1)` |
| `customer/dto/CustomerResponse.java`, `customer/service/CustomerServiceImpl.java`, `customer/repository/CustomerRepository.java`, `customer/controller/CustomerController.java` | trường `account`, nạp theo lô, tham số `hasAccount` |
| `scheduling/dto/response/EmployeeResponse.java`, `scheduling/service/impl/SchedulingServiceImpl.java` | trường `account`, nạp theo lô |
| `frontend/src/shared/types/contracts.ts`, `features/scheduling/types/scheduling.ts` | kiểu `AccountSummary` |
| `frontend/src/shared/components/AccountStatusBadge.tsx` (mới) | nhãn + "Đăng nhập gần nhất …" dùng chung |
| `features/customer/pages/CustomersPage.tsx`, `components/CustomerFilters.tsx`, `api/customerApi.ts` | cột, bộ lọc, tham số |
| `features/staff/components/EmployeeDirectory.tsx`, `EmployeeDetailSheet.tsx` | cột, thông tin chi tiết |
| `features/identity/pages/UsersPage.tsx` | cột đăng nhập gần nhất |
| Test backend/frontend tương ứng | theo AC |

## 4. Hợp Đồng Kỹ Thuật

- JSON mới trong `CustomerResponse` và `EmployeeResponse`:
  ```json
  "account": { "status": "ACTIVE", "username": "nguyenvanan", "lastLoginAt": "2026-11-10T08:15:00Z" }
  ```
  Khách không có tài khoản: `{ "status": "NONE", "username": null, "lastLoginAt": null }`.
- Thứ tự xét trạng thái: `LOCKED` (không hoạt động hoặc đã xóa) → `ACTIVE` (có `identity_id` hoặc `last_login_at`) → `PENDING`.
- `GET /customers?hasAccount=` kiểu `Boolean`, bỏ trống là tất cả; quyền giữ nguyên `HAS_FRONT_DESK`.
- Module `customer` và `scheduling` chỉ gọi `AccountDirectory` (shared contract), không đọc thẳng `UserRepository`.
- UI: tái sử dụng `Badge` trong `shared/components/ui`; nhãn tiếng Việt cố định trong `AccountStatusBadge`; ngày giờ theo `shared/utils/format`.

## 5. Checklist Thực Thi

- [x] **Step 1: `AccountStatus`, `AccountSummary`, `AccountDirectory` và bản cài đặt trong `identity`, kèm test** _(AC-1, AC-3)_
- [x] **Step 2: `UserResponse` thêm `lastLoginAt`, `accountStatus`** _(AC-4)_
- [x] **Step 3: `me()` làm mới `last_login_at` theo khoảng 1 giờ + test** _(AC-5)_
- [x] **Step 4: Khách hàng: trường `account`, nạp theo lô, tham số `hasAccount` + test** _(AC-1, AC-2)_
- [x] **Step 5: Nhân viên: trường `account` cho danh sách và chi tiết + test** _(AC-3)_
- [x] **Step 6: Kiểu frontend và `AccountStatusBadge`** _(AC-6, AC-7, AC-8)_
- [x] **Step 7: Màn Khách hàng: cột, bộ lọc, gợi ý tự nối + test** _(AC-6)_
- [x] **Step 8: Màn Quản lý nhân viên và khung chi tiết + test** _(AC-7)_
- [x] **Step 9: Màn Người dùng + test** _(AC-8)_
- [x] **Step 10: Chạy toàn bộ test; kiểm tra trên trình duyệt nếu có phiên đăng nhập** _(AC-9)_

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-11. Kết luận: **partial** — mọi tiêu chí có bằng chứng bằng test; phần giao diện chưa thao tác trên trình duyệt (container chưa build lại với code mới).

**Thay đổi so với hợp đồng mục 4:** không tạo `AccountDirectory`. Module nhân viên vốn đã đọc `users` theo lô qua `UserRepository`, nên khách hàng làm theo cùng cách; cách tính trạng thái nằm ở một chỗ là `identity/dto/response/AccountSummary` (cùng `AccountStatus`). `last_login_at` được làm mới bằng câu `UPDATE` có điều kiện (`UserRepository.touchLastLogin`) để không đụng khóa phiên bản lạc quan khi hai tab cùng tải.

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 `account` trong danh sách khách, nạp theo lô | Đạt | `CustomerServiceTest.searchCustomers_AddsAccountStatusInOneLookup` (một lần `findAllById`); chi tiết và tra theo mã cũng có `account` |
| AC-2 Lọc `hasAccount` | Đạt | `CustomerAccountFilterIntegrationTest` trên MySQL |
| AC-3 `account` của nhân viên | Đạt | `EmployeeServiceTest`; quy tắc trạng thái ở `AccountSummaryTest` (4 trường hợp) |
| AC-4 `UserResponse` có `lastLoginAt`, `accountStatus` | Đạt | Ánh xạ qua MapStruct; hiển thị kiểm ở `UsersPage.test.tsx` |
| AC-5 Làm mới `last_login_at` tối đa 1 lần/giờ | Đạt | `AuthServiceTest.meRecordsTheSignInAtMostOncePerHour` + `LastLoginRefreshIntegrationTest` trên MySQL |
| AC-6 Màn Khách hàng: cột, bộ lọc, gợi ý | Đạt | `CustomersPage.test.tsx`; thẻ di động cũng hiện nhãn; chưa xác minh trên trình duyệt |
| AC-7 Màn Quản lý nhân viên | Đạt | `StaffPage.test.tsx`; chưa xác minh trên trình duyệt |
| AC-8 Màn Người dùng | Đạt | `UsersPage.test.tsx`; chưa xác minh trên trình duyệt |
| AC-9 Toàn bộ test | Đạt | `./mvnw test` 619/619 trước khi thêm `LastLoginRefreshIntegrationTest` (chạy riêng, xanh); vitest 196/196 (`--maxWorkers=3 --testTimeout=30000`); `tsc -b`, `eslint` xanh |

**Điều chỉnh sau hội tụ (2026-11-11):** gợi ý "tự nối khi khách đăng ký bằng email" ở AC-6 không đúng thực tế, vì đăng ký trong app tạo tài khoản Keycloak với email chưa xác thực và realm không bật xác thực email, nên `linkExistingCustomerByVerifiedEmail` không nối được. Gợi ý đã đổi thành "Khách tự đăng ký sẽ tạo hồ sơ mới, chưa nối vào hồ sơ này" và hiện cho mọi khách chưa có tài khoản. Luồng nối tài khoản cho khách có sẵn để plan sau.

Lỗi phát hiện khi làm: `users.last_login_at` trước đây chỉ ghi ở lần đăng nhập đầu, đã sửa theo AC-5. `frontend/src/shared/types/schemas.ts` vốn chưa theo định dạng prettier nên không định dạng lại cả file.


# Kế Hoạch Lễ Tân Tạo Hoặc Nối Tài Khoản Đăng Nhập Cho Khách Có Sẵn

> **Mã plan**: `customer-account-linking`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-account-status-visibility.md` (mục "Ngoài phạm vi": lễ tân tạo/mời tài khoản cho khách; điều chỉnh sau hội tụ: khách tự đăng ký tạo hồ sơ trùng).

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- Khách do lễ tân tạo tại quầy (`customers.user_id` trống) không có cách nào để có tài khoản gắn với hồ sơ đó.
- Khi khách tự đăng ký (`RegistrationServiceImpl.register`), Keycloak tạo tài khoản với `emailVerified=false` (`KeycloakIdentityServiceImpl.createOrLinkUser`). Ở lần đăng nhập đầu, `AuthServiceImpl.me()` → `ensureCustomerProfileLinked` chỉ nối vào hồ sơ có sẵn khi email **đã xác thực** (`linkExistingCustomerByVerifiedEmail`); nếu không thì tạo **hồ sơ mới** mã `CUS-OIDC-…`. Kết quả: một người có hai hồ sơ, xe và lịch sử sửa chữa nằm ở hồ sơ cũ, tài khoản nằm ở hồ sơ mới.
- Realm Keycloak không bật xác thực email và không có máy chủ gửi mail (`docker/keycloak/realm-export.json`), nên con đường "xác thực email để tự nối" hiện không thể xảy ra.
- Nhân viên đã có tiền lệ: quản trị viên tạo tài khoản kèm mật khẩu ban đầu (`UserServiceImpl.createUser` → `createOrLinkUser(..., password)`, mật khẩu không phải tạm thời vì form đăng nhập dùng password grant, không xử lý được "required action").

### 1.2. Mục tiêu
- **A. Tạo tài khoản cho khách có sẵn:** ở màn Khách hàng, khách "Chưa có tài khoản" có nút **Tạo tài khoản**. Lễ tân nhập tên đăng nhập, email (điền sẵn từ hồ sơ), mật khẩu ban đầu (có nút sinh ngẫu nhiên). Hệ thống tạo tài khoản Keycloak vai trò CUSTOMER, dòng `users`, và nối ngay vào hồ sơ. Khách đăng nhập lần đầu thấy đúng xe và lịch sử của mình, không phát sinh hồ sơ mới.
- **B. Gộp hồ sơ trùng:** khách đã lỡ tự đăng ký (có hồ sơ `CUS-OIDC-…` riêng) thì lễ tân mở hồ sơ cũ, bấm **Nối tài khoản**, chọn hồ sơ đã có tài khoản. Hệ thống chuyển tài khoản sang hồ sơ cũ và ngừng hoạt động hồ sơ trùng. Chỉ cho gộp khi hồ sơ trùng chưa có xe, lịch hẹn, phiếu sửa chữa, hóa đơn hay bảo hành; nếu có thì báo rõ lý do.
- Gợi ý ở cột "Tài khoản" đổi thành hướng dẫn dùng hai nút trên.

### 1.3. Ngoài phạm vi
- **Bật xác thực email để tự nối.** Cần máy chủ gửi mail (SMTP hoặc Mailpit cho môi trường dev), bật `verifyEmail` trong realm, và đổi `ensureCustomerProfileLinked` để chưa tạo hồ sơ mới cho tới khi email được xác thực. Nên làm thành plan riêng sau khi A và B chạy ổn.
- Gửi mật khẩu cho khách qua email/SMS; khách tự đổi mật khẩu trong ứng dụng (hiện đổi qua trang tài khoản Keycloak).
- Gộp hồ sơ khi hồ sơ trùng đã có dữ liệu nghiệp vụ (chuyển xe, phiếu, hóa đơn giữa hai hồ sơ).
- Chặn đăng ký khi email trùng hồ sơ có sẵn (email chưa xác thực nên không đủ tin cậy, và làm lộ việc email đã tồn tại).

## 2. Tiêu Chí Chấp Nhận

- **AC-1**: `POST /api/v1/customers/{id}/account` (lễ tân, quản trị viên) với khách đang hoạt động, chưa có tài khoản: tạo người dùng Keycloak vai trò CUSTOMER, dòng `users` có `identity_id`, gán `customers.user_id`; trả khách hàng kèm `account.status = PENDING`. Ghi nhật ký `CUSTOMER_ACCOUNT_CREATED`. _Kiểm chứng_: test service với `KeycloakIdentityService` giả.
- **AC-2**: Từ chối với lý do tiếng Việt khi: khách đã có tài khoản, khách ngừng hoạt động, tên đăng nhập hoặc email đã dùng, mật khẩu không đạt chính sách. Keycloak không được gọi trong các trường hợp này. _Kiểm chứng_: test service.
- **AC-3**: Sau AC-1, lần gọi `GET /auth/me` đầu tiên của khách không tạo hồ sơ mới. _Kiểm chứng_: test tích hợp MySQL đi qua `me()` với JWT giả của tài khoản vừa tạo.
- **AC-4**: `POST /api/v1/customers/{id}/account/link` với `{ "sourceCustomerId": … }`: chuyển `user_id` từ hồ sơ nguồn sang hồ sơ đích, hồ sơ nguồn bị bỏ `user_id` và ngừng hoạt động, trong một giao dịch; ghi nhật ký `CUSTOMER_ACCOUNT_LINKED`. _Kiểm chứng_: test tích hợp MySQL.
- **AC-5**: AC-4 bị từ chối khi hồ sơ đích đã có tài khoản, hồ sơ nguồn không có tài khoản, hai hồ sơ trùng nhau, hoặc hồ sơ nguồn còn xe, lịch hẹn, phiếu sửa chữa, hóa đơn, bảo hành; thông báo nêu đúng loại dữ liệu đang chặn. _Kiểm chứng_: test service/tích hợp.
- **AC-6**: Màn Khách hàng: khách "Chưa có tài khoản" có nút **Tạo tài khoản** (Modal giữa màn hình, 2 cột trên máy tính, 1 cột trên điện thoại) và **Nối tài khoản** (Modal chọn hồ sơ đã có tài khoản, tìm theo tên/SĐT/email). Thành công thì danh sách cập nhật trạng thái. _Kiểm chứng_: test RTL; kiểm tra trên trình duyệt.
- **AC-7**: Toàn bộ test backend và frontend xanh.

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `customer/dto/CreateCustomerAccountRequest.java` (mới) | `username`, `email`, `password` + validation |
| `customer/dto/LinkCustomerAccountRequest.java` (mới) | `sourceCustomerId` |
| `customer/service/CustomerService.java`, `CustomerServiceImpl.java` | `createAccount`, `linkAccount` |
| `customer/controller/CustomerController.java` | hai endpoint, `HAS_FRONT_DESK` |
| `customer/repository/CustomerRepository.java` | khóa dòng khi gộp (`findByIdForUpdate`) |
| `common/constant/ApiPaths.java`, `AuditConstants.java` | đường dẫn `/{id}/account`, `/{id}/account/link`; hai hành động nhật ký |
| `identity/repository/UserRepository.java` | dùng lại `existsByUsername`, `existsByEmail` |
| `identity/service/KeycloakIdentityService.java` | dùng lại `createOrLinkUser` |
| `frontend/src/features/customer/api/customerApi.ts`, `hooks/useCustomers.ts` | `createAccount`, `linkAccount` + mutation |
| `frontend/src/features/customer/components/CustomerAccountModal.tsx` (mới) | form tạo tài khoản |
| `frontend/src/features/customer/components/LinkAccountModal.tsx` (mới) | chọn hồ sơ có tài khoản (dùng `GET /customers?hasAccount=true&search=`) |
| `frontend/src/features/customer/pages/CustomersPage.tsx`, `components/CustomerCards.tsx` | nút, gợi ý mới |
| Test: `CustomerServiceTest`, `CustomerAccountIntegrationTest` (mới), `CustomersPage.test.tsx` | theo AC |
| `docs/api/SECURITY.md` | mô tả hai endpoint |

## 4. Hợp Đồng Kỹ Thuật

- `POST /api/v1/customers/{id}/account` — `@PreAuthorize(HAS_FRONT_DESK)`, body:
  ```json
  { "username": "an.nguyen", "email": "an@example.com", "password": "…" }
  ```
  `username` theo `SecurityConstants.USERNAME_PATTERN`; `email` tùy chọn, mặc định email hồ sơ; `password` qua `PasswordPolicy.validateNewPassword`. Trả `200` + `CustomerResponse`.
- `POST /api/v1/customers/{id}/account/link` — `@PreAuthorize(HAS_FRONT_DESK)`, body `{ "sourceCustomerId": 12 }`. Trả `200` + `CustomerResponse` của hồ sơ đích.
- Lỗi: `DUPLICATE_RESOURCE` (tên đăng nhập/email trùng), `BusinessRuleException` cho các điều kiện nghiệp vụ, `AUTHENTICATION_UNAVAILABLE` khi Keycloak lỗi.
- Thứ tự tạo tài khoản: kiểm tra đầy đủ trong MySQL → `createOrLinkUser` → lưu `users` và `customers.user_id` trong cùng giao dịch. Giống `UserServiceImpl.createUser`: nếu lưu MySQL thất bại sau khi Keycloak đã tạo thì còn tài khoản Keycloak mồ côi; ghi log cảnh báo kèm tên đăng nhập để quản trị viên xử lý.
- Gộp: khóa cả hai hồ sơ theo thứ tự id tăng dần; đặt `user_id` của nguồn về `NULL` và `flush` trước khi gán cho đích (ràng buộc `uq_customers_user`).
- UI: `Modal` trong `shared/components/ui`, `Field`, `FormError`; mật khẩu sinh ngẫu nhiên đạt `policy.passwordMinLength`.

## 5. Checklist Thực Thi

- [x] **Step 1: DTO, hằng số đường dẫn và hành động nhật ký** _(AC-1, AC-4)_
- [x] **Step 2: `createAccount` + các điều kiện từ chối + test** _(AC-1, AC-2)_
- [x] **Step 3: Test tích hợp: tài khoản vừa tạo đăng nhập lần đầu không sinh hồ sơ mới** _(AC-3)_
- [x] **Step 4: `linkAccount` + điều kiện chặn theo dữ liệu nghiệp vụ + test tích hợp** _(AC-4, AC-5)_
- [x] **Step 5: Hai endpoint trong `CustomerController`** _(AC-1, AC-4)_
- [x] **Step 6: API client, hook, `CustomerAccountModal`, `LinkAccountModal`** _(AC-6)_
- [x] **Step 7: Nút và gợi ý ở màn Khách hàng (bảng và thẻ) + test RTL** _(AC-6)_
- [x] **Step 8: Cập nhật tài liệu API** _(AC-1, AC-4)_
- [x] **Step 9: Chạy toàn bộ test; build lại container; kiểm tra trên trình duyệt nếu có phiên đăng nhập** _(AC-7)_

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-11. Kết luận: **partial** — mọi tiêu chí có bằng chứng bằng test; chưa thao tác trên trình duyệt (chưa có phiên đăng nhập trong trình duyệt của phiên làm việc). Container đã build lại với code mới.

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Tạo tài khoản cho khách có sẵn | Đạt | `CustomerServiceTest.createAccount_CreatesIdentityAndLinksProfile`; `CustomerAccountIntegrationTest` kiểm `users.identity_id`, email mặc định từ hồ sơ |
| AC-2 Các điều kiện từ chối, không gọi Keycloak | Đạt | 3 test: đã có tài khoản, trùng tên đăng nhập/email, khách ngừng hoạt động (`verifyNoInteractions`) |
| AC-3 Đăng nhập lần đầu không sinh hồ sơ mới | Đạt | `createdAccountSignsIntoTheExistingProfile` đi qua `AuthService.me()` thật trên MySQL |
| AC-4 Nối tài khoản | Đạt | `linkMovesTheAccountAndRetiresTheDuplicate` |
| AC-5 Chặn gộp khi còn dữ liệu nghiệp vụ | Đạt | `linkRefusesWhenTheDuplicateHasBusinessData` (thông báo "1 xe"); các điều kiện còn lại nằm trong `linkAccount` |
| AC-6 Giao diện | Đạt (chưa xác minh trình duyệt) | `CustomersPage.test.tsx`: nút chỉ hiện với khách chưa có tài khoản, tạo tài khoản gửi tên gợi ý, email hồ sơ, mật khẩu sinh ngẫu nhiên; thẻ di động cũng có hai nút |
| AC-7 Toàn bộ test | Đạt | `./mvnw test` 627/627; vitest 198/198 (`--maxWorkers=3 --testTimeout=30000`); `tsc -b`, `eslint` xanh |

**Điều chỉnh kèm theo:** quy tắc trạng thái tài khoản (`AccountSummary.statusOf`, plan `account-status-visibility`) đổi thành "Đang hoạt động" chỉ khi đã có `last_login_at`. Trước đó chỉ cần có `identity_id`, nên tài khoản do lễ tân/quản trị viên vừa tạo hiện "Đang hoạt động" dù chưa đăng nhập. Hệ quả: các tài khoản cũ chưa từng ghi `last_login_at` hiện "Chưa đăng nhập lần nào" cho tới lần đăng nhập kế tiếp.

**Còn lại:** nếu lưu MySQL thất bại sau khi Keycloak đã tạo tài khoản thì còn tài khoản Keycloak mồ côi (giống `UserServiceImpl.createUser`); chỉ ghi log cảnh báo. Bật xác thực email để tự nối vẫn ngoài phạm vi.


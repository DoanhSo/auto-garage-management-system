# PLAN: Kiểm tra chi tiết luồng Đăng ký / Đăng nhập / Cổng tài khoản Keycloak, Khởi tạo tài khoản cho tất cả các Role & Nâng cấp toàn diện các phân hệ nghiệp vụ

> **Chế độ**: `PLAN:` (Tuân thủ `AutoGarage/AGENTS.md`, `AutoGarage/docs/CODING_GUIDELINES.md`, `AutoGarage/docs/UI_DESIGN_GUIDELINES.md` và `AutoGarage/docs/KEYCLOAK_SETUP.md`).

---

## 1. Mục tiêu & Phạm vi

Bản kế hoạch này bao phủ toàn diện:
1. **Kiểm tra chi tiết các luồng Đăng ký, Đăng nhập, Khôi phục phiên (`AG_REFRESH`), Thu hồi phiên (`auth_version`) và Cổng tài khoản Keycloak (`http://localhost:8081/realms/autogarage-realm/account?...`)**:
   - Phân tích 5 luồng xác thực end-to-end giữa Frontend (`LoginPage.tsx`, `RegistrationPage.tsx`, `AccountPage.tsx`, `AuthProvider.tsx`, `authApi.ts`), Nginx/Vite Proxy, Backend Resource Server (`AuthController`, `RegistrationController`, `BrowserSessionController`, `AccountTokenValidator`, `KeycloakIdentityServiceImpl`) và Keycloak 26 (`autogarage-realm`).
   - Giải quyết triệt để lỗi **`401 Unauthorized` / kẹt URL `?state=...&session_state=...&iss=...&code=...`** trên cổng Keycloak Account Console (`http://localhost:8081/realms/autogarage-realm/account`) do việc gỡ `default-roles-autogarage-realm` làm mất quyền `account:view-profile` và `account:manage-account`.
   - Giải quyết lỗi lệch `auth_version` hoặc thiếu kiểm tra trùng `email`/`username` trước khi gọi Keycloak trong `RegistrationServiceImpl` và đồng bộ danh tính nhân sự giữa MySQL (`users`) với Keycloak.
2. **Khởi tạo tài khoản cho tất cả các Role còn lại theo đúng định dạng của `ADMIN` và lưu vào file thông tin tài khoản Admin (`.docker-local/credentials.txt` & `.docker-local/credentials-3000.txt`)**:
   - Khởi tạo đầy đủ cho cả **5 Role trong `Role.java`** (`ADMIN`, `RECEPTIONIST`, `WAREHOUSE_STAFF`, `TECHNICIAN`, `CUSTOMER`) theo đúng chuẩn `garage.<role>` / mật khẩu `admin` VÀ đồng bộ toàn bộ tài khoản nhân sự/khách hàng trong dữ liệu thực tế (`letan.huong`, `thukho.tuan`, `kythuat.minh`, `kythuat.duc`, `doanhfilwl@gmail.com`).
3. **Nâng cấp chi tiết các phân hệ Sổ nợ & Tín dụng, Báo cáo & Xuất CSV 6 phần, Báo giá, Khoang & Lịch ca (3 tab), Bảo hành và Đối soát kho**.

---

## 2. Phân tích chi tiết các luồng Đăng ký, Đăng nhập & Cổng tài khoản Keycloak

### 2.1. Sơ đồ kiến trúc 5 luồng xác thực Keycloak trong AutoGarage

#### Luồng 1: Đăng nhập bằng Username / Email + Mật khẩu (`Direct Access Grant` + `Session Bridge`)
1. **Frontend (`LoginPage.tsx` $\rightarrow$ `authApi.loginWithPassword`)**:
   - Gửi `POST /realms/autogarage-realm/protocol/openid-connect/token` (qua Nginx/Vite proxy `/realms`) với `grant_type=password`, `client_id=autogarage-frontend`, `username=<identifier>`, `password=<password>`, `scope=openid profile email`.
   - Keycloak đối chiếu username hoặc email (`loginWithEmailAllowed=true`) và trả bộ token (`access_token`, `refresh_token`, `expires_in`).
2. **Frontend $\rightarrow$ Backend Session Bridge (`POST /api/v1/auth/session`)**:
   - `AuthProvider.tsx` gửi `refresh_token` kèm header `Authorization: Bearer <access_token>` và `Origin: http://localhost:3000` đến `BrowserSessionController.open()`.
   - `BrowserSessionServiceImpl` kiểm tra `Origin` khớp allowlist `AUTH_CORS_ORIGINS`, đổi `refresh_token` mới với Keycloak tại `KEYCLOAK_TOKEN_URI`, giải mã `access_token` mới để đối chiếu `sub` khớp với người dùng hiện tại, rồi gắn cookie `AG_REFRESH` (`HttpOnly`, `SameSite=Strict`, `Path=/api/v1/auth/session`).
3. **Đồng bộ danh tính & hồ sơ (`GET /api/v1/auth/me`)**:
   - `AccountTokenValidator.validate(jwt)` kiểm tra:
     - `sub`, `jti`, `iat`, `exp`, `aud` chứa `autogarage-api` (do mapper `autogarage-api-audience` cấp).
     - `KeycloakJwtAuthenticationConverter.extractSingleRole(jwt)` trích xuất role từ `realm_access.roles`.
     - Đối chiếu bản ghi `users` trong MySQL: `is_active == true`, `deleted_at == null`, `users.role == jwt.role`, và **`users.auth_version == jwt.auth_version`** (do mapper `autogarage-auth-version` cấp từ attribute `auth_version` của Keycloak user).
   - Trong `AuthServiceImpl.me()`:
     - Nếu nhân sự trong MySQL có `identity_id == null` nhưng `username` trùng `preferred_username` trong JWT, hệ thống tự động gắn `user.setIdentityId(subject)` và cập nhật `lastLoginAt`.
     - Nếu là `Role.CUSTOMER` đăng nhập lần đầu (JIT provisioning), hệ thống tự động tạo bản ghi `users` và liên kết/tạo bản ghi `customers` (`KH-OIDC-<userId>`).

#### Luồng 2: Đăng ký tài khoản Khách hàng (`POST /api/v1/auth/register` $\rightarrow$ Auto-login $\rightarrow$ JIT `/auth/me`)
1. **Frontend (`RegistrationPage.tsx` $\rightarrow$ `authApi.register`)**:
   - Gửi `POST /api/v1/auth/register` với `{ username, fullName, email, password }`.
2. **Backend (`RegistrationServiceImpl.register` $\rightarrow$ `KeycloakIdentityServiceImpl.createOrLinkUser`)**:
   - Kiểm tra `PasswordPolicy` (`>= 12` ký tự, `<= 72` bytes UTF-8).
   - Chuẩn hóa `username` và `email` về chữ thường (`Locale.ROOT`).
   - Gọi Keycloak Admin REST API (`POST /admin/realms/autogarage-realm/users`) tạo user với `enabled=true`, `emailVerified=false`, `attributes.auth_version=["0"]`, `credentials=[{type:"password", value:..., temporary:false}]`, và gán realm role `CUSTOMER`.
3. **Điểm cần củng cố trong Luồng 2**:
   - Hiện tại `RegistrationServiceImpl.register()` chưa kiểm tra `userRepository.existsByUsername(username)` và `userRepository.existsByEmail(email)` trước khi gọi Keycloak, dẫn tới trường hợp nếu email đã tồn tại trong bảng `users` nội bộ (nhưng chưa có trên Keycloak), user vẫn được tạo trên Keycloak rồi lúc vào `AuthServiceImpl.provisionCustomerFromJwt()` (dòng 107) lại bị xóa `email = null` âm thầm.
   - Ngoài ra, khi `KeycloakIdentityServiceImpl.createOrLinkUser()` gán role cho user mới, cần đảm bảo user luôn có quyền `account:view-profile` và `account:manage-account` để mở được cổng tài khoản Keycloak.

#### Luồng 3: Đăng nhập OIDC Authorization Code + PKCE (`/protocol/openid-connect/auth` $\rightarrow$ `/login?code=...&state=...`)
1. `authApi.startPkceRedirect()` sinh `verifier` (32 bytes random URL-safe), tính `code_challenge` (`SHA-256` base64url), lưu `{ verifier, state, redirectUri, createdAt, returnTo }` vào `sessionStorage['autogarage-oidc-pkce']`, rồi chuyển hướng tới `${env.keycloakPublicOrigin}/protocol/openid-connect/auth`.
2. Sau khi xác thực trên Keycloak, trình duyệt redirect về `http://localhost:3000/login?state=...&session_state=...&iss=...&code=...`.
3. `AuthProvider.tsx` đọc `code` và `state` từ `window.location.search`, gọi `authApi.consumePkceState(state)` để lấy `codeVerifier`, đổi `code` lấy token qua `authApi.exchangeAuthorizationCode()`, mở cookie phiên `AG_REFRESH`, gọi `/auth/me`, và dùng `window.history.replaceState` xóa `?code=...&state=...` khỏi thanh địa chỉ.

#### Luồng 4: Cổng tài khoản tự phục vụ Keycloak (`http://localhost:8081/realms/autogarage-realm/account?state=...&session_state=...&iss=...&code=...`)
- **Nguyên nhân gốc rễ gây lỗi trên URL mà người dùng phản ánh**:
  1. Trên trang `/account/security` (`AccountPage.tsx` dòng 24 & 107), nút **"Mở cổng tài khoản Keycloak"** trỏ tới `http://localhost:8081/realms/autogarage-realm/account`.
  2. Client `account-console` (`id = 8dd765b4-40ef-43da-8bea-78bfead2ec8f` — xuất hiện ở phần đuôi của `code=ee74ee7e-...8dd765b4-40ef-43da-8bea-78bfead2ec8f`) thực hiện PKCE redirect và trả trình duyệt về URL:
     `http://localhost:8081/realms/autogarage-realm/account?state=...&session_state=...&iss=...&code=...`
  3. Sau khi SPA của Keycloak Account Console đổi `code` lấy access token, nó gọi `GET http://localhost:8081/realms/autogarage-realm/account` (với `Accept: application/json`).
  4. Tuy nhiên, client `account-console` dùng `oidc-audience-resolve-mapper` (`fullScopeAllowed: false`), chỉ thêm audience `account` vào token nếu user có **client role của client `account` (`view-profile`, `manage-account`)**.
  5. Trong `docker/keycloak/setup.sh` (dòng 49–50) và `KeycloakIdentityServiceImpl.assignSingleGarageRole()` (dòng 320), để tránh nhân sự bị dính composite role `CUSTOMER` nằm trong `default-roles-autogarage-realm`, code đã **xóa `default-roles-autogarage-realm` khỏi `garage.admin` và các role nhân sự**, vô tình xóa luôn 2 quyền `account:view-profile` và `account:manage-account`!
  6. Kết quả: Token của `garage.admin` và nhân sự không có quyền truy cập Account REST API $\rightarrow$ bị trả **`401 Unauthorized`** ngay tại URL `http://localhost:8081/realms/autogarage-realm/account?state=...&code=...`!
- **Giải pháp khắc phục triệt để**:
  1. Cấp trực tiếp client roles `manage-account` và `view-profile` (thuộc client `account`) cho `garage.admin` và toàn bộ các tài khoản trong Keycloak (đã thực thi trực tiếp trên Keycloak hiện hành và xác minh `GET /realms/autogarage-realm/account` trả về **`200 OK`** cho cả 10 tài khoản).
  2. Cập nhật `docker/keycloak/setup.sh` và `KeycloakIdentityServiceImpl.assignSingleGarageRole()` để khi gán role cho bất kỳ user nào cũng tự động đảm bảo giữ/gán client roles `manage-account` và `view-profile` của client `account`.
  3. Cập nhật `AccountPage.tsx` dòng 24: chuẩn hóa URL `${env.keycloakPublicOrigin.replace(/\/$/, '')}/account/` (có dấu `/` ở cuối khớp với `baseUrl: /realms/autogarage-realm/account/` của `account-console`).

#### Luồng 5: Khôi phục phiên khi F5 (`POST /api/v1/auth/session/refresh`) & Đăng xuất thu hồi token (`POST /api/v1/auth/logout`)
- Khi tải lại trang (F5), `AuthProvider.tsx` gọi `POST /api/v1/auth/session/refresh` (sử dụng `navigator.locks` để chống race condition giữa nhiều tab khi Keycloak bật `revokeRefreshToken: true`, `refreshTokenMaxReuse: 0`).
- Khi đăng xuất, `AuthServiceImpl.logout()` tăng `users.auth_version = users.auth_version + 1` trong MySQL, gọi `KeycloakIdentityServiceImpl.syncAuthVersion()` cập nhật attribute `auth_version` trên Keycloak và gọi `/logout` thu hồi toàn bộ phiên Keycloak, sau đó `DELETE /api/v1/auth/session` xóa cookie `AG_REFRESH`.

---

## 3. Danh sách Tài khoản cho tất cả các Role đã khởi tạo & Lưu vào file Credentials

Toàn bộ **5 Role của hệ thống** (`ADMIN`, `RECEPTIONIST`, `WAREHOUSE_STAFF`, `TECHNICIAN`, `CUSTOMER`) đã được khởi tạo trên Keycloak (`http://localhost:8081`), gán đúng Realm Role + Client Roles `account` (`manage-account`, `view-profile`), đồng bộ `identity_id` & `auth_version` vào MySQL (`users`, `employees`, `customers`), kiểm tra đăng nhập thực tế (`PASS 100%`) và lưu vào cả 2 file:
- [`AutoGarage/.docker-local/credentials.txt`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/.docker-local/credentials.txt)
- [`AutoGarage/.docker-local/credentials-3000.txt`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/.docker-local/credentials-3000.txt)
- Script khởi tạo/đồng bộ tự động: [`AutoGarage/.docker-local/reset-admin-requested.ps1`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/.docker-local/reset-admin-requested.ps1)

| STT | Role (`Role.java`) | Username đăng nhập | Email đăng nhập | Password | Hồ sơ liên kết trong DB | Kiểm tra `/auth/me` & `/account` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | `ADMIN` | `garage.admin` | `admin@autogarage.local` | `admin` | Quản trị viên hệ thống | `1000 OK` · `/account 200` |
| **2** | `RECEPTIONIST` | `garage.receptionist` | `receptionist@autogarage.local` | `admin` | Lễ tân / Cố vấn dịch vụ chuẩn | `1000 OK` · `/account 200` |
| **3** | `WAREHOUSE_STAFF` | `garage.warehouse` | `warehouse@autogarage.local` | `admin` | Thủ kho phụ tùng chuẩn | `1000 OK` · `/account 200` |
| **4** | `TECHNICIAN` | `garage.technician` | `technician@autogarage.local` | `admin` | KTV & QC (`NV-TECH-00`, `can_qc=1`) | `1000 OK` · `/account 200` |
| **5** | `CUSTOMER` | `garage.customer` | `customer@autogarage.local` | `admin` | Khách hàng (`KH-GARAGE-01`) | `1000 OK` · `/account 200` |
| **6** | `RECEPTIONIST` | `letan.huong` | `letan.huong@autogarage.vn` | `admin` | Nguyễn Mai Hương (Dữ liệu thực tế) | `1000 OK` · `/account 200` |
| **7** | `WAREHOUSE_STAFF` | `thukho.tuan` | `thukho.tuan@autogarage.vn` | `admin` | Trần Anh Tuấn (Dữ liệu thực tế) | `1000 OK` · `/account 200` |
| **8** | `TECHNICIAN` | `kythuat.minh` | `kythuat.minh@autogarage.vn` | `admin` | Lê Hoàng Minh (`NV-TECH-01`, `can_qc=1`) | `1000 OK` · `/account 200` |
| **9** | `TECHNICIAN` | `kythuat.duc` | `kythuat.duc@autogarage.vn` | `admin` | Vũ Trọng Đức (`NV-TECH-02`, `can_qc=0`) | `1000 OK` · `/account 200` |
| **10** | `CUSTOMER` | `doanhfilwl@gmail.com` | `doanhfilwl@gmail.com` | `admin` | Nguyễn Quốc Doanh (`KH-0006`, Xe `30L-512.77`) | `1000 OK` · `/account 200` |

---

## 4. Danh sách file mục tiêu cần sửa trong Codebase

| STT | File mục tiêu | Vùng dòng code | Nội dung thêm / sửa chi tiết |
| :--- | :--- | :--- | :--- |
| **1** | `docker/keycloak/setup.sh` | Lines 46–56 | Sau khi gán realm role cho `garage.admin`, gán bổ sung client roles `manage-account` và `view-profile` của client `account` (`kc add-roles -r "$realm" --uid "$GARAGE_ADMIN_ID" --cclientid account --rolename manage-account --rolename view-profile`) để cổng `/realms/autogarage-realm/account/` luôn hoạt động ngay từ lúc khởi tạo container. |
| **2** | `src/main/java/com/Group13_65CNTT/AutoGarage/identity/service/impl/KeycloakIdentityServiceImpl.java` | Lines 300–369 | Trong `assignSingleGarageRole()`, sau khi gỡ `default-roles-autogarage-realm` cho các tài khoản nhân sự (`role != Role.CUSTOMER`), tự động đảm bảo gán client roles `manage-account` và `view-profile` của client `account` (nếu client `account` tồn tại trên Keycloak) để mọi tài khoản nhân viên tạo qua Admin UI đều mở được Keycloak Account Console. |
| **3** | `src/main/java/com/Group13_65CNTT/AutoGarage/auth/service/impl/RegistrationServiceImpl.java` | Lines 20–35 | Kiểm tra trùng `username` và `email` trong `UserRepository` trước khi gọi `keycloakIdentityService.createOrLinkUser()` để trả `BusinessException(ResponseCode.DUPLICATE_RESOURCE)` tức thì, tránh tạo tài khoản trên Keycloak rồi bị mất email khi vào `/auth/me`. |
| **4** | `frontend/src/features/auth/pages/AccountPage.tsx` | Lines 20–120 | Chuẩn hóa `keycloakAccountUrl = `${env.keycloakPublicOrigin.replace(/\/$/, '')}/account/`` (có dấu `/` ở cuối khớp `baseUrl` của `account-console`); hiển thị thêm thông tin định danh tài khoản (`identityId`, `authVersion`, `email`, `role`) rõ ràng trên trang Bảo mật tài khoản. |
| **5** | `frontend/src/features/auth/pages/LoginPage.tsx` & `RegistrationPage.tsx` | Toàn bộ file | Thiết kế bố cục Split Card (Cột ảnh thương hiệu + Cột form gọn trong 1 màn hình không cuộn dọc), đồng bộ với Landing Page và hiển thị thông báo lỗi rõ ràng theo từng mã lỗi Keycloak / Backend (`INVALID_CREDENTIALS`, `DUPLICATE_RESOURCE`, `INVALID_PASSWORD`, `AUTHENTICATION_UNAVAILABLE`). |
| **6** | `frontend/src/features/billing/pages/DebtLedgerPage.tsx`, `frontend/src/features/reporting/pages/ReportsPage.tsx`, `frontend/src/shared/utils/csvExport.ts` | Toàn bộ file | Đã hoàn thiện nâng cấp màn **Sổ nợ & Tín dụng** (4 thẻ KPI, bảng 8 cột Phải thu & 7 cột Phải trả, modal xem chi tiết hóa đơn/phiếu nhập & thu/chi nợ trực tiếp, xuất CSV) và màn **Báo cáo** (preset kỳ báo cáo, 4 tab chi tiết, xuất CSV tổng hợp 6 phần chuẩn UTF-8 BOM). |
| **7** | `frontend/src/features/estimate/...`, `frontend/src/features/scheduling/...`, `frontend/src/features/warranty/...`, `frontend/src/features/inventory/...` | Toàn bộ file | Đã hoàn thiện nâng cấp chi tiết các màn **Báo giá**, **Khoang & Lịch ca (3 tab)**, **Sổ bảo hành & Khiếu nại**, và **Đối soát kho**. |

---

## 5. Checklist thực thi

- [x] **Step 1 (Khởi tạo & Xác minh Tài khoản tất cả các Role trên Keycloak + MySQL)**: Khởi tạo đầy đủ 10 tài khoản cho cả 5 Role (`ADMIN`, `RECEPTIONIST`, `WAREHOUSE_STAFF`, `TECHNICIAN`, `CUSTOMER`), gán quyền `account` (`manage-account`, `view-profile`), kiểm tra đăng nhập OIDC + `/api/v1/auth/me` + `/realms/autogarage-realm/account` (`PASS 100%`) và lưu vào `.docker-local/credentials.txt` & `.docker-local/credentials-3000.txt`.
- [x] **Step 2 (Củng cố Backend & Keycloak Setup cho luồng Đăng ký / Đăng nhập / Account Console)**: Cập nhật `docker/keycloak/setup.sh`, `KeycloakIdentityServiceImpl.java`, `RegistrationServiceImpl.java` và `AccountPage.tsx` để đảm bảo mọi tài khoản tạo mới đều truy cập được cổng Keycloak Account Console (`/realms/autogarage-realm/account/`) và kiểm tra trùng username/email chặt chẽ.
- [x] **Step 3 (Đồng bộ giao diện `/login` & `/register` và Kiểm thử tổng thể)**: Rà soát `LoginPage.tsx`, `RegistrationPage.tsx`, `AccountPage.tsx` và chạy bộ test Backend + Frontend build để xác nhận toàn bộ các luồng Keycloak hoạt động ổn định 100%.

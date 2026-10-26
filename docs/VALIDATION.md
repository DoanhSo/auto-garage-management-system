# Kết quả kiểm chứng backend TV1 — 15/10/2026

## Form đăng nhập/đăng ký trong AutoGarage — 26/10/2026

Theo luồng cuối cùng được yêu cầu, người dùng nhập username hoặc email và mật
khẩu ngay trên AutoGarage. Không có nút mở trang Keycloak hay Google. `/register`
nhận họ tên/email/mật khẩu/xác nhận; backend `POST /api/v1/auth/register` chỉ cấp
CUSTOMER trong Keycloak, không nhận role/identityId. Frontend tự đăng nhập bằng
email sau khi tạo thành công; `/auth/me` JIT tạo User/Customer theo cơ chế sẵn có.
Email được chuẩn hóa, mật khẩu giữ nguyên và kiểm tra PasswordPolicy/UTF-8 byte
limit. Danh tính trùng không được liên kết/nâng quyền. Nếu gán role thất bại,
adapter cố xóa đúng danh tính vừa tạo, không đụng tài khoản đã tồn tại.

`scripts/build-docker.ps1 -OfflineMaven` đạt **365 tests backend, 0 failure/error/
skip**, gồm MySQL thật và H2, package thành công; frontend đạt **83 tests**, lint
và TypeScript/Vite build. Test mới bao phủ đăng ký anonymous, role/identity mass
assignment, email/mật khẩu không hợp lệ, duplicate 409, upstream 503 an toàn,
cleanup khi gán role lỗi, frontend tự login và trường hợp tạo được tài khoản
nhưng tự login thất bại. Database test tách khỏi stack cổng 3000.

Chromium trên stack Docker riêng đã kiểm tra đăng ký ngay trong app với địa chỉ
Gmail tổng hợp, tự vào workspace CUSTOMER, đăng nhập lại bằng email/mật khẩu,
mật khẩu sai, email trùng, API admin 403 và token cũ 401 sau logout. Theo dõi
navigation xác nhận không chuyển khỏi origin AutoGarage trong toàn bộ luồng.

Image mới là `2026.10.26.1`. Launcher giữ credential/volume, đối chiếu image ID
trong manifest và cập nhật tag khi dùng lại cấu hình cũ; seed DEMO dùng PKCE local.
Realm giữ `registrationEmailAsUsername=false` để không đổi username nhân viên;
`registrationAllowed=false` tắt form đăng ký Keycloak, Admin API của backend vẫn
hoạt động. Password grant bật cho web client theo trải nghiệm được chọn. Google
OAuth, OTP/link email và SMTP không thuộc phần triển khai này.

ZIP bản cuối đã đối chiếu manifest/image ID và stream nạp lại đủ 5 images. Đã chạy
launcher được phân phối bằng Windows PowerShell 5.1 trên project mới, cả MySQL/
Keycloak/PostgreSQL/volume đều trống: bootstrap garage.admin, backend healthy,
seed DEMO và UI đều đạt. Credential không có trong ZIP, được tạo riêng tại máy
nhận. Stack kiểm tra đã được tách khỏi dữ liệu cổng 3000.

## Sửa stack cổng 3000 ngày 26/10/2026

Container `autogarage` dùng image `latest` cũ: Nginx không proxy `/realms`, POST
token trả HTTP 405 HTML; client Keycloak chỉ cho redirect về cổng 5173. Đã tái
hiện bằng HTTP probe endpoint token trước sửa. Compose nay mặc định chọn image
đã kiểm thử `2026.10.24`; cập nhật stack tại chỗ bằng `--no-build --pull never`.
Image ID container đã đối chiếu khớp backend `7828e2b03526` và frontend
`f6716dc38050`. Các service healthy; probe sau sửa nhận JSON Keycloak HTTP 401
cho credential cố ý không hợp lệ, thay vì HTML 405.

Chromium mở được form Keycloak với redirect về `http://localhost:3000/login`;
token endpoint trả đúng CORS origin và public UI policy trả HTTP 200. Alias
`latest` local cũng đã đồng bộ về hai image đã kiểm thử. Bộ ZIP được xuất lại bằng
Windows PowerShell 5.1 và stream `images.tar` vào `docker load` đạt đủ 5 images.

`keycloak-setup` luôn đồng bộ cấu hình client/profile/mapper cho realm có sẵn,
kể cả khi không tạo quản trị viên tự động. Hai tài khoản và auth version hiện có
giữ nguyên; không reset mật khẩu hay volume. Không thay Java/TypeScript nên
không chạy lại bộ 354/79 tests đã ghi ở đợt đóng gói; kiểm tra lượt này tập trung
vào image thực chạy, proxy token, runtime URL và redirect trên trình duyệt.

## Cập nhật kiểm chứng ngày 24/10/2026

### Bộ Docker chạy ngay (kiểm chứng bổ sung)

Docker Desktop đã hoạt động trong lượt đóng gói tiếp theo. `scripts/build-docker.ps1 -OfflineMaven`
đạt **354 tests backend, 0 failure/error/skip**, gồm toàn bộ MySQL/H2, và package;
frontend đạt **79 tests**, lint và Vite/TypeScript build. Database test riêng, không
dùng dữ liệu của stack `autogarage` đang chạy.

Stack preview mới đã khởi tạo từ volume mới: MySQL, PostgreSQL, Keycloak 26.0,
setup quản trị viên, backend, frontend. Đã sửa import user-profile không tương thích
26.0; bootstrap dùng ID thực do Keycloak cấp qua volume riêng. PUT hồ sơ Keycloak
giữ email/tên/verification/attributes khi thu hồi phiên hoặc khóa tài khoản; hai
regression mới bảo vệ hành vi này. Fixture của 5 bài MySQL tạo user đã bổ sung
identity ID phù hợp contract OIDC, giữ nguyên assertion rollback/race/quyền.

API thật qua Nginx đã xác nhận login ADMIN, 3 khách hàng DEMO/3 xe/2 dịch vụ/3 lịch
hẹn, logout làm token cũ trả 401, login lại thành công và runtime realm URL đúng.
Chromium đã đăng ký CUSTOMER trên Keycloak thật, hoàn tất PKCE callback và login
SSO lại sau logout; thuộc tính `auth_version` không xuất hiện trên form đăng ký.

Launcher trong bộ phân phối chạy đạt bằng **Windows PowerShell 5.1**, bao gồm chạy
lại trên dữ liệu đã có mà không tạo trùng DEMO hoặc reset mật khẩu. `images.tar`
trong ZIP đã được đọc stream vào `docker load` và nạp đủ 5 image thành công. Bộ ZIP
không chứa `.env`, credentials, volume, log hoặc fixture trình duyệt; mỗi máy nhận
tự sinh secret và tự chọn cổng frontend còn trống.

Chi tiết chạy/nhận bộ đóng gói: [DOCKER.md](DOCKER.md). Giới hạn upload chỉ lưu
metadata/hash vẫn giữ nguyên; chưa triển khai storage nội dung tệp.

### Kiểm chứng trước khi Docker được bật

Các số liệu 15/10 phía dưới là lịch sử. Đợt 24/10 rà soát backend, lỗi HTTP, chính sách xưởng, Keycloak và quyền sở hữu tiếp nhận.

Kết quả cuối ngày 24/10: **BUILD SUCCESS; 266 tests, 0 failures, 0 errors, 0 skipped**, có thực thi test và đóng gói Spring Boot jar `target/AutoGarage-0.0.1-SNAPSHOT.jar` (67.498.100 byte). Lệnh `package` bên dưới hoàn tất trong 1 phút 39 giây lúc 20:17:42 Asia/Bangkok; log local `.codex-backend-local.log`. Vòng riêng ownership/QC trước đó đạt 21 tests; không cộng lặp vào tổng 266.

Môi trường hiện tại có JDK 21.0.10 và Maven dependencies trong cache. MySQL test hiện không kết nối được; `mvnw.cmd -o test` đã chạy nhưng lỗi khởi tạo MySQL với `Connection refused`. Không dùng H2 để kết luận Flyway/MySQL locking/concurrency đã đạt. Các bài unit và H2 được chạy độc lập bằng lệnh PowerShell sau, tại thư mục chứa `pom.xml`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$localTests = Get-ChildItem src/test/java -Recurse -Filter '*.java' |
    Where-Object {
        $_.Name -match 'Tests?\.java$' -and
        -not ([IO.File]::ReadAllText($_.FullName).Contains('@ActiveProfiles("test")'))
    } |
    ForEach-Object BaseName
.\mvnw.cmd -o ("-Dtest=" + ($localTests -join ',')) package
```

Lệnh chọn toàn bộ unit/H2, bao gồm `AuthConfigurationTests`, `HttpErrorContractIntegrationTest`, `UserIntegrationTest`, `TV4IntegrationTests` và `ReceptionOwnershipIntegrationTest`; không dùng skipTests. Chín lớp dùng profile MySQL `test` được giữ nguyên để chạy lại `mvnw.cmd test` khi MySQL test hoạt động: AutoGarageApplicationTests, BootstrapIntegrationTests, MigrationSchemaTests, NotificationConcurrencyTests, SecurityConcurrencyTests, SecurityInfrastructureTests, SecurityIntegrationTests, SecurityRollbackTests, TV1IntegrationTests.

Regression mới kiểm tra actor giả, ownership customer/vehicle/booking/attachment/check qua filter/JWT/DB thật H2, QC assignment/can_qc fail closed, lỗi dependency không bị đổi thành BAD_REQUEST hoặc nuốt mất, cause an toàn, upload 413 và policy khác mặc định/sai cấu hình.

Giới hạn: chưa xác minh MySQL thật hoặc luồng browser với Keycloak thật trong lượt này. Upload hiện lưu metadata/hash/storage key, chưa có storage implementation lưu/đọc nội dung tệp.

Frontend ngày 24/10: `npm run check` đạt lint (không warning), **77 tests pass** và `tsc -b && vite build` thành công. Regression gồm PKCE S256/state/expiry/replay/redirect, logout và refresh chạy đồng thời, token thời hạn ngắn, multipart giữ nguyên bytes, ID/giá chính xác, editor không tự đóng, tạo dịch vụ với trường tùy chọn trống, và khách hàng đặt lịch qua `/customers/me` không tải danh bạ.

Đã xem giao diện đăng nhập/workspace trong Chromium ở desktop 1440×1000 và mobile 390×844; dữ liệu API dùng fixture tại trình duyệt, không thay thế nghiệm thu Keycloak/backend thật. Giao diện dùng một kiểu tối giản, bỏ bộ chọn prototype, giữ điều hướng thu gọn/mobile và ẩn thao tác theo role. Ảnh kiểm tra nằm trong `frontend/output/playwright/` (Git ignore).

Tham chiếu cấu hình OIDC/PKCE: [Keycloak JavaScript adapter](https://www.keycloak.org/securing-apps/javascript-adapter). Trước triển khai, làm theo [KEYCLOAK_SETUP.md](KEYCLOAK_SETUP.md) để cập nhật mapper/profile cho realm đã tồn tại và cấu hình public realm URL.

Source đã viết lại theo kiến trúc 7 tầng: common/auth/identity/audit/notification/reporting, JPA, MapStruct, Lombok, Service/ServiceImpl, AOP audit, Swagger và numeric ApiResponse. Kết quả dưới đây dành cho source sau refactor.

## Lệnh và kết quả

- JDK 21.0.10, Spring Boot 4.1.1, Maven Wrapper 3.10.0.
- MySQL 8.4.11 trong container riêng autogarage-tv1-errors-20261015, DB autogarage_test/cổng 55917; TEST_DB_URL override trong process. H2 dùng profile riêng, Flyway tắt và Hibernate create-drop chỉ cho H2.
- `mvnw.cmd -B -ntp package`: **BUILD SUCCESS; 145 tests, 0 failures, 0 errors, 0 skipped** cho bản sửa cấu hình/exception TV1. Có chạy test, không dùng skipTests. Kết thúc 14:24:42 Asia/Bangkok ngày 15/10/2026. Log cuối: `target/tv1-errors-verified.log`.
- Đã chạy trước đó các vòng unit tập trung 54 bài, full 142/144 bài và riêng HTTP server test. Số lượng tăng do bổ sung các bài kiểm tra rủi ro thực tế, không tính lặp các vòng chạy vào tổng 145 bài.
- HTTP thật qua `HttpErrorContractIntegrationTest`: Tomcat cổng ngẫu nhiên, bind 127.0.0.1, JDK HttpClient và H2 schema riêng. 12 request xác nhận thiếu token, CORS allow/reject JSON/cache 11 giây, JSON sai/media sai, login, policy page size 25/default 10, password tối thiểu 20 và không ghi account sai, report tối đa 30 ngày, logout/token bị thu hồi. Các policy cố ý khác mặc định để kiểm tra cấu hình có hiệu lực.
- Profile test/h2 tắt bootstrap, worker, reminder và email/SMS provider bằng giá trị explicit. Vòng cuối còn đặt bốn env bật worker/reminder/email/SMS thành true trong process Maven để xác nhận test profile vẫn vô hiệu hóa gửi ngoài; HTTP test kiểm tra trực tiếp bốn flag false.
- Flyway validate V1–V7 đạt, không pending. Metadata: 30 bảng nghiệp vụ, 430 cột, 76 FK, 76 CHECK, 44 UNIQUE, hai view; V1–V4 không có Git diff, V5 giữ nguyên khi thêm V6/V7.
- Helper env tạo signing key/password ngẫu nhiên, import process và docker compose config đạt; không in credential, không ghi đè file có sẵn. File secret tạm được dọn sau kiểm tra.

## 145 bài tự động

- Context 1; schema/migrations/views 16; typed auth/bootstrap configuration 8; bootstrap integration 5.
- UserService Mockito 10 (gồm duplicate race giữ cause và constraint khác không bị đổi thành duplicate); JPA/MapStruct/service/audit integration H2 4.
- HTTP security 31: login, JWT signature/algorithm/claims/time, generic credentials, logout/password revocation, năm role, input/mass assignment/PUT/version, last admin, ownership A/B/chuyển chủ, technician/QC, private notification, CORS/default deny và safe error.
- MySQL account concurrency 3: demote admin cuối, same-version update, duplicate registration.
- Audit rollback 3 và infrastructure error 1: audit fail không để mutation/auth_version commit một phần, DB token lookup lỗi safe 503/4002.
- Delivery Mockito 10: WEB, SMTP enabled/disabled, SMS enabled/disabled, config HTTPS/token, headers/idempotency/configured timeout, reject response, giữ cause mail/IO và interrupt flag; provider I/O được mock.
- Worker Mockito 5: disabled, provider failure/retry, complete cùng lease, expired lease không gọi provider và DB complete lỗi không bị coi là provider failure.
- MySQL notification/replay concurrency 6: disjoint claims, lease recovery/stale worker, retry giới hạn, enqueue/replay rollback cùng commit, concurrent identical replay chỉ mutation một lần, hash/actor isolation.
- Report period unit 4: ngược khoảng, quá hạn, 366 ngày +1 giây và giới hạn 30 ngày từ cấu hình.
- TV1 HTTP/MySQL integration 10: envelope/path/numeric code, notification dedup/owner/read, payload conflict, report quyền/range/CSV, audit metadata privacy, OpenAPI, search/sort allowlist, số liệu report từ nhiều payment/ledger/labor và loại VOID, reminder dedup và chuyển chủ.

- GlobalExceptionHandler HTTP 24: BusinessException, credentials/auth/authorization, MySQL/H2 UNIQUE, FK/CHECK/NOT NULL, locking/timeout, DB/transaction unavailable, SQL bug/provider/unexpected, validation field/object, JSON/type/enum/date/missing parameter, 404/405 (Allow header)/406/415. Assertion response/log không lộ giá trị nhạy cảm; nguyên nhân kỹ thuật được phân loại đúng.
- Typed policy configuration 3: password/pagination khác mặc định, UTF-8 byte limit, config sai fail startup, report/provider timeout bind đúng.
- HTTP server integration 1 với 12 request như mô tả ở trên.

Report fixture đối soát revenue 100, cash in/out 50/50, customer/supplier debt 50/150, ledger stock value 300, closed labor 1 giờ; không chỉ thử báo cáo trống.

## Môi trường và giới hạn

Docker Desktop đã được phục hồi lỗi socket Inference/Secrets Engine bằng bảo toàn và đổi tên runtime directory; docker info xác nhận engine 29.4.0, MySQL test hoạt động. Chi tiết ở DOCKER_TROUBLESHOOTING.md. Container/volume test do phiên làm việc tạo được dọn sau kiểm tra; Docker Desktop tiếp tục chạy.

Log ERROR/WARN trong các bài cố ý inject DB/audit/provider failure là hành vi được kiểm thử. Spring Data cảnh báo serialization PageImpl; API hiện giữ Page<T> theo mẫu người dùng và có test contract content/totalElements. Khi nâng framework cần kiểm tra lại contract này.

Chưa gửi SMTP/SMS thật hoặc xác nhận credential/provider môi trường; SMS adapter cần gateway đúng contract trong api/TV1.md. Reminder hiện enqueue WEB cho account active. UI React, tích hợp I01/I08 vào các service TV2–TV4 và toàn bộ T01–T38/G0–G5 chưa nghiệm thu. Không tuyên bố coverage %, load test hoặc production rollout.

Kết quả 17/75 bài trước đây là lịch sử của scaffold/security cũ; số kiểm chứng source hiện tại là 145 bài ở trên. Vòng 110 bài là bản viết lại 7 tầng trước khi hoàn thiện policy và exception. Logs/surefire/package trong target là output local được ignore, nhóm chạy lại bằng README trên DB test riêng.

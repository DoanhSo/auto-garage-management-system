# AutoGarage

Hệ thống quản lý gara một chi nhánh, Java 21/Spring Boot 4.1.1/MySQL 8.4. Backend TV1 được viết lại theo tài liệu kiến trúc người dùng cung cấp: common, auth, identity, audit, notification và reporting. Frontend TV1 đã có React/TypeScript tích hợp API; các module nghiệp vụ TV2–TV4 còn là khung/hợp đồng.

## Cấu trúc và tài liệu

- [Quy chuẩn viết code](docs/CODING_GUIDELINES.md): 7 tầng, Lombok, MapStruct, JPA, Service/ServiceImpl, Swagger, RBAC, transaction và kiểm thử.
- [Kiến trúc thực tế](docs/ARCHITECTURE.md), [phân công TV1–TV4](docs/WORK_BREAKDOWN.md), [kế hoạch/trạng thái TV1](docs/TV1_IMPLEMENTATION_PLAN.md).
- [API auth/tài khoản](docs/api/SECURITY.md), [API audit/notification/report và I08](docs/api/TV1.md).
- [Database](docs/DATABASE.md), [data dictionary](docs/DATA_DICTIONARY.md), [I01–I08](docs/SERVICE_CONTRACTS.md), [T01–T38](docs/ACCEPTANCE_TESTS.md).
- [Kết quả kiểm chứng thực tế](docs/VALIDATION.md).

## Chạy local trên PowerShell

Từ thư mục chứa pom.xml; cần JDK 21 và Docker đang chạy:

```powershell
.\scripts\initialize-local-env.ps1 -WithBootstrapAdministrator
. .\scripts\import-local-env.ps1
docker compose up -d --wait mysql
.\mvnw.cmd spring-boot:run
```

Script tạo secret ngẫu nhiên trong .env riêng và không ghi đè file đã có. Đọc username/password bootstrap trong file riêng trên máy; sau lần khởi tạo đặt AUTH_BOOTSTRAP_ENABLED=false và xóa bootstrap password. Spring Boot nhận biến process, nên phải import cấu hình. Password trong .env phải khớp volume MySQL đã provision.

Backend cổng 8080, MySQL local 3307. Swagger UI `/swagger-ui/index.html` và JSON `/v3/api-docs` khi OPENAPI_ENABLED=true; mặc định ứng dụng tắt docs, helper local bật. Login `/api/v1/auth/login`, sau đó dùng Authorization: Bearer token.

JSON thống nhất `code` số, `message`, `success`, `data`, `timestamp`, `path`. Admin create user 201; logout/password/deactivate trả 200 có envelope. List trả Spring Page trong data.content/totalElements. API chi tiết và các thay đổi contract ở docs/api.

## Kiểm thử

```powershell
.\scripts\initialize-local-env.ps1
. .\scripts\import-local-env.ps1
docker compose --profile test up -d --wait mysql-test
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
docker compose stop mysql-test
```

H2 integration dùng profile h2 riêng, tự chạy trong suite. MySQL test cổng 3308/DB autogarage_test; TEST_DB_URL/TEST_DB_USERNAME/TEST_DB_PASSWORD có thể override. Test sinh signing key riêng và tắt bootstrap/workers/provider. Test transaction rollback hoặc cleanup namespace trên DB riêng; không chạy suite cạnh tranh trên cùng DB fixture.

V1–V8 có 30 bảng nghiệp vụ, 431 cột, 76 FK, 77 CHECK và hai view; Flyway history riêng. V5 auth_version, V6 audit columns users, V7 audit/version notifications. Giữ checksum migrations đã áp dụng; schema mới thêm migration mới.

## Worker và provider

NOTIFICATION_WORKER_ENABLED và NOTIFICATION_REMINDERS_ENABLED mặc định false. Bật chủ động sau khi cấu hình; reminder chạy 08:00 theo GARAGE_TIME_ZONE. WEB hoạt động qua inbox; EMAIL dùng SMTP, SMS dùng HTTPS gateway có contract trong docs/api/TV1.md. Không có credential/provider mặc định. Test gửi ngoài qua mock; cần cấu hình provider của môi trường để gửi thật.

Frontend TV1 P0 đã triển khai tại [frontend/](frontend/README.md); tích hợp I01/I08 vào nghiệp vụ TV2–TV4 và demo toàn bộ T01–T38 vẫn theo WORK_BREAKDOWN. Kết quả backend không đồng nghĩa đã nghiệm thu toàn ứng dụng gara.

Cấu hình policy và quy tắc exception/response của TV1: [docs/CONFIGURATION_AND_ERRORS.md](docs/CONFIGURATION_AND_ERRORS.md).

Kế hoạch frontend TV1: [UI/UX, component, hooks, validation, API dependencies và lộ trình nghiệm thu](docs/frontend/TV1_FRONTEND_PLAN.md).

Frontend: xem [hướng dẫn chạy và cấu trúc](frontend/README.md) và [kiểm chứng TV1 frontend](docs/frontend/TV1_FRONTEND_VALIDATION.md). Cần Node 24; chạy npm ci rồi npm run dev trong frontend/.

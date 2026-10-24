# AutoGarage

Hệ thống quản lý gara một chi nhánh, Java 21/Spring Boot 4.1.1/MySQL 8.4 tích hợp **Keycloak 26 IAM (OIDC / OAuth 2.0 / RS256)** và PostgreSQL 16 riêng cho Keycloak. Backend TV1 theo kiến trúc 7 tầng: `common`, `auth`, `identity`, `audit`, `notification` và `reporting`.

## Cấu trúc và tài liệu

- [Quy chuẩn viết code](docs/CODING_GUIDELINES.md): 7 tầng, Lombok, MapStruct, JPA, Service/ServiceImpl, Swagger, RBAC, transaction và kiểm thử.
- [Kiến trúc thực tế](docs/ARCHITECTURE.md), [phân công TV1–TV4](docs/WORK_BREAKDOWN.md), [kế hoạch/trạng thái TV1](docs/TV1_IMPLEMENTATION_PLAN.md).
- [API auth/tài khoản (Keycloak OIDC Resource Server)](docs/api/SECURITY.md), [API audit/notification/report và I08](docs/api/TV1.md).
- [Thiết lập Keycloak, quản trị viên đầu tiên và nâng cấp realm đã tồn tại](docs/KEYCLOAK_SETUP.md).
- [Database](docs/DATABASE.md), [data dictionary](docs/DATA_DICTIONARY.md), [I01–I08](docs/SERVICE_CONTRACTS.md), [T01–T38](docs/ACCEPTANCE_TESTS.md).
- [Kết quả kiểm chứng thực tế](docs/VALIDATION.md).

## Chạy local trên PowerShell

Từ thư mục chứa `pom.xml`; cần JDK 21 và Docker đang chạy:

```powershell
.\scripts\initialize-local-env.ps1
. .\scripts\import-local-env.ps1
docker compose up -d --wait mysql keycloak-postgres keycloak
# Tạo quản trị viên gara và cấu hình bootstrap theo docs/KEYCLOAK_SETUP.md trước lần chạy đầu.
.\mvnw.cmd spring-boot:run
```

Script tạo secret ngẫu nhiên trong `.env` riêng và không ghi đè file đã có. Dịch vụ `keycloak` tự động nạp cấu hình realm từ [`docker/keycloak/realm-export.json`](docker/keycloak/realm-export.json) (`autogarage-realm`, 5 roles, `autogarage-frontend` với PKCE S256 & Refresh Token Rotation, `autogarage-api` Resource Server).

Backend cổng 8080, MySQL local 3307, Keycloak cổng 8081. Swagger UI `/swagger-ui/index.html` và JSON `/v3/api-docs` khi `OPENAPI_ENABLED=true`. Client xác thực OIDC với Keycloak để nhận cặp Access Token + Refresh Token (RS256), sau đó gửi `Authorization: Bearer <access_token>` tới Backend.

Frontend dùng `VITE_KEYCLOAK_PUBLIC_ORIGIN` là URL realm công khai đầy đủ khi Keycloak chạy riêng, ví dụ local `http://localhost:8081/realms/autogarage-realm`; token request vẫn có thể dùng `VITE_KEYCLOAK_ISSUER=/realms/autogarage-realm` qua proxy. Chỉ dùng public origin tương đối khi reverse proxy cùng origin chuyển tiếp đầy đủ `/realms` và `/resources`, với public hostname Keycloak được cấu hình đúng. Xem [thiết lập Keycloak](docs/KEYCLOAK_SETUP.md).

JSON thống nhất `code` số, `message`, `success`, `data`, `timestamp`, `path`. Admin create user 201; logout/deactivate trả 200 có envelope. List trả Spring Page trong `data.content`/`totalElements`.

## Kiểm thử

```powershell
.\scripts\initialize-local-env.ps1
. .\scripts\import-local-env.ps1
docker compose --profile test up -d --wait mysql-test
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
docker compose stop mysql-test
```

H2 integration dùng profile `h2` riêng, tự chạy trong suite. MySQL test cổng 3308/DB `autogarage_test`. Test tự động khởi tạo loopback JWKS endpoint và cặp khóa RSA 2048-bit (`RS256`) trong JVM để kiểm chứng toàn bộ đường đi của `NimbusJwtDecoder` mà không phụ thuộc mạng ngoài.

V1–V9 quản lý toàn bộ schema nghiệp vụ qua Flyway (`V9__add_keycloak_identity_id.sql` bổ sung `identity_id` UNIQUE liên kết Keycloak `sub` UUID và cho phép `password_hash` nullable).

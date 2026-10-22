# API auth, tài khoản và phân quyền (Keycloak OIDC Resource Server)

Backend theo 7 tầng: `AuthController`/`AuthServiceImpl` và `UserController`/`UserServiceImpl`, MapStruct `UserMapper`, `UserRepository` JPA và `common/security`. Package gốc `com.Group13_65CNTT.AutoGarage`; Java 21/Spring Boot 4.1.1.

## Envelope chuẩn

```json
{"code":1000,"message":"Success","success":true,"data":{"id":1,"identityId":"550e8400-e29b-41d4-a716-446655440000"},"timestamp":"2026-10-22T00:00:00Z","path":"/api/v1/auth/me"}
```

## Kiến trúc Keycloak OIDC & OAuth2 Resource Server

- **Identity Provider (IdP):** Keycloak 26 (Quarkus) kết nối container PostgreSQL 16 độc lập (`keycloak-postgres` trong `compose.yaml`), cấu hình sẵn tại [`docker/keycloak/realm-export.json`](../../docker/keycloak/realm-export.json).
- **Cấp phát & Làm mới Token (Access + Refresh Token):** Thực hiện trực tiếp tại Keycloak (`autogarage-realm`) theo chuẩn OAuth 2.0 Authorization Code Flow + PKCE (`S256`) với Refresh Token Rotation (`revokeRefreshToken: true`, `refreshTokenMaxReuse: 0`).
- **Xác thực Token tại Backend (RS256 & JWKS):**
  - `IdentityConfiguration` cấu hình `NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).jwsAlgorithm(SignatureAlgorithm.RS256).build()`.
  - `KeycloakJwtAuthenticationConverter` trích xuất vai trò từ `realm_access.roles` (hoặc claim `role`) và ánh xạ sang `ROLE_<ROLE>` trong `SecurityContextHolder`.
  - `AccountTokenValidator` xác minh chữ ký bất đối xứng, `iss`, `aud`, `exp`, `iat`, `jti`, đối chiếu tài khoản trong bảng `users` (qua `identity_id` hoặc `preferred_username`), kiểm tra trạng thái `is_active`, `deleted_at`, `role` và `auth_version`.
  - `CurrentUserProvider` ánh xạ `JwtAuthenticationToken` trong `SecurityContextHolder` về `users.id` (`long`) để giữ nguyên tính tương thích với toàn bộ các tầng nghiệp vụ và kiểm soát truy cập đối tượng (I01).

## Endpoint

- `GET /api/v1/auth/me`: Yêu cầu Bearer JWT hợp lệ từ Keycloak. Trả về `200 UserResponse` của chính người dùng đang đăng nhập; tự động liên kết `identity_id` (Keycloak `sub` UUID) và cập nhật `last_login_at` trong lần truy cập đầu tiên.
- `POST /api/v1/auth/logout`: Yêu cầu Bearer JWT hợp lệ. Tăng `auth_version` của tài khoản và ghi audit `AUTH_LOGOUT_ALL`, thu hồi tức thì mọi token đã cấp của người dùng đó trên Resource Server.
- `POST /api/v1/admin/users`: `ADMIN`, body `username`, `email`, `fullName`, `role`, và `identityId` (tùy chọn). Trả `201/1001` và header `Location`.
- `GET /api/v1/admin/users`: `ADMIN`, hỗ trợ `keyword`, `active`, `page`, `size`, `sortBy`, `sortDir` với allowlist sort (`id`, `username`, `fullName`, `role`, `active`, `createdAt`).
- `GET /api/v1/admin/users/{id}`: `ADMIN`, trả `200 UserResponse` hoặc `404`.
- `PUT /api/v1/admin/users/{id}`: `ADMIN`, bắt buộc `fullName`, `role`, `active`, `version`. Đổi `role` hoặc `active` tự động tăng `auth_version` để thu hồi token cũ.
- `DELETE /api/v1/admin/users/{id}?version=...`: `ADMIN`, soft-delete (`deleted_at`), đặt `is_active = false`, tăng `auth_version` thu hồi token và giữ toàn vẹn khóa ngoại lịch sử.

# API auth, tài khoản và phân quyền

Backend theo 7 tầng: AuthController/AuthServiceImpl và UserController/UserServiceImpl, MapStruct UserMapper, UserRepository JPA và common security. Package gốc com.Group13_65CNTT.AutoGarage; Java 21/Spring Boot 4.1.1.

## Envelope và thay đổi contract

```json
{"code":1000,"message":"Success","success":true,"data":{"id":1},"timestamp":"2026-10-15T00:00:00Z","path":"/api/v1/auth/me"}
```

Đợt viết lại theo yêu cầu chuyển từ code string/result sang code số/data và bổ sung success/timestamp/path. Logout/password/deactivate thành công chuyển 204 sang 200 có envelope. List tài khoản chuyển sang Page với data.content/data.totalElements. Client cần cập nhật các điểm này; chưa có frontend dùng contract cũ trong repository.

Mã: 1000 success, 1001 created, 1002 updated, 1003 deleted; 2001 validation, 2002 not found, 2003 duplicate, 2004 bad request, 2005 stale version, 2006 last admin, 2007 password policy, 2008 method, 2009 media, 2010 concurrent modification, 2011 response format, 2012 bootstrap account conflict; 3001 unauthorized, 3002 forbidden, 3003 invalid token, 3005 bad credentials; 4001 server, 4002 authentication infrastructure unavailable, 4003 data service unavailable, 4004 notification provider unavailable. HTTP status độc lập: 400/401/403/404/409/405/406/415/500/503. Hạ tầng auth không sẵn sàng dùng 503/4002 (trước đây 500); bug/FK/CHECK/NOT NULL không bị báo nhầm là trùng username/email.

## Endpoint

- POST `/api/v1/auth/login`: public. Body identifier (username/email), password. 200 data.accessToken/tokenType/expiresAt/user; unknown/wrong/inactive cùng 401/3005.
- GET `/api/v1/auth/me`: token hợp lệ, 200 UserResponse chính mình.
- POST `/api/v1/auth/logout`: token hợp lệ, 200; thu hồi mọi token của tài khoản.
- POST `/api/v1/auth/password`: body currentPassword/newPassword, 200/1002; kiểm tra password cũ, hash password mới và thu hồi token.
- POST `/api/v1/admin/users`: ADMIN, body username/email/fullName/role/password. 201/1001 và Location.
- GET `/api/v1/admin/users`: ADMIN, keyword/active/page/size/sortBy/sortDir. Mặc định page 0–100000, size 1–100; giới hạn theo PaginationProperties; sort allowlist id/username/fullName/role/active/createdAt, asc/desc.
- GET `/api/v1/admin/users/{id}`: ADMIN, 200 UserResponse hoặc 404.
- PUT `/api/v1/admin/users/{id}`: ADMIN, bắt buộc fullName/role/active/version; email null hoặc thiếu nghĩa xóa email. 200/1002.
- DELETE `/api/v1/admin/users/{id}?version=...`: ADMIN, 200/1003; ghi deleted_at, đặt inactive, thu hồi token và loại khỏi search/get/update. Giữ lịch sử/FK, không có restore API. Username/email vẫn được giữ, không tái sử dụng. Khóa/mở khóa dùng PUT cùng version với active=false/true, không ghi deleted_at.

UserResponse gồm id/username/email/fullName/role/active/version/createdAt; không Entity, password/hash/auth_version. Username 3–100 ASCII chữ/số/dot/underscore/hyphen, lowercase và bất biến. Email lowercase, blank thành null; fullName tối đa 150, email tối đa 254. Password mới theo AUTH_PASSWORD_MIN_LENGTH (mặc định ít nhất 12 Unicode code point), tối đa 72 byte UTF-8 của BCrypt. JSON field lạ bị 400. Password không writable qua PUT.

Năm role: ADMIN, RECEPTIONIST, WAREHOUSE_STAFF, TECHNICIAN, CUSTOMER. Tạo account không tự tạo customer/employee profile của TV2/TV3. Chỉ ADMIN quản trị tài khoản. Update yêu cầu version mới nhất; stale 409/2005, trùng username/email 409/2003, không hạ role/khóa admin active cuối cùng 409/2006.

## JWT và config

Authorization: Bearer token, stateless, không cookie/session/Basic/form login. Token chỉ nhận qua header, không query. HS256, Base64 secret decode ít nhất 32 byte ngẫu nhiên; issuer/audience/iat/nbf/exp/jti/role/auth_version. Validator kiểm tra required claim/type/time, signature/algorithm và account active/role/auth_version trong DB. DB token lookup không sẵn sàng trả safe 503/4002, token lỗi 401.

Role/active/password/logout thay đổi auth_version; login/profile thay đổi version nhưng giữ token nếu quyền không đổi. Login ghi last_login_at; GET/me sau login để lấy version mới khi PUT.

DB_URL/DB_USERNAME/DB_PASSWORD và AUTH_JWT_SECRET/ISSUER/AUDIENCE/TTL từ môi trường. TTL >0 và <=24h, clock skew 0–60s, BCrypt mặc định 12 (test 4); không fallback secret. CORS origin explicit, không wildcard hoặc cookie credentials.

Bootstrap mặc định tắt. Helper README tạo secret/admin password ngẫu nhiên; chỉ bootstrap admin đầu tiên, không reset/nâng quyền account có sẵn. Sau khởi tạo tắt bootstrap/xóa bootstrap password. Không commit .env. Swagger chỉ mở khi OPENAPI_ENABLED=true.

## I01

Bean ObjectAccessServiceImpl implement SecurityAccess; giữ requireRole/requireVehicleAccess/requireRepairOrderAccess. Thêm requireResourceAccess và requireQualityCheckAccess qua ObjectAccessService. Principal lấy từ JWT, actor ID khác principal bị 403. Đây là quyền truy cập đối tượng; mutation còn cần role/trạng thái của use case.

- ADMIN/RECEPTIONIST truy cập resource nghiệp vụ; notification vẫn chỉ recipient.
- CUSTOMER: xe theo chủ hiện tại; invoice/order/estimate/task/attachment/warranty theo customer lịch sử của giao dịch.
- TECHNICIAN: assignment/employee active cho order/task/attachment, xe theo phiếu đang mở có phân công; không có quyền invoice/finance.
- QC: TECHNICIAN, can_qc và assignment QC active trên đúng phiếu.
- WAREHOUSE_STAFF: quyền repair order phục vụ kho, không tự có quyền finance/ảnh/customer.
- Notification: recipient riêng kể cả ADMIN.

Routes mới phải khai báo matcher và quyền; default deny còn hiệu lực. AccessProbe chỉ ở source test, không vào JAR. TV2–TV4 cần tích hợp I01 vào từng API khi triển khai. Các phần audit/outbox/report xem TV1.md.

Chi tiết typed properties, exception boundary, xử lý CORS và kiểm chứng: [CONFIGURATION_AND_ERRORS.md](../CONFIGURATION_AND_ERRORS.md).

# Cấu hình và xử lý lỗi backend TV1

Áp dụng cho `auth`, `identity`, `audit`, `notification`, `reporting` và `common`. Cấu trúc theo source HRMS đã đối chiếu: DTO → Controller → Service/ServiceImpl → JpaRepository; MapStruct chuyển dữ liệu; `BusinessException`/`ResponseCode` và `GlobalExceptionHandler` tạo lỗi HTTP thống nhất.

## Quy tắc cấu hình

Secret, URL, issuer, audience, thời hạn token, chi phí BCrypt và origin CORS lấy từ môi trường. Secret không có giá trị dự phòng. Giá trị policy được bind vào `@ConfigurationProperties`, đăng ký tại configuration và kiểm tra khi khởi động.

- `AuthProperties`, `BootstrapProperties`: JWT, CORS (AUTH_CORS_MAX_AGE mặc định 1h, không âm) và khởi tạo admin chủ động.
- `PasswordProperties`: `AUTH_PASSWORD_MIN_LENGTH`, mặc định 12, cho phép 12–72. Service dùng cùng `PasswordPolicy` cho create user, đổi mật khẩu và bootstrap. Giới hạn 72 byte UTF-8 là ràng buộc của BCrypt; không cho tăng bằng env. Độ dài tối thiểu đếm Unicode code point.
- `PaginationProperties`: `PAGINATION_DEFAULT_SIZE=20`, `PAGINATION_MAX_SIZE=100`, `PAGINATION_MAX_PAGE=100000`. Default size không được vượt max size; hard ceiling cấu hình max size là 1000. Controller và service dùng chung `PaginationPolicy`; từ chối unpaged để tránh truy vấn không giới hạn. Allowlist trường sort vẫn thuộc contract API.
- `ReportProperties`: `REPORT_MAX_PERIOD=366d`; giới hạn phải dương. So sánh toàn bộ Duration, không cắt phần lẻ ngày.
- `NotificationProperties`: batch, số lần thử, lease, retry delay, múi giờ, reminder days, provider và timeout. `NOTIFICATION_CONNECT_TIMEOUT=5s`, `NOTIFICATION_REQUEST_TIMEOUT=10s`, `NOTIFICATION_POLL_DELAY=5s`, `NOTIFICATION_REMINDER_CRON=0 0 8 * * *`, `NOTIFICATION_REMINDER_MAX_BATCHES=100`, `NOTIFICATION_DEFAULT_SUBJECT=AutoGarage notification`. Timeout phải dương, connect ≤ request < lease; cron hợp lệ; subject không chứa CR/LF. SMS yêu cầu HTTPS và secret riêng, không credential/query/fragment trong URL.
- SMTP timeout riêng: `SMTP_CONNECT_TIMEOUT_MS=5000`, `SMTP_READ_TIMEOUT_MS=10000`, `SMTP_WRITE_TIMEOUT_MS=10000`, theo đơn vị thư viện mail.

Xem đầy đủ biến trong `.env.example`. `scripts/import-local-env.ps1` đã cho phép các biến mới. Mặc định an toàn đặt ở cấu hình/properties; không lặp giới hạn policy trong service. Route, role, mã lỗi, tên claim và giá trị schema ổn định thuộc constant/enum/validation contract, không đổi thành biến môi trường. Claim role/auth_version và token type dùng chung `SecurityConstants`.

## Luồng lỗi

1. Controller nhận DTO và `@Valid`, không tự bọc try/catch mỗi endpoint.
2. Service kiểm tra quyền/invariant và ném `BusinessException(ResponseCode)`. Khi chuyển một lỗi kỹ thuật đã biết, truyền thêm cause để giữ nguyên nguyên nhân.
3. `GlobalExceptionHandler` chọn HTTP status theo `ResponseCode`. Mọi lỗi API được xử lý tại đây có `code`, `message`, `success=false`, `data`, `timestamp`, `path`.
4. Lỗi xảy ra trong security filter đi qua entry point/access denied handler/`SecurityErrorWriter`. CORS bị từ chối cũng trả cùng JSON envelope. CorsFilter chỉ đăng ký trong SecurityFilterChain để tránh chạy hai lần.
5. Job không có HTTP request. Worker/scheduler ghi lỗi an toàn ở boundary của job; outbox lưu trạng thái retry/FAILED theo policy, không giả vờ gửi thành công.

## Phân loại

- DTO validation: HTTP 400 / code 2001, `data` là map lỗi field hoặc object. Không trả rejected value; không ép mọi ObjectError thành FieldError.
- JSON sai, thiếu/sai kiểu parameter, validation tham số: 400 / 2004. Không đưa thông báo parser hoặc giá trị đầu vào vào response.
- Bootstrap trùng tài khoản không phải active admin: BusinessException / code 2012; ApplicationRunner dừng khởi động, không tự nâng quyền/reset password.
- Không tìm thấy: 404 / 2002. Không đủ quyền: 403 / 3002. Sai thông tin đăng nhập: 401 / 3005; token thiếu/sai: 401 / 3001 hoặc 3003. Unknown account và password sai dùng cùng lỗi công khai.
- Trùng dữ liệu/idempotency conflict: 409 / 2003, message dùng chung được cho user và notification/replay. Race được DB UNIQUE bảo vệ. Chỉ nhận diện duplicate từ Spring DuplicateKeyException, MySQL vendor code 1062 hoặc SQL state 23505; không đọc/parse message SQL.
- Version user gửi đã cũ: 409 / 2005. Last admin: 409 / 2006. Optimistic/pessimistic lock hoặc query timeout: 409 / 2010. Không tự retry mutation vô điều kiện.
- Method sai: 405 / 2008; request content type không hỗ trợ: 415 / 2009; response format không hỗ trợ: 406 / 2011.
- DB/transaction không thể truy cập: 503 / 4003; hạ tầng xác thực không sẵn sàng: 503 / 4002. Code 4002 được giữ nhưng HTTP status đã sửa từ 500 sang 503.
- Provider thông báo không sẵn sàng: `NotificationDeliveryException`, 503 / 4004 nếu đi qua HTTP handler; trong worker thì retry/FAILED. Mail/IO/interruption giữ cause, interruption giữ interrupt flag. Lỗi ghi complete sau khi gửi là lỗi outbox, không chạy nhánh fail như lỗi gửi provider.
- FK/CHECK/NOT NULL không được tự đổi thành lỗi trùng username/email. Lỗi kỹ thuật không xác định, SQL sai hoặc bug chương trình: 500 / 4001; giữ rollback và thông báo an toàn.

`SafeFailureLog` của ứng dụng ghi loại exception, chuỗi cause và tối đa 12 stack frame mỗi cause; bỏ message exception để tránh log SQL, credential hoặc nội dung provider. Response không chứa stack trace, password/hash, token, SQL hay thông tin kết nối. Không nuốt exception hoặc đổi mọi RuntimeException thành BAD_REQUEST; JSON chỉ bắt JacksonException tại boundary parse input.

## Kiểm chứng

Các bài mới kiểm tra response thực qua MockMvc cho từng nhóm lỗi, object-level validation, input sai, format HTTP, nội dung nhạy cảm trong response/log, bind policy khác mặc định và cấu hình sai, provider cause/interruption, cùng việc không gọi fail khi DB complete lỗi. Bộ integration hiện có kiểm tra filter/CORS, RBAC, thu hồi token, audit rollback và cạnh tranh trên MySQL; xem kết quả thực tế trong `VALIDATION.md`.

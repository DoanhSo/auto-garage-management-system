# Backend TV1: audit, thông báo, reminder và reporting

Tất cả JSON dùng envelope trong SECURITY.md; route cần JWT. Module theo Entity/DTO/MapStruct/JpaRepository/Service/ServiceImpl/Controller, có Swagger annotations. Các worker mặc định tắt, bootstrap test tắt.

## Audit và I08

GET `/api/v1/admin/audit-events`: ADMIN, actorId/action/page/size; Page AuditEventResponse, không trả requestHash/replay result/credential. Mutation identity dùng @Audited và cùng commit; audit fail rollback mutation. Failed-login ghi event độc lập, không lưu identifier/password/JWT.

NotificationAuditServiceImpl implement shared.contracts.NotificationAudit. Các method I08 yêu cầu transaction hiện có (MANDATORY):

1. Caller xác định principal, action/resource và canonical payload; SHA-256 lowercase gồm actor/action/resource/payload.
2. `findCompletedRequest(key, hash)` trước mutation: khóa actor đến commit, null nếu chưa có; identical actor/hash trả result đã lưu, khác actor/hash 409.
3. Nếu chưa có: mutation, `enqueue(Notice)` và `recordCompletedRequest(...)` trong cùng transaction. Result phải là JSON response an toàn, không credential.
4. Commit toàn bộ; lỗi/rollback không lưu thành công hoặc gửi provider. Unique request_key bảo vệ race; key toàn cục, nên dùng UUID hoặc scope ổn định.

Giới hạn key 100 ký tự, hash 64 hex lowercase, replay result 64KiB. I08 hiện là contract nội bộ cho service nghiệp vụ; không mở API cho client đọc replay tùy ý. Không đặt raw password/JWT trong payload được audit hoặc resultJson.

## Notification API

- POST `/api/v1/notifications`: ADMIN; body userId/customerId (ít nhất một), eventKey, channel WEB/EMAIL/SMS, destination, subject tùy chọn, payloadJson dạng JSON object. 200/code1001, notification response.
- GET `/api/v1/notifications?unread=false&page=0&size=20`: inbox WEB của principal, Page.
- GET `/api/v1/notifications/{id}`: chỉ đúng recipient WEB, kể cả admin.
- POST `/api/v1/notifications/{id}/read`: chỉ recipient, readAt idempotent; 200/code1002.

Server tạo recipient_key từ hồ sơ, kiểm tra user/customer link và canonical JSON; (eventKey,channel,recipientKey) duy nhất. Gửi lặp cùng nội dung trả bản cũ; nội dung/destination/subject khác 409, không ghi đè. Không trả destination/last_error/provider metadata ở inbox.

WEB cần tài khoản; email destination hợp lệ; SMS E.164. Provider gửi ngoài sau commit. Worker claim bằng FOR UPDATE SKIP LOCKED, lease UUID riêng mỗi batch, deadline và attempt count; recovery lấy lease hết hạn, stale worker không cập nhật kết quả mới. Retry chờ cấu hình rồi FAILED khi hết hạn mức. Sự kiện delivery có thể được gửi hơn một lần khi crash giữa provider và commit (at-least-once).

## Cấu hình provider và lịch

NOTIFICATION_WORKER_ENABLED=true bật dispatch (poll mặc định 5 giây). Batch mặc định 20, lease 2m, retry 5m, max 3; biến nằm trong .env.example, typed NotificationProperties validate.

EMAIL: NOTIFICATION_EMAIL_ENABLED=true, NOTIFICATION_EMAIL_FROM và SMTP_HOST/PORT/USERNAME/PASSWORD/AUTH/STARTTLS. Spring Mail với connect/read/write timeout. Mặc định provider tắt và credential rỗng; cấu hình provider thật trước khi bật.

SMS: NOTIFICATION_SMS_ENABLED=true, SMS_GATEWAY_URL HTTPS và SMS_GATEWAY_TOKEN. Adapter dành cho gateway có contract:

```json
{"destination":"+84901234567","payload":{"message":"..."},"idempotencyKey":"notification:123"}
```

POST JSON, Authorization Bearer gateway token, Idempotency-Key header; 2xx thành công, lỗi retry. Gateway cần dedup theo key. Nhà cung cấp có API khác cần adapter tương ứng, không thể chỉ thay URL. Test dùng mock; chưa gửi email/SMS thật hoặc kiểm chứng credential của môi trường.

NOTIFICATION_REMINDERS_ENABLED=true bật scheduler 08:00 GARAGE_TIME_ZONE (mặc định Asia/Bangkok). Chọn bảo hành ACTIVE trước hạn ngày 15 ngày hoặc chạm km; bảo dưỡng theo lần bàn giao đã có invoice/service mới nhất và interval tháng/km. Event key theo warranty/invoice item/service, loại bản đã enqueue để không đói batch sau; scheduler tối đa 100 batch/lần. Không nhắc khách lịch sử khi xe đổi chủ. Đợt hiện tại enqueue WEB cho customer có account active; khách chưa có account cần policy kênh/provider được bổ sung theo vận hành.

## Reporting

Chỉ ADMIN:

- GET `/api/v1/reports/dashboard?from=<UTC ISO>&until=<UTC ISO>`: revenue, cashIn, cashOut, customerDebt, supplierDebt, stockValue, laborHours.
- GET `/api/v1/reports/outstanding-invoices?page=0&size=20`: Page invoice debt >0 từ view.
- GET `/api/v1/reports/dashboard/export?from=...&until=...`: file CSV UTF-8, Content-Disposition; response binary download riêng.

Khoảng [from,until), giới hạn theo REPORT_MAX_PERIOD (mặc định 366 ngày). Revenue/cash/labor thuộc khoảng; debt và stockValue là snapshot hiện tại ghi snapshotAt. Invoice VOID bị loại, OUT chỉ tính chứng từ IN POSTED; OPENING không tạo supplier debt. StockValue từ signed inventory ledger quantity_change*unit_cost, không lấy giá danh mục làm giá vốn. Labor tính phiên đã dừng và giao với khoảng báo cáo. Các aggregate độc lập tránh nhân tổng khi nhiều payment.

## Phạm vi bàn giao

Backend TV1 đã có auth/accounts/RBAC/I01/audit/I08/outbox/inbox/reminder WEB/report CSV theo cấu trúc yêu cầu. Frontend và demo xuyên module còn phụ thuộc TV2–TV4; refresh token/MFA/rate limiting chưa có. Kiểm thử và lệnh thực tế tại VALIDATION.md. Không dùng kết quả test backend để đánh dấu toàn bộ T01–T38 đã nghiệm thu.

Policy timeout/poll/cron/batch và phân loại lỗi dùng chung: [CONFIGURATION_AND_ERRORS.md](../CONFIGURATION_AND_ERRORS.md).

# TV1: triển khai lại theo kiến trúc người dùng

Yêu cầu ngày 15/10/2026: toàn bộ backend TV1 theo 7 tầng và package-by-feature trong CODING_GUIDELINES.md. Giữ Java 21/Spring Boot 4.1.1, schema gara và chữ ký shared contracts. Các đầu việc tổng thể vẫn gồm frontend/tích hợp nên checkbox trong WORK_BREAKDOWN chỉ xong khi đủ tiêu chí.

## Các mốc đã triển khai backend

1. common: BaseEntity, numeric ApiResponse/ResponseCode/BusinessException, route/security constants, JWT/typed config/error handlers, Swagger và worker config.
2. auth/identity: Entity User/JpaRepository/MapStruct/DTO riêng/ServiceImpl/Controller; login/me/logout/password, admin CRUD, search/filter/sort/Pageable, version/last-admin/revocation, I01 owner/assignment/QC.
3. audit: @Audited/AOP trong transaction, JPA audit/query API và I08 replay actor/hash/locking cùng transaction.
4. notification: Entity BaseEntity, DTO/mapper/JPA/service/controller; inbox private/read, dedup enqueue, worker lease/retry/recovery, WEB/SMTP/HTTPS SMS adapter có config.
5. reminder: lịch 08:00 theo zone, maintenance cycle mới nhất/warranty deadline, dedup/batch và bảo vệ chuyển chủ; hiện hỗ trợ WEB cho account active.
6. reporting: view/projection, dashboard/debt pagination và CSV ADMIN, aggregate dữ liệu nguồn và ledger.
7. migration V6/V7, unit Mockito, H2 integration, MySQL HTTP/schema/lock/race/rollback và build/JAR verification theo VALIDATION.md.

## Tiếp theo để nghiệm thu toàn TV1

- UI React login/admin/inbox/report và xử lý contract mới code số/data/Page/envelope.
- TV2–TV4 tích hợp I01 và I08 đúng transaction; T01–T38/G0–G5 cần các nghiệp vụ của nhóm được triển khai.
- Provision SMTP/SMS gateway của môi trường, kiểm thử delivery thực tế; quyết định kênh nhắc cho khách không có account.
- Worker/provider mặc định tắt; bật chủ động và vận hành theo chính sách retry/delivery at-least-once đã ghi trong API docs.

Đợt này thay controller/facade/port/JDBC cũ bằng cấu trúc layered yêu cầu. Kết quả test cũ 75 bài là lịch sử trước refactor; chỉ dùng kết quả vòng kiểm chứng mới trong VALIDATION.md để đánh giá source hiện tại.

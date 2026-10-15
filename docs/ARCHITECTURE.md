# Kiến trúc triển khai AutoGarage

Backend TV1 theo kiến trúc 7 tầng trong tài liệu người dùng: migration → Entity → DTO → MapStruct → Repository → Service → Controller. Java 21/Spring Boot 4.1.1, package com.Group13_65CNTT.AutoGarage. Maven/Git ở AutoGarage bên trong workspace.

```text
common/
  config/        typed properties, security, JWT, workers và OpenAPI
  constant/      ApiPaths, SecurityConstants
  dto/           ApiResponse
  entity/        BaseEntity
  enums/         Role, ResponseCode, AccessResource
  exception/     BusinessException, GlobalExceptionHandler
  security/      current user, JWT provider/validator, JSON filter error
  util/          PasswordPolicy
auth/
  controller/, dto/request/, dto/response/, service/impl/
identity/
  controller/, dto/request/, dto/response/, entity/, mapper/, repository/, service/impl/
audit/
  annotation/, aspect/, controller/, dto/response/, entity/, mapper/, repository/, service/impl/
notification/
  controller/, dto/request/, dto/response/, entity/, mapper/, repository/, service/impl/
reporting/
  controller/, dto/response/, entity/, mapper/, repository/, service/impl/
shared/contracts/  các chữ ký I01–I08 cho nhóm
```

Auth dùng User/UserRepository của identity. Entity User và Notification kế thừa BaseEntity; AuditEvent append-only có lifecycle riêng, InvoiceBalance là view @Immutable. Reporting tổng hợp qua projection, không tạo bảng thống kê. Các package module TV2–TV4 giữ skeleton để thành viên triển khai theo cùng chuẩn.

Controller validate DTO, gọi Service interface và tạo HTTP response; @Tag/@Operation và @PreAuthorize trên các method được bảo vệ. ServiceImpl dùng @Service/@Slf4j/@RequiredArgsConstructor và transaction; mapper MapStruct allowlist trường. JpaRepository dùng JPQL/Pageable cho search; native SQL phục vụ projection liên module, view và khóa SKIP LOCKED của MySQL.

Transaction advice order 0 bao ngoài AuditLogAspect order 10: audit fail rollback mutation. Failed-login audit là sự kiện độc lập REQUIRES_NEW vì login thất bại bị rollback. Không serialize request/credential vào audit; metadata/result được allowlist. User version dùng optimistic update, auth_version riêng để thu hồi JWT. Lock admin theo thứ tự và refresh entity sau khi chờ để tránh snapshot cũ.

I01 ObjectAccessServiceImpl dùng current principal và resource ownership/assignment projections. I08 NotificationAuditServiceImpl bắt buộc transaction hiện có, khóa actor trước replay/mutation, kiểm tra hash và actor; outbox enqueue cùng commit. Các service nghiệp vụ phải gọi các contract này trước khi mở rộng API.

Worker claim và commit lease trước provider I/O, complete/fail trong transaction riêng và kiểm tra lease owner/deadline. Retry có giới hạn; recovery xử lý lease hết hạn. Delivery là at-least-once; SMS gateway cần dedup Idempotency-Key. Reminder chọn chu kỳ bảo dưỡng mới nhất/khách hiện tại và event key ổn định, enqueue WEB cho tài khoản active.

H2 kiểm tra JPA/MapStruct/service/audit tích hợp; MySQL kiểm tra Flyway, native queries, generated columns, CHECK, khóa và race. Hibernate MySQL ddl-auto=validate, Flyway duy nhất thay schema. Tiền BigDecimal, thời gian UTC. Chi tiết chuẩn và checklist tại CODING_GUIDELINES.md.

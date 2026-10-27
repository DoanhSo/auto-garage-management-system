# Quy chuẩn kiến trúc và cách viết code AutoGarage

Theo tài liệu `CODEX_ARCHITECTURE_GUIDELINES.md` người dùng cung cấp; yêu cầu ngày 15/10/2026 là viết lại toàn bộ backend TV1 theo cấu trúc này. Bản này thay thế hướng dẫn trước từng cho phép domain/application/adapter, JDBC cho user CRUD và mapper thủ công.

## Kiến trúc 7 tầng

Luồng: Controller → Service/ServiceImpl → Repository → Entity/DB; MapStruct chuyển Request DTO/Entity/Response DTO. Flyway quản lý schema. Không đặt nghiệp vụ ở Controller/Mapper hoặc duy trì một implementation domain/application/adapter song song.

```text
com.Group13_65CNTT.AutoGarage/
  common/{config,constant,dto,entity,enums,exception,security,util}/
  auth/{controller,dto/request,dto/response,service/impl}/
  identity/{controller,dto/request,dto/response,entity,mapper,repository,service/impl}/
  audit/{annotation,aspect,controller,dto/response,entity,mapper,repository,service/impl}/
  notification/{controller,dto/request,dto/response,entity,mapper,repository,service/impl}/
  reporting/{controller,dto/response,entity,mapper,repository,service/impl}/
  shared/contracts/
```

Giữ package gara, Java 21/Spring Boot 4.1.1 và năm role gara. Auth dùng Entity User của identity; báo cáo dùng view/projection read-only. Không tạo Entity/table giả cho một response tổng hợp hoặc bản ghi auth chỉ để đủ thư mục.

## Migration và Entity

- Migration `V{n}__{snake_case_description}.sql`, bảng số nhiều, cột snake_case, BIGINT AUTO_INCREMENT, InnoDB/utf8mb4. Mọi FK có index; index mới dùng `idx_`, FK dùng `fk_`.
- Không sửa V1–V5 đã có; V6/V7 tiến hóa users/notifications để có audit metadata. Flyway là nguồn schema; Hibernate validate ở môi trường MySQL.
- Entity thông thường kế thừa `common/entity/BaseEntity`: id, createdAt/updatedAt, createdBy/updatedBy. Lifecycle UTC; cột phải có trong migration.
- Lombok @Getter/@Setter/@NoArgsConstructor/@AllArgsConstructor/@Builder; default builder dùng @Builder.Default. Không @Data hoặc toString tự động lộ credentials/quan hệ.
- Quan hệ LAZY; collection Set khi đúng ngữ nghĩa, tránh vòng equals/toString và N+1. @Version cho cập nhật cạnh tranh, generated column read-only.
- Audit append-only và view @Immutable có lifecycle riêng; không thêm setter/audit update giả lên view. ID snapshot actor không làm giả một quan hệ JPA chưa có trong schema.

## DTO và MapStruct

- Request create/update và response riêng từng file ở dto/request và dto/response; Jakarta Validation và @Valid, message cụ thể. Không trả Entity/password/hash/signing key.
- MapStruct @Mapper(componentModel="spring"), unmapped target ERROR; null strategy IGNORE theo mặc định mẫu. PUT xóa email được khai báo SET_TO_NULL rõ ràng; fullName/role/active/version bắt buộc. PATCH chưa có.
- Mapper create/update chỉ allowlist trường không nhạy cảm. Service gán role, active, encoded password và audit metadata sau kiểm tra quyền.
- Password theo PasswordProperties (mặc định tối thiểu 12 ký tự), tối đa 72 byte UTF-8 của BCrypt; PasswordEncoder chịu trách nhiệm băm, mapper không lưu raw password. Không lặp policy password trong DTO/service.

## Repository và Service

- Repository @Repository extends JpaRepository<Entity,Long>; find trả Optional, exists dùng derived query, search JPQL với keyword/filter/Pageable.
- Giá trị từ request phải bind bằng JPA derived query, @Query với @Param hoặc PreparedStatement/NamedParameterJdbcTemplate nếu có adapter JDBC; không nối chuỗi, format/interpolate dữ liệu người dùng vào SQL/JPQL. Tên bảng/cột, biểu thức ORDER BY và chiều sort không lấy trực tiếp từ request: ánh xạ/allowlist tại boundary, kiểm tra lại Pageable trong service khi nhận sort động. Không dùng JpaSort.unsafe với dữ liệu người dùng. Không thay parameter binding bằng xóa dấu nháy hoặc blacklist từ khóa SQL; chuỗi hợp lệ chứa dấu nháy vẫn được lưu/tìm như dữ liệu. CONCAT trong một truy vấn cố định với tham số bind chỉ tạo giá trị tìm kiếm, không xây dựng mã SQL.
- Native query dành cho projection liên module/view và chức năng đặc thù MySQL; không tự mutation dữ liệu module khác. Không query trong vòng lặp nếu có thể batch.
- Service interface và service/impl; @Service, @Slf4j, @RequiredArgsConstructor, dependency private final; không field injection.
- Read @Transactional(readOnly=true), mutation @Transactional; transaction boundary ở ServiceImpl. Khóa theo thứ tự thống nhất và re-read sau khi chờ; version/UNIQUE bảo vệ race.
- Ghi quan trọng @Audited với aspect đã triển khai, nằm trong transaction để audit fail rollback mutation. Failed-login event dùng transaction riêng vì bản thân login bị từ chối; không ghi thông tin credential.
- Cache nếu được dùng phải evict đúng khi mutation; hiện không cache quyền/tài khoản. Không thêm cache chỉ để có annotation.
- Sinh mã nghiệp vụ nếu feature có mã tùy chọn; constraint/locking phải bảo vệ concurrency, không coi count()+1 là đủ an toàn.

## Controller, errors và security

- @RestController, @RequestMapping(ApiPaths.*), @RequiredArgsConstructor, @Tag và @Operation.
- @PreAuthorize(SecurityConstants.*) trên mỗi protected method; login public. Service kiểm tra lại role/quyền đối tượng trước mutation, không tin actor ID từ client.
- JSON ApiResponse<T>: code số, message, success, data, timestamp UTC và path. Dải 1000 thành công, 2000 input/dữ liệu, 3000 security, 4000 server. HTTP status đúng ngữ nghĩa và độc lập code.
- BusinessException(ResponseCode) cho lỗi có dự kiến, GlobalExceptionHandler cho validation/JSON/conflict/unexpected. Entry point/access denied handler xử lý lỗi filter cùng envelope. Lỗi server không trả stack/SQL.
- JWT Bearer stateless, CORS origin explicit; secret và credential từ môi trường/secret store, typed configuration validate, không fallback secret. Đổi role/active/password/logout thu hồi token.
- Policy phân trang/kỳ báo cáo/notification dùng typed properties; không đặt magic number trong nghiệp vụ. Role/route/claim/mã lỗi/schema ổn định dùng constant/enum/contract validation.
- Không catch RuntimeException rồi đổi mọi lỗi thành BAD_REQUEST/conflict. Chuyển lỗi có kiểu và giữ cause; chỉ UNIQUE đã nhận diện mới trả duplicate, lỗi DB không sẵn sàng trả 503, lỗi bất ngờ 500. Log ứng dụng dùng SafeFailureLog để loại message có dữ liệu nhạy cảm. Quy tắc và biến cấu hình chi tiết: CONFIGURATION_AND_ERRORS.md.
- Last-admin, historical ownership khi xe đổi chủ, technician/QC assignment và private notification recipient đều là invariant bắt buộc.
- Phân trang có page/size/sortBy/sortDir và allowlist sort; không bind tên cột SQL tùy ý. Export file là response binary riêng có Content-Type/filename và quyền như báo cáo.

## Thiết kế giao diện

Tuân thủ [UI_DESIGN_GUIDELINES.md](UI_DESIGN_GUIDELINES.md) cho toàn bộ frontend.
Dùng chung design token và component, phong cách minimalism thống nhất giữa các
màn hình. Form thêm/sửa là dialog ở giữa, desktop chia hai cột bằng nhau; xác nhận
xóa dùng dialog cùng hệ thống. Không tạo drawer CRUD cố định hai bên hoặc form
dài có thanh cuộn nội bộ mặc định. Khi thay đổi component chung, kiểm tra các màn
đang sử dụng và khả năng thao tác trên mobile, khi zoom và bằng bàn phím.

## Kiểm thử và checklist bàn giao

1. Migration mới nếu schema đổi; không sửa checksum cũ.
2. ApiPaths và SecurityConstants trước endpoint.
3. Entity/BaseEntity khớp DB và lifecycle.
4. DTO create/update/response riêng, validate và null semantics.
5. MapStruct mapper allowlist, kiểm tra generated code qua compiler.
6. JpaRepository, JPQL search/Pageable và index/lock phù hợp.
7. Service/ServiceImpl, @Transactional, @Audited; outbox/idempotency chung transaction khi nghiệp vụ cần.
8. Controller/Swagger/ApiResponse/RBAC; unit Mockito @InjectMocks/AssertJ và integration H2. Chạy thêm MySQL riêng cho native SQL, Flyway, CHECK/generated columns, lock/race/rollback.

Không dùng DB nghiệp vụ/credential thật để test, không sửa target, skip test hoặc nâng version để né lỗi. Bàn giao file/hành vi/API/schema thay đổi, lệnh/kết quả đã chạy và giới hạn thực tế. Windows `.\mvnw.cmd test`; Linux/macOS `./mvnw test`. Package bỏ test chỉ kiểm tra đóng gói.

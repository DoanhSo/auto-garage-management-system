# Quy tắc Codex cho AutoGarage

Đọc `docs/CODING_GUIDELINES.md` trước khi sửa code. Đây là bản áp dụng tài liệu
`CODEX_ARCHITECTURE_GUIDELINES.md` do người dùng cung cấp cho AutoGarage.

- Maven tại thư mục chứa file này: Java 21, Spring Boot 4.1.1, package `com.Group13_65CNTT.AutoGarage`.
- Bắt buộc cấu trúc 7 tầng: migration → Entity → DTO → MapStruct → JpaRepository → Service/ServiceImpl → Controller.
- Thành phần chung trong `common/{config,constant,dto,entity,enums,exception,security,util}`.
- Module theo feature: `controller`, `dto/request`, `dto/response`, `entity`, `mapper`, `repository`, `service/impl`.
- Entity thông thường kế thừa BaseEntity, quan hệ LAZY, không @Data; không sửa migrations đã chia sẻ.
- DTO riêng từng file; Jakarta Validation có message; MapStruct componentModel spring, mutation mapping allowlist.
- Service/Controller dùng @RequiredArgsConstructor, dependency private final; nghiệp vụ và transaction ở ServiceImpl.
- Ghi quan trọng dùng @Audited thật, audit và mutation chung commit; cache eviction nếu module có cache.
- Controller dùng ApiPaths, SecurityConstants, @PreAuthorize từng method và Swagger @Tag/@Operation.
- JSON ApiResponse gồm code số, message, success, data, timestamp, path. BusinessException dùng ResponseCode.
- Truy vấn list bằng JPQL/Pageable; batch tránh N+1; authorization theo principal và quyền đối tượng.
- Unit Mockito/@InjectMocks/AssertJ và integration H2; bổ sung MySQL cho Flyway/native SQL/locking/race/rollback.
- Secret từ môi trường, cấu hình validate; không commit credential hoặc sửa generated trong target.
- Chạy Maven wrapper test/package từ đúng module; bàn giao kết quả thực tế, không suy diễn test pass từ compile.

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

## Không hard code và xử lý exception

- Không hard code credential/secret, URL theo môi trường, policy, timeout, giới hạn phân trang hoặc giá trị cấu hình trong code. Dùng typed properties có validation; secret lấy từ môi trường/secret store, không fallback secret. Role, route, claim, mã lỗi và giá trị hợp đồng ổn định dùng constant/enum dùng chung, không lặp magic number/string trong nghiệp vụ.
- Lỗi nghiệp vụ phải throw `BusinessException(ResponseCode)` với mã lỗi đúng ngữ nghĩa; thông báo bổ sung nếu cần vẫn phải đi kèm `ResponseCode`. Không chỉ throw message như `new RuntimeException("...")`, `new Exception("...")`, `new IllegalArgumentException("...")` hoặc throw chuỗi để báo lỗi nghiệp vụ.
- Khi chuyển đổi exception từ dependency, chỉ catch loại lỗi cần xử lý và giữ nguyên cause, ví dụ `new BusinessException(responseCode, cause)`. Không nuốt exception, catch rỗng hoặc đổi mọi lỗi thành `BAD_REQUEST`/conflict.
- Exception phải được xử lý tập trung qua `GlobalExceptionHandler`; lỗi security tại filter dùng entry point/access denied handler cùng chuẩn. Tất cả lỗi API phải trả HTTP status đúng ngữ nghĩa và `ApiResponse` gồm `code` số, `message`, `success=false`, `data`, `timestamp` UTC, `path`; không trả riêng chuỗi message hoặc JSON lỗi tự phát.
- Không trả stack trace, SQL, token hay credential cho client. Log lỗi dùng `SafeFailureLog`; khi sửa luồng exception, kiểm chứng cả HTTP status và envelope lỗi, không chỉ nội dung message.

## Chuẩn thiết kế giao diện

- Đọc `docs/UI_DESIGN_GUIDELINES.md` trước khi sửa UI. Dùng chung phong cách minimalism và design token; không để mỗi màn hình có một cách thiết kế hoặc một bộ component khác nhau.
- Tái sử dụng component trong `frontend/src/shared/components`, CSS/token dùng chung trong `frontend/src/styles.css`. Khi cần biến thể, mở rộng component chung; không sao chép form, button, field, table hoặc dialog rồi chỉnh riêng theo màn.
- Form thêm/sửa dùng `Modal` hoặc `Sheet placement="center"`; xác nhận xóa dùng `ConfirmDialog`/`ConfirmModal`. Dialog nằm giữa cả hai chiều, không dùng drawer hay form cố định sát cạnh trái/phải.
- Desktop chia field thành hai cột bằng nhau, căn thẳng hàng; mobile một cột. Trường nội dung dài, lỗi chung và hàng nút chiếm trọn hàng. Cùng một đối tượng dùng cùng thứ tự/nhóm trường ở form thêm và sửa.
- Form thông thường phải vừa khung nhìn và thấy được hàng nút, không mặc định cuộn nội bộ/cuộn ngang. Form dài chia thành bước hoặc nhóm hợp lý; không dùng chiều cao cố định, cắt nội dung hay thu nhỏ chữ để ép vừa. Khi zoom/mở bàn phím, cho phép cuộn tối thiểu cần thiết để mọi trường và nút vẫn truy cập được; không cuộn lồng nhiều lớp.
- Giữ thống nhất label, validation, loading, lỗi API, xác nhận xóa và vị trí nút Hủy/Lưu. Khi sửa layout dùng chung, kiểm tra các màn bị ảnh hưởng trên desktop/mobile, bàn phím và lỗi validation; không coi việc thêm quy chuẩn là đã sửa xong UI hiện tại.

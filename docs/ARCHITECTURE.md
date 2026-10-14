# Cấu trúc triển khai

Đây là một ứng dụng Spring Boot cho một gara, một tồn kho; không tách thành microservices. Giữ package gốc và Spring Boot 4.1.1/Java 21 của dự án đang có. Báo cáo ghi Spring Boot 3.5; giữ 4.1.1 là quyết định khởi tạo để không thay nền tảng hiện tại.

```text
AutoGarage/
  pom.xml, mvnw, mvnw.cmd
  compose.yaml, .env.example
  docs/
    WORK_BREAKDOWN.md
    SERVICE_CONTRACTS.md
    DATABASE.md
    ACCEPTANCE_TESTS.md
    api/, erd/
  frontend/
    src/app/
    src/features/<14 module>/
    src/shared/{api,components,hooks,types,utils}/
    public/
  scripts/
  src/main/java/com/Group13_65CNTT/AutoGarage/
    AutoGarageApplication.java
    config/
    shared/{contracts,exception,validation,util}/
    <14 module>/{controller,dto,entity,repository,service,mapper}/
  src/main/resources/
    application.yaml
    db/migration/V1__...sql đến V4__...sql
  src/test/java/com/Group13_65CNTT/AutoGarage/
    AutoGarageApplicationTests.java
    MigrationSchemaTests.java
    <14 module>/
  src/test/resources/application-test.yaml
```

Mỗi package module có `package-info.java` ghi module/chủ trì. `.gitkeep` giữ thư mục chưa có implementation khi đưa vào Git. Các controller, entity, repository/service nghiệp vụ chưa được triển khai. Tám interface có chữ ký để thống nhất giao tiếp; chúng chưa phải service bean chạy được. Frontend hiện chỉ là khung thư mục và README.

## Luồng phụ thuộc

Controller nhận DTO và principal → service kiểm tra quyền và nghiệp vụ → repository/entity truy cập dữ liệu → mapper trả DTO. Transaction đặt tại service, không ở controller. Module dùng contract của module khác, không gọi controller hoặc tự cập nhật bảng do người khác sở hữu. Repository chỉ viết trong module sở hữu dữ liệu; M10 quality điều phối service checklist của M05 reception và workflow M08 repair.

JPA dùng `ddl-auto=validate`; Flyway là nơi duy nhất thay đổi schema. Thêm entity với `@Version` cho các bảng có `version`; các cột generated chỉ map read-only (`insertable=false, updatable=false`). Với DATETIME(6), chọn một quy ước ánh xạ UTC thống nhất; không lưu giờ địa phương dạng không xác định timezone. Tiền sử dụng BigDecimal và rounding đã quy định trong hợp đồng.

Thông báo lưu PENDING trong cùng transaction; worker gửi sau commit. Audit/idempotency lưu cùng nghiệp vụ. Snapshot hóa đơn và ledger chỉ được ghi theo service chuyên trách. Tất cả FK hiện dùng RESTRICT mặc định; danh mục ngừng hoạt động qua is_active=false.

## Nguyên tắc bàn giao giữa thành viên

TV1 tích hợp schema và quyền dùng chung. TV2 cung cấp danh mục, tiếp nhận, checklist/ảnh. TV3 cung cấp lịch khoang, phiếu, báo giá, task/timer, QC và bảo hành. TV4 cung cấp kho, hóa đơn và thu chi. Mỗi service phải có kiểm thử tích hợp trên MySQL thật cho nghiệp vụ cạnh tranh; H2 không thay thế MySQL ở các bài khóa hàng, CHECK hoặc generated column.

# AutoGarage

Khung hệ thống quản lý gara một chi nhánh. Backend giữ Spring Boot 4.1.1 và Java 21 từ dự án ban đầu; MySQL 8.4 dùng Flyway V1–V4. Frontend đã chia thư mục cho React/TypeScript, chưa khởi tạo ứng dụng chạy được. Chức năng nghiệp vụ đang ở mức phân công và hợp đồng interface.

## Tài liệu làm việc

- [Phân công 42 đầu việc cho TV1–TV4](docs/WORK_BREAKDOWN.md): module, đầu ra, phụ thuộc và tiêu chí nghiệm thu.
- [Cấu trúc dự án](docs/ARCHITECTURE.md): 14 module, phân lớp và quyền sở hữu dữ liệu.
- [Tám hợp đồng service I01–I08](docs/SERVICE_CONTRACTS.md): chữ ký, transaction và quy tắc tích hợp.
- [Thiết kế database và quyết định bổ sung](docs/DATABASE.md).
- [Từ điển cột thực tế](docs/DATA_DICTIONARY.md).
- [Kế hoạch T01–T38](docs/ACCEPTANCE_TESTS.md): các kịch bản nghiệm thu nghiệp vụ theo báo cáo.

## Chạy backend trên Windows PowerShell

Thực hiện trong thư mục chứa `pom.xml` (thư mục `AutoGarage` bên trong workspace). Cần JDK 21 và Docker đang chạy.

```powershell
docker compose up -d --wait mysql
.\mvnw.cmd spring-boot:run
```

MySQL local dùng cổng 3307, database `autogarage`, user `autogarage`, password `autogarage_local`. Backend chạy cổng 8080; Flyway tự áp dụng migrations khi khởi động. Spring Security hiện vẫn dùng cấu hình mặc định của starter; API đăng nhập/role nghiệp vụ chưa được triển khai.

Có thể dùng `mvn` nếu đã cài Maven. Nếu đổi mật khẩu/cổng Compose, phải đồng thời đặt biến môi trường cho Spring Boot:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3307/autogarage?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true'
$env:DB_USERNAME='autogarage'
$env:DB_PASSWORD='mat_khau_local_cua_ban'
.\mvnw.cmd spring-boot:run
```

`.env.example` là ví dụ cho Compose; file `.env` không tự được Spring Boot đọc. Các thông tin mặc định chỉ dùng local. Database cần được tạo trước; migrations không tạo user/database.

## Kiểm thử schema trên database riêng

```powershell
docker compose --profile test up -d --wait mysql-test
.\mvnw.cmd test
docker compose stop mysql-test
```

Profile test dùng cổng 3308 và database `autogarage_test`. Có thể đổi bằng `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`. Tests ghi fixtures và rollback sau từng bài; Flyway vẫn khởi tạo schema test. Không dùng database nghiệp vụ để chạy test.

`AutoGarageApplicationTests` kiểm tra context. `MigrationSchemaTests` kiểm tra 30 bảng, hai view, bốn phiên bản, FK/CHECK/UNIQUE, timer, thợ chính, tồn kho, nguồn thanh toán, chống post trùng, thông báo và công nợ nhiều lần thu/chi. Hiện có 17 bài tự động; kế hoạch T01–T38 là công việc cho giai đoạn triển khai, chưa được chạy như test nghiệp vụ.

## Quy ước migrations

V1 tạo 12 bảng nền tảng/danh mục; V2 tạo 9 bảng điều phối/sửa chữa; V3 tạo 9 bảng kho/tài chính/bảo hành; V4 hoàn thiện liên kết vòng và hai view. Có đúng 30 bảng nghiệp vụ, thêm `flyway_schema_history` do Flyway tự tạo. Không có seed tài khoản/password trong migrations.

Sau khi áp dụng chung, không sửa V1–V4; thay đổi mới dùng V5 trở lên. Không dùng Hibernate để tạo schema, không bật baseline tự động, không chạy Flyway clean trên dữ liệu đã có.

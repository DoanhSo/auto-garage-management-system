# Kết quả kiểm tra nền tảng ngày 14/10/2026

- JDK 21.0.10, Spring Boot 4.1.1, Maven Wrapper 3.10.0.
- MySQL 8.4.11 chạy trong container riêng, không sử dụng database nghiệp vụ có sẵn.
- `mvnw.cmd -B -ntp test`: BUILD SUCCESS; 17 tests, 0 failures, 0 errors, 0 skipped.
- V1–V4 áp dụng thành công trên database sạch; kiểm tra `flyway.validate()` đạt, không có migration pending.
- 30 bảng nghiệp vụ, 423 cột, 76 FK, 75 CHECK, hai view. Flyway tự tạo thêm bảng kỹ thuật `flyway_schema_history`.
- `docker compose config --quiet`: cấu hình hợp lệ.

Các kiểm thử tự động xác nhận nguồn dữ liệu/ràng buộc, tính toán view với nhiều khoản thanh toán, timer/MAIN duy nhất, đúng quan hệ QC, khóa ngoại và bằng chứng claim. Chúng chưa kiểm thử các service/controller/frontend chưa triển khai, quyền theo đối tượng, khóa hàng nhiều giao dịch hoặc toàn bộ T01–T38. Các mục đó có người chủ trì và tiêu chí trong WORK_BREAKDOWN/ACCEPTANCE_TESTS.

Container kiểm tra tạm được xóa sau khi hoàn tất. Docker Compose và database test riêng đã được cấu hình để nhóm chạy lại theo README.

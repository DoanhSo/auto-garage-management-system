# Sửa thao tác và giao diện thông báo — 22/10/2026

## Nguyên nhân và thay đổi

- Proxy Vite `/api` trước đây đổi Host sang upstream (`changeOrigin: true`) nhưng giữ Origin của trình duyệt. Request POST/DELETE cùng nguồn ở frontend trở thành cross-origin tại Spring, có thể bị CORS trả 403/3002 khi origin đó không nằm trong allowlist. Giữ nguyên Host/Origin cho proxy API, không xóa Origin hoặc mở wildcard CORS. Proxy Keycloak giữ cấu hình riêng.
- Ở bảng quản trị, thiếu `userId` từng được coi là thông báo của tài khoản hiện tại. Chỉ cho phép đánh dấu thông báo WEB từ inbox riêng hoặc có `userId` khớp người đăng nhập; thông báo của người khác/không xác định người nhận được xem từ dữ liệu bảng quản trị, không gọi API đọc riêng hoặc tự đánh dấu.
- Thu hồi lỗi giữ hộp xác nhận và lỗi để thử lại; chỉ đóng khi thành công. Không tạo bản ghi thông báo giả để thu hồi khi selection biến mất.
- Giao diện giảm còn tab hộp thư/chưa đọc/toàn hệ thống, tiêu đề kèm nội dung tóm tắt, trạng thái chữ và thao tác gọn. Form phát dùng React Hook Form/Zod, required/độ dài/tiêu đề một dòng, lỗi tại từng trường, khóa tương tác khi đang gửi. Nội dung thông báo luôn render như text.
- Tách BroadcastModal, NoticeDetails và hàm đọc payload khỏi page. Hỗ trợ payload `message` và `text`; giữ fallback an toàn cho JSON sai. Query inbox không chạy khi đang mở danh sách quản trị.

## Kiểm chứng

- Regression trước sửa: test proxy ghi nhận 3 request POST read/DELETE/POST broadcast trả 403 thay vì 200; UI test ghi nhận nút đánh dấu sai quyền khi `userId` null.
- Sau sửa: 4/4 bài proxy pass, gồm origin lạ vẫn bị từ chối.
- Vitest `run --maxWorkers=1 --testTimeout=20000`: 58/58 bài pass. Chạy tuần tự với timeout rộng hơn do máy đang chạy Java/MySQL/Keycloak; vòng chạy song song trước đó có hai test form tài khoản cũ hết timeout 5 giây.
- Maven `-Dtest=TV1IntegrationTests test`: 21/21 bài MySQL pass, không skip. Test mới đi qua Spring Security với Host/Origin của proxy; admin phát và thu hồi được, recipient đọc được, admin không đọc thay recipient, customer không phát/thu hồi được.
- TypeScript, ESLint toàn frontend và Vite production build pass.
- Chromium đăng nhập Keycloak thật qua frontend 127.0.0.1:5173: phát tới một admin test, đánh dấu đã đọc và thu hồi đều trả 200; bản ghi đã biến mất khỏi inbox sau thu hồi. Mobile 390×844 không tràn ngang toàn trang; ảnh desktop/form/mobile được kiểm tra trực quan.
- Form phát nằm trong viewport ở desktop 1366×768 và mobile 390×844; form desktop không có scroll nội bộ.

## Môi trường

Backend thử nghiệm 55818, MySQL riêng `autogarage-notification-test` cổng 55918, bootstrap chỉ tài khoản test; worker/reminder tắt. Các thao tác kiểm tra không dùng DB nghiệp vụ cổng 3307. Frontend chạy thử cổng 5173 proxy tới backend test. Log, snapshot, ảnh và dữ liệu đăng nhập nằm ngoài Git; không lưu token vào source.

Ảnh và log cục bộ: `frontend/output/playwright/notifications-*.png`, `frontend/.cache/notification-*.log`, `target/notification-permissions.log`. Không thay schema, migration hoặc nới quyền backend trong bản sửa này.

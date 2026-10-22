# Docker Desktop: lỗi socket runtime trên Windows

Ngày 15/10/2026 máy gặp lỗi khởi động ở `Docker/run/dockerInference`, sau đó `docker-secrets-engine/engine.sock`: `The file cannot be accessed by the system`. Kiểm tra cho thấy các file là reparse point 0 byte. Triệu chứng và cách đổi tên thư mục cha được người dùng báo cáo trong [Docker desktop-feedback #460](https://github.com/docker/desktop-feedback/issues/460).

Cách đã thực hiện và kiểm chứng trên máy:

1. Kiểm tra engine chưa chạy, process Docker bị kẹt và nội dung hai thư mục runtime. `docker-secrets-engine` chỉ có file engine.sock 0 byte.
2. Dừng các process Docker Desktop/backend của lần khởi động lỗi.
3. Kiểm tra đường dẫn tuyệt đối, thư mục cha là directory thường; đổi tên `LocalAppData/Docker/run` và `LocalAppData/docker-secrets-engine` thành bản `.stale.<timestamp>` trong cùng thư mục cha.
4. Tạo lại hai directory runtime rỗng và khởi động Docker Desktop.
5. `docker info` trả engine 29.4.0; container MySQL test khởi động và mysqladmin ping thành công.

Các bản runtime cũ được giữ làm dự phòng; data VHDX/volumes/settings được giữ nguyên. Đây là xử lý lỗi runtime hiện tại, không phải factory reset hay thay đổi schema dự án. Nếu gặp lại, kiểm tra đúng endpoint đang lỗi và nội dung thư mục trước khi xử lý; thao tác trên data directory cần quy trình riêng.

# Khung frontend React và TypeScript

Thư mục này hiện là khung phân chia công việc; chưa cài dependencies, chưa có ứng dụng React chạy được. TV1 khởi tạo runtime/bundler/router tại TV1-02 khi triển khai frontend. Tổ chức module tương ứng backend để TV1–TV4 phát triển màn hình theo phân công.

- `src/app`: entrypoint, router, layout và provider.
- `src/features/<module>`: màn hình và thành phần nghiệp vụ; README mỗi feature ghi người chủ trì.
- `src/shared/api`: HTTP client, xử lý xác thực và lỗi API thống nhất.
- `src/shared/components`: thành phần UI dùng chung.
- `src/shared/hooks`, `types`, `utils`: mã dùng chung không phụ thuộc nghiệp vụ riêng.
- `public`: tài nguyên tĩnh công khai; ảnh bằng chứng nghiệp vụ lấy qua API có kiểm tra quyền.

Đối chiếu `../docs/WORK_BREAKDOWN.md` và `../docs/SERVICE_CONTRACTS.md`. Quyền hiển thị menu giúp UX; backend vẫn phải kiểm tra quyền ở mọi request. API DTO không được trả entity JPA, password_hash hoặc storage_key trực tiếp.

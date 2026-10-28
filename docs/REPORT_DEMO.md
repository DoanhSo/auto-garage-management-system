# Dữ liệu mẫu màn báo cáo

Chỉ chạy thủ công trên Docker local dùng để demo, từ thư mục chứa `compose.yaml`:

```powershell
.\scripts\seed-report-demo.ps1 -ConfigPath .env
```

Script sao lưu database vào `.docker-local/` trước khi thêm dữ liệu. Không commit
file backup hoặc cấu hình môi trường. Đây là fixture SQL riêng cho demo báo cáo,
không phải Flyway migration và không thay thế kiểm thử luồng nghiệp vụ qua API.

Các bản ghi mang mã `DEMO-RPT-*`: 3 khách hàng, 3 xe, 1 dịch vụ, 24 hồ sơ sửa chữa
với báo giá/dòng dịch vụ/hóa đơn, 24 phiên công, 2 nhà cung cấp, 3 phụ tùng và
3 chứng từ nhập kho. Có 12 phiếu thu, 3 phiếu chi; 3 hóa đơn thanh toán đủ,
9 hóa đơn thanh toán một phần và 12 hóa đơn chưa thanh toán.

Số liệu của riêng bộ mẫu khi chọn kỳ bao gồm sáu ngày gần nhất:

- Doanh thu: 49.500.000 VND; tiền thu: 16.500.000 VND; tiền chi: 4.000.000 VND.
- Giờ công: 36; phải thu khách hàng: 33.000.000 VND, trên 21 hóa đơn.
- Phải trả nhà cung cấp: 4.500.000 VND; giá trị tồn kho: 8.500.000 VND.

Ngày giao dịch được tạo tương đối với ngày UTC khi chạy lần đầu. Chạy lại giữ
nguyên bản ghi và ngày cũ, không xóa/ghi đè dữ liệu hiện có. Nếu database đã có
giao dịch khác, tổng báo cáo sẽ bao gồm cả các giao dịch đó. Số dư hiện tại và
danh sách hóa đơn còn phải thu không phụ thuộc bộ lọc kỳ báo cáo.

Nhân viên lịch sử `demo.report.technician` bị vô hiệu hóa, không có password hay
Keycloak identity và không thể đăng nhập. Script không tạo credential. Chỉ dùng
các địa chỉ email `example.test`; không gửi email/SMS hay thực hiện thanh toán.

## Kiểm chứng ngày 28/10/2026

- Chạy fixture hai lần trên MySQL 8.4 riêng với schema hiện tại: số bản ghi và
  tổng tiền/giờ công không đổi; sau đó thêm fixture vào Docker local.
- Màn `/admin/reports` hiển thị đúng các tổng ở trên và 21 hóa đơn còn phải thu,
  chia hai trang khi chọn 20 dòng/trang.
- Chọn Hôm nay và Áp dụng: doanh thu 9.500.000 VND, thu 6.500.000 VND,
  chi 4.000.000 VND, 6 giờ công. CSV tải qua nút Xuất CSV khớp các số liệu này.
- Ngày bắt đầu sau ngày kết thúc: có thông báo lỗi và khóa nút Áp dụng.
- Kiểm tra desktop và viewport mobile 390 × 844: không tràn ngang; mobile
  chia thẻ chỉ số theo hai cột, số dư theo một cột, bảng ẩn hai cột phụ.
- Refresh giữ đăng nhập và trang phân trang hiện tại. Bộ lọc ngày trở lại kỳ
  mặc định sau refresh vì đang lưu trong state của trang.

Ảnh kiểm tra cục bộ nằm tại `frontend/output/playwright/report-audit/` (bị Git
bỏ qua). Chưa kiểm tra đầy đủ screen reader, tương phản theo WCAG hoặc các
trạng thái mất kết nối/lỗi máy chủ; không coi đây là chứng nhận accessibility.

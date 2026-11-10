# 1. Mô tả bài toán

> Thuộc plan `system-analysis-erd`. Nguồn ghi ở cuối mỗi mục: **[BC]** = báo cáo gốc (`docs/source/bao-cao-goc.md`, 14/10/2026), **[Code]** = mã nguồn và migration hiện tại (V19).

## 1.1. Bối cảnh

Gara ô tô **một chi nhánh** tiếp nhận cả xe đặt lịch trước lẫn xe đến trực tiếp. Mỗi lần sửa, một chiếc xe đi qua nhiều bước:
- Kiểm tra hiện trạng và chẩn đoán.
- Báo giá, khách duyệt.
- Nhiều công việc sửa chữa và thay phụ tùng.
- Kiểm định chất lượng (QC).
- Thu tiền, bàn giao, rồi bảo hành.

Cùng lúc có nhiều xe dùng chung khoang và nhiều kỹ thuật viên phối hợp. Dữ liệu khách hàng, chi phí, vật tư và thời gian phải nối với nhau theo từng lần sửa. [BC §1.1]

**Vấn đề khi ghi giấy hoặc bảng tính rời** [BC §1.1]:
- Xếp trùng khoang, trùng thợ.
- Giữ và xuất phụ tùng lệch với tồn kho thực tế.
- Thu tiền từng phần khó đối soát công nợ.
- Thiếu căn cứ khi khách yêu cầu bảo hành.

## 1.2. Mục tiêu

| # | Mục tiêu | Nguồn |
|---|---|---|
| M1 | Một nguồn dữ liệu gốc cho khách, xe và từng lần sửa | [BC §1.2] |
| M2 | Lễ tân điều phối khoang và thợ theo ca, tay nghề; không xếp trùng khi thao tác đồng thời | [BC §1.2] |
| M3 | Khách duyệt từng hạng mục trước khi làm, kể cả báo giá phát sinh | [BC §1.2] |
| M4 | Kho chính xác: tồn thực, lượng giữ, xuất, trả, giá vốn bình quân; mọi biến động có chứng từ | [BC §1.2] |
| M5 | Bấm giờ công theo phiên, tự loại thời gian tạm dừng; lưu kết quả QC và biên bản bàn giao | [BC §1.2] |
| M6 | Hóa đơn chụp snapshot bất biến; thu chi nhiều lần; kiểm soát công nợ khách và nhà cung cấp | [BC §1.2] |
| M7 | Bảo hành theo từng linh kiện; nhắc bảo dưỡng; báo cáo quản trị | [BC §1.2] |
| M8 | Lương theo ca, giờ công và hoa hồng theo phiếu | [Code V12–V17] — bổ sung so với báo cáo |

## 1.3. Phạm vi và giả định

- **Một chi nhánh, một tồn kho tổng**, nhiều khoang và nhiều nhân viên. [BC §1.3]
- **Thiết kế gốc có 30 bảng nghiệp vụ** (Flyway V1–V4). **Hệ thống hiện tại có 34 bảng** (V1–V19). Bốn bảng thêm mới là `payroll_periods`, `payroll_items`, `order_commissions`, `repair_order_assignments`. [BC §1.3], [Code]
- **Đăng nhập và cấp phiên** chuyển sang Keycloak (OIDC). Bảng `users` giữ hồ sơ và vai trò, cột `identity_id` nối sang Keycloak. [Code V9]
- **Không thuộc phạm vi:** nhiều chi nhánh, hóa đơn điện tử, cổng thanh toán trực tuyến.

## 1.4. Tác nhân

### Tác nhân trong hệ thống

Gồm đúng 5 vai trò, khớp với enum `Role`. [BC §1.4], [Code `common/enums/Role.java`]

| Mã | Tác nhân | Trách nhiệm chính |
|---|---|---|
| `ADMIN` | Quản lý, quản trị viên | Tài khoản, phân quyền, cấu hình; duyệt ngoại lệ; chi tiền nhà cung cấp; quyết định bảo hành; xem mọi báo cáo; điều chỉnh hoặc hủy sổ bảo hành, hủy hóa đơn |
| `RECEPTIONIST` | Lễ tân (cố vấn dịch vụ) | Đón khách, tiếp nhận hiện trạng, đặt và điều phối lịch, xếp khoang, phân công thợ, lập báo giá, lập hóa đơn, thu tiền, bàn giao |
| `WAREHOUSE_STAFF` | Quản lý kho | Phụ tùng, nhà cung cấp, nhập mua, giữ hàng, xuất cho sửa chữa, nhập trả, điều chỉnh tồn |
| `TECHNICIAN` | Kỹ thuật viên | Chẩn đoán, bấm giờ (bắt đầu, tạm dừng, tiếp tục), hoàn thành việc; làm QC nếu `can_qc = true`; đăng ký ca |
| `CUSTOMER` | Khách hàng | Quản lý xe, đặt lịch, duyệt từng hạng mục báo giá, theo dõi tiến độ, xem hóa đơn, gửi yêu cầu bảo hành |

### Tác nhân ngoài hệ thống

Các tác nhân này xuất hiện trong sơ đồ luồng dữ liệu (DFD).
- **Nhà cung cấp:** nhận tiền thanh toán, gửi hóa đơn mua hàng.
- **Keycloak:** dịch vụ định danh.
- **Kênh EMAIL/SMS:** gửi thông báo cho khách không có tài khoản.
- **Tác vụ nền:** nhắc bảo dưỡng, gửi thông báo theo hàng đợi.

## 1.5. Yêu cầu chức năng theo phân hệ

| Phân hệ | Use case | Tóm tắt |
|---|---|---|
| Tài khoản, danh mục | UC01–UC06 | Đăng nhập/đăng xuất, tài khoản và vai trò, khách hàng, xe, dịch vụ và gói |
| Lịch hẹn, tiếp nhận | UC07–UC12 | Đặt, dời, hủy lịch; tiếp nhận và hiện trạng; điều phối khoang; phân công; chẩn đoán |
| Báo giá, thi công | UC13–UC19 | Lập và gửi báo giá, khách duyệt từng dòng, báo giá phát sinh, bấm giờ, hoàn thành việc |
| Kho | UC20–UC26 | Nhà cung cấp, phụ tùng, nhập mua và đầu kỳ, giữ hàng, xuất, trả, điều chỉnh tồn |
| QC, tài chính, bàn giao | UC27–UC31 | QC và làm lại, hóa đơn, thu tiền khách, chi nhà cung cấp, bàn giao và đóng phiếu |
| Hậu mãi | UC32–UC35, UC38 | Yêu cầu và quyết định bảo hành, thông báo, nhắc bảo dưỡng, tra cứu lịch sử xe |
| Quản trị | UC36–UC37 | Dashboard và báo cáo, ca làm và tài nguyên khoang |

Chi tiết use case, tác nhân và quy tắc nghiệp vụ nằm ở [02-nghiep-vu.md](02-nghiep-vu.md).

## 1.6. Yêu cầu phi chức năng

| Mã | Yêu cầu | Cách đáp ứng | Nguồn |
|---|---|---|---|
| NF1 | Bảo mật theo đối tượng | Kiểm tra vai trò và quyền trên từng khách, xe, phiếu, ảnh ở backend (I01), không dựa vào ẩn menu | [BC BR02], [Code `identity/service/ObjectAccessService`] |
| NF2 | Toàn vẹn tiền tệ | `DECIMAL(18,2)`, Java `BigDecimal`, làm tròn HALF_UP từng dòng | [Code `docs/DATABASE.md`] |
| NF3 | Đồng thời an toàn | Khóa hàng (`FOR UPDATE`) trước khi kiểm tra trùng khoang, tồn kho, timer; cột sinh và UNIQUE cho "một thợ chính", "một timer" | [BC BR06, BR10], [Code V2] |
| NF4 | Chống lệnh lặp | `request_key` + `request_hash` ghi trong `audit_events` | [BC BR23] |
| NF5 | Thông báo tin cậy | Ghi PENDING cùng giao dịch nghiệp vụ; tác vụ nền gửi sau commit, có lease và retry | [BC BR23] |
| NF6 | Kiểm toán | Thao tác nhạy cảm gắn `@Audited`; không ghi mật khẩu hay token | [Code `audit/`] |
| NF7 | Bảo toàn lịch sử | Không xóa vật lý; dùng `is_active = false`; khóa ngoại RESTRICT; hóa đơn và sổ kho bất biến | [BC BR15, BR18, BR24] |
| NF8 | Thời gian thống nhất | Lưu UTC (`DATETIME(6)`), hiển thị theo múi giờ người dùng | [Code `docs/DATABASE.md`] |

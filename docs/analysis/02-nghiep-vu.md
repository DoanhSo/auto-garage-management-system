# 2. Phân tích nghiệp vụ

> Thuộc plan `system-analysis-erd`.
> **Nguồn:**
> - Quy tắc và use case lấy từ báo cáo gốc (`docs/source/bao-cao-goc.md` §2.1, §5).
> - Cột "Nơi thực thi" là vị trí trong code hiện tại. Đường dẫn Java tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`; migration tính từ `src/main/resources/db/migration/`.

## 2.1. Quy tắc nghiệp vụ BR01–BR24

**Tầng thực thi:**
- **DB**: ràng buộc CSDL (khóa ngoại, UNIQUE, CHECK, cột sinh). Tự bảo đảm, không phụ thuộc code.
- **SV**: kiểm tra trong service.
- **SV+DB**: cả hai tầng.

| Mã | Quy tắc | Nội dung kiểm soát | Tầng | Nơi thực thi |
|---|---|---|---|---|
| BR01 | Năm vai trò chuẩn | Chỉ ADMIN, RECEPTIONIST, WAREHOUSE_STAFF, TECHNICIAN, CUSTOMER; chi tiền nhà cung cấp thuộc ADMIN | SV+DB | `common/enums/Role.java:3`; CHECK `users.role` (V1); `@PreAuthorize` ở controller |
| BR02 | Quyền theo đối tượng | Kiểm tra vai trò và quyền trên từng khách, xe, phiếu, ảnh, thông báo ở backend | SV | `common/security/CustomerScope.java:57`; `identity/service/ObjectAccessService` (I01) |
| BR03 | Khách không bắt buộc tài khoản | `customers.user_id` NULL được; một user gắn tối đa 1 khách và 1 nhân viên | DB | UNIQUE `customers.user_id`, `employees.user_id` (V1) |
| BR04 | Chủ xe hiện tại và khách lịch sử | `vehicles.customer_id` là chủ hiện tại; phiếu và hóa đơn giữ khách lúc giao dịch | SV+DB | `repair_orders.customer_id`, `invoices.customer_snapshot`; ẩn tài chính chủ cũ ở `vehicle/dto/VehicleHistoryEntryDto.java:26` |
| BR05 | Dịch vụ và gói | Lịch hẹn chọn một dịch vụ chính; gói PACKAGE có chu kỳ km/tháng | SV+DB | `services.service_type`, `interval_km/interval_months`; **V11 cho phép lịch hẹn không chọn dịch vụ** (xem phát hiện F2) |
| BR06 | Khoang không trùng | Khoảng `[starts_at, ends_at)`; khóa hàng khoang rồi mới kiểm tra giao nhau | SV | `scheduling/repository/BayReservationRepository.java:38`; ca: `scheduling/service/impl/SchedulingServiceImpl.java:198,222` |
| BR07 | Giải phóng nguyên tử | Dời, hủy lịch hoặc bàn giao thì giải phóng khoang và lượng giữ trong cùng giao dịch | SV | `reception/service/BookingServiceImpl.java:300,338,367`; `repair/service/impl/RepairServiceImpl.java` (`handOver`, hủy phiếu) |
| BR08 | Phiếu từ tiếp nhận | Tạo phiếu khi xe vào xưởng; một lịch hẹn sinh tối đa một phiếu; chưa duyệt thì chưa sửa | SV+DB | UNIQUE `repair_orders.booking_id`; `RepairServiceImpl.java:173`; `repair/service/RepairStageRules.java` |
| BR09 | Phân công đủ điều kiện | Đúng kỹ năng, ca và lịch; QC cần `can_qc`; mỗi việc tối đa một thợ chính đang hoạt động | SV+DB | Cột sinh `active_main_task_id` + UNIQUE (V2:172); `quality/service/impl/QualityServiceImpl.java:88` |
| BR10 | Một timer mỗi thợ | Mỗi kỹ thuật viên mở tối đa một phiên bấm giờ | DB | Cột sinh `active_employee_id` + UNIQUE (V2:192) |
| BR11 | Giờ công trừ nghỉ | Tạm dừng chốt phiên; tiếp tục mở phiên mới; giờ công là tổng các phiên | SV | `repair/service/impl/LaborExecutionServiceImpl.java:265,278` |
| BR12 | Phiên bản báo giá | `version_no` tăng trong phiếu; báo giá phát sinh trỏ `parent_estimate_id` về bản gốc | SV+DB | UNIQUE `(repair_order_id, version_no)`; FK composite parent cùng phiếu (V2); `EstimateServiceImpl.createEstimate` |
| BR13 | Duyệt theo từng dòng | Chỉ dòng APPROVED mới sinh công việc hoặc giữ kho | SV+DB | UNIQUE `repair_tasks.estimate_item_id`; `RepairServiceImpl.java:636`; `EstimateServiceImpl.decide` |
| BR14 | Tồn thực và lượng giữ | Khả dụng = tồn − giữ; tổng giữ ở các dòng báo giá khớp `parts.reserved_quantity` dưới khóa | SV | `inventory/service/impl/InventoryServiceImpl.java:467` (`reserve`) |
| BR15 | Sổ kho ghi một lần | DRAFT không đổi tồn; POSTED sinh đúng một dòng sổ cho mỗi dòng chứng từ | SV+DB | UNIQUE `inventory_transactions.document_item_id` (V3); `InventoryDocumentController` ghi sổ |
| BR16 | Checklist và bằng chứng | Checklist JSON có `schema_version`; tệp lưu storage, CSDL giữ metadata | SV | `reception/service/VehicleCheckServiceImpl`; `reception/service/AttachmentServiceImpl` (kiểm MIME, kích thước, sha256, giai đoạn) |
| BR17 | QC đạt trước hóa đơn | QC mới nhất PASSED, việc xong, không còn timer mới được lập hóa đơn và bàn giao | SV+DB | FK composite `latest_qc_check_id` (V4); `RepairServiceImpl.java:351`; `BillingServiceImpl.issueInvoice` |
| BR18 | Snapshot giá | Hóa đơn chụp tên, đơn vị, giá bán, giá vốn lúc phát hành | SV | `billing/service/impl/BillingServiceImpl.java:166` |
| BR19 | Thu tiền và hạn mức nợ | Thu IN gắn hóa đơn, không vượt dư nợ; nợ trong `credit_limit` | SV+DB | CHECK nguồn thanh toán (V3); `BillingServiceImpl.java:622` |
| BR20 | Chi tiền nhà cung cấp | Chi OUT chỉ cho chứng từ IN đã POSTED có nhà cung cấp; không chi vượt nợ | SV | `BillingServiceImpl.java:555` |
| BR21 | Bảo hành từ hóa đơn | Sổ bảo hành chụp chính sách từ dòng hóa đơn khi bàn giao; hết hạn khi vượt ngày hoặc km | SV+DB | UNIQUE `warranties.invoice_item_id`; `RepairServiceImpl.java:433` → `WarrantyOperations.createFromInvoice` |
| BR22 | Bảo hành là phiếu mới | Yêu cầu được duyệt tạo phiếu sửa chữa mới, không ghi đè phiếu cũ | SV+DB | UNIQUE `warranty_claims.handling_order_id`; `RepairServiceImpl.java:267` |
| BR23 | Chống lệnh trùng, hàng đợi | `request_key` + `request_hash`; thông báo PENDING cùng giao dịch | SV+DB | UNIQUE `audit_events.request_key`; `audit/service/impl/NotificationAuditServiceImpl.java:57,111` |
| BR24 | Không xóa vật lý | Danh mục có lịch sử chỉ ngừng `is_active = false`; có thể mở lại | SV+DB | `catalog/service/CatalogServiceImpl.java:111`, `customer/service/CustomerServiceImpl.java:136`; mọi FK RESTRICT |

**Quy tắc bổ sung khi hiện thực.** Các quy tắc này không có trong báo cáo, mô tả chi tiết ở [`docs/REPAIR_ORDER_LIFECYCLE.md`](../REPAIR_ORDER_LIFECYCLE.md):
- **Chốt dữ liệu theo giai đoạn** (`RepairStageRules`).
- **Mở lại chẩn đoán** khi khách từ chối toàn bộ báo giá.
- **Hủy hóa đơn rồi lập lại** (V19).
- **Hủy sổ hoặc điều chỉnh sổ bảo hành** (V18).

## 2.2. Use case UC01–UC38

**Ký hiệu các cột:**
- **P**: tiến trình DFD chứa use case, xem [03-luong-du-lieu.md](03-luong-du-lieu.md).
- **A**: sơ đồ hoạt động chứa use case, xem [04-luong-hoat-dong.md](04-luong-hoat-dong.md). "—" là use case đơn giản (CRUD danh mục), không vẽ.

| UC | Tên | Tác nhân chính | Tác nhân phụ | P | A |
|---|---|---|---|---|---|
| UC01 | Đăng nhập | Cả 5 vai trò | — | P1 | — |
| UC02 | Đăng xuất | Cả 5 vai trò | — | P1 | — |
| UC03 | Quản lý tài khoản và vai trò | ADMIN | — | P1 | — |
| UC04 | Quản lý hồ sơ khách hàng | RECEPTIONIST, ADMIN | CUSTOMER | P1 | — |
| UC05 | Quản lý phương tiện | RECEPTIONIST, ADMIN | CUSTOMER | P1 | — |
| UC06 | Quản lý dịch vụ và gói | ADMIN | — | P1 | — |
| UC07 | Đặt lịch hẹn | CUSTOMER | RECEPTIONIST | P2 | A1 |
| UC08 | Xác nhận, dời hoặc hủy lịch | RECEPTIONIST, ADMIN | CUSTOMER | P2 | A1 |
| UC09 | Tiếp nhận xe và hiện trạng | RECEPTIONIST, ADMIN | CUSTOMER | P3 | A2 |
| UC10 | Điều phối xe đến khoang | RECEPTIONIST, ADMIN | TECHNICIAN | P2 | A2 |
| UC11 | Phân công kỹ thuật viên | RECEPTIONIST, ADMIN | TECHNICIAN | P5 | A2 |
| UC12 | Ghi chẩn đoán kỹ thuật | TECHNICIAN | RECEPTIONIST, ADMIN | P3 | A2 |
| UC13 | Lập và gửi báo giá | RECEPTIONIST, ADMIN | CUSTOMER | P4 | A3 |
| UC14 | Khách duyệt từng hạng mục | CUSTOMER | RECEPTIONIST | P4 | A3 |
| UC15 | Lập báo giá phát sinh | RECEPTIONIST, ADMIN | TECHNICIAN, CUSTOMER | P4 | A3 |
| UC16 | Bắt đầu công việc và timer | TECHNICIAN | — | P5 | A2 |
| UC17 | Tạm dừng công việc | TECHNICIAN | — | P5 | A2 |
| UC18 | Tiếp tục công việc | TECHNICIAN | — | P5 | A2 |
| UC19 | Hoàn thành công việc | TECHNICIAN | ADMIN | P5 | A2 |
| UC20 | Quản lý nhà cung cấp | WAREHOUSE_STAFF, ADMIN | — | P1 | — |
| UC21 | Quản lý phụ tùng vật tư | WAREHOUSE_STAFF, ADMIN | — | P1 | — |
| UC22 | Nhập kho mua hàng hoặc đầu kỳ | WAREHOUSE_STAFF | ADMIN | P6 | A4 |
| UC23 | Giữ phụ tùng sau khi khách duyệt | Hệ thống | RECEPTIONIST, WAREHOUSE_STAFF | P6 | A3, A4 |
| UC24 | Xuất phụ tùng cho sửa chữa | WAREHOUSE_STAFF | TECHNICIAN | P6 | A4 |
| UC25 | Trả phụ tùng chưa sử dụng | WAREHOUSE_STAFF | TECHNICIAN | P6 | A4 |
| UC26 | Điều chỉnh tồn kho | WAREHOUSE_STAFF | ADMIN | P6 | A4 |
| UC27 | Kiểm tra chất lượng và làm lại | TECHNICIAN (can_qc), ADMIN | TECHNICIAN | P5 | A2 |
| UC28 | Lập và phát hành hóa đơn | RECEPTIONIST, ADMIN | CUSTOMER | P7 | A5 |
| UC29 | Ghi nhận thanh toán khách | RECEPTIONIST, ADMIN | CUSTOMER | P7 | A5 |
| UC30 | Thanh toán nhà cung cấp | ADMIN | Nhà cung cấp | P7 | A5 |
| UC31 | Bàn giao xe và đóng phiếu | RECEPTIONIST, ADMIN | CUSTOMER | P8 | A2, A5 |
| UC32 | Tiếp nhận yêu cầu bảo hành | CUSTOMER | RECEPTIONIST | P8 | A6 |
| UC33 | Quyết định và xử lý bảo hành | ADMIN | TECHNICIAN | P8 | A6 |
| UC34 | Chăm sóc khách, theo dõi thông báo | RECEPTIONIST, ADMIN | CUSTOMER | P9 | A8 |
| UC35 | Nhắc bảo dưỡng và hạn bảo hành | Tác vụ nền | RECEPTIONIST | P9 | A8 |
| UC36 | Dashboard và báo cáo | ADMIN | RECEPTIONIST, WAREHOUSE_STAFF | P10 | — |
| UC37 | Ca làm và tài nguyên khoang | ADMIN | RECEPTIONIST | P2 | A7 |
| UC38 | Tra cứu lịch sử xe | CUSTOMER | RECEPTIONIST, ADMIN | P9 | — |

## 2.3. Ma trận use case × vai trò

**Ký hiệu:** ● tác nhân chính · ○ tác nhân phụ.
- Cột "Hệ thống" gồm tác vụ nền và các bước hệ thống tự kích hoạt.
- Lương (V12–V17) không có use case trong báo cáo; xem luồng A7.

| UC | ADMIN | RECEPTIONIST | WAREHOUSE_STAFF | TECHNICIAN | CUSTOMER | Hệ thống |
|---|:-:|:-:|:-:|:-:|:-:|:-:|
| UC01–02 | ● | ● | ● | ● | ● | |
| UC03 | ● | | | | | |
| UC04–05 | ● | ● | | | ○ | |
| UC06 | ● | | | | | |
| UC07 | | ○ | | | ● | |
| UC08 | ● | ● | | | ○ | |
| UC09 | ● | ● | | | ○ | |
| UC10–11 | ● | ● | | ○ | | |
| UC12 | ○ | ○ | | ● | | |
| UC13 | ● | ● | | | ○ | |
| UC14 | | ○ | | | ● | |
| UC15 | ● | ● | | ○ | ○ | |
| UC16–18 | | | | ● | | |
| UC19 | ○ | | | ● | | |
| UC20–21 | ● | | ● | | | |
| UC22, UC26 | ○ | | ● | | | |
| UC23 | | ○ | ○ | | | ● |
| UC24–25 | | | ● | ○ | | |
| UC27 | ● | | | ● | | |
| UC28–29, UC31 | ● | ● | | | ○ | |
| UC30 | ● | | | | | |
| UC32 | | ○ | | | ● | |
| UC33 | ● | | | ○ | | |
| UC34 | ● | ● | | | ○ | |
| UC35 | | ○ | | | | ● |
| UC36 | ● | ○ | ○ | | | |
| UC37 | ● | ○ | | | | |
| UC38 | ○ | ○ | | | ● | |

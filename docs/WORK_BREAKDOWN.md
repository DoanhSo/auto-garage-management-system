# Phân công triển khai AutoGarage

Bản phân công ngày 14/10/2026 dựa trên báo cáo được cung cấp. Dùng TV1–TV4 vì báo cáo chưa có tên thành viên. Tổng cộng 42 đầu việc: TV1 10, TV2 10, TV3 11, TV4 11. Đây là phân rã mới để triển khai; báo cáo chỉ nêu tổng số, chưa liệt kê 42 việc cụ thể.

## Quy tắc làm việc

- Mỗi đầu việc có một người chủ trì chịu trách nhiệm cả backend, màn hình frontend tương ứng, kiểm thử và tài liệu API. Người phối hợp cung cấp service contract, không tự sửa nghiệp vụ của người chủ trì.
- Dùng mã bên dưới khi tạo issue/branch/PR. TV1-01 đã hoàn thành nền tảng G0; backend common/auth/identity/audit/notification/reporting đã có theo TV1_IMPLEMENTATION_PLAN.md. Checkbox đầu việc bao gồm cả frontend/tích hợp nên chỉ đánh dấu khi đủ toàn bộ tiêu chí.
- Trình tự trong từng nhóm là thứ tự ưu tiên; phụ thuộc ghi rõ là điều kiện để tích hợp hoàn chỉnh, không cấm chuẩn bị UI hoặc test trước.
- TV1 điều phối phiên bản migration. Sau khi migration được chia sẻ/chạy chung, mọi sửa đổi schema phải dùng V5 trở lên; không sửa checksum V1–V4.
- M01–M14 được đặt tên cụ thể trong lần khởi tạo này, theo phạm vi phân hệ của báo cáo. Không tự mở rộng thành nhiều chi nhánh hoặc thêm bảng role, skill, history.

## Sở hữu module và dữ liệu

- **M01 `identity` — TV1**: Đăng nhập và phân quyền. Bảng: `users`, `audit_events`.
- **M02 `customer` — TV2**: Khách hàng. Bảng: `customers`.
- **M03 `vehicle` — TV2**: Phương tiện. Bảng: `vehicles`.
- **M04 `catalog` — TV2**: Dịch vụ và gói bảo dưỡng. Bảng: `services`.
- **M05 `reception` — TV2**: Lịch hẹn tiếp nhận và tệp. Bảng: `bookings`, `vehicle_checks`, `attachments`.
- **M06 `scheduling` — TV3**: Nhân viên ca làm và khoang. Bảng: `employees`, `employee_shifts`, `service_bays`, `bay_reservations`.
- **M07 `estimate` — TV3**: Báo giá và duyệt từng dòng. Bảng: `estimates`, `estimate_items`.
- **M08 `repair` — TV3**: Phiếu sửa chữa công việc và timer. Bảng: `repair_orders`, `repair_tasks`, `task_assignments`, `labor_sessions`.
- **M09 `inventory` — TV4**: Phụ tùng và kho. Bảng: `suppliers`, `parts`, `repair_order_parts`, `inventory_documents`, `inventory_document_items`, `inventory_transactions`.
- **M10 `quality` — TV3**: Điều phối kiểm định chất lượng. Không sở hữu bảng riêng; dùng hợp đồng của module dữ liệu.
- **M11 `billing` — TV4**: Hóa đơn và thanh toán. Bảng: `invoices`, `invoice_items`, `payments`.
- **M12 `warranty` — TV3**: Bảo hành. Bảng: `warranties`, `warranty_claims`.
- **M13 `notification` — TV1**: Thông báo và nhắc bảo dưỡng. Bảng: `notifications`.
- **M14 `reporting` — TV1**: Dashboard và báo cáo. Không sở hữu bảng riêng; dùng hợp đồng của module dữ liệu.

M10 do TV3 điều phối QC, còn entity/repository `vehicle_checks` và `attachments` do TV2 quản lý trong M05. `repair_orders` do TV3 sở hữu; TV2 tiếp nhận qua I03. M14 đọc dữ liệu, không tạo bảng thống kê dư thừa.

## TV1

### TV1-01 Khởi tạo nền tảng và cấu hình

- [x] Hoàn thành nền tảng; 17 kiểm thử tự động đạt trên MySQL 8.4.
- **Module/mốc:** M01 / G0.
- **Đầu ra:** pom, cấu hình DB/Flyway, cấu trúc module, hướng dẫn chạy.
- **Phụ thuộc:** Không.
- **Nghiệm thu:** Build thành công; V1–V4 chạy trên MySQL sạch.

### TV1-02 Đăng nhập và đăng xuất

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** login/me/logout/password JWT/BCrypt, token revocation và test; chưa có UI login.
- **Module/mốc:** M01 / G1.
- **Đầu ra:** API login/logout, băm BCrypt, phiên hoặc JWT, UI đăng nhập.
- **Phụ thuộc:** TV1-01.
- **Nghiệm thu:** UC01–02 thành công; tài khoản khóa và token đã logout bị từ chối.

### TV1-03 Tài khoản và năm vai trò

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** ADMIN create/list/get/PUT/soft deactivate, năm role, last-admin/version/concurrency; chưa có UI admin.
- **Module/mốc:** M01 / G1.
- **Đầu ra:** CRUD tài khoản, đổi role, khóa tài khoản, giao diện admin.
- **Phụ thuộc:** TV1-02.
- **Nghiệm thu:** UC03; đúng năm role; không trả password_hash.

### TV1-04 Bảo mật theo đối tượng I01

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** SecurityAccess/ObjectAccessServiceImpl ownership/assignment/QC và test A/B; cần tích hợp vào API/file của TV2–TV4 khi các module được triển khai.
- **Module/mốc:** M01 / G1.
- **Đầu ra:** SecurityAccess và kiểm tra ownership ở mọi API/file.
- **Phụ thuộc:** TV1-03, TV2-01.
- **Nghiệm thu:** Khách A bị 403 khi đọc xe, phiếu, hóa đơn, ảnh của khách B.

### TV1-05 Lỗi API và kiểm tra đầu vào

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** API advice và security filter handlers, strict DTO/JSON/status; chưa có UI hiển thị lỗi và chưa nghiệm thu các API nghiệp vụ mới.
- **Module/mốc:** M01 / G1.
- **Đầu ra:** Định dạng lỗi thống nhất, mã 400/403/404/409, validation.
- **Phụ thuộc:** TV1-01.
- **Nghiệm thu:** UI hiển thị lỗi trường; xung đột tài nguyên trả 409.

### TV1-06 Audit và chống lệnh lặp I08

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** audit AOP/JPA/query API và I08 replay theo actor/hash, unique/locking và rollback cùng nghiệp vụ; chờ tích hợp TV2–TV4.
- **Module/mốc:** M01 / G2.
- **Đầu ra:** Nhật ký actor, request_key/hash, kết quả replay.
- **Phụ thuộc:** TV1-04.
- **Nghiệm thu:** Cùng key/hash trả kết quả cũ; khác hash trả 409; rollback không lưu thành công.

### TV1-07 Outbox thông báo I08

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** Inbox WEB/private/read, dedup enqueue, worker lease/retry/recovery; SMTP/HTTPS gateway có config, chờ provider thật và UI.
- **Module/mốc:** M13 / G2.
- **Đầu ra:** Ghi PENDING cùng transaction, worker lease/retry, UI thông báo.
- **Phụ thuộc:** TV1-06.
- **Nghiệm thu:** UC34; rollback không gửi; hết lease có thể thử lại; cùng event/recipient/channel không trùng.

### TV1-08 Nhắc bảo dưỡng và hết hạn

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** Scheduler 08:00 zone gara, maintenance cycle/warranty deadline và dedup/chuyển chủ; hiện WEB cho account active, chờ UI và policy khách offline.
- **Module/mốc:** M13 / G5.
- **Đầu ra:** Worker 08:00 giờ gara; chu kỳ km/tháng; nhắc trước 15 ngày.
- **Phụ thuộc:** TV1-07, TV2-04, TV3-10.
- **Nghiệm thu:** UC35; chạy lại cùng chu kỳ không tạo trùng; khách chỉ đọc thông báo của mình.

### TV1-09 Dashboard và xuất báo cáo

- [ ] Hoàn thành và được nghiệm thu.
- **Đã có backend:** ADMIN dashboard/debt Page/CSV, aggregation dữ liệu nguồn/ledger; chờ UI, dữ liệu nghiệp vụ TV2–TV4 và nghiệm thu tổng thể.
- **Module/mốc:** M14 / G5.
- **Đầu ra:** UC36; doanh thu, thu chi, công nợ, tồn kho, giờ công; UI và export.
- **Phụ thuộc:** TV4-08, TV3-08.
- **Nghiệm thu:** Đối soát view với giao dịch; loại VOID; quyền báo cáo đúng role.

### TV1-10 Tích hợp và phát hành bản demo

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M14 / G5.
- **Đầu ra:** Checklist G0–G5, chạy T01–T38, hướng dẫn demo, tổng hợp kết quả.
- **Phụ thuộc:** TV2-10, TV3-11, TV4-11, TV1-09.
- **Nghiệm thu:** Các tiêu chí của G5 đạt; không còn lỗi chặn quy trình xuyên suốt.


## TV2

### TV2-01 Hồ sơ khách hàng

- [x] Hoàn thành backend (Entity, Repository, Service, Controller, Unit Tests UC04).
- **Module/mốc:** M02 / G1.
- **Đầu ra:** Entity/repository/service/controller, UI CRUD khách.
- **Phụ thuộc:** TV1-01.
- **Nghiệm thu:** UC04; khách trực tiếp không cần user; hạn mức/điều khoản nợ không âm.

### TV2-02 Hồ sơ phương tiện

- [x] Hoàn thành backend (Entity, Repository, Service, Controller, Unit Tests UC05).
- **Module/mốc:** M03 / G1.
- **Đầu ra:** CRUD xe, chuẩn hóa biển số, VIN, ODO; UI xe.
- **Phụ thuộc:** TV2-01.
- **Nghiệm thu:** UC05; biển số/VIN không trùng; ODO không âm.

### TV2-03 Đổi chủ và lịch sử giao dịch

- [x] Hoàn thành backend (Chuyển nhượng phương tiện, bảo toàn dữ liệu lịch sử).
- **Module/mốc:** M03 / G1.
- **Đầu ra:** Luồng đổi vehicles.customer_id; lọc dữ liệu lịch sử.
- **Phụ thuộc:** TV2-02, TV1-04.
- **Nghiệm thu:** Đổi chủ không đổi customer_id trên phiếu/hóa đơn; chủ mới không xem hóa đơn chủ cũ.

### TV2-04 Danh mục dịch vụ và gói

- [x] Hoàn thành backend (Entity, Repository, Service, Controller, Unit Tests UC06).
- **Module/mốc:** M04 / G1.
- **Đầu ra:** CRUD SINGLE/PACKAGE, giá, kỹ năng, loại khoang, chu kỳ, bảo hành.
- **Phụ thuộc:** TV1-01.
- **Nghiệm thu:** UC06; giá không âm; thay danh mục không thay snapshot cũ.

### TV2-05 Đặt lịch hẹn

- [x] Hoàn thành backend (Entity, Repository, Service, Controller, Unit Tests UC07, tích hợp hợp đồng I02 giữ khoang).
- **Module/mốc:** M05 / G2.
- **Đầu ra:** API/UI lịch hẹn; một dịch vụ chính; gọi I02 để giữ khoang.
- **Phụ thuộc:** TV2-02, TV2-04, TV3-02.
- **Nghiệm thu:** UC07; hai khách tranh một khoang chỉ một lịch thành công.

### TV2-06 Xác nhận dời hủy lịch

- [x] Hoàn thành backend (State transition PENDING/CONFIRMED/CANCELLED/NO_SHOW, dời lịch, hủy lịch, giải phóng khoang I02, Unit Tests UC08).
- **Module/mốc:** M05 / G2.
- **Đầu ra:** State transition, optimistic version, gọi I02 giải phóng khoang.
- **Phụ thuộc:** TV2-05, TV3-02.
- **Nghiệm thu:** UC08; dời/hủy nguyên tử; tiếp giáp [start,end) được phép.

### TV2-07 Tiếp nhận xe

- [x] Hoàn thành backend (CheckInVehicleRequest/Response, Service, Controller, Unit Tests UC09, cập nhật booking RECEIVED, gọi I03 RepairWorkflow).
- **Module/mốc:** M05 / G2.
- **Đầu ra:** UI tiếp nhận; gọi I03 tạo phiếu; đổi booking thành RECEIVED.
- **Phụ thuộc:** TV2-02, TV3-03.
- **Nghiệm thu:** UC09; booking sinh tối đa một phiếu; walk-in booking_id NULL.

### TV2-08 Checklist và ảnh bằng chứng

- [x] Hoàn thành backend (VehicleCheck & Attachment entities, repos, services, controllers, upload an toàn sha256 & storageKey, Unit Tests BR16).
- **Module/mốc:** M05 / G2.
- **Đầu ra:** Service lưu vehicle_checks/attachments, JSON schema, upload/download có quyền.
- **Phụ thuộc:** TV2-07, TV1-04.
- **Nghiệm thu:** BR16; MIME/kích thước hợp lệ; không truy cập storage bằng tên file tùy ý.

### TV2-09 Tra cứu lịch sử xe

- [x] Hoàn thành backend (VehicleTimelineResponse, phân quyền chủ mới ẩn tài chính chủ cũ theo BR04, Unit Tests UC38).
- **Module/mốc:** M03 / G5.
- **Đầu ra:** UC38; UI cây lịch sử, dữ liệu kỹ thuật/hóa đơn/bảo hành theo quyền.
- **Phụ thuộc:** TV2-03, TV4-06, TV3-10.
- **Nghiệm thu:** Đúng thứ tự lần sửa; chủ mới chỉ thấy kỹ thuật; ảnh tuân theo quyền phiếu.

### TV2-10 Kiểm thử và bàn giao phân hệ tiếp nhận

- [x] Hoàn thành backend (TV2ReceptionIntegrationTest kiểm thử liên thông 100% đạt 34/34 test cases, tài liệu bàn giao RECEPTION_MODULE_API.md).
- **Module/mốc:** M05 / G5.
- **Đầu ra:** Test UC04–09/38, xử lý 403/409, tài liệu API tiếp nhận.
- **Phụ thuộc:** TV2-06, TV2-08, TV2-09.
- **Nghiệm thu:** Luồng đặt lịch và walk-in hoàn chỉnh; kiểm thử cạnh tranh và đổi chủ đạt.


## TV3

### TV3-01 Nhân viên kỹ năng ca và khoang

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M06 / G1.
- **Đầu ra:** CRUD employees/shifts/bays, can_qc, UI ca làm UC37.
- **Phụ thuộc:** TV1-03.
- **Nghiệm thu:** Ca end>start; từ chối phân công sai ca/kỹ năng; lưu lịch bảo trì.

### TV3-02 Lịch khoang I02

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M06 / G2.
- **Đầu ra:** BayScheduling; khóa bay; truy vấn giao nhau; release/expiry.
- **Phụ thuộc:** TV3-01.
- **Nghiệm thu:** UC10; hai request trùng khoang chỉ một thành công; khoảng tiếp giáp hợp lệ.

### TV3-03 Vòng đời phiếu I03

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M08 / G2.
- **Đầu ra:** RepairWorkflow; nhận xe, phiếu chẩn đoán, transition status.
- **Phụ thuộc:** TV2-01, TV2-02.
- **Nghiệm thu:** UC09/12; không sửa phiếu đóng; giữ đúng khách lịch sử; diagnosis trước báo giá.

### TV3-04 Lập gửi và phát sinh báo giá

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M07 / G2.
- **Đầu ra:** Header/items, snapshot giá, version_no, INITIAL/SUPPLEMENTAL; UI báo giá.
- **Phụ thuộc:** TV3-03, TV2-04, TV4-02.
- **Nghiệm thu:** UC13/15; version tăng dưới khóa phiếu; phát sinh cùng phiếu và tham chiếu bản gốc.

### TV3-05 Duyệt từng dòng I04

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M07 / G3.
- **Đầu ra:** EstimateApproval, tạo task từ SERVICE, gọi I05 giữ PART, notify.
- **Phụ thuộc:** TV3-04, TV4-04, TV1-06.
- **Nghiệm thu:** UC14/23; REJECTED/PENDING không sinh task/giữ kho; replay không tạo trùng.

### TV3-06 Phân công kỹ thuật viên

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M08 / G3.
- **Đầu ra:** UC11; MAIN/ASSISTANT/QC, lịch nhân viên và khóa task.
- **Phụ thuộc:** TV3-01, TV3-05.
- **Nghiệm thu:** Tối đa một MAIN active; không chồng ca công việc; QC phải can_qc.

### TV3-07 Timer bắt đầu tạm dừng tiếp tục

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M08 / G3.
- **Đầu ra:** UC16–18; labor_sessions, cost snapshot; UI bảng việc KTV.
- **Phụ thuộc:** TV3-06.
- **Nghiệm thu:** Một timer mở mỗi thợ; pause đóng phiên; resume tạo phiên mới; xử lý request cạnh tranh.

### TV3-08 Hoàn thành công việc

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M08 / G3.
- **Đầu ra:** UC19; đóng timer, cập nhật task, tổng giờ công thực.
- **Phụ thuộc:** TV3-07.
- **Nghiệm thu:** Tổng giờ loại trừ pause; hoàn thành không còn timer mở; yêu cầu làm lại giữ lịch sử.

### TV3-09 QC và yêu cầu làm lại

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M10 / G4.
- **Đầu ra:** UC27; gọi service lưu checklist của TV2; latest_qc_check_id và attempt.
- **Phụ thuộc:** TV3-08, TV2-08.
- **Nghiệm thu:** QC mới nhất PASSED; QC FAILED chặn lập hóa đơn; task/timer chưa xong bị chặn.

### TV3-10 Bảo hành I07

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M12 / G4.
- **Đầu ra:** UC32–33; snapshot policy khi bàn giao; claim và phiếu xử lý mới.
- **Phụ thuộc:** TV4-06, TV3-03.
- **Nghiệm thu:** Kiểm tra đồng thời ngày/km; ADMIN duyệt có lý do; không ghi đè phiếu cũ.

### TV3-11 Bàn giao đóng phiếu và nghiệm thu

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M08 / G5.
- **Đầu ra:** UC31; phối hợp I05/I06/I07, biên bản HANDOVER, release nguyên tử.
- **Phụ thuộc:** TV3-09, TV3-10, TV4-08.
- **Nghiệm thu:** QC hợp lệ, task xong, nợ hợp lệ; đóng phiếu, giải phóng khoang/lượng giữ và tạo bảo hành cùng commit.


## TV4

### TV4-01 Nhà cung cấp

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M09 / G1.
- **Đầu ra:** UC20; CRUD NCC, MST, điều khoản thanh toán, UI.
- **Phụ thuộc:** TV1-01.
- **Nghiệm thu:** Mã NCC duy nhất; danh mục có lịch sử dùng is_active=false.

### TV4-02 Danh mục phụ tùng

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M09 / G1.
- **Đầu ra:** UC21; SKU, giá bán, giá vốn, bảo hành, reorder level; UI.
- **Phụ thuộc:** TV4-01.
- **Nghiệm thu:** SKU duy nhất; available=stock-reserved; không chỉnh trực tiếp tồn qua CRUD.

### TV4-03 Nhập mua và tồn đầu kỳ

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M09 / G2.
- **Đầu ra:** UC22; chứng từ/dòng, DRAFT/POSTED, bình quân gia quyền.
- **Phụ thuộc:** TV4-02, TV1-06.
- **Nghiệm thu:** DRAFT không đổi tồn; POST một lần; OPENING không tạo nợ NCC.

### TV4-04 Giữ và giải phóng hàng I05

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M09 / G3.
- **Đầu ra:** InventoryOperations.reserve/release; khóa part theo ID tăng.
- **Phụ thuộc:** TV4-02.
- **Nghiệm thu:** UC23; không giữ quá khả dụng; tổng lượng giữ các dòng bằng parts.reserved_quantity.

### TV4-05 Xuất trả và điều chỉnh kho I05

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M09 / G3.
- **Đầu ra:** UC24–26; post sổ kho, tham chiếu dòng nguồn, repair_order_parts.
- **Phụ thuộc:** TV4-03, TV4-04, TV3-05.
- **Nghiệm thu:** Xuất không vượt duyệt; trả không vượt đã xuất; giữ dòng quantity=0; sổ kho không bị sửa.

### TV4-06 Phát hành hóa đơn I06

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M11 / G4.
- **Đầu ra:** UC28; snapshot khách/xe/dòng/cost, BigDecimal, UI hóa đơn.
- **Phụ thuộc:** TV3-09, TV4-05.
- **Nghiệm thu:** Một hóa đơn mỗi phiếu; gọi I03 kiểm tra QC; đổi danh mục không thay hóa đơn cũ.

### TV4-07 Thu tiền khách I06

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M11 / G4.
- **Đầu ra:** UC29; IN, khóa invoice, mã giao dịch, UI phiếu thu.
- **Phụ thuộc:** TV4-06, TV1-06.
- **Nghiệm thu:** 0<amount<=dư nợ; replay không thu hai lần; cập nhật PARTIALLY_PAID/PAID.

### TV4-08 Chi NCC và công nợ I06

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M11 / G4.
- **Đầu ra:** UC30; OUT, khóa chứng từ; hai view; kiểm tra hạn mức khi bàn giao.
- **Phụ thuộc:** TV4-03, TV4-07.
- **Nghiệm thu:** Chỉ ADMIN chi; chỉ IN POSTED có NCC; không chi vượt dư nợ; loại OPENING/VOID đúng.

### TV4-09 Đối soát kho và giá vốn

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M09 / G5.
- **Đầu ra:** Truy vấn đối soát ledger, snapshot cost, tổng giữ, xuất/trả theo phiếu.
- **Phụ thuộc:** TV4-05, TV4-06.
- **Nghiệm thu:** Số tồn và sổ kho khớp; lượng giữ không âm; doanh thu và giá vốn không lẫn.

### TV4-10 Màn hình kho và tài chính tích hợp

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M11 / G5.
- **Đầu ra:** Hoàn thiện màn hình chứng từ, phiếu thu/chi, sổ công nợ và in.
- **Phụ thuộc:** TV4-08.
- **Nghiệm thu:** Đúng quyền WAREHOUSE_STAFF/RECEPTIONIST/ADMIN; lỗi cạnh tranh hiển thị 409.

### TV4-11 Kiểm thử tiền kho và bàn giao

- [ ] Hoàn thành và được nghiệm thu.
- **Module/mốc:** M11 / G5.
- **Đầu ra:** Test song song POST/thu/chi, đối soát báo cáo, tài liệu API.
- **Phụ thuộc:** TV4-09, TV4-10.
- **Nghiệm thu:** Không double-post/double-pay/overpay; đối soát số liệu G5 đạt.

## Các mốc tích hợp

- **G0 — Khởi tạo, TV1 chủ trì:** skeleton backend/frontend, cấu hình môi trường, V1–V4 chạy trên CSDL sạch, kiểm thử schema và hai view. Đã khởi tạo trong lần làm việc này.
- **G1 — Danh mục và quyền, TV1/TV2 chủ trì:** UC01–06 và phần danh mục UC20–21/37; mỗi tài khoản chỉ thấy dữ liệu được phép.
- **G2 — Tiếp nhận và báo giá, TV2/TV3 chủ trì:** UC07–10/12–13/15; đủ luồng walk-in và booking; audit/outbox chung transaction.
- **G3 — Thi công và kho, TV3/TV4 chủ trì:** UC11/14/16–19/22–26; timer, duyệt từng dòng, giữ/xuất/trả kho chạy liên thông và chống tranh chấp.
- **G4 — QC và tiền, TV3/TV4 chủ trì:** UC27–30/32–33; hóa đơn bị chặn khi QC lỗi; nhiều lần thu/chi không vượt công nợ.
- **G5 — Bàn giao và hậu mãi, cả nhóm:** UC31/34–38; đóng phiếu nguyên tử, bảo hành, nhắc định kỳ, báo cáo; chạy toàn bộ T01–T38 và demo xuyên suốt.

Chưa gán ngày hoặc số giờ vì chưa có thời hạn và năng lực mỗi thành viên. TV3 có 12 bảng và nhiều luồng cạnh tranh; TV1 hỗ trợ kiểm thử concurrency, TV2 hỗ trợ UI QC, TV4 hỗ trợ tích hợp bảo hành từ hóa đơn để giảm tải nhưng TV3 vẫn là người nghiệm thu.

## Điều kiện hoàn thành một đầu việc

Code đúng phạm vi module, API và UI liên thông, kiểm tra role/ownership ở backend, đầu vào có validation, giao dịch nghiệp vụ có rollback đúng, kiểm thử luồng chính và ngoại lệ trọng yếu đạt, cập nhật tài liệu API. Chỉ đánh dấu checkbox sau khi người chủ trì nghiệm thu được đầu ra; việc tạo thư mục/interface không có nghĩa chức năng đã hoàn thành.

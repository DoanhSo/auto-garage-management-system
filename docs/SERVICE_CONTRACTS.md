# Hợp đồng giao tiếp giữa các module

Tám hợp đồng I01–I08 dưới đây được đề xuất cụ thể trong lần khởi tạo này. Báo cáo chỉ nhắc đến số lượng hợp đồng, không cung cấp chữ ký. Interface nằm trong `src/main/java/com/Group13_65CNTT/AutoGarage/shared/contracts`; chưa có implementation hoặc bean Spring tương ứng.

## Quy ước chung

- Các tham số `actorUserId` phải lấy từ principal đã xác thực, không tin giá trị do frontend gửi. Mỗi service tự kiểm tra role và quyền đối tượng bằng I01.
- Tiền dùng `BigDecimal`, số lượng dùng `BigDecimal` ba chữ số thập phân, thời gian qua hợp đồng dùng `Instant`. CSDL lưu UTC, UI đổi sang giờ gara. Quy tắc làm tròn: HALF_UP tại từng dòng, hai chữ số thập phân cho tiền; tổng header bằng tổng dòng sau giảm giá/thuế.
- Service ghi dữ liệu mở hoặc tham gia `@Transactional` với propagation REQUIRED. Không dùng REQUIRES_NEW cho kho, audit, outbox, bàn giao hoặc bảo hành thuộc cùng lệnh. Hỏng bất kỳ bước nào phải rollback toàn bộ.
- Lệnh nhạy cảm mang request_key; hash SHA-256 từ payload chuẩn hóa bao gồm actor/action/entity. Cùng key/hash trả lại kết quả đã lưu, khác hash trả 409. Khóa nghiệp vụ và ràng buộc UNIQUE vẫn cần thiết khi hai request cùng đến.
- Khóa bản ghi cha trước kiểm tra rồi ghi: repair_order → estimate/item → part theo id tăng khi luồng báo giá/thi công/kho; bay khi xếp lịch; employee/task khi phân công và timer. Nhóm thống nhất thứ tự chi tiết trước G3 để tránh deadlock.
- Không gọi Email/SMS hoặc storage upload bên ngoài trong transaction giữ khóa lâu. Metadata/outbox lưu trong transaction; gửi thông báo sau commit; tệp tải lên staging trước rồi finalize/cleanup có kiểm soát.
- Mã lỗi tích hợp: 400 dữ liệu sai, 403 thiếu quyền, 404 đối tượng không tồn tại, 409 trạng thái/xung đột/idempotency. Các interface hiện dùng ngoại lệ do module triển khai, TV1 chuẩn hóa thành HTTP.

## I01 SecurityAccess do TV1 cung cấp

Người dùng: mọi module. `requireRole`, `requireVehicleAccess`, `requireRepairOrderAccess` phải kiểm tra tài khoản active, role và ownership hiện tại/lịch sử theo BR02/BR04. Xe đổi chủ không làm chủ mới có quyền đọc hóa đơn/ảnh cá nhân của chủ cũ. Kiểm tra attachments, invoices, warranties, notifications phải dựa thêm vào đối tượng gốc; ba method này là nền tảng, không thay thế toàn bộ chính sách truy cập.

## I02 BayScheduling do TV3 cung cấp

Người dùng: TV2 đặt/dời/hủy lịch, TV3 điều phối và đóng phiếu. `reserveBooking`, `reserveRepair` khóa `service_bays` rồi kiểm tra `existing.starts_at < new.ends_at AND existing.ends_at > new.starts_at` với HELD/IN_USE chưa hết hạn. Khoang phải active, đúng loại dịch vụ. `releaseForBooking`/`releaseForRepair` cập nhật reservation trong cùng transaction với thay đổi booking/phiếu. Chuyển từ BOOKING sang REPAIR phải release bản cũ và tạo bản mới, không sửa reservation thành hai nguồn cùng lúc.

## I03 RepairWorkflow do TV3 cung cấp

Người dùng: TV2 tiếp nhận, TV4 hóa đơn, TV3 bảo hành/bàn giao. `receive` kiểm tra booking thuộc đúng xe, trạng thái nhận được và không có phiếu trùng; chốt customer lịch sử. Tạo task DIAGNOSIS nội bộ để có thể phân công chẩn đoán trước khi duyệt sửa. `requireInvoiceReady` kiểm tra QC mới nhất đúng loại QC/PASSED, task xong, không còn timer mở, phần xuất vật tư đã hoàn tất. `handOver` gọi I06 kiểm tra nợ, lưu HANDOVER qua service `reception`, cập nhật phiếu, giải phóng I02/I05, tạo I07, ghi I08 cùng commit. `createWarrantyOrder` tạo phiếu mới loại WARRANTY và trả ID.

## I04 EstimateApproval do TV3 cung cấp

Người dùng: khách/lễ tân qua API báo giá. `decide` nhận các LineDecision; xác minh toàn bộ dòng thuộc estimate/phiếu được phép, khóa phiếu và các dòng. Chỉ APPROVED mới tạo task SERVICE hoặc gọi I05 reserve PART. Quyết định lại dòng đã triển khai phải bị chặn; phát sinh dùng SUPPLEMENTAL. TV3 quyết định trạng thái tổng báo giá từ trạng thái từng dòng. Thông báo và kết quả replay qua I08 cùng transaction.

## I05 InventoryOperations do TV4 cung cấp

Người dùng: TV3 duyệt/bàn giao và UI kho. `reserve` kiểm tra dòng PART APPROVED, lượng cần giữ và stock-reserved, khóa parts và estimate_items. `releaseForOrder` trả lượng giữ của phiếu về khả dụng mà không đổi stock. `post` khóa header DRAFT, parts theo id tăng, kiểm tra loại nguồn/số lượng, cập nhật stock/reserved/cost, repair_order_parts, ledger và POSTED nguyên tử. Mỗi inventory_document_item chỉ sinh một transaction. RETURN phải thuộc đúng OUT/dòng nguồn/phiếu/part; tổng trả không vượt đã xuất. ADJUSTMENT có số lượng signed, các loại còn lại quantity dương; sổ kho quantity_change âm cho OUT.

## I06 BillingOperations do TV4 cung cấp

Người dùng: lễ tân/admin và I03. `issueInvoice` gọi I03, khóa phiếu rồi tạo một hóa đơn với snapshot bất biến. DIAGNOSIS nội bộ không vào hóa đơn trừ khi có dòng dịch vụ được duyệt. `receivePayment` khóa invoice rồi kiểm tra 0<amount<=balance. `paySupplier` chỉ ADMIN, khóa chứng từ IN/POSTED có supplier, loại OPENING, kiểm tra dư nợ. `requireHandoverAllowed` khóa customer và invoice/đối tượng liên quan để hai xe bàn giao đồng thời không vượt tổng credit_limit; kiểm tra payment_term_days/due_date. View chỉ để tính toán/đọc; không thay thế khóa giao dịch.

## I07 WarrantyOperations do TV3 cung cấp

Người dùng: I03 bàn giao và UI bảo hành. `createFromInvoice` chỉ tạo một warranty cho mỗi dòng có policy; snapshot ngày/km/điều kiện. `openClaim` kiểm tra quyền xe, thời gian và ODO; mốc bằng hạn được coi là hết hạn. Policy bằng 0 ở một chiều nghĩa là không giới hạn chiều đó, NULL ở expires_at/end_odometer. `decideClaim` chỉ ADMIN, lý do không trống, khóa claim; khi duyệt gọi I03 tạo phiếu mới và cập nhật handling_order_id cùng transaction. Vật tư xử lý bảo hành vẫn qua I05 và lưu giá vốn.

## I08 NotificationAudit do TV1 cung cấp

Người dùng: mọi service có thay đổi nhạy cảm. `enqueue` ghi thông báo PENDING; `recipientKey` định danh người nhận ổn định, event_key/channel/recipient_key chống trùng. `findCompletedRequest` kiểm tra hash và trả kết quả replay nếu đã có; `recordCompletedRequest` ghi actor/action/result và key/hash sau khi nghiệp vụ thành công trong cùng transaction. Worker claim bằng lease/locked_by, gửi sau commit, tăng attempt_count, đặt next_attempt_at, recovery khi lease hết hạn. Hai worker không được xử lý cùng lease; gửi mạng không đảm bảo exactly-once nếu provider không hỗ trợ idempotency.

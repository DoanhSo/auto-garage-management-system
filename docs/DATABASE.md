# Thiết kế database khởi tạo

Nguồn: `BaoCao_PhanTich_Va_ThietKe_HeThong_QuanLy_GaraOto.docx`, ngày 14/10/2026. Báo cáo cung cấp tên 30 bảng, ERD Chen với một phần thuộc tính, BR01–BR24 và đặc tả 38 use case. Phần thân tài liệu không có data dictionary 373 cột hay DDL 71 FK như giới thiệu. Vì vậy V1–V4 là schema khởi tạo được suy ra từ luồng nghiệp vụ và có các quyết định bổ sung dưới đây; không khẳng định khớp chính xác 373 cột/71 FK.

Schema V1–V8 hiện gồm **30 bảng nghiệp vụ, 431 cột (kể cả hai cột generated), 76 khóa ngoại, 77 CHECK và hai view**. V1–V4 ban đầu có 423 cột/75 CHECK; V5 thêm auth_version và CHECK không âm. Không tính `flyway_schema_history` hoặc cột của view. Danh sách cột trong `DATA_DICTIONARY.md`; SQL là nguồn thực thi chính thức.

## Nội dung V1–V8

- **V1 create_identity_and_catalogs:** users, customers, vehicles, services, employees, employee_shifts, service_bays, bookings, suppliers, parts, notifications, audit_events.
- **V2 create_repair_workflow:** repair_orders, bay_reservations, estimates, estimate_items, repair_tasks, task_assignments, labor_sessions, vehicle_checks, attachments.
- **V3 create_inventory_billing_and_warranty:** repair_order_parts, inventory_documents, inventory_document_items, inventory_transactions, invoices, invoice_items, payments, warranties, warranty_claims.
- **V4 add_workflow_links_and_balance_views:** FK vòng latest_qc_check_id và attachment của claim, v_invoice_balances, v_supplier_balances.
- **V5 add_user_auth_version:** users.auth_version để thu hồi JWT, CHECK không âm và index role/is_active. Không sửa checksum V1–V4, không thêm bảng/seed credential. Version account dùng cho optimistic update, auth_version riêng cho token revocation.

- **V6 add_user_audit_columns:** users.created_by/updated_by và index để map BaseEntity.
- **V7 add_notification_audit_columns:** notifications.updated_at/created_by/updated_by/version, inbox index; giữ checksum V1–V5.

- **V8 separate_user_deletion_from_locking:** users.deleted_at, index deleted_at/is_active, CHECK tài khoản đã xóa không được active. Khóa dùng is_active=false và có thể mở lại; DELETE ghi deleted_at và loại khỏi quản trị, giữ FK/lịch sử.

## Quyết định bổ sung để triển khai

1. Mọi PK dùng BIGINT tự tăng, tên bảng/cột snake_case; InnoDB, utf8mb4_0900_ai_ci. Request key, hash, event key và mã giao dịch dùng so sánh binary để không nhập nhằng hoa/thường hoặc dấu. Chuẩn hóa username/email/biển số ở service trước khi ghi.
2. Giá và tiền dùng DECIMAL(18,2), số lượng DECIMAL(14,3); Java BigDecimal. HALF_UP tại từng dòng, header cộng các dòng đã làm tròn. CHECK xác thực công thức dòng/header, còn đối soát tổng các dòng với header thuộc service.
3. DATETIME(6) lưu UTC; JDBC ép timezone session UTC. Cột `version` là optimistic lock, map `@Version`; một số bảng append-only không có version/updated_at.
4. `customers.user_id` NULL và UNIQUE để hỗ trợ khách trực tiếp; employees.user_id NOT NULL và UNIQUE. Không thêm bảng roles; users.role là một trong năm role cố định.
5. Booking yêu cầu một service chính. ERD nhóm 1 ghi quan hệ service 0..1 nhưng BR05 nêu một dịch vụ chính; chọn NOT NULL theo BR05. Nếu muốn lịch chẩn đoán chung, tạo một dịch vụ danh mục phù hợp, không thêm dòng service rỗng.
6. `repair_tasks.task_type=DIAGNOSIS` cho phép phân công trước báo giá, không có estimate_item_id; REPAIR bắt buộc dòng dịch vụ được duyệt. Điều này giải quyết UC12 cần phân công chẩn đoán nhưng BR13 cấm sửa chưa duyệt. Service xác thực approval_status và cùng phiếu; FK một cột chưa kiểm tra mọi quan hệ nghiệp vụ.
7. Một booking sinh tối đa một repair_order; một estimate_item sinh tối đa một repair_task; version_no duy nhất trong phiếu. Parent estimate được FK composite đảm bảo cùng phiếu; service kiểm tra INITIAL/SUPPLEMENTAL, tăng version và chống self/cycle.
8. `active_main_task_id` là generated column + UNIQUE để chỉ một MAIN active; `active_employee_id` tương tự để chỉ một timer mở mỗi thợ. FK composite labor session đảm bảo đúng employee của assignment. Không ghi trực tiếp vào hai cột generated.
9. Khoang dùng HELD/IN_USE/RELEASED/EXPIRED/CANCELLED. Reservation có đúng một nguồn BOOKING/REPAIR hoặc không nguồn cho MAINTENANCE. Index hỗ trợ truy vấn giao nhau, nhưng chống chồng lịch cần khóa hàng bay ở service. Ca/thợ cũng cần khóa và kiểm tra thời gian.
10. Tách `inventory_documents.document_type=OPENING` khỏi IN mua hàng để tránh chi tiền tồn đầu kỳ. DRAFT không đổi tồn; POSTED phải có posted_by/posted_at. quantity của ADJUSTMENT signed, loại khác dương; OUT lưu quantity_change âm ở ledger. UNIQUE document_item_id chống ghi sổ hai lần. Chỉ service được sửa số tồn; ledger phải append-only.
11. `repair_order_parts` giữ số lượng ròng, kể cả zero sau trả toàn bộ. Giá vốn là snapshot bình quân của lượng thực dùng cho dòng; cập nhật theo xuất/trả và ledger, không đọc lại giá danh mục khi phát hành hóa đơn. RETURN trỏ cả document/dòng nguồn để truy vết và giới hạn lượng trả.
12. Hóa đơn duy nhất trên mỗi phiếu; snapshot customer/vehicle JSON, item_name/unit/price/cost/warranty policy độc lập danh mục. Trạng thái UNPAID/PARTIALLY_PAID/PAID/VOID. Một invoice VOID không tạo hóa đơn thứ hai cho cùng phiếu trong khung này; quy trình phát hành lại/hoàn tiền chưa thuộc phạm vi báo cáo và cần migration/nghiệp vụ riêng.
13. payments IN chỉ có invoice_id; OUT chỉ có inventory_document_id; amount>0 và mã giao dịch UNIQUE. Service kiểm tra nguồn OUT là IN/POSTED có NCC, chặn vượt nợ và kiểm tra role. Không có CHECK xuyên bảng để tự bảo đảm điều kiện này.
14. latest_qc_check_id dùng FK composite để trỏ check cùng phiếu. Service phải kiểm tra check_type QC, PASSED, lần mới nhất, task hoàn thành và không timer mở. Constraint không thể thay thế điều kiện BR17.
15. Bảo hành snapshot từ invoice_item. Policy 0 ở chiều ngày/km nghĩa là không có giới hạn chiều đó, cột hạn tương ứng NULL; cần ít nhất một giới hạn. Chạm hạn ngày hoặc km là hết hiệu lực theo UC32. Claim APPROVED/IN_PROGRESS/CLOSED có người duyệt, thời gian, lý do và handling_order_id duy nhất. Service bảo đảm phiếu xử lý mới khác phiếu gốc và đúng xe.
16. Attachment gắn phiếu sửa chữa hoặc claim bảo hành; claim có thể tải bằng chứng trước khi được duyệt/tạo phiếu mới. FK claim thêm ở V4 vì bảng tạo sau. Attachment của checklist dùng FK composite cùng phiếu; service kiểm tra quyền, MIME, size, checksum, category, JSON schema. Không lưu file nhị phân trong database.
17. Thông báo chống trùng `(event_key, channel, recipient_key)` thay vì chỉ event/channel để cùng sự kiện gửi cho khách và nhân viên. user_id/customer_id có thể cùng có khi là một người; recipient_key do service tạo ổn định. WEB cần tài khoản; EMAIL/SMS dùng hồ sơ khách không có tài khoản.
18. Audit có request_key UNIQUE nullable và request_hash đi cùng; các sự kiện đọc/login không cần key. Actor NULL dành cho system worker. Không ghi password/token vào audit JSON. Chống replay và outbox phải tham gia transaction nghiệp vụ.
19. Tất cả FK dùng RESTRICT mặc định, không CASCADE DELETE dữ liệu lịch sử. `is_active=false` cho danh mục có lịch sử. Immutability của snapshot/ledger/audit được service và quyền database kiểm soát; khung này chưa cài trigger chống sửa mọi cột.

## Hai view công nợ

`v_invoice_balances` có một dòng mỗi hóa đơn không VOID: total_amount - SUM(payments IN). Gồm invoice_id, invoice_code, customer_id, repair_order_id, issued_at, due_date, status, total_amount, paid_amount, balance_amount.

`v_supplier_balances` có một dòng mỗi supplier, kể cả chưa mua: tổng chứng từ IN POSTED có supplier trừ payments OUT của đúng các chứng từ đó. OPENING/DRAFT/CANCELLED không tạo dư nợ. Aggregation thanh toán thực hiện trước join để nhiều lần thu/chi không nhân tổng chứng từ. Muốn xem chi tiết từng chứng từ/due_date, service truy vấn documents + payments, không đổi grain của view tổng supplier.

View là dữ liệu suy ra, không lưu dư nợ ở một bảng riêng. View không tự chặn overpay; thao tác ghi phải khóa invoice/document/customer phù hợp.

## Phân chia ràng buộc

Database bảo đảm PK/FK/UNIQUE, giá/số lượng hợp lệ, nguồn thanh toán/dòng báo giá đúng cấu trúc, một timer mở, một MAIN active, công thức tiền tại dòng và một lần ghi ledger mỗi dòng chứng từ. Backend identity đã có role/ownership/assignment/QC access policy và account transaction. Service nghiệp vụ TV2–TV4 còn cần trạng thái, overlap, kỹ năng/ca, QC đúng loại/mới nhất, duyệt trước thi công, khớp reserved totals, không xuất/trả/thu/chi vượt hạn, immutable snapshot và transaction nhiều bảng theo 42 đầu việc.

## Tài liệu kỹ thuật đã đối chiếu

- [MySQL 8.4 CHECK constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html).
- [Spring Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html).
- [Flyway versioned migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/versioned-migrations).

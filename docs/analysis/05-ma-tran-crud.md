# 5. Ma trận CRUD

> Thuộc plan `system-analysis-erd`.
> **Nguồn:** danh sách "Bảng CSDL tác động" của từng use case trong báo cáo gốc. Loại thao tác C/R/U/D được bổ sung theo service hiện tại.
> **Ký hiệu:** **C** tạo · **R** đọc · **U** sửa · **D** xóa vật lý.
> Hệ thống gần như không xóa vật lý (BR24); **D** chỉ xuất hiện ở các bản ghi tạm như công việc chưa làm hoặc tệp lỗi.

## 5.1. Theo use case

| UC | C (tạo) | U (sửa) | R (đọc) |
|---|---|---|---|
| UC01–02 | audit_events | users (`last_login_at`, `auth_version`) | users |
| UC03 | users, audit_events | users (khóa, vai trò) | users |
| UC04 | customers, audit_events | customers | users |
| UC05 | vehicles | vehicles (đổi chủ, ngừng) | customers, repair_orders |
| UC06 | services, audit_events | services | — |
| UC07 | bookings, bay_reservations, notifications | — | services, service_bays, vehicles |
| UC08 | bay_reservations, notifications, audit_events | bookings, bay_reservations | — |
| UC09 | repair_orders, vehicle_checks, attachments | bookings (RECEIVED), vehicles (ODO) | customers |
| UC10 | bay_reservations, audit_events | repair_orders (`service_bay_id`) | service_bays |
| UC11 | task_assignments, notifications | repair_tasks (ASSIGNED) | employees, employee_shifts |
| UC12 | attachments, repair_order_assignments, audit_events, notifications | repair_orders (`diagnosis`, DIAGNOSING) | — |
| UC13 | estimates, estimate_items, notifications | repair_orders (WAITING_APPROVAL), estimates (SENT, CANCELLED bản cũ) | services, parts |
| UC14 | repair_tasks, notifications | estimate_items, estimates, parts (`reserved_quantity`), repair_orders (APPROVED) | — |
| UC15 | estimates, estimate_items, notifications | — | repair_orders |
| UC16 | labor_sessions, audit_events | repair_tasks (IN_PROGRESS), repair_orders (IN_PROGRESS) | task_assignments |
| UC17 | audit_events | labor_sessions (`ends_at`), repair_tasks (PAUSED) | — |
| UC18 | labor_sessions | repair_tasks | task_assignments |
| UC19 | notifications | repair_tasks (COMPLETED), labor_sessions, repair_orders (WAITING_QC) | — |
| UC20 | suppliers, audit_events | suppliers | — |
| UC21 | parts, audit_events | parts | suppliers |
| UC22 | inventory_documents, inventory_document_items, inventory_transactions | parts (tồn, giá vốn), inventory_documents (POSTED) | suppliers |
| UC23 | audit_events, notifications | parts (`reserved_quantity`), estimate_items (`reserved_quantity`) | estimates |
| UC24 | inventory_documents, inventory_document_items, inventory_transactions, repair_order_parts | parts, estimate_items | — |
| UC25 | inventory_documents, inventory_document_items, inventory_transactions | parts, repair_order_parts | — |
| UC26 | inventory_documents, inventory_document_items, inventory_transactions, audit_events | parts | — |
| UC27 | vehicle_checks, notifications | repair_orders (`latest_qc_check_id`, COMPLETED/QC_FAILED), repair_tasks (REWORK) | employees |
| UC28 | invoices, invoice_items | repair_orders (INVOICED) | repair_tasks, repair_order_parts, customers, vehicles |
| UC29 | payments, audit_events, notifications | invoices (PARTIALLY_PAID/PAID) | — |
| UC30 | payments, audit_events | — | inventory_documents, suppliers |
| UC31 | vehicle_checks (HANDOVER), warranties, order_commissions, notifications | repair_orders (CLOSED), bay_reservations (RELEASED), parts, vehicles (ODO) | invoices |
| UC32 | warranty_claims, attachments, notifications | — | warranties, vehicles |
| UC33 | repair_orders (WARRANTY), audit_events, notifications | warranty_claims, warranties (điều chỉnh, VOID) | — |
| UC34 | notifications, audit_events | notifications (`read_at`) | users, customers |
| UC35 | notifications | — | services, repair_tasks, warranties, vehicles, customers |
| UC36 | — | — | invoices, payments, inventory_transactions, labor_sessions, parts, v_invoice_balances, v_supplier_balances |
| UC37 | employee_shifts, service_bays, bay_reservations, audit_events | employee_shifts (duyệt, chấm công), service_bays | employees |
| UC38 | — | — | vehicles, repair_orders, repair_tasks, repair_order_parts, invoices, warranties, customers |
| Lương (V12–V17) | payroll_periods, payroll_items, payments | payroll_periods (duyệt, chi), order_commissions (đưa vào lương) | employee_shifts, repair_order_assignments, order_commissions |
| Hủy hóa đơn (V19) | — | invoices (VOID), repair_orders (COMPLETED) | payments |

**Xóa vật lý (D):**
- `repair_tasks` chưa làm và không sinh từ báo giá (`RepairServiceImpl.deleteTask`).
- Bản ghi phân công được giải phóng: dùng `released_at`, không xóa.

## 5.2. Theo bảng — ai tạo, ai sửa

| Bảng | Tạo bởi | Sửa bởi |
|---|---|---|
| users | UC03 | UC01–03 |
| customers | UC04 | UC04 |
| vehicles | UC05 | UC05, UC09, UC31 |
| services | UC06 | UC06 |
| bookings | UC07 | UC08, UC09 |
| service_bays | UC37 | UC37 |
| bay_reservations | UC07, UC08, UC10, UC37 | UC08, UC31 |
| employees | UC03 + quản lý nhân sự | UC37 |
| employee_shifts | UC37 | UC37 |
| repair_orders | UC09, UC33 | UC10, UC12–14, UC16, UC19, UC27, UC28, UC31, hủy hóa đơn |
| vehicle_checks | UC09, UC27, UC31 | — (bất biến) |
| attachments | UC09, UC12, UC32 | — |
| estimates | UC13, UC15 | UC13, UC14 |
| estimate_items | UC13, UC15 | UC14, UC23, UC24 |
| repair_tasks | UC14 | UC11, UC16–19, UC27 |
| task_assignments | UC11 | UC11 (giải phóng) |
| labor_sessions | UC16, UC18 | UC17, UC19 |
| repair_order_assignments | UC12, UC16 | UC19, UC27 |
| suppliers | UC20 | UC20 |
| parts | UC21 | UC14, UC21–26, UC31 |
| repair_order_parts | UC24 | UC25 |
| inventory_documents | UC22, UC24–26 | ghi sổ (POSTED) |
| inventory_document_items | UC22, UC24–26 | — |
| inventory_transactions | UC22, UC24–26 | — (bất biến, BR15) |
| invoices | UC28 | UC29, hủy hóa đơn |
| invoice_items | UC28 | — (bất biến, BR18) |
| payments | UC29, UC30, lương | — (bất biến) |
| warranties | UC31 | UC33 (điều chỉnh, hủy sổ) |
| warranty_claims | UC32 | UC33 |
| notifications | mọi sự kiện, UC35 | tác vụ nền gửi, UC34 (đã đọc) |
| audit_events | mọi thao tác `@Audited` | — (chỉ ghi thêm) |
| payroll_periods | lương | lương |
| payroll_items | lương | lương |
| order_commissions | UC31 | lương |

Cả 34 bảng đều có ít nhất một tiến trình tạo dữ liệu.

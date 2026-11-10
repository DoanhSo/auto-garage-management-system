# ERD chuẩn Chen

> Thuộc plan `system-analysis-erd`. Mã nguồn là PlantUML (`@startchen`); ảnh nằm ở `svg/`. Các sơ đồ khớp schema **V19**.

## Ký hiệu

| Hình | Ý nghĩa |
|---|---|
| Hình chữ nhật | Thực thể. Nhãn `NHÃN (bảng)` là thực thể thuộc nhóm này; `NHÃN ‹nhóm N›` là thực thể nhóm khác, chỉ vẽ tên |
| Hình chữ nhật viền đôi | Thực thể yếu, có khóa bộ phận gạch chân. Gồm: `vehicle_checks`, `estimate_items`, `inventory_document_items`, `invoice_items` |
| Hình thoi | Quan hệ, mỗi quan hệ ứng với một khóa ngoại. Thoi viền đôi là quan hệ xác định của thực thể yếu |
| Hình oval | Thuộc tính. Gạch chân là khóa. Viền đứt là thuộc tính suy diễn (cột sinh hoặc giá trị tính từ view) |
| `(min,max)` | Bản số, xem cách đọc bên dưới |

**Cách đọc bản số.** Bản số `(min,max)` cho biết thực thể ở đầu đó tham gia quan hệ bao nhiêu lần. Giá trị được suy ra từ schema:
- Phía bảng con: cột khóa ngoại NOT NULL thì `(1,1)`, cho phép NULL thì `(0,1)`.
- Phía bảng cha: cột khóa ngoại có UNIQUE thì `(0,1)`, không có thì `(0,N)`.

**Vì sao chọn 4 thực thể yếu.** Mỗi thực thể chỉ tồn tại trong thực thể cha, và được định danh bằng khóa bộ phận cộng khóa của cha:
- `estimate_items`: `line_no` trong `estimate_id`.
- `invoice_items`: `line_no` trong `invoice_id`.
- `inventory_document_items`: `line_no` trong `document_id`.
- `vehicle_checks`: `check_type` + `attempt_no` trong `repair_order_id`.

`labor_sessions` và `task_assignments` cũng phụ thuộc vào cha, nhưng không có khóa bộ phận nên giữ là thực thể thường.

## Danh sách sơ đồ

| File | Nội dung | Thực thể | Quan hệ |
|---|---|---|---|
| [00-tong-quan](svg/00-tong-quan.svg) | Thực thể lõi và quan hệ chính, không thuộc tính | 19 | quan hệ giữa các thực thể lõi |
| [01-khach-xe-lich-hen](svg/01-khach-xe-lich-hen.svg) | users, customers, vehicles, services, bookings | 5 | 5 |
| [02-tiep-nhan-ho-so](svg/02-tiep-nhan-ho-so.svg) | repair_orders, vehicle_checks, attachments | 3 | 12 |
| [03-nhan-su-ca-khoang](svg/03-nhan-su-ca-khoang.svg) | employees, employee_shifts, service_bays, bay_reservations | 4 | 6 |
| [04-bao-gia-thi-cong](svg/04-bao-gia-thi-cong.svg) | estimates, estimate_items, repair_tasks, task_assignments, labor_sessions, repair_order_assignments | 6 | 16 |
| [05-kho](svg/05-kho.svg) | suppliers, parts, repair_order_parts, inventory_documents, inventory_document_items, inventory_transactions | 6 | 14 |
| [06-hoa-don-bao-hanh](svg/06-hoa-don-bao-hanh.svg) | invoices, invoice_items, payments, warranties, warranty_claims | 5 | 13 |
| [07-thong-bao-kiem-toan](svg/07-thong-bao-kiem-toan.svg) | notifications, audit_events | 2 | 3 |
| [08-luong-hoa-hong](svg/08-luong-hoa-hong.svg) | payroll_periods, payroll_items, order_commissions (bổ sung V12–V17) | 3 | 9 |
| [09-nguoi-thuc-hien](svg/09-nguoi-thuc-hien.svg) | 20 khóa ngoại "người thực hiện" (created_by, approved_by, …) trỏ về users | — | 20 |

**Tổng cộng:**
- **34/34 bảng** là thực thể đầy đủ, mỗi bảng xuất hiện đúng một lần.
- **98/98 khóa ngoại** được vẽ thành quan hệ: 78 quan hệ trong 01–08, cộng 20 quan hệ ở 09.
- **2 view công nợ** thể hiện bằng thuộc tính suy diễn `balance_amount` của hóa đơn.

Sơ đồ nhiều quan hệ (02, 04, 05, 06) khá rộng. Nên mở file SVG rồi phóng to để đọc.

## Render và kiểm tra

```bash
# Kiểm kê schema và đối chiếu độ phủ (34 bảng, 98 FK)
python -I scripts/analysis/schema_inventory.py --check docs/erd

# Render SVG (không cần cài PlantUML, dùng Docker)
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)/docs/erd:/data" plantuml/plantuml -charset UTF-8 -tsvg -o /data/svg "/data/*.puml"
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd -W)/docs/analysis/activity:/data" plantuml/plantuml -charset UTF-8 -tsvg -o /data/svg "/data/*.puml"
```

`schema-inventory.md` sinh tự động từ migration: danh sách bảng, cột, PK, FK, UNIQUE, CHECK.

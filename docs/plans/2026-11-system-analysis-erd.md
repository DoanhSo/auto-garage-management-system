# Kế Hoạch Phân Tích Luồng Dữ Liệu, Nghiệp Vụ, Luồng Hoạt Động Và Thiết Kế CSDL (ERD Chen)

> **Mã plan**: `system-analysis-erd`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`). Plan này chỉ tạo tài liệu, **không sửa code ứng dụng hay schema**.
> **Kế thừa từ**: `docs/DATABASE.md` (thiết kế V1–V8 suy từ báo cáo gốc), `docs/REPAIR_ORDER_LIFECYCLE.md` (luồng cốt lõi đã chốt), `plans/2026-11-repair-lifecycle-lock.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

- **Báo cáo gốc không có trong repo.** `docs/DATABASE.md:3` dẫn nguồn `BaoCao_PhanTich_Va_ThietKe_HeThong_QuanLy_GaraOto.docx` (14/10/2026) gồm mô tả bài toán, ERD Chen một phần, BR01–BR24 và 38 use case. Không tìm thấy file này trong `IdeaProjects`, `Documents`, `Downloads` hay `Desktop`.
- **Mô tả bài toán, nghiệp vụ và use case đang rải rác:**
  - Tên 38 use case UC01–UC38 nằm ở `docs/WORK_BREAKDOWN.md` và `docs/ACCEPTANCE_TESTS.md`.
  - Các quy tắc BR01–BR24 chỉ được nhắc rời rạc trong các tài liệu và comment.
  - Chưa có tài liệu nào gom thành một mô tả bài toán thống nhất.
- **Chưa có sơ đồ luồng dữ liệu hay sơ đồ hoạt động.** Có các sơ đồ sau, nhưng không có DFD (mức ngữ cảnh, mức 0, mức 1), cũng không có sơ đồ hoạt động theo làn cho các luồng khác:
  - Sơ đồ trạng thái phiếu và ma trận khóa theo giai đoạn (`docs/REPAIR_ORDER_LIFECYCLE.md`).
  - Sơ đồ trạng thái báo giá, cùng tài liệu đó.
- **Thư mục `docs/erd/` đang trống** (chỉ có `.gitkeep`).
- **Schema thực tế đã vượt xa tài liệu.** Migration V1–V19 tạo **34 bảng**, khoảng **98 khóa ngoại** và 2 view. `DATA_DICTIONARY.md` chỉ mô tả 30 bảng của V1–V8. Bốn bảng chưa có tài liệu: `payroll_periods`, `payroll_items`, `order_commissions`, `repair_order_assignments`. Các cột mới từ V9–V19 cũng chưa có, ví dụ:
  - `keycloak_id`
  - Ca trực và chấm công.
  - Hồ sơ nhân sự và hệ số lương.
  - Hủy sổ và điều chỉnh bảo hành.
  - `active_repair_order_id` của hóa đơn.

### 1.2. Mục tiêu

Tạo bộ tài liệu phân tích và thiết kế trong `docs/analysis/` và `docs/erd/`, khớp với hệ thống đang chạy (V19):

1. **Mô tả bài toán**: bối cảnh gara một chi nhánh, mục tiêu, phạm vi, 5 tác nhân theo enum `Role` (ADMIN, RECEPTIONIST, WAREHOUSE_STAFF, TECHNICIAN, CUSTOMER) cùng các tác nhân ngoài (nhà cung cấp, Keycloak, kênh EMAIL/SMS), yêu cầu chức năng và phi chức năng.
2. **Phân tích nghiệp vụ**:
   - Danh mục BR01–BR24, mỗi quy tắc ghi nơi thực thi (`file:dòng`) hoặc "chưa thực thi".
   - Danh mục UC01–UC38, gom theo phân hệ.
   - Ma trận UC × tác nhân.
3. **Luồng dữ liệu (DFD)**:
   - Mức ngữ cảnh: hệ thống và các tác nhân ngoài như khách, nhà cung cấp, Keycloak, kênh EMAIL/SMS.
   - Mức 0: 8–10 tiến trình chính, các kho dữ liệu D1…Dn ánh xạ sang bảng thật.
   - Mức 1 cho ba tiến trình phức tạp nhất: sửa chữa, kho, thanh toán.
4. **Luồng hoạt động**: sơ đồ hoạt động có làn theo vai trò cho 8 luồng:
   - Đặt lịch.
   - Tiếp nhận → bàn giao.
   - Báo giá và duyệt.
   - Kho (nhập, giữ, xuất, trả, điều chỉnh).
   - Hóa đơn và thanh toán, gồm cả hủy rồi lập lại.
   - Bảo hành.
   - Ca làm, chấm công và lương.
   - Thông báo và nhắc bảo dưỡng.
5. **CSDL dạng Chen**:
   - Thực thể là hình chữ nhật; thực thể yếu viền đôi.
   - Thuộc tính là hình elip; khóa gạch chân; thuộc tính suy diễn viền đứt.
   - Quan hệ là hình thoi, ghi bản số (min,max).
   - Chia theo 7 phân hệ, cộng 1 sơ đồ tổng quan không có thuộc tính.
6. **Đồng bộ từ điển dữ liệu**: cập nhật `DATA_DICTIONARY.md` lên V19 (34 bảng), kèm ma trận CRUD giữa tiến trình hoặc use case và bảng.

### 1.3. Ngoài phạm vi

- Không sửa code, migration hay dữ liệu. Nếu phát hiện chỗ schema lệch nghiệp vụ, chỉ ghi vào mục "Phát hiện" của tài liệu, không sửa.
- Không dựng lại nguyên văn báo cáo `.docx`.
- Không vẽ sơ đồ lớp UML, sơ đồ tuần tự, sơ đồ triển khai.
- Không xuất Word/PDF. Đầu ra là Markdown cùng mã nguồn sơ đồ. Có thể thêm ảnh SVG nếu render được (xem Step 9).

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1 (Nguồn bài toán)**: `docs/analysis/01-mo-ta-bai-toan.md` ghi rõ nguồn của từng phần. Nguồn là báo cáo gốc (nếu người dùng cung cấp), tài liệu trong repo, hoặc suy ra từ code. Không có câu khẳng định nào thiếu nguồn. _Kiểm chứng_: rà mục "Nguồn" ở cuối file.
- **AC-2 (Quy tắc nghiệp vụ)**: Bảng BR01–BR24 đủ 24 dòng. Mỗi dòng có nội dung, use case liên quan và nơi thực thi (`file:dòng`, hoặc "DB constraint `<tên>`", hoặc "chưa thực thi"). _Kiểm chứng_: đếm dòng; mở ngẫu nhiên 5 vị trí `file:dòng` để xác nhận.
- **AC-3 (Use case)**: UC01–UC38 đủ 38 dòng. Mỗi use case gắn tác nhân, tiến trình DFD và sơ đồ hoạt động chứa nó (hoặc "đơn giản, không vẽ"). _Kiểm chứng_: đếm dòng, đối chiếu tên với `WORK_BREAKDOWN.md`.
- **AC-4 (DFD)**:
  - Có đủ ba mức: ngữ cảnh, mức 0, mức 1 (×3).
  - Mỗi kho dữ liệu D-x ghi rõ các bảng thật tương ứng.
  - Mỗi luồng dữ liệu có nhãn.
  - Không có tiến trình "hố đen" (chỉ có vào) hay "phép màu" (chỉ có ra).
  - _Kiểm chứng_: checklist cân bằng mức 0 ↔ mức 1 ghi trong file.
- **AC-5 (Luồng hoạt động)**: 8 sơ đồ hoạt động có làn theo vai trò, điểm quyết định và trạng thái kết thúc. Luồng sửa chữa khớp sơ đồ trạng thái trong `REPAIR_ORDER_LIFECYCLE.md`. _Kiểm chứng_: so từng chuyển trạng thái `RepairOrderStatus` / `EstimateStatus` / `InvoiceStatus` với sơ đồ.
- **AC-6 (ERD Chen phủ đủ schema)**:
  - Mỗi bảng trong V1–V19 (34 bảng) xuất hiện đúng một lần như thực thể trong các sơ đồ phân hệ.
  - Mỗi khóa ngoại xuất hiện như một quan hệ có bản số.
  - Hai view là thuộc tính hoặc thực thể suy diễn.
  - Bảng chi tiết (`*_items`, `labor_sessions`…) được đánh giá là thực thể yếu hay thực thể liên kết, kèm lý do.
  - _Kiểm chứng_: script đối chiếu danh sách bảng và FK trích từ migration với tên trong file `.puml` (Step 3, Step 8).
- **AC-7 (Render được)**: mọi file `.puml` render không lỗi bằng PlantUML (cú pháp `@startchen` cho Chen, `@startuml` cho hoạt động), và mọi khối Mermaid render được trên GitHub. _Kiểm chứng_: lệnh render ở Step 9, hoặc mở preview.
- **AC-8 (Từ điển dữ liệu)**: `DATA_DICTIONARY.md` mô tả 34 bảng theo V19, cột và ràng buộc khớp migration. Ma trận CRUD phủ mọi bảng. _Kiểm chứng_: script đếm bảng và cột so với migration.
- **AC-9 (Phát hiện)**: các chỗ nghiệp vụ và schema lệch nhau được liệt kê kèm đề xuất, không sửa. Ví dụ: `DATABASE.md` mục 12 vẫn ghi "hóa đơn duy nhất mỗi phiếu" dù V19 đã đổi. _Kiểm chứng_: mục "Phát hiện" tồn tại; `DATABASE.md` được cập nhật các câu đã lỗi thời.

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `docs/source/` (**mới, tùy chọn**) | Chỗ đặt báo cáo `.docx` gốc nếu người dùng cung cấp; trích văn bản sang `docs/source/bao-cao-goc.md` để dẫn nguồn |
| `docs/analysis/01-mo-ta-bai-toan.md` (**mới**) | Bối cảnh, mục tiêu, phạm vi, tác nhân, yêu cầu chức năng/phi chức năng, nguồn |
| `docs/analysis/02-nghiep-vu.md` (**mới**) | BR01–BR24 kèm nơi thực thi; UC01–UC38 theo phân hệ; ma trận UC × tác nhân |
| `docs/analysis/03-luong-du-lieu.md` (**mới**) | DFD ngữ cảnh, mức 0, mức 1 (sửa chữa, kho, thanh toán) dạng Mermaid; bảng kho dữ liệu ↔ bảng; checklist cân bằng |
| `docs/analysis/04-luong-hoat-dong.md` (**mới**) | 8 sơ đồ hoạt động; nhúng ảnh hoặc liên kết `.puml` |
| `docs/analysis/activity/*.puml` (**mới**) | Mã nguồn 8 sơ đồ hoạt động có làn (PlantUML) |
| `docs/analysis/05-ma-tran-crud.md` (**mới**) | Ma trận tiến trình/UC × 34 bảng (C/R/U/D) |
| `docs/analysis/06-phat-hien.md` (**mới**) | Chỗ lệch nghiệp vụ ↔ schema ↔ tài liệu, đề xuất xử lý |
| `docs/erd/00-tong-quan.puml` (**mới**) | Chen tổng quan: 34 thực thể + quan hệ, không thuộc tính |
| `docs/erd/01-dinh-danh-khach-xe.puml` | users, customers, vehicles, notifications, audit_events |
| `docs/erd/02-danh-muc-lich-hen.puml` | services, bookings, service_bays, bay_reservations |
| `docs/erd/03-sua-chua.puml` | repair_orders, vehicle_checks, attachments, estimates, estimate_items, repair_tasks, task_assignments, labor_sessions, repair_order_assignments |
| `docs/erd/04-kho.puml` | suppliers, parts, repair_order_parts, inventory_documents, inventory_document_items, inventory_transactions |
| `docs/erd/05-hoa-don-thanh-toan.puml` | invoices, invoice_items, payments (+ view `v_invoice_balances`, `v_supplier_balances` là suy diễn) |
| `docs/erd/06-bao-hanh.puml` | warranties, warranty_claims |
| `docs/erd/07-nhan-su-luong.puml` | employees, employee_shifts, payroll_periods, payroll_items, order_commissions |
| `docs/erd/README.md` (**mới**) | Quy ước ký hiệu Chen, cách render, danh sách sơ đồ |
| `docs/DATA_DICTIONARY.md` | Bổ sung 4 bảng và các cột V9–V19 |
| `docs/DATABASE.md` | Cập nhật số bảng/FK, mục 5 (booking service tùy chọn từ V11) và mục 12 (hóa đơn, V19); thêm danh sách V9–V19 |
| `scripts/analysis/schema_inventory.py` (**mới**) | Trích bảng, cột, FK từ migration để đối chiếu AC-6/AC-8 (chỉ đọc file SQL) |
| `README.md` | Thêm liên kết tới `docs/analysis/` và `docs/erd/` |

## 4. Hợp Đồng Kỹ Thuật

- **Nguồn sự thật**: migration Flyway V1–V19 cho schema; enum Java (`RepairOrderStatus`, `EstimateStatus`, `InvoiceStatus`, `BookingStatus`, `TaskStatus`, `WarrantyStatus`, `ClaimStatus`, `Role`…) cho trạng thái; service/controller cho nghiệp vụ và phân quyền. Tài liệu cũ chỉ dùng khi không mâu thuẫn với code.
- **Ký hiệu Chen** (PlantUML `@startchen`, cần PlantUML ≥ 1.2023.x):
  - `entity X {}` cho thực thể; `entity X <<weak>>` cho thực thể yếu.
  - `relationship R {}` cho quan hệ; `<<identifying>>` cho quan hệ xác định của thực thể yếu.
  - Thuộc tính khóa dùng `<<key>>`; thuộc tính suy diễn dùng `<<derived>>`; thuộc tính đa trị dùng `<<multi>>`.
  - Bản số ghi dạng `X -(1,N)- R`, đọc theo kiểu min,max.
  - Tên thực thể và quan hệ dùng tiếng Việt có dấu, kèm tên bảng trong ngoặc. Ví dụ: `entity "Phiếu sửa chữa (repair_orders)"`.
  - Mỗi sơ đồ phân hệ chỉ vẽ khóa, thuộc tính nghiệp vụ chính và trạng thái. Cột kỹ thuật (`version`, `created_by`, `updated_at`) gom vào ghi chú cuối sơ đồ.
  - Thực thể thuộc phân hệ khác chỉ vẽ tên, không thuộc tính, để thể hiện quan hệ liên phân hệ.
- **DFD** dùng Mermaid `flowchart`, vì GitHub render được:
  - Tiến trình: `((P1 Tên))`.
  - Kho dữ liệu: `[(D1 Tên)]`.
  - Tác nhân ngoài: `[Tên]`.
  - Nhãn luồng ghi trên mũi tên.
  - Đánh số P1…Pn ở mức 0 và P3.1… ở mức 1.
- **Sơ đồ hoạt động**: PlantUML `@startuml` có làn (`|Lễ tân|`, `|KTV|`, `|Khách|`, `|Hệ thống|`), nhánh `if/else`, `fork` khi song song. Trạng thái phiếu ghi trong ghi chú tại bước gây chuyển trạng thái.
- **Render (Step 9)**: ưu tiên `docker run --rm -v <docs>:/data plantuml/plantuml -tsvg /data/erd/*.puml` (không cài thêm vào máy). Nếu không pull được image thì giữ mã nguồn và ghi rõ chưa render.

## 5. Checklist Thực Thi

> **Gợi ý chia agent (mục 6 `AGENTS.md`)**:
> - Step 3–4 (ERD) và Step 5–6 (DFD, hoạt động) độc lập, có thể giao hai agent song song. Agent ERD chỉ đọc migration; agent luồng đọc service/controller.
> - Mọi agent chỉ ghi file của mình. `DATABASE.md` và `README.md` để agent điều phối sửa ở Step 10.

- [x] **Step 1: Thu thập nguồn bài toán** _(AC-1)_
  - Nếu người dùng đặt báo cáo `.docx` vào `docs/source/`, trích văn bản bằng `python -I` (thư viện `python-docx` hoặc giải nén `word/document.xml`) sang `docs/source/bao-cao-goc.md`.
  - Nếu không có, ghi rõ trong tài liệu: mô tả bài toán được dựng lại từ repo.
- [x] **Step 2: Viết `01-mo-ta-bai-toan.md`** _(AC-1)_
  - Bối cảnh và mục tiêu.
  - Phạm vi: một chi nhánh, có/không có những gì.
  - Tác nhân lấy theo enum `Role`.
  - Yêu cầu chức năng theo phân hệ; yêu cầu phi chức năng: bảo mật Keycloak/RBAC, audit, toàn vẹn tiền tệ, UTC, idempotency.
  - Mục "Nguồn".
- [x] **Step 3: Viết `scripts/analysis/schema_inventory.py` và chạy** _(AC-6, AC-8)_
  - Đọc V1–V19 theo thứ tự `sort -V`.
  - Gộp `CREATE TABLE` / `ALTER TABLE ADD/DROP`.
  - Xuất danh sách bảng, cột, PK, FK (bảng nguồn → bảng đích), UNIQUE, CHECK ra `docs/erd/schema-inventory.md`.
  - Đây là đầu vào cho Step 4 và Step 7.
- [x] **Step 4: Vẽ 7 sơ đồ Chen phân hệ và 1 sơ đồ tổng quan** _(AC-6)_
  - Theo nhóm bảng ở mục 3.
  - Mỗi FK là một quan hệ có bản số. Min suy từ NOT NULL / NULL; max suy từ UNIQUE (1) hoặc không (N).
  - Ghi thực thể yếu và lý do.
  - Thêm `docs/erd/README.md`.
- [x] **Step 5: Viết `02-nghiep-vu.md`** _(AC-2, AC-3)_
  - Gom BR01–BR24 từ tài liệu và comment; xác định nơi thực thi bằng grep có chọn lọc theo mã BR và theo các guard trong service.
  - Lập bảng UC01–UC38 theo phân hệ; lập ma trận UC × tác nhân từ `@PreAuthorize`.
- [x] **Step 6: Viết `03-luong-du-lieu.md`** _(AC-4)_
  - DFD ngữ cảnh, mức 0 (P1 Quản lý tài khoản & danh mục … P10 Báo cáo), mức 1 cho P-Sửa chữa, P-Kho, P-Thanh toán.
  - Bảng kho dữ liệu ↔ bảng thật.
  - Checklist cân bằng.
- [x] **Step 7: Viết `04-luong-hoat-dong.md` và 8 file `activity/*.puml`** _(AC-5)_
  - Luồng sửa chữa dựa trên `REPAIR_ORDER_LIFECYCLE.md`.
  - Luồng hóa đơn có nhánh hủy rồi lập lại (V19).
  - Luồng bảo hành có hủy sổ/điều chỉnh (V18).
  - Luồng lương có ca, chấm công, hoa hồng (V12–V17).
- [x] **Step 8: Ma trận CRUD và từ điển dữ liệu** _(AC-8)_
  - Viết `05-ma-tran-crud.md` từ repository/service: tiến trình nào ghi hay đọc bảng nào.
  - Cập nhật `DATA_DICTIONARY.md` lên 34 bảng.
  - Chạy lại script Step 3 để đối chiếu.
- [x] **Step 9: Render và kiểm tra sơ đồ** _(AC-6, AC-7)_
  - Render `.puml` sang SVG (`docs/erd/svg/`, `docs/analysis/activity/svg/`) bằng image Docker PlantUML.
  - Kiểm tra không lỗi cú pháp.
  - Script đối chiếu: 34/34 bảng có trong ERD, mọi FK có quan hệ.
- [x] **Step 10: Phát hiện, cập nhật tài liệu cũ và nghiệm thu** _(AC-9, tất cả AC)_
  - Viết `06-phat-hien.md`.
  - Sửa câu lỗi thời trong `DATABASE.md` (số bảng/FK, mục 5, mục 12).
  - Thêm liên kết vào `README.md`.
  - Chạy reviewer (chỉ đọc) trên toàn bộ diff tài liệu.
  - Đối chiếu từng AC rồi `/converge`.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

**Ghi chú khi thực hiện (CODE):**
- ERD chia thành 10 file, khác danh sách ở mục 3. Lý do là bám đúng 7 nhóm của báo cáo gốc:
  - `00-tong-quan`, `01`–`07` theo 7 nhóm báo cáo.
  - `08-luong-hoa-hong` cho các bảng V12–V17.
  - `09-nguoi-thuc-hien` gom 20 khóa ngoại kiểu `created_by` trỏ về `users`, để các nhóm không rối.
- Kết quả đối chiếu (`schema_inventory.py --check`): **34/34 bảng, 98/98 khóa ngoại**.
- 10 ERD và 8 sơ đồ hoạt động render bằng `plantuml/plantuml` 1.2026.9, không lỗi cú pháp.
- **Chưa xác minh:** các khối Mermaid DFD chưa render bằng công cụ, chỉ viết theo cú pháp chuẩn. Cần mở `docs/analysis/03-luong-du-lieu.md` trên GitHub hoặc VS Code để xác nhận AC-7.
- Chưa chạy reviewer và `/converge`.

# Vòng Đời Phiếu Sửa Chữa (Luồng Cốt Lõi)

Tài liệu này mô tả luồng chính của AutoGarage: **tiếp nhận xe → chẩn đoán → báo giá → khách duyệt → sửa chữa → QC → hóa đơn → bàn giao → bảo hành**.

Mỗi quy tắc được gắn một trong hai nhãn:

- **✅ Đã chặn**: code đã làm đúng. Có dẫn `file:dòng` (đường dẫn tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`).
- **🔒 Còn thiếu**: chưa chặn (hiện không còn mục nào).

Nguyên tắc chung là **chốt theo giai đoạn**: dữ liệu của một giai đoạn chỉ sửa được khi phiếu còn ở giai đoạn đó. Khi phiếu đã sang giai đoạn sau, dữ liệu cũ thành hồ sơ chỉ đọc. Muốn đổi thì phải tạo một bản ghi mới (báo giá phát sinh, biên bản QC mới, hóa đơn mới…), không được sửa đè bản cũ.

---

## 1. Sơ đồ trạng thái phiếu (`RepairOrderStatus`)

```
                 ┌──────────── CANCELLED (chỉ từ NEW / DIAGNOSING / WAITING_APPROVAL)
                 │
NEW ──► DIAGNOSING ──► WAITING_APPROVAL ──► APPROVED ──► IN_PROGRESS ◄──► WAITING_PARTS
 (tiếp nhận)  (chẩn đoán)  (đã lập báo giá)  (khách duyệt ≥1 dòng)  │
                 ▲               │                                 ▼ (mọi công việc COMPLETED)
                 └── mở lại ─────┘                             WAITING_QC ──► QC_FAILED ──► IN_PROGRESS
                     chẩn đoán                                     │ PASSED
                                                                   ▼
                                                 COMPLETED ──► INVOICED ──► CLOSED (bàn giao)
```

| Trạng thái | Nghĩa | Ai/việc gì đưa phiếu vào trạng thái này |
|---|---|---|
| `NEW` | Xe vừa vào xưởng | Lễ tân bấm "Tiếp nhận xe" (`ReceptionServiceImpl.checkInVehicle` → `RepairServiceImpl.receive`, dòng 128) |
| `DIAGNOSING` | KTV đang chẩn đoán | KTV bấm "Bắt đầu chẩn đoán", hoặc lưu chẩn đoán lần đầu (`RepairServiceImpl.updateDiagnosis`, dòng 533) |
| `WAITING_APPROVAL` | Đã có báo giá, chờ khách quyết định | Tự động khi lập báo giá đầu tiên (`EstimateServiceImpl.createEstimate`, dòng 204) |
| `APPROVED` | Khách đã duyệt ít nhất 1 dòng | Tự động trong `EstimateServiceImpl.decide` (dòng 442) |
| `IN_PROGRESS` | Đang thi công | KTV bấm "Bắt đầu sửa chữa", hoặc phân công công việc đầu tiên (`LaborExecutionServiceImpl`, dòng 145) |
| `WAITING_PARTS` | Tạm dừng vì chờ phụ tùng | Chuyển tay từ `APPROVED` hoặc `IN_PROGRESS` |
| `WAITING_QC` | Thi công xong, chờ kiểm tra | Tự động khi công việc cuối cùng hoàn thành (`LaborExecutionServiceImpl`, dòng 356), hoặc chuyển tay |
| `QC_FAILED` | QC không đạt, các công việc lỗi chuyển sang `REWORK` | `QualityServiceImpl` (dòng 150) |
| `COMPLETED` | QC đạt | `QualityServiceImpl` (dòng 146) |
| `INVOICED` | Đã phát hành hóa đơn | `BillingServiceImpl.issueInvoice` (dòng 318) |
| `CLOSED` | Đã bàn giao xe, sổ bảo hành đã được tạo | `RepairServiceImpl.handOver` (dòng 352) |
| `CANCELLED` | Hủy phiếu | Chuyển tay, chỉ khi chưa thi công (`RepairServiceImpl`, dòng 562–573) |

**Các trạng thái chỉ được đặt qua nghiệp vụ, không chuyển tay được:** `APPROVED`, `QC_FAILED`, `COMPLETED`, `INVOICED`, `CLOSED` ✅ (`RepairServiceImpl`, dòng 558).

**Ràng buộc chuyển tay** ✅ (`RepairServiceImpl.validateTransition` / `transitionStatus`):

- Không chuyển tay sang `WAITING_APPROVAL`; chỉ việc lập báo giá mới đưa phiếu sang đó.
- `IN_PROGRESS → WAITING_QC` bị chặn khi còn công việc chưa `COMPLETED` hoặc đồng hồ đang chạy.
- `WAITING_APPROVAL → DIAGNOSING` là lối quay lại duy nhất (xem 2.2).
- Hủy phiếu thì các báo giá `DRAFT` / `SENT` / `PARTIALLY_APPROVED` chuyển `CANCELLED`.

---

## 2. Từng giai đoạn: dữ liệu tạo ra và khi nào bị chốt

### 2.1. Tiếp nhận (`NEW`)

**Dữ liệu tạo ra:**

- Phiếu sửa chữa: mã phiếu, khách, xe, `odometerIn`, mức nhiên liệu, lời khách kể (`customerComplaint`).
- Lịch hẹn chuyển sang `RECEIVED`.
- Biên bản `CHECK_IN`: ngoại thất, nội thất, đèn cảnh báo.
- Ảnh `CHECK_IN`.
- KTV chẩn đoán và giữ khoang trong 12 giờ.

**Quy tắc:**

| Quy tắc | Trạng thái |
|---|---|
| ODO vào không nhỏ hơn ODO đã ghi nhận của xe; xe và khách phải đang hoạt động; lịch hẹn khớp xe và khách, chưa được tiếp nhận | ✅ `RepairServiceImpl`, dòng 133–168 |
| Biên bản `CHECK_IN` chỉ ghi khi phiếu là `NEW` | ✅ `VehicleCheckServiceImpl`, dòng 60 |
| Mức nhiên liệu và lời khách kể khi tiếp nhận chỉ ghi khi phiếu là `NEW` | ✅ `RepairServiceImpl.recordReceptionDetails`, dòng 219 |
| `PUT /repair-orders/{id}` chỉ sửa nhiên liệu và lời khách kể khi `NEW`; KTV trưởng đổi được tới trước `COMPLETED` | ✅ `RepairServiceImpl.updateRepairOrder` + `repair/service/RepairStageRules.java` |
| Ảnh `CHECK_IN` chỉ tải lên khi `NEW` | ✅ `AttachmentServiceImpl` dùng `RepairStageRules.attachmentWindow` |

### 2.2. Chẩn đoán (`NEW` / `DIAGNOSING`)

**Dữ liệu tạo ra:**

- Kết luận chẩn đoán (`diagnosis`).
- Ảnh `DIAGNOSIS`.
- Nhật ký công đoạn `DIAGNOSIS` của KTV.

**Điểm chốt:** khi lập báo giá đầu tiên, phiếu sang `WAITING_APPROVAL`. Từ lúc này, **kết luận chẩn đoán là căn cứ của báo giá, không được sửa nữa**.

| Quy tắc | Trạng thái |
|---|---|
| Chỉ sửa chẩn đoán khi `NEW` / `DIAGNOSING`; giao diện hiện "Đã chốt" kèm lý do | ✅ `updateDiagnosis` + `frontend/src/features/repair/utils/stageLocks.ts` |
| Nhật ký chẩn đoán chỉ "hoàn tất" ở lần lưu đầu | ✅ `updateDiagnosis` |
| Phải có kết luận chẩn đoán trước khi lập báo giá ban đầu | ✅ `EstimateServiceImpl.createEstimate` |
| Ảnh `DIAGNOSIS` chỉ tải lên khi `NEW` / `DIAGNOSING` | ✅ `AttachmentServiceImpl` |
| **Mở lại chẩn đoán** (`WAITING_APPROVAL → DIAGNOSING`) khi khách từ chối toàn bộ: không còn báo giá được duyệt hay đang chờ (chưa hết hạn); các bản nháp bị hủy | ✅ `RepairServiceImpl.reopenDiagnosis`, nút "Mở lại chẩn đoán" có hộp xác nhận |

### 2.3. Báo giá (`WAITING_APPROVAL`)

**Vòng đời một báo giá (`EstimateStatus`):**

```
DRAFT ──gửi──► SENT ──khách duyệt từng dòng──► PARTIALLY_APPROVED ──► APPROVED / REJECTED
  │              │
  └──────────────┴──► CANCELLED (bị phiên bản mới thay thế, mở lại chẩn đoán, hoặc phiếu bị hủy)
                 └──► EXPIRED (chỉ hiển thị: SENT quá validUntil, dữ liệu DB giữ SENT)
```

| Quy tắc | Trạng thái |
|---|---|
| Báo giá **không có API sửa**. Muốn đổi thì lập phiên bản mới (`versionNo` tăng dần, tính dưới khóa phiếu) | ✅ `EstimateServiceImpl`, dòng 112–115 |
| Chỉ gửi được báo giá `DRAFT` | ✅ dòng 280 |
| Không duyệt được báo giá `DRAFT`, `REJECTED`, `EXPIRED`, `CANCELLED`, báo giá quá `validUntil`, hay báo giá của phiếu đã kết thúc | ✅ dòng 339–353 |
| **Không đảo được quyết định** của một dòng đã duyệt hoặc đã từ chối; phải lập báo giá phát sinh | ✅ dòng 365–367 |
| Dòng dịch vụ được duyệt sinh công việc `PENDING`; dòng phụ tùng được duyệt giữ hàng trong kho | ✅ dòng 383–411 |
| Báo giá **ban đầu (`INITIAL`)** chỉ lập khi phiếu còn `NEW` / `DIAGNOSING` / `WAITING_APPROVAL` và chưa có dòng nào được duyệt | ✅ `EstimateServiceImpl.createEstimate` |
| Lập phiên bản `INITIAL` mới thì các bản `DRAFT` / `SENT` cũ chuyển `CANCELLED` | ✅ `EstimateServiceImpl.createEstimate` |
| Nút duyệt dòng chỉ hiện với báo giá `SENT` / `PARTIALLY_APPROVED` | ✅ `stageLocks.canDecideOn` |
| Báo giá `SENT` quá hạn hiển thị là `EXPIRED` | ✅ `EstimateMapper.markTimeExpired` |

### 2.4. Thi công (`APPROVED` → `IN_PROGRESS` ⇄ `WAITING_PARTS`)

**Điểm chốt:** khi khách duyệt dòng đầu tiên, **báo giá ban đầu và chẩn đoán bị khóa vĩnh viễn**. Mọi thay đổi về phạm vi hoặc giá chỉ đi qua **báo giá phát sinh (`SUPPLEMENTAL`)**, có `parentEstimateId` trỏ về báo giá gốc.

| Quy tắc | Trạng thái |
|---|---|
| Không tạo công việc tay; công việc chỉ sinh từ dòng dịch vụ đã duyệt | ✅ `RepairServiceImpl.createTask`, dòng 608 |
| Không sửa số lượng hay đơn giá của công việc sinh từ báo giá | ✅ dòng 627 |
| Không xóa công việc sinh từ báo giá, công việc đang làm hoặc đã xong | ✅ dòng 647–652 |
| Trạng thái công việc chỉ đổi qua bấm giờ hoặc hoàn thành | ✅ dòng 624 |
| Báo giá phát sinh chỉ lập và chỉ duyệt khi phiếu `APPROVED` / `IN_PROGRESS` / `WAITING_PARTS` / `QC_FAILED`; báo giá gốc phải đã được duyệt | ✅ `EstimateServiceImpl.createEstimate` và `decide` |
| Không sửa công việc đã `COMPLETED` | ✅ `RepairServiceImpl.updateTask` |
| Giao diện: sau khi đã duyệt, nút đổi thành "Lập báo giá phát sinh" và tự gắn báo giá gốc; trang Báo giá tự xác định loại theo phiếu | ✅ `stageLocks.newEstimateMode`, `CreateEstimateModal`, `EstimateEditor` |

### 2.5. QC (`WAITING_QC` → `COMPLETED` / `QC_FAILED`)

| Quy tắc | Trạng thái |
|---|---|
| QC chỉ làm khi `WAITING_QC`, mọi công việc đã `COMPLETED` và không còn đồng hồ chạy | ✅ `QualityServiceImpl`, dòng 71–82 |
| Người QC phải đang trong ca và không phải thợ chính của phiếu | ✅ dòng 94–100 |
| Kỹ thuật viên QC của phiếu `WAITING_QC` được phân công ngay tại tab QC (admin, lễ tân): `POST/GET /repair-orders/{id}/qc-assignment`. Chỉ chọn được người có `canQc`, đang trong ca, không phải thợ chính; đổi người thì phân công cũ được giải phóng, phiếu luôn có tối đa một người QC đang hoạt động | ✅ `LaborExecutionServiceImpl.assignQcInspector` |
| Danh sách người QC (`GET /repair-orders/{id}/qc-candidates`) luôn theo ca **đang trực lúc này**, không theo ngày nhập xe, kể cả với lệnh cũ. Người có ca gắn khoang chọn được bình thường; thợ chính của phiếu vẫn hiện nhưng bị khóa kèm lý do `MAIN_REPAIRER` | ✅ `LaborExecutionServiceImpl.getQcCandidates` |
| Admin được ghi QC thay kỹ thuật viên QC đã phân công: người kiểm định, nhật ký công và thưởng QC tính cho kỹ thuật viên đó, biên bản lưu `checkedBy` là admin. Phiếu chưa phân công QC (`QC_INSPECTOR_NOT_ASSIGNED`) hoặc có nhiều hơn một người, hoặc người đó không đủ điều kiện (`QC_INSPECTOR_INVALID`) thì bị từ chối | ✅ `QualityServiceImpl.resolveInspector` |
| Mỗi lần QC tạo một biên bản mới, không sửa biên bản cũ; `latestQcCheckId` trỏ biên bản mới nhất | ✅ |
| `IN_PROGRESS → WAITING_QC` (chuyển tay) bị chặn khi còn việc chưa xong hoặc đồng hồ chạy | ✅ `RepairServiceImpl.transitionStatus` |
| Ảnh `QC` chỉ tải lên khi `WAITING_QC` / `QC_FAILED` / `COMPLETED` | ✅ `AttachmentServiceImpl` |

### 2.6. Hóa đơn & bàn giao (`COMPLETED` → `INVOICED` → `CLOSED`)

| Quy tắc | Trạng thái |
|---|---|
| Hóa đơn chỉ phát hành khi QC gần nhất đạt, mọi công việc xong, không còn đồng hồ chạy; mỗi phiếu một hóa đơn | ✅ `BillingServiceImpl`, dòng 110–147 |
| Hóa đơn chụp lại thông tin khách tại thời điểm phát hành (sửa khách sau đó không đổi hóa đơn) | ✅ dòng 148+ |
| Bàn giao: ODO ra ≥ ODO vào; hóa đơn đã thanh toán đủ hoặc nằm trong hạn mức công nợ; giải phóng khoang và lượng giữ kho; tạo sổ bảo hành | ✅ `RepairServiceImpl.handOver`, dòng 352–431 |
| Bắt đầu công việc (`startTimer`) chỉ được khi phiếu đã duyệt báo giá (`APPROVED`, `IN_PROGRESS`, `QC_FAILED`) và mọi phụ tùng đã giữ chỗ của phiếu đã xuất kho; ngược lại trả HTTP 409, `code = 2026` (`WORK_START_BLOCKED`) nêu tên phụ tùng. Phân công việc vẫn làm được trước khi xuất kho | ✅ `LaborExecutionServiceImpl.requireWorkStartAllowed`, `InventoryServiceImpl.requireIssuedForWorkStart` |
| Hoàn thành công việc sửa chữa (`completeTask`) và chuyển phiếu sang `WAITING_QC` bị từ chối khi còn phụ tùng giữ chỗ chưa xuất kho (bước QC còn chặn cả phiếu kho nháp): HTTP 409, `code = 2028` (`PARTS_PENDING_ISSUE`), lý do nêu đúng bước đang làm | ✅ `InventoryServiceImpl.requireIssuedBeforeCompletion`, `requireIssuedBeforeQc` |
| Điều kiện thanh toán khi bàn giao: khách không có hạn mức nợ phải thu đủ hóa đơn; có hạn mức thì tổng nợ sau hóa đơn không được vượt hạn mức, tức phải thu tối thiểu `nợ − hạn mức` (nợ không vượt hạn mức thì chưa cần trả). Khách có hóa đơn quá hạn luôn bị chặn. `GET /billing/invoices/order/{id}/handover-readiness` trả công nợ, hạn mức và số cần thu tối thiểu; thông điệp 409 `HANDOVER_BLOCKED` ghi rõ số tiền | ✅ `BillingServiceImpl.evaluateHandover` |
| Thêm bảo hành trước bàn giao: `PATCH /billing/invoices/{id}/items/{itemId}/warranty` (ADMIN/lễ tân) đặt số ngày, số km và điều khoản cho dòng hóa đơn chưa có sổ bảo hành, chỉ khi hóa đơn chưa hủy và phiếu chưa `CLOSED` (409 `WARRANTY_BLOCKED` = 2027), có ghi nhật ký `INVOICE_WARRANTY_SET`. Sổ bảo hành vẫn được tạo lúc bàn giao từ chính sách trên dòng | ✅ `BillingServiceImpl.updateInvoiceItemWarranty`, `HandoverModal.tsx` |
| Xuất kho phụ tùng cho lệnh: thủ kho/quản trị viên mở "Xuất kho sửa chữa", chọn lệnh theo mã hoặc biển số; `GET /inventory/documents/issue-candidates?repairOrderId=` liệt kê phụ tùng đã duyệt và còn giữ chỗ (kèm tồn kho, cờ thiếu hàng); chứng từ `OUT` bắt buộc mỗi dòng gắn dòng báo giá (`estimateItemId`) và không vượt số giữ chỗ; ghi sổ trừ tồn, trừ giữ chỗ và ghi `repair_order_parts`. Từ chối trả HTTP 409, `code = 2024` (`INVENTORY_ISSUE_BLOCKED`) kèm lý do tiếng Việt | ✅ `InventoryServiceImpl.getIssueCandidates`, `post` |
| Thu tiền hóa đơn (`POST /billing/payments/receive`) và chi trả nhà cung cấp (`pay-supplier`): mã giao dịch `transactionCode` tùy chọn, bỏ trống thì máy chủ tự sinh `PT-yyyyMMdd-NNNN` (thu) hoặc `PC-yyyyMMdd-NNNN` (chi), gợi ý trước bằng `GET /billing/payments/suggest-code?direction=IN|OUT`; chỉ gồm chữ, số và `- _ .` (tối đa 50 ký tự), tự đổi sang chữ in hoa, trùng thì 409. Ô nhập số tiền không dùng `step` (trình duyệt tính bước từ `min` nên chặn số tròn), có nút "Thu đủ số còn lại". Từ chối trả HTTP 409, `code = 2025` (`PAYMENT_BLOCKED`) kèm lý do tiếng Việt: hóa đơn đã hủy, đã thu đủ, vượt số còn nợ, mã tham chiếu hoặc mã giao dịch trùng | ✅ `BillingServiceImpl.receivePayment`, `paySupplier`, `suggestTransactionCode` |
| Lập hóa đơn bị từ chối trả HTTP 409, `code = 2023` (`INVOICE_BLOCKED`) kèm lý do tiếng Việt riêng: còn phụ tùng đã giữ chỗ nhưng chưa xuất kho (nêu tên phụ tùng), còn phiếu kho nháp, chưa có QC hoặc QC chưa đạt, còn đồng hồ đang chạy, không có hạng mục tính tiền. `GET /billing/invoices/order/{orderId}/readiness` trả `ready` và `reason` theo đúng quy tắc đó; hộp phát hành hóa đơn dùng nó để báo trước và khóa nút | ✅ `BillingServiceImpl.requireIssueReady`, `InventoryServiceImpl.requireIssueComplete` |
| Bàn giao bị từ chối trả HTTP 409, `code = 2022` (`HANDOVER_BLOCKED`) kèm lý do tiếng Việt riêng: chưa có hóa đơn còn hiệu lực, hóa đơn đã hủy, công nợ vượt hạn mức, khách không được ghi công nợ, khách có hóa đơn quá hạn. Tab Bàn giao hiện trạng thái hóa đơn và khóa nút khi chưa có hóa đơn | ✅ `BillingServiceImpl.requireHandoverAllowed` |
| **Hủy hóa đơn** (chỉ ADMIN, khi chưa thu tiền và xe chưa bàn giao): hóa đơn chuyển `VOID` nhưng được giữ lại, phiếu quay về `COMPLETED` và phát hành được hóa đơn mới | ✅ `BillingServiceImpl.voidInvoice`; migration V19 thay `uq_invoices_order` bằng ràng buộc duy nhất trên hóa đơn khác `VOID` (`uq_invoices_active_order`) |
| Phiếu `INVOICED` / `CLOSED` / `CANCELLED`: mọi dữ liệu là chỉ đọc | ✅ |

### 2.7. Bảo hành (sau `CLOSED`)

- **Sổ bảo hành:** tạo khi bàn giao, mỗi dòng hóa đơn có ngày > 0 hoặc km > 0 tạo một sổ. Chỉ quản trị viên được **điều chỉnh** hoặc **hủy sổ**, bắt buộc ghi lý do, và có ghi nhật ký.
- **Khiếu nại:** lễ tân mở; quản trị viên quyết định. Khiếu nại được duyệt tạo phiếu `WARRANTY` mới, phiếu này đi lại từ `NEW` theo đúng luồng trên.

---

## 3. Ma trận khóa theo giai đoạn

Ký hiệu: ✏️ sửa được · 🔒 chỉ đọc · ➕ chỉ thêm bản ghi mới.

| Dữ liệu \ Trạng thái phiếu | NEW | DIAGNOSING | WAITING_APPROVAL | APPROVED → WAITING_PARTS | WAITING_QC / QC_FAILED | COMPLETED | INVOICED / CLOSED / CANCELLED |
|---|---|---|---|---|---|---|---|
| Nhiên liệu, lời khách kể, biên bản và ảnh `CHECK_IN` | ✏️ | 🔒 | 🔒 | 🔒 | 🔒 | 🔒 | 🔒 |
| Kết luận và ảnh chẩn đoán | ✏️ | ✏️ | 🔒 (mở lại khi khách từ chối hết) | 🔒 | 🔒 | 🔒 | 🔒 |
| Báo giá ban đầu | ➕ | ➕ | ➕ phiên bản mới | 🔒 | 🔒 | 🔒 | 🔒 |
| Báo giá phát sinh | — | — | — | ➕ | ➕ chỉ khi `QC_FAILED` | 🔒 | 🔒 |
| Duyệt dòng báo giá (dòng `PENDING` của bản `SENT`) | — | — | ✏️ | ✏️ | ✏️ chỉ khi `QC_FAILED` | 🔒 | 🔒 |
| Công việc: phân công, bấm giờ, hoàn thành | — | — | — | ✏️ | ✏️ làm lại khi `QC_FAILED` | 🔒 | 🔒 |
| KTV trưởng | ✏️ | ✏️ | ✏️ | ✏️ | ✏️ | 🔒 | 🔒 |
| Biên bản QC, ảnh QC | — | — | — | — | ➕ | ➕ ảnh | 🔒 |
| Hóa đơn | — | — | — | — | — | ➕ (lập lại được sau khi hủy) | `INVOICED`: hủy được khi chưa thu tiền → quay lại `COMPLETED`; `CLOSED`: không hủy |

---

## 4. Quy tắc giao diện

- Trên tab của giai đoạn đã qua, hiện nhãn **"Đã chốt"**, kèm câu giải thích vì sao bị khóa. Ví dụ: "Đã lập báo giá BG-… ngày …; chẩn đoán được giữ nguyên làm căn cứ." Ô nhập chuyển sang chỉ đọc, ẩn nút Lưu.
- Ảnh chỉ cho chọn những loại còn mở ở giai đoạn hiện tại.
- Giao diện chỉ phản chiếu quy tắc. **Backend mới là nơi chặn thật**, giao diện không thay được việc đó.

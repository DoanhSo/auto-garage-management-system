# Kế Hoạch Chốt Dữ Liệu Theo Giai Đoạn Của Phiếu Sửa Chữa

> **Mã plan**: `repair-lifecycle-lock`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Đặc tả luồng**: [`docs/REPAIR_ORDER_LIFECYCLE.md`](REPAIR_ORDER_LIFECYCLE.md). Các mục 🔒 trong tài liệu đó là phạm vi của plan này.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

Luồng tiếp nhận → chẩn đoán → báo giá → thi công vẫn cho sửa dữ liệu của giai đoạn đã qua:

1. **Chẩn đoán sửa được tới tận `COMPLETED`.**
   - `repair/service/impl/RepairServiceImpl.java:524` chỉ chặn khi phiếu `CLOSED` / `CANCELLED` / `INVOICED`.
   - Mỗi lần lưu lại gọi `workLogRecorder.finish` (dòng 528).
   - Giao diện luôn cho sửa: `features/repair/components/RepairOrderDetailModal.tsx:455-476`.
2. **Nhiên liệu và lời khách kể sửa được qua `PUT /repair-orders/{id}`** tới `COMPLETED` (`RepairServiceImpl.java:500`), dù `recordReceptionDetails` chỉ cho phép khi `NEW`.
3. **Báo giá ban đầu lập được ở mọi trạng thái** trước `INVOICED` (`estimate/service/impl/EstimateServiceImpl.java:90`).
   - Có thể có hai báo giá ban đầu cùng chờ duyệt. Duyệt cả hai thì công việc bị nhân đôi.
   - Bản cũ không bị thay thế.
4. **Báo giá phát sinh lập và duyệt được khi phiếu đã `WAITING_QC` / `COMPLETED`.** Công việc `PENDING` mới sinh ra làm phiếu đã QC đạt không lập được hóa đơn (`EstimateServiceImpl.java:346`).
5. **Chuyển trạng thái tay có lỗ hổng** (`RepairServiceImpl.java:670-694`):
   - `NEW` / `DIAGNOSING → WAITING_APPROVAL` đi được mà không cần báo giá.
   - `IN_PROGRESS → WAITING_QC` đi được khi còn công việc chưa xong.
   - Không có đường hợp lệ để chẩn đoán lại khi khách từ chối toàn bộ báo giá.
6. **Ảnh đính kèm tải lên được theo mọi loại ở mọi giai đoạn** (`reception/service/AttachmentServiceImpl.java:70-90`).
7. **Giao diện lệch với backend:**
   - Nhân viên thấy nút duyệt trên báo giá `DRAFT` (`RepairOrderDetailModal.tsx:632-635`), bấm vào thì backend trả lỗi.
   - Nút "Lập báo giá mới" luôn tạo loại `INITIAL`.
8. **Hủy hóa đơn làm phiếu kẹt ở `INVOICED`**:
   - `voidInvoice` không đưa phiếu về lại (`billing/service/impl/BillingServiceImpl.java:390`).
   - `existsByRepairOrderId` vẫn đếm hóa đơn `VOID`, nên không phát hành lại được (dòng 113).
9. **`EstimateStatus.EXPIRED` / `CANCELLED` không bao giờ được đặt.** Báo giá quá hạn hoặc thuộc phiếu đã hủy vẫn hiện "Đã gửi".

### 1.2. Mục tiêu

- Mỗi loại dữ liệu chỉ sửa được trong giai đoạn của nó, đúng theo ma trận ở mục 3 của `REPAIR_ORDER_LIFECYCLE.md`. Backend chặn, giao diện hiện "Đã chốt" và giải thích lý do.
- Sau khi khách duyệt, mọi thay đổi chỉ đi qua báo giá phát sinh gắn với báo giá gốc.
- Có đúng một lối quay lại hợp lệ là **"Mở lại chẩn đoán"**, khi khách từ chối hết.
- Hủy hóa đơn chưa thu tiền thì phiếu quay về `COMPLETED` và phát hành lại được.

### 1.3. Ngoài phạm vi

- Không đổi tập trạng thái `RepairOrderStatus` / `EstimateStatus`, không thêm cột mới, nên không cần migration.
- Không làm job nền tự đổi báo giá sang `EXPIRED`. Chỉ hiển thị `EXPIRED` qua mapper, giống cách sổ bảo hành đang làm.
- Không đổi luồng bảo hành, công nợ, lương hay kho, ngoài lời gọi `releaseForOrder` có sẵn.
- Không viết lại `RepairOrderDetailModal` thành nhiều file. Chỉ sửa các vùng nêu trong mục 3.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1 (Chốt chẩn đoán)**: Phiếu ở `WAITING_APPROVAL` trở đi mà gọi `PATCH /repair-orders/{id}/diagnosis` thì nhận lỗi "Chẩn đoán đã chốt…". Ở `NEW` / `DIAGNOSING` thì lưu được, và nhật ký `DIAGNOSIS` chỉ hoàn tất ở lần lưu đầu. _Kiểm chứng_: `RepairOrderServiceTest`.
- **AC-2 (Chốt tiếp nhận)**: `PUT /repair-orders/{id}` có `fuelLevelPercent` / `customerComplaint` khi phiếu khác `NEW` thì bị từ chối. `leadTechnicianId` vẫn đổi được tới trước `COMPLETED`. Trường `diagnosis` trong request này bị bỏ qua như hiện nay. _Kiểm chứng_: `RepairOrderServiceTest`.
- **AC-3 (Báo giá ban đầu)**:
  - Lập báo giá `INITIAL` khi phiếu đã `APPROVED` trở đi, hoặc khi đã có dòng được duyệt, thì bị từ chối.
  - Lập `INITIAL` khi chưa có kết luận chẩn đoán thì bị từ chối.
  - Lập phiên bản `INITIAL` mới thì các bản `DRAFT` / `SENT` cũ không có dòng nào được duyệt chuyển `CANCELLED`.
  - _Kiểm chứng_: `EstimateServiceTest`.
- **AC-4 (Báo giá phát sinh)**:
  - Báo giá `SUPPLEMENTAL` chỉ lập và chỉ duyệt được khi phiếu `APPROVED` / `IN_PROGRESS` / `WAITING_PARTS` / `QC_FAILED`.
  - Báo giá gốc phải là `APPROVED` hoặc `PARTIALLY_APPROVED`.
  - Duyệt khi phiếu `WAITING_QC` / `COMPLETED` thì bị từ chối.
  - _Kiểm chứng_: `EstimateServiceTest`, `EstimateApprovalServiceTest`.
- **AC-5 (Chuyển trạng thái)**:
  - `PATCH /status` sang `WAITING_APPROVAL` bị từ chối; chỉ lập báo giá mới đưa phiếu sang đó.
  - `IN_PROGRESS → WAITING_QC` bị từ chối khi còn công việc chưa `COMPLETED` hoặc còn đồng hồ chạy.
  - `WAITING_APPROVAL → DIAGNOSING` chỉ được khi mọi báo giá là `REJECTED` / `CANCELLED`, hoặc là `DRAFT` (bản `DRAFT` khi đó chuyển `CANCELLED`).
  - _Kiểm chứng_: `RepairOrderServiceTest`.
- **AC-6 (Hủy phiếu)**: Hủy phiếu thì mọi báo giá `DRAFT` / `SENT` / `PARTIALLY_APPROVED` còn dòng chờ chuyển `CANCELLED`. _Kiểm chứng_: `RepairOrderServiceTest`.
- **AC-7 (Ảnh theo giai đoạn)**: Mỗi loại ảnh chỉ tải lên được ở giai đoạn của nó; ngoài giai đoạn đó thì bị từ chối kèm thông báo tiếng Việt. _Kiểm chứng_: `AttachmentServiceTest`.
  - `CHECK_IN`: chỉ khi `NEW`.
  - `DIAGNOSIS`: chỉ khi `NEW` / `DIAGNOSING`.
  - `QC`: chỉ khi `WAITING_QC` / `QC_FAILED` / `COMPLETED`.
  - `HANDOVER`: chỉ khi `COMPLETED` / `INVOICED` / `CLOSED`.
- **AC-8 (Báo giá quá hạn)**: Báo giá `SENT` / `PARTIALLY_APPROVED` đã quá `validUntil` được trả về với `status = EXPIRED`. Dữ liệu trong DB không đổi. _Kiểm chứng_: `EstimateMapperTest` (mới).
- **AC-9 (Hủy hóa đơn)**:
  - Hủy hóa đơn chưa thu tiền thì phiếu `INVOICED → COMPLETED`.
  - Sau đó phát hành hóa đơn mới cho phiếu đó được.
  - Hủy khi phiếu đã `CLOSED` thì bị từ chối.
  - _Kiểm chứng_: `BillingServiceTest`.
- **AC-10 (Giao diện)**: _Kiểm chứng_: `RepairOrderDetailModal.test.tsx` cùng thao tác trên trình duyệt (không lưu dữ liệu thật).
  - Ô chẩn đoán chuyển chỉ đọc kèm nhãn "Đã chốt" và lý do khi phiếu đã qua `DIAGNOSING`.
  - Nút "Mở lại chẩn đoán" chỉ hiện khi điều kiện ở AC-5 thỏa.
  - Nút duyệt dòng chỉ hiện cho báo giá `SENT` / `PARTIALLY_APPROVED`.
  - Sau khi duyệt, nút "Lập báo giá mới" đổi thành "Lập báo giá phát sinh" và tự gửi `estimateKind: SUPPLEMENTAL` cùng `parentEstimateId`.
  - Danh sách loại ảnh chỉ gồm các loại còn mở.
- **AC-11 (Không hồi quy)**: `./mvnw test`, `npm --prefix frontend run lint`, `test` và `build` đều xanh. _Kiểm chứng_: chạy lệnh.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `repair/workflow/RepairStageRules.java` (**mới**) | | Lớp tĩnh gom quy tắc giai đoạn: `RECEPTION_EDITABLE`, `DIAGNOSIS_EDITABLE`, `INITIAL_ESTIMATE_ALLOWED`, `SUPPLEMENTAL_ALLOWED`, `TERMINAL`, cùng `attachmentWindow(AttachmentCategory)`. Đặt cạnh `RepairWorkflow` nếu package đó đã có, nếu chưa thì để trong `repair/service` |
| `repair/service/impl/RepairServiceImpl.java` | 496–537, 541–578, 662–723 | Áp dụng AC-1, AC-2, AC-5, AC-6; thêm nhánh `WAITING_APPROVAL → DIAGNOSING`; chặn `→ WAITING_APPROVAL` ở cả `NEW` và `DIAGNOSING` |
| `estimate/repository/EstimateRepository.java` | | Thêm `findByRepairOrderIdAndStatusIn(Long, Collection<EstimateStatus>)` |
| `estimate/repository/EstimateItemRepository.java` | | Thêm `existsApprovedByRepairOrderId(Long)` (JPQL join qua `Estimate`) |
| `estimate/service/impl/EstimateServiceImpl.java` | 82–110, 339–353 | Áp dụng AC-3, AC-4 |
| `estimate/mapper/EstimateMapper.java` | | `@AfterMapping` để hiển thị `EXPIRED` (AC-8) |
| `reception/service/AttachmentServiceImpl.java` | 70–90 | Áp dụng AC-7, nạp trạng thái phiếu qua repository có sẵn |
| `billing/service/impl/BillingServiceImpl.java` | 110–114, 372–395 | Áp dụng AC-9: kiểm tra trùng chỉ với hóa đơn khác `VOID`; khi hủy thì cập nhật phiếu về `COMPLETED` trong cùng transaction |
| `billing/repository/InvoiceRepository.java` | 25 | Thêm `existsByRepairOrderIdAndStatusNot`; `findByRepairOrderId` ưu tiên hóa đơn khác `VOID` |
| `repair/service/impl/RepairServiceImpl.java` | 431 (handOver) | Lấy hóa đơn khác `VOID` khi tạo sổ bảo hành |
| `features/repair/utils/stageLocks.ts` (**mới**) | | Bản tương ứng của `RepairStageRules` ở frontend: `isDiagnosisLocked`, `canReopenDiagnosis`, `estimateKindFor`, `allowedAttachmentCategories`, `lockReason` |
| `features/repair/components/RepairOrderDetailModal.tsx` | 140–200, 268–310, 446–490, 496–560, 630–640 | Áp dụng AC-10 |
| `features/repair/components/CreateEstimateModal.tsx` | | Nhận prop `kind` và `parentEstimateId`; đổi tiêu đề theo loại |
| `features/repair/components/DiagnosisAttachmentGallery.tsx` | 80–125 | Nhận `allowedCategories`; ẩn ô tải lên khi danh sách rỗng |
| `features/repair/hooks/useRepair.ts` | | Thêm hook mở lại chẩn đoán, dùng lại `useTransitionStatusMutation` với `DIAGNOSING` |
| Test backend | | `RepairOrderServiceTest`, `EstimateServiceTest`, `EstimateApprovalServiceTest`, `AttachmentServiceTest`, `BillingServiceTest`, `EstimateMapperTest` (mới). Sửa các test hiện đang dựa vào chuyển tay sang `WAITING_APPROVAL` |
| Test frontend | | `features/repair/utils/stageLocks.test.ts` (mới), `RepairOrderDetailModal.test.tsx` |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | | Đổi các mục 🔒 đã làm thành ✅ |

## 4. Hợp Đồng Kỹ Thuật

- **Không thêm endpoint mới.** "Mở lại chẩn đoán" dùng `PATCH /api/v1/repair-orders/{id}/status` với `newStatus = DIAGNOSING`, giữ `@PreAuthorize(HAS_TECHNICAL_ACCESS)`.
- **Lỗi nghiệp vụ** dùng `BusinessRuleException` như các guard cùng file, để thống nhất thông điệp tiếng Việt và mã lỗi đang có. Thông điệp:
  - "Chẩn đoán đã chốt khi lập báo giá. Hãy dùng báo giá phát sinh hoặc mở lại chẩn đoán khi khách từ chối toàn bộ."
  - "Thông tin tiếp nhận chỉ sửa được khi phiếu vừa tiếp nhận."
  - "Phiếu đã có hạng mục được duyệt, chỉ lập được báo giá phát sinh."
  - "Cần ghi kết luận chẩn đoán trước khi lập báo giá."
  - "Báo giá phát sinh chỉ lập khi phiếu đang thi công hoặc QC chưa đạt."
  - "Báo giá gốc phải đã được khách duyệt."
  - "Chỉ chuyển sang chờ duyệt bằng cách lập báo giá."
  - "Còn công việc chưa hoàn thành hoặc đồng hồ đang chạy."
  - "Chỉ mở lại chẩn đoán khi khách đã từ chối toàn bộ báo giá."
  - "Ảnh <loại> chỉ tải lên ở giai đoạn <giai đoạn>."
  - Billing giữ `BusinessException(ResponseCode.BAD_REQUEST)` như hiện tại.
- **`RepairStageRules`** chỉ chứa `EnumSet` và hàm thuần, không phụ thuộc Spring. `EstimateServiceImpl` và `AttachmentServiceImpl` dùng chung nó, nên không còn chép tay danh sách trạng thái.
- **Audit:** "Mở lại chẩn đoán" đi qua `transitionStatus`, giữ nguyên audit có sẵn của endpoint status nếu có. Không thêm action mới.
- **UI:**
  - Dùng `Badge` (`variant="secondary"`) với chữ "Đã chốt", và `info-strip` để giải thích lý do.
  - Ô `textarea` dùng `readOnly`, không dùng `disabled`, để vẫn chọn và sao chép được chữ.
  - Nút "Mở lại chẩn đoán" là `Button variant="ghost"`, có hộp xác nhận vì thao tác này hủy các bản nháp.

## 5. Checklist Thực Thi

- [x] **Step 1: Thêm `RepairStageRules` và test thuần cho nó** _(nền cho AC-1 → AC-7)_
- [x] **Step 2: Chốt tiếp nhận và chẩn đoán trong `RepairServiceImpl`** (`updateRepairOrder`, `updateDiagnosis`: chỉ hoàn tất nhật ký ở lần đầu) _(AC-1, AC-2)_
- [x] **Step 3: Siết `validateTransition` / `transitionStatus`** _(AC-5, AC-6)_
  - Bỏ việc chuyển tay sang `WAITING_APPROVAL`.
  - Chặn `→ WAITING_QC` khi còn việc chưa xong.
  - Thêm `WAITING_APPROVAL → DIAGNOSING`, kèm hủy các bản `DRAFT`.
  - Khi hủy phiếu thì hủy luôn các báo giá còn mở.
- [x] **Step 4: Siết `EstimateServiceImpl.createEstimate` và `decide`, thêm repository query** _(AC-3, AC-4)_
- [x] **Step 5: `EstimateMapper` hiển thị `EXPIRED`** _(AC-8)_
- [x] **Step 6: Khóa ảnh theo giai đoạn trong `AttachmentServiceImpl`** _(AC-7)_
- [x] **Step 7 (làm sau theo yêu cầu, kèm migration V19 — ngoại lệ của mục 1.3): Hủy hóa đơn đưa phiếu về `COMPLETED` và cho phát hành lại; `handOver` lấy hóa đơn khác `VOID`** _(AC-9)_
- [x] **Step 8: Cập nhật test backend bị ảnh hưởng và chạy `./mvnw test`** _(AC-1 → AC-9, AC-11)_
- [x] **Step 9: Thêm `stageLocks.ts` cùng test** _(AC-10)_
- [x] **Step 10: Cập nhật `RepairOrderDetailModal`, `CreateEstimateModal`, `DiagnosisAttachmentGallery`** _(AC-10)_
  - "Đã chốt" và lý do.
  - Nút "Mở lại chẩn đoán".
  - Nút duyệt chỉ cho `SENT` / `PARTIALLY_APPROVED`.
  - Báo giá phát sinh.
  - Loại ảnh theo giai đoạn.
- [x] **Step 11: Kiểm thử và xác minh** _(tất cả AC)_
  - Chạy lint, test, build frontend.
  - Build lại container backend và frontend.
  - Mở trên trình duyệt một phiếu ở mỗi trạng thái `DIAGNOSING`, `WAITING_APPROVAL`, `IN_PROGRESS`, `CLOSED` để xem khóa. Không lưu dữ liệu thật nếu người dùng chưa đồng ý.
  - Đổi các mục 🔒 trong `docs/REPAIR_ORDER_LIFECYCLE.md` thành ✅.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

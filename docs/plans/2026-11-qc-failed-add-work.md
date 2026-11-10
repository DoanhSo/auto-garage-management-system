# Kế Hoạch Bổ Sung Công Việc Khi QC Không Đạt Qua Báo Giá Phát Sinh

> **Mã plan**: `qc-failed-add-work`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: plan trước `payment-code-row-lock` đã lưu tại `plans/2026-11-payment-code-row-lock.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
Khi lệnh sửa chữa ở `QC_FAILED`, kỹ thuật viên bấm **"Thêm công việc"** ở tab Công việc và nhận:
```json
{ "code": 2004, "message": "Request fields or JSON are invalid", "path": "/api/v1/repair-orders/7/tasks" }
```
Nguyên nhân gốc có hai lớp:

1. **Lỗi hợp đồng (gây 2004):** form gửi trường `taskType`
   (`frontend/src/features/repair/components/CreateTaskModal.tsx:54`, kiểu ở `repairApi.ts:84`, `useRepair.ts:113`),
   còn `RepairTaskCreateRequest` (`src/main/java/.../repair/dto/request/RepairTaskCreateRequest.java`) không có trường này.
   `application.yaml:26` bật `fail-on-unknown-properties: true` nên Jackson ném `HttpMessageNotReadableException`
   → `GlobalExceptionHandler.invalidInput` (dòng 95–104) trả `BAD_REQUEST` 2004 với message chung.
   Form cũng không gửi `unitPrice` thật (không có ô nhập, mặc định 0).
2. **Sai luồng nghiệp vụ:** dù sửa payload, `RepairServiceImpl.createTask` (dòng 626–637) **luôn** ném
   `BusinessRuleException("Công việc sửa chữa phải được tạo từ hạng mục dịch vụ đã duyệt trong báo giá")`.
   Đây là quy tắc có chủ đích (`docs/REPAIR_ORDER_LIFECYCLE.md` dòng 10, 119, 177): sau khi khách đã duyệt,
   mọi thay đổi phạm vi phải đi qua **báo giá phát sinh (`SUPPLEMENTAL`)**; hóa đơn tính tiền công từ
   `estimate_items` (`BillingServiceImpl` dòng 178–190, 352). Backend đã hỗ trợ đủ:
   `RepairStageRules.SUPPLEMENTAL_ALLOWED` gồm `QC_FAILED`; `EstimateServiceImpl.createEstimate` (dòng 96–101)
   và `decide` (dòng 369–370) cho phép ở `QC_FAILED`; dòng được duyệt tự sinh `RepairTask` (khoảng dòng 417).

Như vậy nút "Thêm công việc" + `CreateTaskModal` là **UI chết**: không bao giờ thành công, và dẫn người dùng sai luồng.

### 1.2. Mục tiêu
- Ở tab Công việc, khi cần thêm việc mới (đặc biệt sau QC không đạt), người dùng được dẫn vào **"Lập báo giá phát sinh"**
  (tái dùng `CreateEstimateModal` đã có, tự gắn báo giá gốc), không còn form tạo công việc thủ công.
- Không còn đường nào trên UI gọi `POST /repair-orders/{id}/tasks`, nên không còn lỗi 2004 này.
- Backend giữ quy tắc chặn, nhưng phản hồi khi bị gọi trực tiếp phải mang thông báo nghiệp vụ rõ ràng (không phải 2004 chung chung do payload lệch).

### 1.3. Ngoài phạm vi
- **Không** cho phép tạo công việc thủ công ngoài báo giá (trái quy tắc chốt theo giai đoạn và luồng hóa đơn).
  Nếu muốn đổi quy tắc này cần plan riêng (ảnh hưởng hóa đơn, hoa hồng, ERD).
- Không đổi `fail-on-unknown-properties` và không đổi cách `GlobalExceptionHandler` báo 2004 cho JSON lỗi.
- Không xóa endpoint `POST /api/v1/repair-orders/{id}/tasks` và `RepairService.createTask` (giữ hợp đồng API, tránh lan rộng).
- Không đổi luồng QC (`QualityServiceImpl`) và việc chuyển task sang `REWORK`.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Lệnh ở `QC_FAILED` (đã có báo giá duyệt), người dùng có quyền kỹ thuật mở tab **Công việc** thấy nút
  **"Lập báo giá phát sinh"** thay cho "Thêm công việc"; bấm mở `CreateEstimateModal` ở chế độ phát sinh
  (tiêu đề "Lập báo giá phát sinh", `parentEstimateId` = báo giá gốc). _Kiểm chứng_: trình duyệt + `npm run build`.
- **AC-2**: Ở trạng thái không cho lập báo giá phát sinh (`newEstimateMode` trả `null`, ví dụ `WAITING_QC`, `COMPLETED`),
  tab Công việc **không** hiển thị nút tạo công việc/báo giá. Empty state không còn câu "bạn có thể bổ sung thủ công". _Kiểm chứng_: trình duyệt.
- **AC-3**: Sau khi báo giá phát sinh được gửi và khách duyệt dòng ở `QC_FAILED`, công việc mới xuất hiện trong tab Công việc
  ở trạng thái `PENDING`, phân công và bắt đầu được. _Kiểm chứng_: test service `EstimateServiceImpl.decide` ở `QC_FAILED` sinh `RepairTask` (thêm nếu chưa có).
- **AC-4**: Frontend không còn mã gọi `POST /repair-orders/{id}/tasks`: xóa `CreateTaskModal.tsx`, `useCreateTaskMutation`,
  `repairApi.createTask`. _Kiểm chứng_: tìm `createTask\(|CreateTaskModal` trong `frontend/src` không còn kết quả; `npm run build` và `npm run lint` pass.
- **AC-5**: Gọi trực tiếp `POST /api/v1/repair-orders/{id}/tasks` với body hợp lệ theo DTO ở lệnh đang mở trả HTTP 400,
  envelope `ApiResponse` với `code` 2004 và `message` là thông báo nghiệp vụ "Công việc sửa chữa phải được tạo từ hạng mục dịch vụ đã duyệt trong báo giá"
  (hành vi hiện có, khóa bằng test). _Kiểm chứng_: `RepairOrderServiceTest.createTask_RequiresApprovedEstimate` + `./mvnw test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 57 (import), 105 (state), 794–818 (header + empty state tab Công việc), 1117–1121 (render `CreateTaskModal`) | Thay nút "Thêm công việc" bằng nút "Lập báo giá phát sinh" gọi `setCreateEstimateOpen(true)`, chỉ hiện khi `estimateMode?.kind === 'SUPPLEMENTAL'`; sửa mô tả Empty; xóa import/state/render `CreateTaskModal` |
| `frontend/src/features/repair/components/CreateTaskModal.tsx` | toàn file | Xóa file |
| `frontend/src/features/repair/hooks/useRepair.ts` | 102–123 | Xóa `useCreateTaskMutation` |
| `frontend/src/features/repair/api/repairApi.ts` | 79–93 | Xóa `createTask` |
| `src/test/java/com/Group13_65CNTT/AutoGarage/repair/RepairOrderServiceTest.java` | 289–302 | Bổ sung assert message nghiệp vụ (`hasMessageContaining("hạng mục dịch vụ đã duyệt")`) |
| `src/test/java/com/Group13_65CNTT/AutoGarage/estimate/...` (test service báo giá hiện có) | — | Thêm test: báo giá `SUPPLEMENTAL` duyệt khi phiếu `QC_FAILED` sinh `RepairTask` `PENDING`, phiếu giữ `QC_FAILED` (nếu chưa có test tương đương) |

Tham chiếu (không sửa): `frontend/src/features/repair/utils/stageLocks.ts:58–70` (`newEstimateMode`),
`frontend/src/features/repair/components/CreateEstimateModal.tsx`, `RepairOrderDetailModal.tsx:106, 190, 553–580, 1122–1128`.

## 4. Hợp Đồng Kỹ Thuật

- **Backend không đổi chữ ký**: `RepairService.createTask(Long, Long, RepairTaskCreateRequest)` tiếp tục ném
  `BusinessRuleException` (→ `ResponseCode.BAD_REQUEST` 2004, message nghiệp vụ được lộ qua `GlobalExceptionHandler.authoredDetail`).
  `@PreAuthorize(SecurityConstants.HAS_TECHNICAL_ACCESS)` giữ nguyên.
- **UI** (tab Công việc, `RepairOrderDetailModal.tsx`):
  ```tsx
  {estimateMode?.kind === 'SUPPLEMENTAL' && (
    <Button variant="primary" onClick={() => setCreateEstimateOpen(true)}>
      <Plus size={15} /> Lập báo giá phát sinh
    </Button>
  )}
  ```
  - `estimateMode` đã tính sẵn ở dòng 190 (`null` khi không có quyền kỹ thuật), không cần điều kiện `canTechnical` riêng.
  - Đặt nút ở header danh sách (thay dòng 798–802) và làm `action` của `Empty` (thay dòng 811–817).
  - Mô tả Empty: "Công việc được tạo tự động khi khách duyệt báo giá. Cần thêm việc thì lập báo giá phát sinh."
  - Khi phiếu `QC_FAILED`, có thể thêm một dòng `text-xs muted` dưới header: "QC không đạt: làm lại các công việc được đánh dấu, hoặc lập báo giá phát sinh nếu cần thêm hạng mục." (dùng class có sẵn, không thêm CSS).
  - Tái dùng `Button`, `Empty` từ `shared/components/ui` và `CreateEstimateModal` sẵn có (dialog giữa màn hình, đúng `UI_DESIGN_GUIDELINES.md`). Không tạo component mới.

## 5. Checklist Thực Thi

- [x] **Step 1: Thay nút ở tab Công việc** — sửa `RepairOrderDetailModal.tsx` theo mục 4; xóa import, state `createTaskOpen`, render `CreateTaskModal`. _(AC-1, AC-2)_
- [x] **Step 2: Gỡ mã tạo công việc thủ công ở frontend** — xóa `CreateTaskModal.tsx`, `useCreateTaskMutation`, `repairApi.createTask`; kiểm tra không còn tham chiếu. _(AC-4)_
- [x] **Step 3: Khóa hành vi backend bằng test** — bổ sung assert message trong `RepairOrderServiceTest.createTask_RequiresApprovedEstimate`. _(AC-5)_
- [x] **Step 4: Test báo giá phát sinh ở `QC_FAILED` sinh công việc** — tìm test service báo giá hiện có (`src/test/java/.../estimate`), thêm case nếu chưa có. _(AC-3)_
- [ ] **Step 5: Kiểm thử & xác minh** — `cd frontend; npm run check` (lint + vitest + build); `./mvnw test` tại `AutoGarage/`; thao tác trình duyệt: lệnh QC_FAILED → tab Công việc → "Lập báo giá phát sinh" → gửi → khách duyệt → công việc mới xuất hiện. _(tất cả AC)_

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

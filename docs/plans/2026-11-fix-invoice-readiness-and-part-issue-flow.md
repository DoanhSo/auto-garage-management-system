# Kế Hoạch Sửa Lỗi Kiểm Tra Điều Kiện Lập Hóa Đơn (4001/500), Lỗi 404 Khi Tra Hóa Đơn Lệnh Chưa Lập Và Chặn Thi Công Khi Chưa Xuất Kho Phụ Tùng

> **Mã plan**: `fix-invoice-readiness-and-part-issue-flow`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: _(không)_ — plan trước `repair-invoiced-handover-navigation` đã lưu tại `docs/plans/2026-11-repair-invoiced-handover-navigation.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
Khi thao tác trên lệnh sửa chữa `#20` (`RO-20261101-13286`, trạng thái `COMPLETED`), hệ thống phát sinh 3 lỗi liên tiếp:

1. **Lỗi 1 — `404 / code: 2002` tại `GET /api/v1/billing/invoices/order/20`**:
   - Tại `HandoverModal.tsx` (`frontend/src/features/repair/components/HandoverModal.tsx:123–124`), khi mở hộp thoại bàn giao cho lệnh `#20` (chưa lập hóa đơn), hook `useInvoiceByOrderQuery(order.id, open)` gọi ngay `GET /api/v1/billing/invoices/order/20` dù `GET /api/v1/billing/invoices/order/20/handover-readiness` đã trả về `invoiceId: null`. Backend `BillingServiceImpl.getInvoiceByOrderId` ném `BusinessException(ResponseCode.RESOURCE_NOT_FOUND)` (`code: 2002`).
2. **Lỗi 2 — `500 / code: 4001` tại `GET /api/v1/billing/invoices/order/20/readiness`**:
   - Tại `BillingServiceImpl.getInvoiceReadiness` (`src/main/java/com/Group13_65CNTT/AutoGarage/billing/service/impl/BillingServiceImpl.java:347–365`), hàm được đánh dấu `@Transactional(readOnly = true)` và bọc `try { requireIssueReady(...); } catch (BusinessException blocked)`.
   - Bên trong `requireIssueReady`, hàm gọi sang các bean Spring khác có `@Transactional`: `RepairServiceImpl.requireInvoiceReady` (`RepairServiceImpl.java:321`) -> `InventoryServiceImpl.requireIssueComplete` (`InventoryServiceImpl.java:511`).
   - Khi `InventoryServiceImpl.requireIssueComplete` hoặc `RepairServiceImpl.requireInvoiceReady` ném `ConflictException` / `BusinessRuleException` (kế thừa `BusinessException` -> `RuntimeException`), `TransactionInterceptor` của Spring đánh dấu giao dịch vật lý dùng chung là **`rollbackOnly = true`**.
   - Sau đó `BillingServiceImpl.getInvoiceReadiness` bắt `BusinessException` và trả về bình thường `new InvoiceReadinessResponse(false, blocked.getMessage())`. Khi kết thúc hàm, Spring commit giao dịch ngoài, phát hiện `rollbackOnly = true` nên ném `org.springframework.transaction.UnexpectedRollbackException`, bị `GlobalExceptionHandler` trả về HTTP 500 (`code: 4001`, `"Unable to complete the request"`).
3. **Lỗi 3 — `409 / code: 2023` tại `POST /api/v1/billing/invoices/issue`**:
   - Do `/readiness` bị lỗi 500 (Lỗi 2), hộp thoại **Phát hành hóa đơn dịch vụ mới** trên `InvoicesPage.tsx` (`frontend/src/features/billing/pages/InvoicesPage.tsx:117–121, 693–765`) không nhận được `readiness.data` (`ready: false, reason: "..."`), nên không hiển thị cảnh báo sớm và không khóa nút **Phát hành hóa đơn**. Khi người dùng bấm nút, `POST /api/v1/billing/invoices/issue` trả về `409 INVOICE_BLOCKED` (`code: 2023`, `detail: "Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho: Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)..."`).
   - Đồng thời, tại sao lệnh `#20` lại qua được bước hoàn thành công việc & QC trong khi phụ tùng đã duyệt vẫn còn giữ chỗ (`reserved_quantity > 0`) chưa xuất kho?
     - Tại `LaborExecutionServiceImpl.completeTask` (`src/main/java/com/Group13_65CNTT/AutoGarage/repair/service/impl/LaborExecutionServiceImpl.java:410–468`), nút **Hoàn thành** công việc không kiểm tra `requireWorkStartAllowed(order)` (vốn chỉ đặt ở `startTimer`), nên công việc sửa chữa được bấm hoàn thành và tự chuyển phiếu sang `WAITING_QC` rồi `COMPLETED` mà chưa xuất kho phụ tùng.
   - Ngoài ra, khi một lệnh ở trạng thái `COMPLETED` bị chặn lập hóa đơn do còn phụ tùng chưa xuất kho, giao diện `InvoicesPage.tsx` và `HandoverModal.tsx` chưa có nút điều hướng nhanh sang màn **Chứng từ kho — Xuất kho sửa chữa** (`/inventory/documents?issueOrderId=<id>`) để xuất kho ngay cho lệnh đó.

### 1.2. Mục tiêu
- **Xử lý triệt để Lỗi 2 (`4001 / UnexpectedRollbackException` tại `/readiness`)**:
  - Thêm `noRollbackFor = BusinessException.class` vào `@Transactional` trên `BillingServiceImpl.getInvoiceReadiness`, `RepairServiceImpl.requireInvoiceReady`, `InventoryServiceImpl.requireIssueComplete` và `InventoryServiceImpl.requireIssuedForWorkStart` (cùng class-level `@Transactional(readOnly = true, noRollbackFor = BusinessException.class)` của `BillingServiceImpl` và `InventoryServiceImpl`).
  - Nhờ đó `GET /api/v1/billing/invoices/order/{orderId}/readiness` luôn trả về HTTP 200 kèm `{ "ready": false, "reason": "Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho: ..." }` thay vì lỗi 500 (`4001`).
- **Xử lý triệt để Lỗi 1 (`2002 / 404` tại `GET /api/v1/billing/invoices/order/{orderId}`)**:
  - Trong `HandoverModal.tsx`, chỉ kích hoạt `useInvoiceByOrderQuery` khi lệnh đã ở trạng thái `INVOICED` / `CLOSED` hoặc sau khi `readiness.data?.invoiceId != null`. Không gọi `GET /api/v1/billing/invoices/order/{id}` khi phiếu chưa có hóa đơn.
- **Xử lý triệt để Lỗi 3 (`2023 / INVOICE_BLOCKED` do chưa xuất kho phụ tùng)**:
  - Tại backend: trong `LaborExecutionServiceImpl.completeTask`, nếu công việc thuộc loại `TaskType.REPAIR`, bắt buộc kiểm tra `inventoryOperations.requireIssuedForWorkStart(task.getRepairOrderId())` trước khi cho phép hoàn thành; trong `RepairServiceImpl.transitionStatus` (khi chuyển sang `WAITING_QC`) cũng kiểm tra không còn phụ tùng giữ chỗ chưa xuất kho.
  - Tại frontend (`RepairOrderDetailModal.tsx`, `HandoverModal.tsx`, `InvoicesPage.tsx`, `InventoryDocumentsPage.tsx`):
    - Khi bấm **Lập hóa đơn** từ `RepairOrderDetailModal` hoặc `HandoverModal` của lệnh chưa có hóa đơn, điều hướng tới `/billing/invoices?issueOrderId=<orderId>` để mở sẵn hộp thoại **Phát hành hóa đơn dịch vụ mới** với lệnh đó được chọn sẵn.
    - Trong hộp thoại **Phát hành hóa đơn dịch vụ mới** (`InvoicesPage.tsx`), khi `issueBlockedReason` báo còn phụ tùng giữ chỗ chưa xuất kho hoặc còn phiếu kho nháp, hiển thị cảnh báo rõ ràng kèm nút **Đi tới Xuất kho sửa chữa** (`/inventory/documents?issueOrderId=<orderId>`).
    - Tại `InventoryDocumentsPage.tsx`, đọc tham số `?issueOrderId=<orderId>` trên URL để tự động mở hộp thoại **Lập chứng từ kho mới** ở chế độ **Xuất sửa chữa (`OUT`)** và chọn sẵn lệnh sửa chữa `#<orderId>` cùng danh sách phụ tùng chờ xuất.

### 1.3. Ngoài phạm vi
- Không tạo migration Flyway mới.
- Không tự động xóa hoặc tự động xuất kho ngầm dữ liệu phụ tùng của lệnh `#20` khi không có thao tác xác nhận của người dùng.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Gọi `GET /api/v1/billing/invoices/order/{orderId}/readiness` trên lệnh sửa chữa chưa đủ điều kiện lập hóa đơn (chưa xuất kho phụ tùng giữ chỗ, chưa QC, còn đồng hồ chạy...) trả về HTTP 200 với `data.ready = false` và `data.reason` tiếng Việt cụ thể, tuyệt đối không ném `UnexpectedRollbackException` (`code: 4001`). _Kiểm chứng_: integration test trong `PaymentCodeIntegrationTest` (chạy qua Spring `@Transactional` proxy thật) và kiểm tra thực tế trên lệnh `#20`.
- **AC-2**: Mở `HandoverModal` trên lệnh sửa chữa ở trạng thái `COMPLETED` chưa có hóa đơn (`invoiceId == null`) không phát sinh request lỗi `404 (code: 2002)` tới `/api/v1/billing/invoices/order/{orderId}`; bấm nút **Mở Hóa đơn dịch vụ để lập hóa đơn & thu tiền** mở thẳng `/billing/invoices?issueOrderId={orderId}` với lệnh sửa chữa được chọn sẵn. _Kiểm chứng_: test RTL trong `HandoverModal.test.tsx`.
- **AC-3**: Trong hộp thoại **Phát hành hóa đơn dịch vụ mới** (`InvoicesPage.tsx`), khi lệnh đang chọn có `readiness.ready === false` do còn phụ tùng giữ chỗ chưa xuất kho, nút **Phát hành hóa đơn** bị khóa, hiển thị lý do chi tiết và có nút **Đi tới Xuất kho sửa chữa** dẫn sang `/inventory/documents?issueOrderId={orderId}` (tự mở modal xuất kho với lệnh đó được chọn sẵn). _Kiểm chứng_: `npm --prefix frontend run check` và thao tác trình duyệt.
- **AC-4**: Tại backend, gọi `completeTask` cho công việc `TaskType.REPAIR` khi lệnh còn phụ tùng giữ chỗ chưa xuất kho bị chặn với `409 WORK_START_BLOCKED` (`code: 2026`). _Kiểm chứng_: `./mvnw test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `src/main/java/com/Group13_65CNTT/AutoGarage/billing/service/impl/BillingServiceImpl.java` | 76, 346–365 | Thêm `noRollbackFor = BusinessException.class` vào `@Transactional` của class và `getInvoiceReadiness` |
| `src/main/java/com/Group13_65CNTT/AutoGarage/repair/service/impl/RepairServiceImpl.java` | 320–356, 579–585 | Thêm `noRollbackFor = BusinessException.class` vào `@Transactional(readOnly = true)` của `requireInvoiceReady`; kiểm tra `inventoryOperations.requireIssueComplete(id)` khi chuyển tay sang `WAITING_QC` |
| `src/main/java/com/Group13_65CNTT/AutoGarage/inventory/service/impl/InventoryServiceImpl.java` | 61, 510–538 | Thêm `noRollbackFor = BusinessException.class` vào `@Transactional` của class, `requireIssueComplete` và `requireIssuedForWorkStart` |
| `src/main/java/com/Group13_65CNTT/AutoGarage/repair/service/impl/LaborExecutionServiceImpl.java` | 420–430 | Trong `completeTask`, nếu `task.getTaskType() == TaskType.REPAIR` và `inventoryOperations != null`, gọi `inventoryOperations.requireIssuedForWorkStart(task.getRepairOrderId())` |
| `frontend/src/features/repair/components/HandoverModal.tsx` | 120–137 | Chỉ bật `useInvoiceByOrderQuery` khi `open && (order.status === 'INVOICED' \|\| order.status === 'CLOSED' \|\| readiness.data?.invoiceId != null)`; khi chưa có hóa đơn (`missingInvoice`), điều hướng tới `/billing/invoices?issueOrderId=${order.id}` |
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 155–165 | Khi chưa có hóa đơn (`!handoverInvoice`), `invoiceHref` trỏ tới `/billing/invoices?issueOrderId=${order.id}` |
| `frontend/src/features/billing/pages/InvoicesPage.tsx` | 75–95, 693–768 | Đọc `?issueOrderId=` từ `useSearchParams()` để tự mở modal **Phát hành hóa đơn** và chọn sẵn lệnh; hiển thị nút **Đi tới Xuất kho sửa chữa** (`/inventory/documents?issueOrderId=${issueOrderId}`) khi bị chặn do phụ tùng/kho |
| `frontend/src/features/inventory/pages/InventoryDocumentsPage.tsx` | 65–135 | Đọc `?issueOrderId=` từ `useSearchParams()` để tự động mở modal tạo chứng từ `OUT` (Xuất sửa chữa) với `orderId` được chọn sẵn |
| `src/test/java/com/Group13_65CNTT/AutoGarage/billing/PaymentCodeIntegrationTest.java` | cuối file | Thêm integration test gọi `billingService.getInvoiceReadiness` qua Spring proxy khi lệnh còn phụ tùng giữ chỗ chưa xuất kho, xác minh trả về `InvoiceReadinessResponse(false, ...)` và không ném `UnexpectedRollbackException` |
| `frontend/src/features/repair/components/HandoverModal.test.tsx` | 80–130 | Cập nhật/bổ sung test xác minh không gọi `GET /api/v1/billing/invoices/order/{id}` khi `readiness.invoiceId == null` |

## 4. Hợp Đồng Kỹ Thuật

- **Transaction Rollback Rule (`BillingServiceImpl`, `RepairServiceImpl`, `InventoryServiceImpl`)**:
  - `@Transactional(readOnly = true, noRollbackFor = BusinessException.class)` trên `BillingServiceImpl.getInvoiceReadiness`, `RepairServiceImpl.requireInvoiceReady`, `InventoryServiceImpl.requireIssueComplete`, `InventoryServiceImpl.requireIssuedForWorkStart`.
- **Cổng kiểm tra xuất kho khi hoàn thành công việc (`LaborExecutionServiceImpl.completeTask`)**:
  - Nếu `task.getTaskType() == TaskType.REPAIR && inventoryOperations != null`, gọi `inventoryOperations.requireIssuedForWorkStart(task.getRepairOrderId())` (ném `ConflictException(ResponseCode.WORK_START_BLOCKED, ...)` nếu còn phụ tùng `reserved_quantity > 0` chưa xuất kho).
- **Liên kết chéo giữa Lệnh sửa chữa -> Phát hành hóa đơn -> Xuất kho sửa chữa**:
  - Chưa có hóa đơn: `/billing/invoices?issueOrderId=<orderId>` -> mở modal Phát hành hóa đơn, chọn sẵn `issueOrderId`.
  - Bị chặn lập hóa đơn do phụ tùng chưa xuất kho: `/inventory/documents?issueOrderId=<orderId>` -> mở modal Lập chứng từ kho (`docType = 'OUT'`, `orderId = <orderId>`).

## 5. Checklist Thực Thi

- [x] **Step 1: Sửa lỗi `UnexpectedRollbackException` (`4001`) tại `getInvoiceReadiness` và chặn hoàn thành task khi chưa xuất kho phụ tùng** — Cập nhật `BillingServiceImpl.java`, `RepairServiceImpl.java`, `InventoryServiceImpl.java` và `LaborExecutionServiceImpl.java`. _(AC-1, AC-4)_
- [x] **Step 2: Sửa lỗi gọi 404 `getInvoiceByOrder` trong `HandoverModal.tsx` và nối luồng điều hướng `issueOrderId` giữa `RepairOrderDetailModal.tsx`, `InvoicesPage.tsx`, `InventoryDocumentsPage.tsx`** — Chỉ gọi `useInvoiceByOrderQuery` khi đã có hóa đơn; tự mở modal lập hóa đơn và modal xuất kho theo `?issueOrderId=`. _(AC-2, AC-3)_
- [x] **Step 3: Viết test tích hợp & frontend test, build lại Docker backend + frontend** — Thêm test `getInvoiceReadiness` vào `PaymentCodeIntegrationTest.java`, cập nhật `HandoverModal.test.tsx`, chạy `./mvnw test`, `npm --prefix frontend run check` và `docker compose up -d --build`. _(AC-1, AC-2, AC-3, AC-4)_

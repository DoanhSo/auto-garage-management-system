# Kế Hoạch Báo Đúng Lý Do Khi Không Lập Được Hóa Đơn

> **Mã plan**: `invoice-blocked-reasons`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-qc-candidates-clarity.md` (Lỗi 3: bàn giao bị từ chối nhưng lý do bị nuốt). Cùng căn nguyên, lần này ở bước lập hóa đơn.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

**Bấm phát hành hóa đơn báo `2004 - Request fields or JSON are invalid` (`POST /api/v1/billing/invoices/issue`, 07:59 UTC).** Đây không phải dữ liệu gửi lên sai mà là một điều kiện nghiệp vụ bị từ chối, lý do bị nuốt:

- Dữ liệu gửi lên hợp lệ: `features/billing/pages/InvoicesPage.tsx:119-128` gửi `repairOrderId`, `taxAmount`, `discountAmount` đều là số.
- Hai lệnh đang `COMPLETED` (#8 và #21) đều có QC `PASSED`, không còn công việc dở, không còn đồng hồ chạy và đều có hạng mục tính tiền.
- **Nguyên nhân tìm được trên lệnh #21:** phụ tùng "Lốp xe Michelin Primacy 4 ST 215/55R17" (dòng báo giá #71, đã duyệt) còn `reserved_quantity = 1`, chưa có phiếu xuất kho nào cho lệnh (`inventory_documents` rỗng). Hàm kiểm tra `InventoryServiceImpl.requireIssueComplete` (`inventory/service/impl/InventoryServiceImpl.java:453-463`) ném `BusinessException(ResponseCode.BAD_REQUEST)` không kèm lời nhắn, được gọi từ `RepairServiceImpl.requireInvoiceReady` (`repair/service/impl/RepairServiceImpl.java:353-355`).
- `GlobalExceptionHandler.authoredDetail` (`common/exception/GlobalExceptionHandler.java:59-67`) chỉ cho phép lời nhắn của `BusinessRuleException`, `ResourceNotFoundException`, `ConflictException`, nên người dùng chỉ thấy câu chung của mã 2004.
- Lúc báo lỗi không còn log của lần bấm (container đã được tạo lại sau đó), nên kết luận trên dựa vào dữ liệu thật và thứ tự kiểm tra trong code; cần xác nhận lại ở Step 1.
- Cùng hàm `issueInvoice` (`billing/service/impl/BillingServiceImpl.java:101-340`) còn nhiều chỗ ném `BusinessException(...)` không lời nhắn, người dùng cũng chỉ thấy câu chung: phiếu chưa có QC (dòng 139), QC chưa đạt (dòng 147), còn đồng hồ đang chạy (dòng 159), không có hạng mục tính tiền (dòng 298), giảm giá lớn hơn tạm tính (dòng 302) và phiếu đã có hóa đơn (dòng 110, mã `DUPLICATE_RESOURCE`).
- Giao diện không báo trước: hộp "Phát hành hóa đơn" (`InvoicesPage.tsx:632-695`) liệt kê mọi lệnh `COMPLETED` dù chưa lập được hóa đơn, chỉ biết lỗi sau khi bấm.
- Lưu ý ngoài phạm vi: lệnh #21 vẫn sang `COMPLETED` và QC đạt dù còn phụ tùng chưa xuất kho, nên lỗi chỉ lộ ra ở bước hóa đơn.

### 1.2. Mục tiêu

- Khi không lập được hóa đơn, người dùng thấy đúng lý do bằng tiếng Việt, ví dụ "Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho: Lốp xe Michelin Primacy 4 ST 215/55R17. Hãy xuất kho hoặc hủy giữ chỗ trước khi lập hóa đơn."
- Mã lỗi nói đúng bản chất: bị chặn bởi điều kiện nghiệp vụ (409), không phải dữ liệu gửi lên sai.
- Hộp phát hành hóa đơn báo trước lý do khi chọn một lệnh chưa lập được và khóa nút, giống tab Bàn giao.
- Sau khi xuất kho phụ tùng của lệnh #21 qua màn Kho, lập được hóa đơn.

### 1.3. Ngoài phạm vi

- Không đổi điều kiện lập hóa đơn: vẫn phải xuất hết phụ tùng đã giữ chỗ và không còn phiếu kho nháp.
- Không tự động xuất kho hay hủy giữ chỗ thay người dùng.
- Không chặn chuyển lệnh sang `COMPLETED`/QC khi còn phụ tùng chưa xuất (ghi nhận để xem xét riêng).
- Không đổi `GlobalExceptionHandler.authoredDetail` và không sửa các `BusinessException` không lời nhắn ở module khác.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: `POST /billing/invoices/issue` trên lệnh còn phụ tùng giữ chỗ chưa xuất kho trả HTTP 409, `code = 2023`, `message` tiếng Việt nêu tên phụ tùng còn lại. Không còn trả 2004. _Kiểm chứng_: unit test cho `InventoryServiceImpl.requireIssueComplete` (mã, thông điệp có tên phụ tùng) và gọi API trên lệnh #21.
- **AC-2**: Khi còn phiếu kho xuất/trả ở trạng thái nháp, thông điệp riêng khác AC-1: "Còn phiếu kho nháp…". Khi hết cả hai thì `requireIssueComplete` qua. _Kiểm chứng_: unit test ba nhánh.
- **AC-3**: Mỗi nhánh còn lại của `issueInvoice` báo thông điệp riêng bằng tiếng Việt: phiếu chưa có QC, QC chưa đạt, còn đồng hồ đang chạy, không có hạng mục tính tiền, giảm giá lớn hơn tạm tính, đã có hóa đơn. Trạng thái nghiệp vụ trả 409/2023; giảm giá sai là 400/2004 kèm thông điệp. _Kiểm chứng_: unit test từng nhánh trong `BillingServiceTest`.
- **AC-4**: `GET /api/v1/billing/invoices/order/{orderId}/readiness` trả `ready` và `reason` (rỗng khi lập được) bằng đúng quy tắc của `issueInvoice`, không ghi dữ liệu. _Kiểm chứng_: unit test và gọi API trên lệnh #21 trước và sau khi xuất kho.
- **AC-5**: Hộp "Phát hành hóa đơn" chọn lệnh chưa lập được thì hiện lý do dưới ô chọn và khóa nút "Phát hành"; chọn lệnh lập được thì nút bật. Lỗi từ máy chủ nếu vẫn xảy ra hiển thị đúng thông điệp. _Kiểm chứng_: thao tác trình duyệt với lệnh #21.
- **AC-6**: Sau khi xuất kho phụ tùng của lệnh #21 ở màn Kho, hộp phát hành cho phép lập hóa đơn và tạo thành công, lệnh chuyển `INVOICED`. _Kiểm chứng_: thao tác trình duyệt.
- **AC-7**: Test hiện có vẫn qua: `BillingServiceTest`, `RepairWorkflowServiceTest`, `BusinessWorkflowIntegrationTests`, Vitest, lint, `tsc`. _Kiểm chứng_: `./mvnw test` (bật `mysql-test`) và `npm --prefix frontend test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `common/enums/ResponseCode.java` | sau `HANDOVER_BLOCKED` (dòng 57-58) | Thêm `INVOICE_BLOCKED(2023, "Invoice cannot be issued in the current state", HttpStatus.CONFLICT)` |
| `inventory/service/impl/InventoryServiceImpl.java` | 453-463 (`requireIssueComplete`) | Lấy tên phụ tùng còn giữ chỗ; ném `ConflictException(ResponseCode.INVOICE_BLOCKED, ...)` hai thông điệp riêng |
| `billing/service/impl/BillingServiceImpl.java` | 101-340 (`issueInvoice`); thêm hàm `getInvoiceReadiness` | Thay các `BusinessException(BAD_REQUEST/DUPLICATE_RESOURCE)` không lời nhắn bằng `ConflictException(INVOICE_BLOCKED/…, "…")` hoặc `BusinessRuleException`; tách phần kiểm tra sẵn sàng thành hàm dùng chung cho `issueInvoice` và `getInvoiceReadiness` |
| `billing/service/BillingService.java` | cạnh `issueInvoice` | Thêm `InvoiceReadinessResponse getInvoiceReadiness(Long repairOrderId)` |
| `billing/dto/response/InvoiceReadinessResponse.java` | file mới | Record `ready`, `reason` |
| `billing/controller/InvoiceController.java` | gần dòng 49-60 | Thêm `GET /order/{orderId}/readiness`, `@PreAuthorize(SecurityConstants.HAS_ROLE_RECEPTION_OR_ADMIN)`, `@Operation` |
| `src/test/.../inventory/` (tìm test của `InventoryServiceImpl`) và `billing/service/BillingServiceTest.java` | cuối file | Test AC-1 → AC-4 |
| `frontend/src/features/billing/api/billingApi.ts` | cạnh `getInvoiceByOrder` (dòng 74-75) | Thêm `getInvoiceReadiness(orderId)` |
| `frontend/src/features/billing/hooks/useBilling.ts` | cạnh `useInvoiceQuery` | Thêm `useInvoiceReadinessQuery(orderId, enabled)` |
| `frontend/src/features/billing/types/billing.ts` | cạnh `InvoiceIssueInput` (dòng 66-72) | Thêm `InvoiceReadiness` |
| `frontend/src/features/billing/pages/InvoicesPage.tsx` | 119-135 (`handleIssue`), 632-695 (hộp phát hành) | Lưu lệnh đang chọn; hiện lý do từ `readiness`; khóa nút khi `ready=false` |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | mục 2.6 | Ghi: lập hóa đơn bị chặn kèm lý do 409/2023; phải xuất hết phụ tùng giữ chỗ |

## 4. Hợp Đồng Kỹ Thuật

**Mã lỗi mới** (`ResponseCode.java`):

```java
INVOICE_BLOCKED(2023, "Invoice cannot be issued in the current state", HttpStatus.CONFLICT),
```

Dùng `new ConflictException(ResponseCode.INVOICE_BLOCKED, "<thông điệp>")` (`shared/exception/ConflictException.java` đã có hàm tạo `(ResponseCode, String)`), để `authoredDetail` hiển thị lời nhắn mà không cần đổi handler.

**Thông điệp**:

| Nhánh | Loại | Thông điệp |
|---|---|---|
| Còn phụ tùng giữ chỗ chưa xuất | 409/2023 | "Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho: <tên 1>, <tên 2>. Hãy xuất kho hoặc hủy giữ chỗ trước khi lập hóa đơn." (tối đa 5 tên, thêm "và N phụ tùng khác" nếu nhiều hơn) |
| Còn phiếu kho xuất/trả nháp | 409/2023 | "Còn phiếu kho xuất hoặc trả ở trạng thái nháp. Hãy hoàn tất hoặc hủy phiếu nháp trước khi lập hóa đơn." |
| Chưa có QC | 409/2023 | "Phiếu chưa có kết quả QC. Hãy hoàn tất kiểm tra chất lượng trước khi lập hóa đơn." |
| QC chưa đạt | 409/2023 | "Biên bản QC gần nhất chưa đạt. Hãy kiểm tra chất lượng lại trước khi lập hóa đơn." |
| Còn đồng hồ đang chạy | 409/2023 | "Còn kỹ thuật viên đang bấm giờ làm việc. Hãy dừng bấm giờ trước khi lập hóa đơn." |
| Không có hạng mục tính tiền | 409/2023 | "Phiếu không có hạng mục dịch vụ hoặc phụ tùng nào để tính tiền." |
| Phiếu đã có hóa đơn | 409/2003 (giữ) | "Phiếu đã có hóa đơn còn hiệu lực." |
| Giảm giá lớn hơn tạm tính | 400/2004 (`BusinessRuleException`) | "Số tiền giảm giá không được lớn hơn tạm tính của hóa đơn." |

**Kiểm tra sẵn sàng** (`BillingService`):

```java
InvoiceReadinessResponse getInvoiceReadiness(Long repairOrderId);   // @Transactional(readOnly = true)
public record InvoiceReadinessResponse(boolean ready, String reason) {}
```

- Chạy đúng chuỗi kiểm tra của `issueInvoice` trước khi tạo hóa đơn (đã có hóa đơn, `repairWorkflow.requireInvoiceReady`, QC, đồng hồ, hạng mục tính tiền), bắt `BusinessException` đầu tiên và trả `ready=false`, `reason=<message>`; không ghi dữ liệu và không dùng `FOR UPDATE` (dùng bản đọc của truy vấn đơn hàng).
- Kiểm tra dùng chung nằm trong một hàm private để `issueInvoice` và `getInvoiceReadiness` không lệch quy tắc.

**Frontend**:

```ts
interface InvoiceReadiness { ready: boolean; reason?: string | null }
getInvoiceReadiness(orderId: string | number): Promise<InvoiceReadiness>
useInvoiceReadinessQuery(orderId: string | null, enabled: boolean)
```

- Trong hộp phát hành: `Field` có `hint` hiện `reason` khi `ready=false`; nút "Phát hành" dùng `disabled`. Dùng `Field`, `Button`, `FormError` có sẵn, không thêm style.
- `handleIssue` giữ `notify(errorMessage(err))`; thêm `FormError` trong hộp để thông điệp vẫn thấy sau khi toast biến mất.

## 5. Checklist Thực Thi

- [x] **Step 1: Xác nhận nguyên nhân trên dữ liệu thật** _(AC-1, AC-6)_
  - Gọi API phát hành cho lệnh #21 (hoặc bấm trên giao diện) và đối chiếu: sau Step 2-3, thông điệp phải nêu phụ tùng Lốp Michelin. Nếu thông điệp khác, ghi lại nguyên nhân thật vào mục 1.1 trước khi sửa tiếp.
- [x] **Step 2: `ResponseCode` và `requireIssueComplete`** _(AC-1, AC-2)_
  - Thêm `INVOICE_BLOCKED`; sửa `InventoryServiceImpl.requireIssueComplete` theo bảng thông điệp.
- [x] **Step 3: Thông điệp rõ ràng trong `issueInvoice`** _(AC-3)_
  - Thay các lần ném không lời nhắn theo bảng.
- [x] **Step 4: Kiểm tra sẵn sàng và endpoint** _(AC-4)_
  - Tách hàm kiểm tra dùng chung, thêm `getInvoiceReadiness`, `InvoiceReadinessResponse`, endpoint `GET /order/{orderId}/readiness`.
- [x] **Step 5: Test backend** _(AC-1 → AC-4, AC-7)_
  - Thêm test cho `requireIssueComplete` (còn giữ chỗ, còn phiếu nháp, sạch), từng nhánh `issueInvoice`, `getInvoiceReadiness` sẵn sàng và bị chặn.
  - Chạy `./mvnw test -Dtest=BillingServiceTest,RepairWorkflowServiceTest` và test của Inventory.
- [x] **Step 6: Frontend** _(AC-5)_
  - Thêm kiểu, API, hook; cập nhật hộp phát hành; chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [x] **Step 7: Cập nhật tài liệu** _(AC-5)_
  - Ghi quy tắc vào `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.6.
- [x] **Step 8: Kiểm thử & xác minh** _(tất cả AC)_
  - Bật `mysql-test`, chạy `BusinessWorkflowIntegrationTests`; build lại container theo `README.md`.
  - Trên trình duyệt: chọn lệnh #21 thấy lý do về phụ tùng Michelin và nút khóa; xuất kho phụ tùng ở màn Kho; phát hành thành công (AC-6); rồi bàn giao (nối với plan trước).

- [x] **Step 9 (converge): Bổ sung test nhánh giảm giá lớn hơn tạm tính** _(AC-3)_
  - Trong `src/test/.../billing/service/BillingServiceTest.java` thêm test `issueInvoice` với một dòng dịch vụ tính tiền (tạm tính > 0) và `discountAmount` lớn hơn tạm tính → `BusinessRuleException` (400/2004) có thông điệp chứa "giảm giá" (`BillingServiceImpl.java`, nhánh `invoice.getDiscountAmount().compareTo(subtotal) > 0`).
  - Chạy `./mvnw test -Dtest=BillingServiceTest`.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-06. Kết luận: **partial** (mọi mục trong phạm vi đã có code và test; các mục kiểm chứng bằng giao diện và gọi API thật chưa xác minh).

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Còn phụ tùng giữ chỗ → 409/2023 nêu tên phụ tùng | Đạt | `InventoryServiceImpl.requireIssueComplete`; test kiểm tra mã và tên phụ tùng; chưa gọi API thật trên lệnh #21 |
| AC-2 Thông điệp riêng cho phiếu kho nháp, và trường hợp sạch | Đạt | Test ba nhánh và test cắt tối đa 5 tên |
| AC-3 Mỗi nhánh `issueInvoice` có thông điệp riêng | Đạt | Sau Step 9: test cho đã có hóa đơn, chưa có QC, QC chưa đạt, còn đồng hồ chạy, không có hạng mục, giảm giá lớn hơn tạm tính |
| AC-4 Endpoint `readiness` | Đạt | `BillingServiceImpl.getInvoiceReadiness`; test bị chặn, sẵn sàng không khóa dòng, không có hạng mục, lỗi hạ tầng không bị nuốt; chưa gọi qua HTTP |
| AC-5 Hộp phát hành báo lý do và khóa nút | Chưa xác minh | `InvoicesPage.tsx`; lint, `tsc`, build qua; chưa thử trên trình duyệt |
| AC-6 Xuất kho phụ tùng #21 rồi lập được hóa đơn | Chưa xác minh | Cần thao tác tay trên giao diện |
| AC-7 Test hiện có vẫn qua | Đạt | Toàn bộ 524 test backend (có MySQL test), Vitest 111/111 |

Lỗi phát hiện và đã sửa trong quá trình: converge lần đầu thiếu test nhánh giảm giá lớn hơn tạm tính, đã bổ sung ở Step 9.

Còn cần kiểm tay: chọn lệnh #21 trong hộp phát hành để thấy lý do về phụ tùng lốp Michelin và nút khóa; xuất kho phụ tùng ở màn Kho; phát hành hóa đơn; sau đó bàn giao.

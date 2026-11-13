# Kế Hoạch Sửa Lỗi Sau Khi Rà Soát Commit `7fd0865`

> **Mã plan**: `review-7fd0865-fixes`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-fix-shift-endpoints-and-flat-sidebar-nav.md`, `plans/2026-11-invoice-search-customer-vehicle.md`, `plans/2026-11-fix-invoice-readiness-and-part-issue-flow.md`, `plans/2026-11-payment-code-row-lock.md` (lỗi tìm thấy khi rà soát commit `7fd0865` và các gợi ý còn mở của lần review trước).

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
Phạm vi rà soát: toàn bộ phần code của commit `7fd0865` (backend `src/main`, test, frontend). Frontend: `tsc -b`, `eslint`, 192 test đều qua. Các lỗi tìm thấy:

1. **Tìm hóa đơn khớp cả tên khóa JSON.** `InvoiceRepository.searchInvoices` so `LIKE` trên `CAST(i.customerSnapshot AS string)` và `vehicleSnapshot`. Chuỗi này chứa tên khóa (`fullName`, `phone`, `email`, `address`, `plateNumber`, `model`, `color`…), nên gõ "name", "phone", "model", "color" trả về **mọi** hóa đơn.
2. **Sai mã lỗi và câu chữ khi chặn vì chưa xuất kho.**
   - `LaborExecutionServiceImpl.completeTask` gọi `requireIssuedForWorkStart` → trả `WORK_START_BLOCKED` với câu "…trước khi bắt đầu công việc" trong lúc người dùng đang **hoàn thành** công việc.
   - `RepairServiceImpl` khi chuyển `IN_PROGRESS → WAITING_QC` gọi `requireIssueComplete` → trả `INVOICE_BLOCKED` với câu "…trước khi lập hóa đơn".
   - `completeTask` còn kiểm tra `inventoryOperations != null` dù trường này đã bắt buộc (`startTimer` không kiểm tra).
3. **Lễ tân/Admin chấm công vào ca sớm không giới hạn.** `ShiftAttendanceServiceImpl.checkIn` bỏ hẳn giới hạn "trước giờ ca tối đa N phút" cho ADMIN/RECEPTIONIST, nên có thể chấm vào một ca của tuần sau; `actualStartsAt` bị ghi sớm nhiều ngày và ca được coi là đúng giờ. Thay đổi này cũng làm **test đơn vị `ShiftAttendanceServiceTest.checkInRejectsOutsideTheShiftWindow` đang fail** (dòng 86) trên nhánh hiện tại.
4. **`noRollbackFor = BusinessException.class` đặt ở mức lớp** của `BillingServiceImpl` và `InventoryServiceImpl`. Mục đích chỉ là để `getInvoiceReadiness` không đánh dấu rollback-only; đặt ở mức lớp làm mọi phương thức chỉ đọc khác cũng đổi hành vi và dễ hiểu nhầm là áp dụng cho phương thức ghi (phương thức có `@Transactional` riêng thì không thừa hưởng).
5. **File rác đã commit:** `scripts/analysis/__pycache__/schema_inventory.cpython-310.pyc`; `.gitignore` chưa có `__pycache__`.
6. **Code chết sau khi bỏ cây điều hướng:** `isChildActive`, `NavChild`, trường `children` của `NavItem` (`navMatch.ts`, `navItems.ts`) và CSS `.nav-children`, `.nav-subitem`.
7. **Nút Vào ca/Ra ca:** mọi dòng cùng hiện trạng thái đang xử lý khi chỉ một dòng được bấm (`pending={attendanceMutation.isPending}` ở `SchedulingPage.tsx`, `ShiftTimelineBoard.tsx`).
8. **Còn mở từ review trước:** `lockPaymentCodes()` ném lỗi 500 khó hiểu nếu thiếu dòng `payment_code_lock`; `PaymentCodeConcurrencyIntegrationTest` dùng `sleep` và khẳng định số tuyệt đối `0001`/`0002`, sai nếu DB test đã có phiếu chi trong ngày.

9. **(Phát hiện khi code Step 9) Khóa mã giao dịch lấy quá muộn.** `receivePayment`/`paySupplier` đọc dữ liệu thường trước khi gọi `lockPaymentCodes()`. Với REPEATABLE READ mặc định của MySQL, ảnh đọc đã cố định trước lúc chờ khóa, nên giao dịch sau không thấy mã vừa commit và vẫn có thể sinh trùng. Cách sửa: lấy khóa ở câu lệnh đầu tiên của giao dịch; test tích hợp đi qua `receivePayment` thật.

### 1.2. Mục tiêu
- Tìm hóa đơn chỉ khớp theo giá trị, không khớp theo tên trường.
- Người dùng thấy đúng lý do và đúng bước cần làm khi bị chặn vì phụ tùng chưa xuất kho.
- Lễ tân chỉ chấm công hộ được trong ngày của ca.
- Dọn file rác và code chết; test đồng thời ổn định.

### 1.3. Ngoài phạm vi
- Bỏ việc chặn hoàn thành công việc khi chưa xuất kho (giữ quy tắc của plan `fix-invoice-readiness-and-part-issue-flow`, chỉ sửa mã lỗi và câu chữ).
- Escape ký tự `%`/`_` trong ô tìm kiếm.
- Tối ưu hiệu năng tìm kiếm (LIKE có `%` đầu, 3 truy vấn con `EXISTS`).
- Sắp lại thứ tự V19/V20 trong `docs/DATABASE.md`.

## 2. Tiêu Chí Chấp Nhận

- **AC-1**: Tìm hóa đơn với "fullName", "phone", "model" không trả về hóa đơn nào chỉ vì tên khóa JSON; tìm theo tên khách, mã khách, số điện thoại, biển số, mã phiếu, mã hóa đơn vẫn khớp. _Kiểm chứng_: thêm khẳng định vào `PaymentCodeIntegrationTest.searchInvoices_MatchesCustomerVehicleAndOrder`.
- **AC-2**: Hoàn thành công việc khi còn phụ tùng giữ chỗ trả mã `PARTS_PENDING_ISSUE` với câu "…trước khi hoàn thành công việc"; chuyển sang chờ QC trả cùng mã với câu "…trước khi chuyển sang chờ QC". `startTimer` giữ `WORK_START_BLOCKED`, lập hóa đơn giữ `INVOICE_BLOCKED`. _Kiểm chứng_: `LaborExecutionServiceTest`, `RepairOrderServiceTest`/`RepairWorkflowServiceTest`, `InventoryServiceTest`.
- **AC-3**: ADMIN/RECEPTIONIST chấm công vào ca trước ngày của ca (theo `ShiftPolicy.ZONE`) bị từ chối; trong ngày của ca thì được dù còn sớm hơn cửa sổ của kỹ thuật viên. _Kiểm chứng_: test `ShiftAttendanceServiceTest`.
- **AC-4**: `noRollbackFor` chỉ còn ở các phương thức cần (`getInvoiceReadiness`, `requireIssueComplete`, `requireIssuedForWorkStart`, phương thức mới của AC-2, `requireInvoiceReady`); test `invoiceReadiness_WithReservedParts_…` vẫn xanh. _Kiểm chứng_: grep + test tích hợp.
- **AC-5**: `git ls-files | grep __pycache__` rỗng; `.gitignore` có `__pycache__/`. _Kiểm chứng_: lệnh.
- **AC-6**: Không còn `isChildActive`, `NavChild`, `.nav-children`, `.nav-subitem` trong `frontend/src`; `tsc -b`, `eslint`, vitest xanh. _Kiểm chứng_: grep + lệnh.
- **AC-7**: Bấm Vào ca/Ra ca chỉ dòng đó hiện đang xử lý. _Kiểm chứng_: test RTL trong `SchedulingPage.test.tsx`.
- **AC-8**: Thiếu dòng `payment_code_lock` trả `PAYMENT_BLOCKED` kèm thông báo cấu hình; test đồng thời so `mã thứ hai = mã thứ nhất + 1` và dùng latch thay `sleep`. _Kiểm chứng_: `BillingServiceTest`, `PaymentCodeConcurrencyIntegrationTest`.
- **AC-10**: Khóa mã được lấy trước mọi lần đọc trong `receivePayment`/`paySupplier`; nhiều khoản thu đồng thời qua `receivePayment` đều thành công với mã khác nhau. _Kiểm chứng_: `receivePayment_LocksBeforeAnyRead`, `PaymentCodeConcurrencyIntegrationTest.concurrentReceiptsAllSucceedWithDistinctCodes`.
- **AC-9**: `./mvnw test` và test frontend xanh toàn bộ.

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `billing/repository/InvoiceRepository.java` | bỏ 2 dòng `CAST(...Snapshot AS string) LIKE` |
| `common/enums/ResponseCode.java` | thêm `PARTS_PENDING_ISSUE(2028)` (409) |
| `shared/contracts/InventoryOperations.java` | thêm `requireIssuedBeforeCompletion(long)`, `requireIssuedBeforeQc(long)` |
| `inventory/service/impl/InventoryServiceImpl.java` | hai phương thức mới dùng `pendingIssueNames`; chuyển `noRollbackFor` về mức phương thức |
| `repair/service/impl/LaborExecutionServiceImpl.java` | `completeTask` gọi phương thức mới, bỏ kiểm tra null |
| `repair/service/impl/RepairServiceImpl.java` | chuyển `WAITING_QC` gọi `requireIssuedBeforeQc` |
| `billing/service/impl/BillingServiceImpl.java` | `noRollbackFor` về mức phương thức; `lockPaymentCodes` báo lỗi rõ |
| `scheduling/service/impl/ShiftAttendanceServiceImpl.java` | giới hạn lễ tân theo ngày của ca |
| `frontend/src/app/layouts/navMatch.ts`, `navItems.ts`, `styles.css` | xóa code chết |
| `frontend/src/features/scheduling/pages/SchedulingPage.tsx`, `components/ShiftTimelineBoard.tsx` | pending theo dòng |
| `.gitignore`, `scripts/analysis/__pycache__/` | bỏ theo dõi file rác |
| Test: `PaymentCodeIntegrationTest`, `PaymentCodeConcurrencyIntegrationTest`, `BillingServiceTest`, `LaborExecutionServiceTest`, `InventoryServiceTest`, `ShiftAttendanceServiceTest`, `SchedulingPage.test.tsx`, `navMatch.test.ts` | theo từng AC |
| `docs/REPAIR_ORDER_LIFECYCLE.md`, `docs/CONFIGURATION_AND_ERRORS.md` | mã lỗi mới |

## 4. Hợp Đồng Kỹ Thuật

- `ResponseCode.PARTS_PENDING_ISSUE(2028, HttpStatus.CONFLICT)`.
- `InventoryOperations`:
  - `void requireIssuedBeforeCompletion(long repairOrderId)` → "Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho: … Hãy xuất kho hoặc hủy giữ chỗ trước khi hoàn thành công việc."
  - `void requireIssuedBeforeQc(long repairOrderId)` → cùng mã, câu kết "…trước khi chuyển sang chờ QC."; kiểm tra thêm phiếu kho nháp như `requireIssueComplete`.
- Chấm công hộ: lễ tân/admin được chấm khi `now >= đầu ngày của shift.startsAt theo ShiftPolicy.ZONE`; kỹ thuật viên giữ `EARLY_CHECK_IN_WINDOW`.
- Frontend: `pending={attendanceMutation.isPending && attendanceMutation.variables?.id === shift.id}`.

## 5. Checklist Thực Thi

- [x] **Step 1: Bỏ khớp theo chuỗi JSON trong `searchInvoices`, thêm khẳng định test** _(AC-1)_
- [x] **Step 2: Thêm `PARTS_PENDING_ISSUE` và hai phương thức hợp đồng kho** _(AC-2)_
- [x] **Step 3: `completeTask` và chuyển `WAITING_QC` dùng phương thức mới, bỏ kiểm tra null** _(AC-2)_
- [x] **Step 4: Giới hạn chấm công hộ theo ngày của ca + test** _(AC-3)_
- [x] **Step 5: Đưa `noRollbackFor` về mức phương thức** _(AC-4)_
- [x] **Step 6: Bỏ theo dõi `__pycache__`, thêm `.gitignore`** _(AC-5)_
- [x] **Step 7: Xóa code chết của cây điều hướng** _(AC-6)_
- [x] **Step 8: Pending theo dòng cho nút Vào ca/Ra ca + test** _(AC-7)_
- [x] **Step 9: `lockPaymentCodes` báo lỗi rõ; làm ổn định test đồng thời** _(AC-8)_
- [x] **Step 10: Cập nhật docs mã lỗi** _(AC-2)_
- [x] **Step 11: Chạy toàn bộ test backend và frontend** _(AC-9)_

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-11. Kết luận: **converged**. Các mục giao diện được kiểm chứng bằng test RTL, chưa thao tác trên trình duyệt.

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Tìm hóa đơn không khớp tên khóa JSON | Đạt | Không còn `CAST(i.` trong `InvoiceRepository`; `searchInvoices_MatchesCustomerVehicleAndOrder` xanh trên MySQL |
| AC-2 Mã `PARTS_PENDING_ISSUE` và câu báo đúng bước | Đạt | `requireIssuedBeforeCompletion`, `requireIssuedBeforeQc`; test ở `InventoryServiceTest`, `LaborExecutionServiceTest`, `RepairWorkflowServiceTest` |
| AC-3 Chấm công hộ trong ngày của ca | Đạt | 3 test ở `ShiftAttendanceServiceTest`; test admin chấm sớm được bỏ qua khi chạy sát nửa đêm |
| AC-4 `noRollbackFor` chỉ ở phương thức cần | Đạt | grep còn 6 chỗ đúng danh sách; `invoiceReadiness_WithReservedParts_…` xanh |
| AC-5 Bỏ theo dõi `__pycache__` | Đạt | `git ls-files` không còn; `.gitignore` đã có |
| AC-6 Xóa code chết điều hướng | Đạt | grep 0 kết quả; `tsc -b`, `eslint` xanh |
| AC-7 Pending theo dòng | Đạt | Test RTL `shows the pending state only on the shift being punched`; chưa xác minh trên trình duyệt |
| AC-8 Báo lỗi thiếu dòng khóa; test đồng thời ổn định | Đạt | `receivePayment_MissingLockRow_Blocked`; test so số liền kề và dùng latch để sắp thứ tự, còn 200 ms chờ chỉ ảnh hưởng độ mạnh của test |
| AC-9 Toàn bộ test | Đạt | `./mvnw test` 612/612 trên MySQL; vitest 193/193 (`--maxWorkers=3 --testTimeout=30000`) |
| AC-10 Khóa mã lấy đầu giao dịch | Đạt | `receivePayment_LocksBeforeAnyRead`; `concurrentReceiptsAllSucceedWithDistinctCodes` xanh, và fail với `Duplicate entry` khi tạm đưa khóa về vị trí cũ |

Lỗi phát hiện và đã sửa khi code: khóa mã giao dịch lấy sau lần đọc đầu tiên (REPEATABLE READ) nên vẫn sinh trùng mã; cột bảng ca được ghi nhớ thiếu trạng thái chấm công nên nút không bao giờ hiện đang xử lý.

Ghi chú: `docs/CONFIGURATION_AND_ERRORS.md` không có danh sách các mã `*_BLOCKED` nên không sửa; mã mới ghi ở `docs/REPAIR_ORDER_LIFECYCLE.md`. Vitest với cấu hình mặc định có thể hết giờ khi máy quá tải.


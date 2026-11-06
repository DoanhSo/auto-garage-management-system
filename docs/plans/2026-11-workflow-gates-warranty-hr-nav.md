# Kế Hoạch Chốt Các Cổng Chặn Luồng Sửa Chữa, Bảo Hành Trước Bàn Giao Và Menu Nhân Sự

> **Mã plan**: `workflow-gates-warranty-hr-nav`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-inventory-issue-for-order.md` (cổng lập hóa đơn và xuất kho), `plans/2026-11-invoice-blocked-reasons.md` (lý do bị nuốt thành mã 2004), `plans/2026-11-payment-receipt-fix.md` (thu tiền hóa đơn).

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng (kết quả rà soát luồng)

Luồng cần có: tiếp nhận xe → duyệt báo giá → xuất kho phụ tùng (nếu có) → **mới được bắt đầu công việc** → kiểm định → lập hóa đơn → thanh toán theo hạn mức → (thêm bảo hành) → bàn giao.

| Cổng | Hiện trạng | Kết luận |
|---|---|---|
| Duyệt báo giá để vào thi công | `RepairServiceImpl.validateTransition` (`:693-751`): `WAITING_APPROVAL → APPROVED` chỉ qua duyệt báo giá | Đủ |
| **Bắt đầu công việc** | `LaborExecutionServiceImpl.startTimer` (`:296-347`) chỉ gọi `requireOpenOrder` (`:476`), chỉ chặn phiếu `CLOSED/CANCELLED/INVOICED`. Không kiểm tra phiếu đã `APPROVED`, không kiểm tra phụ tùng đã xuất kho. `assignTask` (`:66`) cũng không kiểm tra | **Thiếu** |
| Lập hóa đơn | `RepairServiceImpl.requireInvoiceReady` (`:323-356`) + `BillingServiceImpl.requireIssueReady` (`:294`): task xong, hết timer, QC `PASSED`, `InventoryOperations.requireIssueComplete` | Đủ |
| **Bàn giao theo hạn mức** | `BillingServiceImpl.requireHandoverAllowed` (`:693-751`): chặn khi tổng nợ khách `>` hạn mức (đúng ý). Nhưng (a) thông điệp không nói cần trả bao nhiêu, (b) bắt thêm điều kiện `payment_term_days > 0` và hóa đơn có `dueDate` (`:735-739`) nên khách còn nợ trong hạn mức vẫn bị chặn nếu hạn thanh toán là 0, (c) giao diện không cho biết trước sẽ bị chặn | **Lệch** |
| **Thêm bảo hành** | Bảo hành chỉ sinh tự động lúc bàn giao từ chính sách đã ghi trên dòng hóa đơn (`WarrantyServiceImpl.createFromInvoice`, `:79-123`, gọi ở `RepairServiceImpl:430-435`). Dòng nào `warranty_days = 0 và warranty_km = 0` thì không có bảo hành, và **không có chỗ nào để thêm** trước khi bàn giao. `HandoverModal.tsx` chỉ có ODO và ghi chú | **Thiếu luồng** |
| Menu quản lý nhân viên | Sidebar `AppShell.tsx:72-226` là danh sách phẳng. "Quản lý nhân viên" nằm trong tab của `PayrollManagerView` (`?tab=employees`), "Lịch ca", "Khoang", "Kỹ thuật viên" nằm trong tab của `SchedulingPage` (`?tab=…`), "Hệ số lương", "Thưởng ca" cũng là tab. Người dùng phải vào trang rồi mới thấy | **Cần đưa ra sidebar** |

### 1.2. Mục tiêu

- Không bấm giờ hay bắt đầu việc được khi phiếu chưa được duyệt hoặc còn phụ tùng đã giữ chỗ chưa xuất kho; thông báo nêu rõ lý do và tên phụ tùng.
- Trước khi bàn giao, người dùng thấy ngay: khách đang nợ bao nhiêu, hạn mức bao nhiêu, **cần thu tối thiểu bao nhiêu** để bàn giao được. Hạn mức 0 thì phải thu hết; hạn mức lớn hơn 0 thì chỉ cần thu để phần còn nợ không vượt hạn mức.
- Trước khi bàn giao, nhân viên thêm được bảo hành (số ngày, số km, điều khoản) cho các dòng hóa đơn chưa có; bàn giao xong, hệ thống sinh sổ bảo hành như cũ.
- Quản lý nhân viên, Lịch ca, Khoang, Đội kỹ thuật viên, Bảng lương, Hệ số lương, Thưởng ca đều là mục riêng trên sidebar, vào thẳng đúng tab.

### 1.3. Gợi ý thời điểm tạo luồng bảo hành (câu hỏi người dùng nêu)

1. **Khuyến nghị: ngay bước Bàn giao** (plan này làm). Lý do: thời điểm bảo hành bắt đầu và số ODO xuất xưởng chỉ chốt lúc bàn giao; hóa đơn đã lập nên mọi dòng và giá đã cố định, chỉ còn chính sách bảo hành là thông tin bổ sung; lễ tân đang nói chuyện với khách nên hỏi nhu cầu bảo hành được luôn.
2. Làm tốt hơn ở gốc (không thuộc plan này, chỉ đề xuất): đặt sẵn số ngày/km bảo hành mặc định cho từng dịch vụ và phụ tùng trong danh mục (`ServiceEntity`, `Part` đã có cột), để đa số dòng đã có bảo hành từ báo giá và bước Bàn giao chỉ bổ sung ngoại lệ.
3. Sau bàn giao (cấp bảo hành bổ sung theo thiện chí) để dành cho đợt sau vì cần quyền admin và lý do riêng.

### 1.4. Ngoài phạm vi

- Không đổi vòng đời trạng thái phiếu, không đổi cách tính hoa hồng.
- Không thêm bảo hành sau khi phiếu đã `CLOSED`; không sửa giá hay số lượng dòng hóa đơn.
- Không gộp hai tab "Nhân viên" của Lương và "Kỹ thuật viên" của Lịch ca thành một màn.
- Không xóa thanh tab trong trang (giữ làm lối vào phụ).

### 1.5. Điểm cần người dùng biết (đã chọn mặc định)

- **Quy tắc hạn mức (đã chốt với người dùng)**: gọi D là tổng nợ của khách sau hóa đơn này, L là hạn mức nợ.
  - Khách không có hạn mức (L = 0): phải thu hết hóa đơn (D phải bằng 0) mới bàn giao.
  - Khách có hạn mức: D nhỏ hơn hoặc bằng L thì không cần thu vội, bàn giao được; D lớn hơn L thì phải thu ít nhất D − L để phần còn nợ không vượt hạn mức.
  - Vì vậy **bỏ** điều kiện bắt buộc `payment_term_days > 0` và `dueDate`; giữ chặn khách có hóa đơn quá hạn.
- **Phụ tùng của việc khác**: cổng bắt đầu việc chặn theo cả phiếu (còn phụ tùng giữ chỗ chưa xuất thì chưa việc nào bấm giờ được), đúng ý "phụ tùng của đơn phải xuất kho mới bắt đầu".

---

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Phiếu chưa `APPROVED` (đang chờ duyệt báo giá) mà bấm giờ một việc thì nhận 409 mã `WORK_START_BLOCKED` kèm thông điệp "Phiếu chưa được duyệt báo giá…". _Kiểm chứng_: test `LaborExecutionServiceTest`.
- **AC-2**: Phiếu `APPROVED` còn phụ tùng giữ chỗ chưa xuất kho thì bấm giờ nhận 409 `WORK_START_BLOCKED` nêu tên phụ tùng và dặn "Hãy xuất kho trước khi bắt đầu"; xuất kho xong thì bấm giờ được; phiếu không có phụ tùng bấm giờ được ngay. _Kiểm chứng_: test dịch vụ cộng test tích hợp MySQL.
- **AC-3**: Phiếu `QC_FAILED` làm lại không bị chặn bởi cổng này khi đã xuất đủ. _Kiểm chứng_: test dịch vụ.
- **AC-4**: Hạn mức 0, khách còn nợ: bàn giao bị chặn, thông điệp ghi "cần thu tối thiểu X đ". Hạn mức L lớn hơn 0, tổng nợ D lớn hơn L: thông điệp ghi tối thiểu `D − L`. Thu đúng `D − L` thì bàn giao được. _Kiểm chứng_: test `BillingServiceTest` các ca 0, đúng bằng hạn mức, vượt 1 đồng.
- **AC-5**: Khách nợ không vượt hạn mức và hạn thanh toán 0 vẫn bàn giao được; khách có hóa đơn quá hạn vẫn bị chặn với thông điệp riêng. _Kiểm chứng_: test dịch vụ.
- **AC-6**: Mở "Bàn giao xe" thấy khung "Điều kiện thanh toán" gồm: tổng nợ khách, hạn mức, số cần thu tối thiểu, trạng thái cho phép hay không; nút bàn giao tắt kèm lý do khi chưa đủ. _Kiểm chứng_: test RTL `HandoverModal`, xem trên trình duyệt.
- **AC-7**: Mở "Bàn giao xe" thấy danh sách dòng hóa đơn kèm bảo hành hiện có; với dòng chưa có, nhập ngày/km/điều khoản và lưu được; dòng đã có sổ bảo hành thì khóa. _Kiểm chứng_: test dịch vụ, test RTL, trình duyệt.
- **AC-8**: Sau khi bổ sung rồi bàn giao, sổ bảo hành sinh đủ cho các dòng có `days > 0 hoặc km > 0` (không trùng). _Kiểm chứng_: test tích hợp MySQL: bổ sung, bàn giao, đếm `warranties`.
- **AC-9**: Chỉ ADMIN/RECEPTION bổ sung bảo hành; chỉ khi hóa đơn còn hiệu lực và phiếu chưa `CLOSED`; mỗi lần bổ sung ghi nhật ký kiểm toán. _Kiểm chứng_: test dịch vụ 403/409, kiểm tra `AuditConstants`.
- **AC-10**: Sidebar có nhóm "NHÂN SỰ & LƯƠNG" gồm các mục: Quản lý nhân viên, Lịch ca làm việc, Đội kỹ thuật viên, Khoang & điều phối, Bảng lương tháng, Hệ số & đơn giá lương, Chính sách thưởng ca. Bấm vào thì mở đúng tab; chỉ đúng một mục sáng lên; mục theo vai trò (kỹ thuật viên chỉ thấy "Phiếu lương của tôi" và lịch ca của mình). _Kiểm chứng_: test RTL `AppShell`, trình duyệt.
- **AC-11**: `lint`, `tsc`, build, toàn bộ test backend và frontend đạt. _Kiểm chứng_: lệnh trong Step cuối.

---

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `common/enums/ResponseCode.java` | cuối nhóm 20xx | thêm `WORK_START_BLOCKED(2026)`, `WARRANTY_BLOCKED(2027)` (409) |
| `shared/contracts/InventoryOperations.java` | `:7` | thêm `requireIssuedForWorkStart(long repairOrderId)` hoặc tham số mục đích cho `requireIssueComplete` |
| `inventory/service/impl/InventoryServiceImpl.java` | `:510-534` | dùng chung truy vấn phụ tùng chưa xuất, thông điệp theo mục đích (bắt đầu việc / lập hóa đơn) |
| `repair/service/impl/LaborExecutionServiceImpl.java` | `:296-347`, `:476` | `startTimer` gọi cổng bắt đầu việc (phiếu `APPROVED/IN_PROGRESS/QC_FAILED`, đã xuất kho) |
| `billing/service/BillingService.java`, `impl/BillingServiceImpl.java` | `:693-751` | tách `evaluateHandover` trả kết quả có số liệu; `requireHandoverAllowed` dùng nó; bỏ điều kiện hạn thanh toán bắt buộc; thêm `getHandoverReadiness`, `updateInvoiceItemWarranty` |
| `billing/dto/response/HandoverReadinessResponse.java` (mới) | | `allowed, reasons[], invoiceId, invoiceBalance, customerDebt, creditLimit, minimumPayment, overdueInvoiceCodes[], items[]` |
| `billing/dto/request/InvoiceItemWarrantyRequest.java` (mới) | | `warrantyDays, warrantyKm, warrantyTerms` |
| `billing/controller/InvoiceController.java`, `common/constant/ApiPaths.java` | | `GET …/handover-readiness`, `PATCH …/items/{itemId}/warranty` |
| `warranty/repository/WarrantyRepository.java` | | `existsByInvoiceItemId` (kiểm tra dòng đã có sổ) |
| `common/constant/AuditConstants.java` | | hành động `INVOICE_ITEM_WARRANTY_SET` |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | | thêm 2 hàng: cổng bắt đầu việc, bảo hành trước bàn giao |
| `frontend/src/features/repair/components/HandoverModal.tsx` (+ test) | | khung điều kiện thanh toán, bảng bảo hành theo dòng |
| `frontend/src/features/repair/api/repairApi.ts`, `hooks/useRepair.ts` hoặc `features/billing/api`, `hooks` | | `getHandoverReadiness`, `setItemWarranty` |
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | bàn giao | hiện lỗi cổng bắt đầu việc nếu có |
| `frontend/src/app/layouts/AppShell.tsx` (+ test) | `:62-226`, `:250-300` | thêm `section`, `search` cho `NavItem`, kích hoạt theo tab, nhóm "NHÂN SỰ & LƯƠNG" |
| `frontend/src/app/access.ts` | `:16-19` | thêm hằng vai trò cho từng mục nhân sự |
| `frontend/src/features/scheduling/pages/SchedulingPage.tsx`, `features/payroll/components/PayrollManagerView.tsx` | `useUrlTab` | giữ tab, đọc `?tab=` từ sidebar |

Test: `src/test/.../repair/service/LaborExecutionServiceTest.java`, `src/test/.../billing/service/BillingServiceTest.java`, `src/test/.../billing/HandoverWarrantyIntegrationTest.java` (mới, MySQL).

---

## 4. Hợp Đồng Kỹ Thuật

**Cổng bắt đầu việc** (`startTimer`, sau `requireOpenOrder`):
- Cho phép khi trạng thái phiếu thuộc `APPROVED, IN_PROGRESS, QC_FAILED`; ngược lại ném `ConflictException(ResponseCode.WORK_START_BLOCKED, "Phiếu chưa được duyệt báo giá. Hãy duyệt báo giá trước khi bắt đầu công việc.")` (hoặc thông điệp theo trạng thái `WAITING_PARTS`).
- Gọi `inventoryOperations.requireIssuedForWorkStart(orderId)`: cùng truy vấn với `requireIssueComplete`, nhưng thông điệp "Còn phụ tùng đã giữ chỗ chưa xuất kho: … Hãy xuất kho trước khi bắt đầu công việc." mang mã `WORK_START_BLOCKED`. Không chặn vì phiếu kho nháp (chỉ chặn ở bước hóa đơn).
- `ConflictException` có thông điệp mới tới được người dùng (`GlobalExceptionHandler.authoredDetail`).

**Cổng bàn giao**: `evaluateHandover(invoice)` trả `HandoverEvaluation{allowed, reasons, customerDebt, creditLimit, minimumPayment}` với `minimumPayment = max(0, customerDebt − creditLimit)`, nợ khách = `calculateTotalOutstandingBalanceForCustomer`. Thông điệp: "Công nợ của khách là {D} đ, vượt hạn mức {L} đ. Cần thu tối thiểu {D−L} đ trước khi bàn giao." Hạn mức 0 thì D−L = D (thu hết). Quá hạn: giữ thông điệp, thêm mã hóa đơn. `requireHandoverAllowed` ném `ConflictException(HANDOVER_BLOCKED, reasons[0])`. Dùng `formatMoney` có sẵn.

**API mới** (`@PreAuthorize(HAS_ROLE_RECEPTION_OR_ADMIN)`):
- `GET /api/v1/billing/invoices/by-order/{repairOrderId}/handover-readiness` → `HandoverReadinessResponse` (chỉ đọc, không khóa).
- `PATCH /api/v1/billing/invoices/{invoiceId}/items/{itemId}/warranty` body `InvoiceItemWarrantyRequest{warrantyDays 0..3650, warrantyKm 0..1_000_000, warrantyTerms ≤ 1000}`. Quy tắc: hóa đơn `ISSUED/PARTIALLY_PAID/PAID` (không `VOID`), phiếu chưa `CLOSED`, dòng chưa có `Warranty` (`existsByInvoiceItemId`), `warrantyDays > 0 hoặc warrantyKm > 0` hoặc cả hai bằng 0 để gỡ. Vi phạm: `ConflictException(WARRANTY_BLOCKED, "…")` tiếng Việt. Không đổi tiền hay tổng hóa đơn. Ghi audit.
- Không đổi `handover`, `createFromInvoice` (đã đọc từ dòng hóa đơn nên tự nhận phần bổ sung).

**Sidebar**: `NavItem` thêm `search?: string` (ví dụ `tab=employees`). Khóa React dùng `to + search`. Kích hoạt thủ công: `location.pathname === to` và (`search` khớp tham số `tab`, hoặc không có `tab` thì mục `search` mặc định sáng). Thêm `NavSection` "NHÂN SỰ & LƯƠNG"; chuyển "Khoang & Lịch ca" và "Bảng lương & Năng suất" vào đây. Dùng `useUrlTab` hiện có, không thêm route. Tiêu đề trang (`currentLink`) lấy theo mục khớp cả tab.

**UI dùng chung**: `Sheet`, `Field`, `Button`, `FormError`, `Badge`, `DataTable`, `useToast`, `errorMessage`; định dạng tiền theo tiện ích tiền tệ hiện có.

---

## 5. Checklist Thực Thi

- [x] **Step 1: Mã lỗi và cổng bắt đầu việc (backend)** _(AC-1, AC-2, AC-3)_
  - Thêm `WORK_START_BLOCKED`, `WARRANTY_BLOCKED` vào `ResponseCode`; thêm `requireIssuedForWorkStart` vào `InventoryOperations` và `InventoryServiceImpl` (dùng chung truy vấn); gọi trong `LaborExecutionServiceImpl.startTimer`. Trước khi code, xác nhận chỗ nào đưa phiếu `APPROVED → IN_PROGRESS` để cổng không chặn nhầm lần đầu.

- [x] **Step 2: Test cổng bắt đầu việc** _(AC-1, AC-2, AC-3)_
  - `LaborExecutionServiceTest`: chờ duyệt, còn phụ tùng, đã xuất, không phụ tùng, làm lại sau QC trượt. Thêm một ca tích hợp MySQL với dữ liệu giữ chỗ thật.

- [x] **Step 3: Cổng bàn giao có số liệu và API sẵn sàng bàn giao** _(AC-4, AC-5)_
  - Tách `evaluateHandover` trong `BillingServiceImpl`, viết lại thông điệp, bỏ điều kiện hạn thanh toán bắt buộc, thêm `HandoverReadinessResponse` và endpoint `handover-readiness`.

- [x] **Step 4: Test cổng bàn giao** _(AC-4, AC-5)_
  - `BillingServiceTest`: hạn mức 0 nợ đúng bằng số hóa đơn, hạn mức 5.000.000 nợ vượt 1 đồng, nợ đúng bằng hạn mức, hạn thanh toán 0 nhưng nợ trong hạn mức, có hóa đơn quá hạn, hóa đơn đã `PAID`.

- [x] **Step 5: Bổ sung bảo hành theo dòng hóa đơn (backend)** _(AC-7, AC-8, AC-9)_
  - `InvoiceItemWarrantyRequest`, `BillingService.updateInvoiceItemWarranty`, endpoint `PATCH`, `WarrantyRepository.existsByInvoiceItemId`, hằng audit; phản hồi hóa đơn đã có `warrantyDays/Km/Terms` nên không đổi mapper.

- [x] **Step 6: Test bảo hành trước bàn giao** _(AC-7, AC-8, AC-9)_
  - `BillingServiceTest` (quyền, trạng thái, dòng đã có sổ, hai giá trị 0 để gỡ) và `HandoverWarrantyIntegrationTest` (bổ sung rồi bàn giao, đếm sổ bảo hành, bàn giao lần hai không tạo trùng).

- [x] **Step 7: Giao diện Bàn giao: điều kiện thanh toán và bảo hành** _(AC-6, AC-7)_
  - API client và hook (`getHandoverReadiness`, `setItemWarranty`), viết lại `HandoverModal.tsx` với khung "Điều kiện thanh toán" (gồm nút mở hóa đơn để thu tiền) và bảng "Bảo hành theo dòng"; tắt nút bàn giao kèm lý do khi `allowed = false`; test RTL.

- [x] **Step 8: Sidebar nhóm Nhân sự & Lương** _(AC-10)_
  - `AppShell.tsx` thêm `search`, kích hoạt theo tab, nhóm và 7 mục; `access.ts` hằng vai trò; kiểm tra `SchedulingPage` và `PayrollManagerView` đọc `?tab=` đúng; test RTL cho mục sáng và quyền theo vai trò.

- [x] **Step 9: Cập nhật tài liệu** _(AC-1, AC-4, AC-7)_
  - `docs/REPAIR_ORDER_LIFECYCLE.md` thêm cổng bắt đầu việc, công thức tối thiểu cần thu và bảo hành trước bàn giao.

- [x] **Step 10: Kiểm thử & xác minh** _(tất cả AC)_
  - `./mvnw test`; `npm --prefix frontend run lint`, `tsc`, `npm --prefix frontend test`, `npm --prefix frontend run build`; build lại và chạy lại container; thử trên trình duyệt: bấm giờ phiếu còn phụ tùng, bàn giao khách có hạn mức 0 và hạn mức lớn hơn 0, thêm bảo hành rồi bàn giao rồi kiểm tra sổ bảo hành, bấm lần lượt 7 mục nhân sự trên sidebar.

- [ ] **Step 11 (converge): Test tích hợp MySQL cho cổng bắt đầu việc và bảo hành trước bàn giao** _(AC-2, AC-8)_
  - Thêm `src/test/.../billing/HandoverWarrantyIntegrationTest.java` (kế thừa `IntegrationTestSupport`): dựng hóa đơn có hai dòng dịch vụ, gọi `BillingServiceImpl.updateInvoiceItemWarranty` cho một dòng, rồi `WarrantyServiceImpl.createFromInvoice` hai lần; kiểm tra bảng `warranties` chỉ có đúng một sổ cho dòng đã đặt bảo hành, không trùng ở lần hai, và dòng đó bị khóa sau khi có sổ (`WARRANTY_BLOCKED`).
  - Cùng lớp (hoặc `InventoryIssueGateIntegrationTest`): với `estimate_items.reserved_quantity > 0`, `InventoryServiceImpl.requireIssuedForWorkStart` ném `WORK_START_BLOCKED` nêu tên phụ tùng; sau khi `reserved_quantity = 0` thì không ném.

- [ ] **Step 12 (converge): Test quyền cho hai endpoint mới** _(AC-9)_
  - Thêm test MockMvc hoặc kiểm tra `@PreAuthorize` cho `InvoiceController.updateItemWarranty` và `getHandoverReadiness`: ADMIN và RECEPTIONIST được, TECHNICIAN và CUSTOMER nhận 403 (`InvoiceController.java`).

## 6. Kết Quả Hội Tụ (lưu trữ khi bắt đầu plan mới)

- **Kết luận**: partial. Step 1–10 xong và đã commit (`0a10539`); backend 594/594, frontend 163/163.
- **Còn thiếu**: Step 11 (test tích hợp MySQL cho cổng bắt đầu việc và bảo hành trước bàn giao, AC-2/AC-8) và Step 12 (test 403 cho hai endpoint mới, AC-9) chưa làm. AC-6, AC-7, AC-10 mới có test, chưa xác minh trên trình duyệt. Menu nhân sự dạng danh sách phẳng 7 mục sẽ được thiết kế lại bởi plan `hr-navigation-hubs`.

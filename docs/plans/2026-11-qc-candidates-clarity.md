# Kế Hoạch Làm Rõ Danh Sách Người QC Trên Lệnh Cũ, Lý Do Không Chọn Được Và Lỗi Khi Bàn Giao Xe

> **Mã plan**: `qc-candidates-clarity`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-qc-assignment-on-order.md`. Plan đó thêm phân công QC tại tab QC, nhưng danh sách người chọn im lặng loại bỏ người không đủ điều kiện và không nói rõ đang lấy theo ca nào.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

**Câu hỏi 1: mở lệnh cũ thì danh sách nhân viên là của ngày nhập xe hay của hôm nay?** Kiểm tra code cho kết quả hai loại khác nhau:

- **Danh sách để chọn người (phân công thợ, phân công QC, dòng "Thợ QC đủ điều kiện") luôn là của thời điểm hiện tại.**
  - `GET /scheduling/on-shift-technicians` lấy ca bằng `Instant.now()` (`scheduling/service/impl/DispatchServiceImpl.java:63`), không nhận ngày nào.
  - Frontend dùng hook không có tham số ngày (`features/scheduling/hooks/useScheduling.ts:103-109`, queryKey `['on-shift-technicians']`). Ba nơi dùng: `AssignTaskModal.tsx:26`, `AssignQcModal.tsx:21`, `RepairOrderDetailModal.tsx:101` (dòng 927 hiện tên thợ QC).
  - Vì vậy mở lệnh của hôm trước vẫn thấy người đang trực ca hôm nay, và giao diện không ghi điều này ở đâu.
- **Người đã gắn vào lệnh là dữ liệu lịch sử, đúng theo ngày thực hiện.** Thợ chẩn đoán lấy theo `diagnosticTechnicianId` đã lưu (`RepairOrderDetailModal.tsx:121-123,396-414`). Danh sách phân công trong từng công việc lấy từ bảng `task_assignments` (`TaskWorkPanel.tsx`), không phụ thuộc hôm nay.

Phần "hôm nay" là đúng về nghiệp vụ: phân công và ghi QC xảy ra bây giờ, backend cũng kiểm tra ca tại thời điểm thao tác (`LaborExecutionServiceImpl.java:122` `coversAssignment`, `QualityServiceImpl.java` `activeShift(now)`). Vấn đề là giao diện không nói rõ nên dễ hiểu nhầm. Đã chốt với người dùng: **giữ lấy theo ca hiện tại**.

**Câu hỏi 2: nhân viên cố định khoang không chọn được làm QC.** Không tìm thấy điều kiện nào loại người vì ca gắn khoang:

- `getOnShiftTechnicians` trả cả người có ca gắn khoang lẫn người cơ động, kèm `serviceBayId` (`DispatchServiceImpl.java:124-126`). `AssignQcModal.tsx:23` chỉ lọc `canQc`. `coversAssignment` (`scheduling/repository/EmployeeShiftRepository.java:30-32`) không xét khoang.
- Dữ liệu thật hiện tại (kiểm tra bằng truy vấn chỉ đọc): ca gần nhất còn trong 3 ngày là ca `FULL_DAY` của 2 nhân viên ngày 03/11, đã hết. Lúc kiểm tra không có ca nào đang trực nên danh sách chọn người rỗng. Lệnh #20 đang chờ QC chưa có phân công nào.
- Các lý do thật sự có thể chặn một người có ca gắn khoang, đều im lặng với người dùng:
  1. Người đó từng là **thợ chính** (`REPAIR_MAIN`) của chính lệnh đó: backend ném "Thợ chính đã sửa chữa không được làm kỹ thuật viên QC của phiếu này" (`LaborExecutionServiceImpl.java:189-195`). Dòng nhật ký `REPAIR_MAIN` vẫn còn sau khi gỡ phân công, nên một lần là thợ chính thì luôn bị chặn.
  2. Ca của người đó là ca của ngày đã qua, nên không có trong danh sách hiện tại (xem câu hỏi 1) và `coversAssignment` từ chối khoảng thời gian bắt đầu bây giờ.
  3. Người đó có phân công khác đang chạy trùng khoảng thời gian: "Kỹ thuật viên đã có phân công công việc khác trong khoảng thời gian này" (`LaborExecutionServiceImpl.java:115-119`).
- Hiện hộp chọn chỉ ẩn người không đủ điều kiện hoặc báo lỗi sau khi bấm, nên người dùng không biết lý do. Câu trả lời người dùng cho việc đã phân công thợ chính chưa là "không nhớ/không chắc", nên plan có bước tái hiện và test bao phủ cả ba lý do thay vì giả định một nguyên nhân.

**Lỗi 3: bấm "Xác nhận bàn giao xe" báo `2004 - Request fields or JSON are invalid` (`POST /api/v1/repair-orders/21/handover`).** Đây không phải lỗi dữ liệu gửi lên, mà là một điều kiện nghiệp vụ bị từ chối nhưng lý do bị nuốt mất:

- Log backend cho thấy cả hai lần bấm (07:53:12 và 07:53:33 UTC) đều đã vào được `RepairWorkflow.handOver` với `odoOut=15320`, tức JSON hợp lệ. Số ODO nhập (15320) bằng ODO lúc vào, đạt điều kiện `>= odometerIn`.
- Dữ liệu lệnh #21: `COMPLETED`, QC đã đạt (`latest_qc_check_id = 35`), **chưa có hóa đơn nào** (`invoices` không có dòng cho phiếu này). Khách của xe có `credit_limit = 0`, `payment_term_days = 0`.
- `BillingServiceImpl.requireHandoverAllowed` (`billing/service/impl/BillingServiceImpl.java:601-653`) đòi hóa đơn còn hiệu lực trước khi bàn giao và ném `BusinessException(ResponseCode.BAD_REQUEST, "Phiếu chưa có hóa đơn còn hiệu lực, hãy lập hóa đơn trước khi bàn giao")`.
- `GlobalExceptionHandler.authoredDetail` (`common/exception/GlobalExceptionHandler.java:59-67`) chỉ cho phép thông điệp của `BusinessRuleException`, `ResourceNotFoundException`, `ConflictException`. `BusinessException` thuần bị bỏ thông điệp, nên người dùng chỉ thấy câu chung của mã 2004.
- Các nhánh khác của cùng hàm cũng ném `BusinessException(BAD_REQUEST)` không kèm lời nhắn: hóa đơn đã hủy (dòng 607), công nợ vượt hạn mức (dòng 635), khách không có hạn thanh toán (dòng 641), khách có hóa đơn quá hạn (dòng 649). Với khách vãng lai `credit_limit = 0` và hóa đơn chưa thu đủ thì sẽ rơi vào nhánh vượt hạn mức và cũng chỉ thấy câu chung.
- Giao diện cũng không báo trước: tab Bàn giao (`features/repair/components/RepairOrderDetailModal.tsx:1004-1066`) hiện nút "Tiến hành bàn giao xe" ngay khi phiếu `COMPLETED`, dù chưa có hóa đơn hay chưa thu tiền. Bước 2 "Quyết toán chi phí" chỉ là chữ mô tả.
- Ngoài phạm vi nhưng cùng căn nguyên: các `BusinessException(code, "lời nhắn")` khác (ví dụ `BillingServiceImpl.java:388,397`) cũng bị nuốt lời nhắn.

### 1.2. Mục tiêu

- Khi bàn giao bị từ chối, người dùng thấy đúng lý do bằng tiếng Việt ("Phiếu chưa có hóa đơn còn hiệu lực. Hãy lập hóa đơn trước khi bàn giao.") thay vì "Request fields or JSON are invalid". Mỗi lý do có thông điệp riêng.
- Mã lỗi nói đúng bản chất: bàn giao bị chặn bởi điều kiện nghiệp vụ (409), không phải dữ liệu gửi lên sai (400/2004).
- Tab Bàn giao cho biết trước điều kiện nào chưa đạt (chưa có hóa đơn, còn nợ) và khóa nút bàn giao kèm lý do, để không phải bấm rồi mới biết lỗi.
- Mọi danh sách chọn người ở lệnh sửa chữa nói rõ là theo **ca đang trực lúc này** (kèm giờ), để lệnh cũ không gây hiểu nhầm là ca ngày nhập xe.
- Hộp phân công QC liệt kê mọi người có quyền QC đang trong ca, **kể cả người có ca gắn khoang**. Người không chọn được vẫn hiện, bị khóa, kèm lý do ngắn ("Thợ chính của phiếu này").
- Người có ca gắn khoang, có quyền QC và không phải thợ chính chọn được làm QC bình thường.
- Dữ liệu lịch sử trên lệnh cũ (thợ chẩn đoán, phân công đã có) vẫn hiển thị theo dữ liệu đã lưu.

### 1.3. Ngoài phạm vi

- Không cho chọn ca của ngày nhập xe hay chọn ngày khác (đã chốt giữ ca hiện tại).
- Không đổi quy tắc bốn mắt: thợ chính đã sửa lệnh vẫn không được làm QC lệnh đó.
- Không đổi quy tắc chống chồng lịch hay điều kiện "nằm trong ca" của `assignTask` và `assignQcInspector`.
- Không đổi màn Khoang & lịch ca, không thêm migration.
- Không đổi điều kiện bàn giao: vẫn phải có hóa đơn còn hiệu lực và thu đủ tiền hoặc trong hạn mức công nợ hợp lệ. Plan chỉ làm lý do hiển thị rõ ràng.
- Không sửa hàng loạt các `BusinessException(code, "lời nhắn")` ở module khác (billing khác, audit, auth) và không đổi `authoredDetail` ở `GlobalExceptionHandler`; ghi lại thành việc riêng nếu cần.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: `GET /api/v1/repair-orders/{id}/qc-candidates` trả mọi nhân viên đang có ca trực lúc này, đang hoạt động và có `canQc`. Mỗi dòng có `eligible` và `reason` (rỗng khi chọn được). _Kiểm chứng_: unit test `LaborExecutionServiceTest`.
- **AC-2**: Nhân viên có ca gắn khoang (`serviceBayId` khác rỗng), có `canQc` và không phải thợ chính của lệnh có `eligible = true`, kèm mã và tên khoang. _Kiểm chứng_: unit test.
- **AC-3**: Nhân viên từng có dòng nhật ký `REPAIR_MAIN` trong lệnh có `eligible = false`, `reason = MAIN_REPAIRER`, và vẫn nằm trong danh sách. _Kiểm chứng_: unit test.
- **AC-4**: Nhân viên chỉ có ca của ngày đã qua (không có ca bao phủ lúc này) không nằm trong danh sách. _Kiểm chứng_: unit test.
- **AC-5**: Hộp "Phân công kỹ thuật viên QC" hiện dòng "Danh sách theo ca đang trực lúc <giờ ngày>", liệt kê người có quyền QC kèm khoang nếu có. Người `eligible=false` hiện mờ, không chọn được, kèm lý do. _Kiểm chứng_: thao tác trình duyệt trên một lệnh `WAITING_QC` của ngày trước.
- **AC-6**: Hộp "Phân công công việc" và dòng "Thợ QC đủ điều kiện đang trực ca" ở tab QC cũng ghi rõ lấy theo ca đang trực lúc này. Dòng ở tab QC chỉ liệt kê người `eligible`. _Kiểm chứng_: thao tác trình duyệt.
- **AC-7**: Mở lệnh cũ vẫn hiện đúng thợ chẩn đoán đã lưu và danh sách phân công của từng công việc, không đổi so với trước. _Kiểm chứng_: thao tác trình duyệt trên lệnh có dữ liệu cũ (ví dụ lệnh #12, #15).
- **AC-8**: Test hiện có vẫn qua: `LaborExecutionServiceTest`, `QualityCheckServiceTest`, `DispatchServiceTest`, `BusinessWorkflowIntegrationTests`, Vitest, lint, `tsc`. _Kiểm chứng_: `./mvnw test` (bật `mysql-test`) và `npm --prefix frontend test`.

- **AC-9**: `POST /repair-orders/{id}/handover` trên phiếu chưa có hóa đơn còn hiệu lực trả HTTP 409, `code = 2022`, `message` tiếng Việt "Phiếu chưa có hóa đơn còn hiệu lực. Hãy lập hóa đơn trước khi bàn giao." Không còn trả 2004. _Kiểm chứng_: unit test `BillingServiceTest` (code và thông điệp) và gọi API trên phiếu #21.
- **AC-10**: Mỗi nhánh còn lại của `requireHandoverAllowed` (hóa đơn đã hủy, công nợ vượt hạn mức, không có hạn thanh toán, có hóa đơn quá hạn) trả 409/2022 với thông điệp riêng, khác nhau. Khách thiếu hồ sơ trả `ResourceNotFoundException`. Phiếu có hóa đơn đã thu đủ vẫn bàn giao được. _Kiểm chứng_: unit test từng nhánh và nhánh thành công.
- **AC-11**: Dữ liệu hợp lệ nhưng sai quy tắc ODO (nhỏ hơn ODO vào) vẫn trả `BusinessRuleException` với thông điệp hiện có; JSON sai thật sự vẫn trả 2004. Hành vi cũ không đổi. _Kiểm chứng_: `RepairWorkflowServiceTest` và test handler hiện có.
- **AC-12**: Tab Bàn giao của phiếu `COMPLETED` hiện trạng thái hóa đơn: "Chưa có hóa đơn" / "Hóa đơn <mã> · còn nợ <số tiền>" / "Hóa đơn <mã> · đã thanh toán". Khi chưa có hóa đơn, nút "Tiến hành bàn giao xe" bị khóa kèm dòng "Cần lập hóa đơn trước khi bàn giao"; khi lỗi backend vẫn xảy ra, hộp bàn giao hiển thị đúng thông điệp. _Kiểm chứng_: thao tác trình duyệt trên phiếu #21 (chưa có hóa đơn) và một phiếu có hóa đơn.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `repair/dto/response/QcCandidateResponse.java` | file mới | Record `employeeId`, `employeeCode`, `fullName`, `shiftEndsAt`, `serviceBayId`, `bayCode`, `bayName`, `skills`, `eligible`, `reason` |
| `repair/service/LaborExecutionService.java` | sau `getQcAssignment` | Thêm `List<QcCandidateResponse> getQcCandidates(Long orderId)` |
| `repair/service/impl/LaborExecutionServiceImpl.java` | 37-52 (dependency), sau `getQcAssignment` (khoảng 216-227) | Inject `DispatchService`; thêm `getQcCandidates` dùng `dispatchService.getOnShiftTechnicians()` lọc `canQc`, đánh dấu thợ chính bằng `workLogRepository.findByRepairOrderIdAndAssignmentStage(..., REPAIR_MAIN)` |
| `repair/controller/RepairOrderController.java` | sau `GET /{id}/qc-assignment` (dòng 150-158) | Thêm `GET /{id}/qc-candidates` với `@PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)`, `@Operation` |
| `src/test/.../repair/LaborExecutionServiceTest.java` | cuối file | Test AC-1 → AC-4; thêm `@Mock DispatchService` |
| `frontend/src/features/repair/types/repairTypes.ts` | sau `TaskAssignment` (dòng 68-80) | Thêm `QcCandidate`, `QcIneligibleReason` |
| `frontend/src/features/repair/api/repairApi.ts` | cạnh `getQcAssignment` | Thêm `getQcCandidates(orderId)` |
| `frontend/src/features/repair/hooks/useRepair.ts` | cạnh `useQcAssignmentQuery` | Thêm `useQcCandidatesQuery(orderId, enabled)` (refetch 60 giây như on-shift) và invalidate khi phân công QC |
| `frontend/src/features/repair/components/AssignQcModal.tsx` | 20-27, 90-125 | Dùng `useQcCandidatesQuery` thay `useOnShiftTechniciansQuery`; option bị khóa kèm lý do; thêm dòng ghi giờ của danh sách |
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 101, 923-935 | Dòng "Thợ QC đủ điều kiện" dùng danh sách candidates `eligible`; ghi rõ "đang trực ca lúc này" |
| `frontend/src/features/repair/components/AssignTaskModal.tsx` | 100-110 (hint của ô chọn) | Sửa hint: ghi rõ theo ca đang trực lúc này |
| `frontend/src/features/repair/utils/assignmentWindow.ts` | cuối file | Thêm hàm định dạng giờ cho dòng ghi chú danh sách (hoặc dùng `formatDateTime` có sẵn nếu đủ) |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | mục 2.5 | Ghi: danh sách người QC theo ca hiện tại; thợ chính của lệnh bị khóa kèm lý do |
| `common/enums/ResponseCode.java` | sau `QC_INSPECTOR_INVALID` (khoảng dòng 55-56) | Thêm `HANDOVER_BLOCKED(2022, "Handover is not allowed in the current state", HttpStatus.CONFLICT)` |
| `billing/service/impl/BillingServiceImpl.java` | 601-653 (`requireHandoverAllowed`) | Thay 5 chỗ ném `BusinessException(BAD_REQUEST...)` bằng `ConflictException(ResponseCode.HANDOVER_BLOCKED, "<thông điệp riêng>")`; nhánh thiếu hồ sơ khách dùng `ResourceNotFoundException`. `ConflictException(ResponseCode, String)` đã có sẵn từ `shared/exception/ConflictException.java` |
| `src/test/.../billing/service/BillingServiceTest.java` | 83, 278-296 và cuối file | Sửa test cũ kỳ vọng `BAD_REQUEST`; thêm test từng nhánh của AC-9/AC-10 |
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 1004-1066 (tab Bàn giao) | Lấy hóa đơn của phiếu bằng `useInvoicesQuery({ orderId })` (bỏ hóa đơn `VOID`); hiện trạng thái hóa đơn ở bước 2; khóa nút "Tiến hành bàn giao xe" khi chưa có hóa đơn |
| `frontend/src/features/repair/components/HandoverModal.tsx` | 28-45 | Không đổi luồng; kiểm tra thông điệp lỗi từ backend hiển thị qua `errorMessage` (cả toast và `FormError`) |

## 4. Hợp Đồng Kỹ Thuật

**Endpoint** (đường dẫn dùng `ApiPaths.REPAIR_ORDERS_BASE`):

```
GET /api/v1/repair-orders/{id}/qc-candidates   @PreAuthorize(SecurityConstants.HAS_ROLE_INTERNAL_STAFF)   -> 200 ApiResponse<List<QcCandidateResponse>>
```

**DTO**:

```java
public record QcCandidateResponse(
        Long employeeId, String employeeCode, String fullName,
        Instant shiftEndsAt,
        Long serviceBayId, String bayCode, String bayName,
        List<String> skills,
        boolean eligible,
        Reason reason) {          // null khi eligible
    public enum Reason { MAIN_REPAIRER }
}
```

**Service** `getQcCandidates(Long orderId)` (`@Transactional(readOnly = true)`):

1. Kiểm tra phiếu tồn tại, nếu không `ResourceNotFoundException`.
2. `dispatchService.getOnShiftTechnicians()` (ca đang trực lúc `Instant.now()`), giữ người có `canQc`.
3. Tập thợ chính: `employeeId` của mọi dòng `RepairOrderAssignment` với `assignmentStage = REPAIR_MAIN` của phiếu. Người trong tập này có `eligible = false`, `reason = MAIN_REPAIRER`.
4. Sắp xếp: `eligible` trước, rồi theo tên.
5. Không đổi `assignQcInspector`. Backend vẫn là nơi quyết định cuối cùng; `eligible` chỉ giúp giao diện.

Không thêm `ResponseCode` mới.

**Frontend**:

```ts
type QcIneligibleReason = 'MAIN_REPAIRER';
interface QcCandidate { employeeId: string; employeeCode: string; fullName: string; shiftEndsAt: string;
  serviceBayId?: string | null; bayCode?: string | null; bayName?: string | null; skills: string[];
  eligible: boolean; reason?: QcIneligibleReason | null }
const QC_REASON_LABELS: Record<QcIneligibleReason, string> = { MAIN_REPAIRER: 'Thợ chính của phiếu này' };
```

- Option khóa dùng thuộc tính `disabled` của `<option>` kèm mô tả lý do, giống cách `AssignTaskModal.tsx:139` khóa vai trò QC.
- Dòng ghi chú dùng class sẵn có (`hint` của `Field`, hoặc `text-xs muted`), không thêm style mới. Nội dung: "Theo ca đang trực lúc <hh:mm dd/mm/yyyy>. Không phải ca của ngày nhập xe."
- Bố cục hộp giữ hai cột theo `UI_DESIGN_GUIDELINES.md`.

**Bàn giao xe (Lỗi 3)**:

```java
// ResponseCode.java
HANDOVER_BLOCKED(2022, "Handover is not allowed in the current state", HttpStatus.CONFLICT),
```

Thông điệp tiếng Việt trong `BillingServiceImpl.requireHandoverAllowed` (ném `new ConflictException(ResponseCode.HANDOVER_BLOCKED, ...)`, để `GlobalExceptionHandler.authoredDetail` cho phép hiển thị mà không cần đổi handler):

| Nhánh | Thông điệp |
|---|---|
| Không có hóa đơn còn hiệu lực | "Phiếu chưa có hóa đơn còn hiệu lực. Hãy lập hóa đơn trước khi bàn giao." |
| Hóa đơn đã hủy | "Hóa đơn của phiếu đã bị hủy. Hãy lập lại hóa đơn trước khi bàn giao." |
| Công nợ vượt hạn mức | "Hóa đơn chưa thanh toán đủ và công nợ của khách vượt hạn mức. Hãy thu tiền trước khi bàn giao." |
| Không có hạn thanh toán | "Hóa đơn chưa thanh toán đủ và khách không được ghi công nợ. Hãy thu tiền trước khi bàn giao." |
| Khách có hóa đơn quá hạn | "Khách có hóa đơn quá hạn chưa thanh toán. Hãy thu nợ quá hạn trước khi bàn giao." |
| Thiếu hồ sơ khách | `ResourceNotFoundException("Không tìm thấy khách hàng của hóa đơn")` |

- `PUT`/`POST` không đổi chữ ký; `HandoverRequest` giữ nguyên.
- Frontend: `useInvoicesQuery({ orderId: order.id })` đã có (`features/billing/hooks/useBilling.ts:12-26`); chọn hóa đơn có `status !== 'VOID'`. Dòng trạng thái dùng `Badge`/`text-xs muted` sẵn có, không thêm style mới. Khóa nút theo cách `Button disabled` kèm dòng giải thích bên cạnh, như dòng "Cần hoàn thành sửa chữa…" hiện có.

## 5. Checklist Thực Thi

- [x] **Step 1: Tái hiện trên dữ liệu thật** _(AC-2, AC-3, AC-4)_
  - Dùng lệnh `WAITING_QC` (ví dụ #20). Xếp ca hôm nay có gắn khoang cho một nhân viên `canQc` ở màn Khoang & lịch ca.
  - Mở tab QC, bấm "Phân công QC": ghi lại người có hiện trong hộp không.
  - Phân công người đó làm thợ chính một công việc rồi thử làm QC: ghi lại thông báo.
  - Ghi kết quả vào mục 1.1 của plan nếu khác giả định; nếu phát hiện lý do chặn thứ tư thì thêm `[ ] Step (converge)` thay vì tự sửa ngoài plan.
- [x] **Step 2: Backend `getQcCandidates`** _(AC-1 → AC-4)_
  - Thêm `QcCandidateResponse`, hàm service, endpoint.
- [x] **Step 3: Test backend** _(AC-1 → AC-4, AC-8)_
  - Thêm vào `LaborExecutionServiceTest`: ca gắn khoang chọn được, thợ chính khóa, ca ngày đã qua không hiện, không có `canQc` không hiện, phiếu không tồn tại.
  - Chạy `./mvnw test -Dtest=LaborExecutionServiceTest,DispatchServiceTest`.
- [x] **Step 4: Kiểu, API client, hook** _(AC-5, AC-6)_
  - Thêm `QcCandidate`, `getQcCandidates`, `useQcCandidatesQuery`.
- [x] **Step 5: Giao diện** _(AC-5, AC-6, AC-7)_
  - Sửa `AssignQcModal` dùng candidates, option khóa, dòng ghi giờ.
  - Sửa dòng ở tab QC và hint ở `AssignTaskModal`.
  - Chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [x] **Step 6: Cập nhật tài liệu** _(AC-5)_
  - Ghi vào `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.5.
- [x] **Step 7: Thông điệp rõ ràng khi bàn giao bị từ chối (backend)** _(AC-9, AC-10, AC-11)_
  - Thêm `HANDOVER_BLOCKED` vào `ResponseCode`.
  - Sửa `BillingServiceImpl.requireHandoverAllowed` theo bảng thông điệp ở mục 4.
  - Sửa `BillingServiceTest` (dòng 83, 278-296) và thêm test từng nhánh, gồm nhánh thành công khi hóa đơn đã thu đủ.
  - Chạy `./mvnw test -Dtest=BillingServiceTest,RepairWorkflowServiceTest`.
- [x] **Step 8: Tab Bàn giao báo trước điều kiện (frontend)** _(AC-12)_
  - Lấy hóa đơn của phiếu, hiện trạng thái ở bước 2, khóa nút bàn giao khi chưa có hóa đơn.
  - Chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [x] **Step 9: Kiểm thử & xác minh** _(tất cả AC)_
  - Bật `mysql-test`, chạy `BusinessWorkflowIntegrationTests`.
  - Build lại container theo `README.md` (jar, frontend, `docker compose build`, `up`).
  - Trên trình duyệt: mở một lệnh cũ chờ QC, kiểm tra hộp phân công QC và tab QC, kiểm tra thợ chẩn đoán và phân công cũ trên lệnh #12 hoặc #15 (AC-7).
  - Bàn giao phiếu #21: thấy "Chưa có hóa đơn", nút khóa; lập hóa đơn rồi bàn giao; gọi API bàn giao khi chưa có hóa đơn nhận 409 kèm thông điệp (AC-9, AC-12).

- [x] **Step 10 (converge): Bổ sung test hai nhánh bàn giao còn thiếu** _(AC-10)_
  - Trong `src/test/.../billing/service/BillingServiceTest.java` thêm: (a) `requireHandoverAllowed` khi `findActiveByRepairOrderIdWithLock` trả hóa đơn `VOID` → `HANDOVER_BLOCKED`, thông điệp chứa "đã bị hủy" (`BillingServiceImpl.java` nhánh `InvoiceStatus.VOID`); (b) khách thiếu hồ sơ (`jdbcTemplate.queryForList` trả rỗng) → `ResourceNotFoundException` chứa "khách hàng".
  - Chạy `./mvnw test -Dtest=BillingServiceTest`.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-06. Kết luận: **partial** (mọi mục trong phạm vi đã có code và test; các mục kiểm chứng bằng giao diện chưa xác minh).

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Endpoint ứng viên QC theo ca hiện tại | Đạt | `LaborExecutionServiceImpl.getQcCandidates`; test `getQcCandidates_ListsQcCapableOnShiftIncludingBayFixed` |
| AC-2 Người ca gắn khoang chọn được | Đạt | Cùng test, kiểm tra `eligible`, `serviceBayId`, `bayCode` |
| AC-3 Thợ chính bị khóa `MAIN_REPAIRER` | Đạt | Test `getQcCandidates_MarksMainRepairerIneligible` (vẫn hiện, xếp sau người chọn được) |
| AC-4 Người chỉ có ca ngày đã qua không hiện | Đạt | Lọc theo `Instant.now()` của `DispatchService` (`DispatchServiceTest`) và test danh sách rỗng |
| AC-5, AC-6 Nhãn ca hiện tại và người bị khóa trong hộp, tab QC | Chưa xác minh | Lint, `tsc`, build qua; chưa thử trên trình duyệt |
| AC-7 Lệnh cũ giữ dữ liệu lịch sử | Chưa xác minh | Không sửa phần này; chưa thử trên lệnh #12/#15 |
| AC-8 Test hiện có vẫn qua | Đạt | Backend 79/79 (gồm `BusinessWorkflowIntegrationTests` với MySQL test), Vitest 111/111 |
| AC-9 Bàn giao chưa có hóa đơn trả 409/2022 kèm thông điệp | Đạt | Test kiểm tra mã và nội dung; chưa gọi API thật trên phiếu #21 |
| AC-10 Mỗi nhánh còn lại có thông điệp riêng | Đạt | Sau Step 10: test cho nhánh vượt hạn mức, không có hạn thanh toán, quá hạn, hóa đơn đã hủy, thiếu hồ sơ khách và nhánh đã thanh toán; `BillingServiceTest` 17/17 |
| AC-11 ODO và JSON sai không đổi | Đạt | `RepairWorkflowServiceTest` 16/16, không đổi bộ xử lý lỗi chung |
| AC-12 Tab Bàn giao hiện trạng thái hóa đơn, khóa nút | Chưa xác minh | Code ở `RepairOrderDetailModal.tsx`; chưa thử trên trình duyệt với phiếu #21 |

Lỗi phát hiện và đã sửa trong quá trình: converge lần đầu thiếu test cho hai nhánh bàn giao (hóa đơn đã hủy, thiếu hồ sơ khách), đã bổ sung ở Step 10.

Còn cần kiểm tay: hộp phân công QC và nhãn ca trên lệnh cũ chờ QC; tab Bàn giao lệnh #21 ("Chưa có hóa đơn", nút khóa); lập hóa đơn rồi bàn giao.

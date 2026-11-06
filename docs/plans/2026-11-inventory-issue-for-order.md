# Kế Hoạch Xuất Kho Cho Lệnh Sửa Chữa, Sửa Lỗi Duyệt Báo Giá, Sơ Đồ Timeline Lịch Ca Và Tìm Kiếm

> **Mã plan**: `inventory-issue-for-order`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-invoice-blocked-reasons.md`. Lập hóa đơn bị chặn vì phụ tùng giữ chỗ chưa xuất kho; plan đó chỉ báo đúng lý do, plan này làm cho người dùng xuất kho được từ giao diện.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

**Câu hỏi: "chức năng xuất kho phụ tùng cho đơn hàng chưa có à?" Trả lời: backend có đủ, giao diện có form nhưng không dùng hoàn tất được.**

*Backend đã có (`inventory/service/impl/InventoryServiceImpl.java`):*

- Chứng từ kho loại `OUT` gắn với lệnh sửa chữa (`repairOrderId`), tạo ở trạng thái nháp rồi ghi sổ: `POST /inventory/documents` và `POST /inventory/documents/{id}/post` (`inventory/controller/InventoryDocumentController.java:50,98`, quyền `ADMIN` hoặc `WAREHOUSE_STAFF`).
- Khi báo giá được duyệt, phụ tùng được giữ chỗ: `EstimateServiceImpl.java:431` gọi `reserve` làm tăng `estimate_items.reserved_quantity` và `parts.reserved_quantity`.
- Ghi sổ `OUT` (dòng 238-336) trừ tồn kho, trừ phần giữ chỗ của cả phụ tùng và dòng báo giá, ghi giao dịch kho và cập nhật `repair_order_parts` (để tính hóa đơn).

*Chỗ không dùng được:*

1. **Form giao diện không gửi dòng báo giá.** `InventoryDocumentsPage.tsx:153-171` chỉ gửi `partId`, `quantity`, `unitCost`, `notes`; kiểu `InventoryDocumentItemInput` (`features/inventory/types/inventory.ts:129-134`) không có `estimateItemId`. Nhưng ghi sổ `OUT` đòi mọi dòng phải có `estimateItemId` (`InventoryServiceImpl.java:249-252`), nếu không ném `BadRequest`. Nghĩa là bản nháp tạo được nhưng **ghi sổ luôn thất bại** với câu chung "Request fields or JSON are invalid", và phụ tùng giữ chỗ không bao giờ được xuất.
2. **Người dùng phải tự gõ mã số của lệnh** (`InventoryDocumentsPage.tsx:704-713`, "VD: 101"), không chọn theo mã lệnh hay biển số, và tự chọn phụ tùng, số lượng. Không có danh sách "phụ tùng đã giữ chỗ chờ xuất" của lệnh.
3. **Lỗi bị nuốt như các lần trước.** Các điều kiện ghi sổ `OUT` ném `BusinessException(BAD_REQUEST)` không lời nhắn: lệnh đã hóa đơn/đóng/hủy (dòng 242), dòng không gắn báo giá hoặc trùng dòng (dòng 251), dòng không khớp phụ tùng đã duyệt, thuộc lệnh khác hoặc vượt số giữ chỗ (dòng 259-264), tồn kho không đủ (dòng 314), giữ chỗ của phụ tùng không đủ (dòng 319). Người dùng chỉ thấy câu chung của mã 2004.
4. Hệ quả thực tế trên lệnh #21: dòng báo giá #71 (Lốp xe Michelin Primacy 4 ST 215/55R17, đã duyệt) còn `reserved_quantity = 1`, chưa có chứng từ kho nào (`inventory_documents` rỗng), nên không lập được hóa đơn.

**Lỗi duyệt báo giá: bấm lưu quyết định trên báo giá #4 báo `2001 - lines: Danh sách quyết định hạng mục không được để trống` (`POST /api/v1/estimates/4/decide`).** Máy chủ nhận mảng `lines` rỗng do giao diện gửi lên, không phải người dùng bỏ sót:

- `EstimateDecisionModal.tsx:66-77` lập `lines` bằng cách lọc các dòng còn `PENDING`: `pending = new Set(items.filter(PENDING).map(it => it.id))` rồi `Object.entries(decisions).filter(([itemId]) => pending.has(itemId))`.
- **Lỗi so sánh kiểu:** `decisions` là `Record<string, …>` nên khóa luôn là chuỗi ("4"), còn `it.id` trong dữ liệu thật là số (backend trả `Long` dạng số JSON, `estimate/dto/response/EstimateItemResponse.java:17`; `estimateApi.ts` không chuyển id sang chuỗi dù kiểu khai báo là `string`). `Set<number>.has("4")` luôn `false`, nên `lines` **luôn rỗng** với mọi báo giá, kể cả báo giá đang chờ duyệt.
- Báo giá #4 (`BG-20261031-002`) còn là trường hợp riêng: đã `APPROVED`, cả 4 dòng đã `APPROVED`, không còn dòng chờ. Dù kiểu id đúng thì danh sách vẫn rỗng, nhưng giao diện vẫn hiện nút "Lưu quyết định phê duyệt" và các nút "Duyệt tất cả/Từ chối tất cả" (`EstimateDecisionModal.tsx:152-160,215-232`) như thể còn duyệt được.
- Nút "Xem & duyệt dòng" (`EstimatesPage.tsx:225-227`) hiện cho mọi báo giá, kể cả đã quyết định xong hoặc ở trạng thái không duyệt được (`REJECTED`, `EXPIRED`, `CANCELLED`; backend từ chối ở `EstimateServiceImpl.java:355-357`).
- Backend không có lỗi: chặn mảng rỗng đúng (`EstimateDecisionRequest.java` `@NotEmpty`), và đã idempotent với dòng đã quyết định giống nhau (`EstimateServiceImpl.java:375-386`).
- Chưa có test frontend cho phần lập `lines`, nên lỗi kiểu không bị phát hiện.

**Lịch ca làm việc chỉ có bảng, không có sơ đồ và không tìm kiếm bằng chữ.**

- Tab "Ca làm việc" (`features/scheduling/pages/SchedulingPage.tsx:828-843`) chỉ là một bảng `DataTable` (cột kỹ thuật viên, ngày và giờ, thời lượng, tình trạng, ghi chú) có phân trang phía trình duyệt. Muốn biết tuần này ai trực lúc nào, ai trùng ngày phải đọc từng dòng.
- Bộ lọc của tab (`SchedulingPage.tsx:646-698`) gồm ba ô chọn: kỹ thuật viên (hộp chọn), ngày làm việc, trạng thái ca. Không có ô gõ chữ để tìm theo tên, mã, tài khoản, chuyên môn, khoang hay ghi chú, trong khi tab Khoang đã có ô gõ (`bayKeyword`, `SchedulingPage.tsx:100,184`).
- Màn đặt lịch đã có sơ đồ tuần: `features/reception/components/BookingTimelineBoard.tsx` (điều hướng tuần trước/sau/tuần này/chọn ngày, thanh tóm tắt, lưới cột ngày, chip bấm ra chi tiết và thao tác, nút `+` ở ô trống), dùng các lớp `booking-timeline*`, `timeline-*` trong `styles.css` (dòng 3207-3312), cùng ô tìm kiếm kiểu `user-search` ở `BookingFilters.tsx:42-67`.
- Dữ liệu cho sơ đồ chưa đủ: `useShiftsQuery({ size: 100 })` (`SchedulingPage.tsx:120`) lấy 100 ca mới nhất rồi lọc ngày ở trình duyệt; `GET /employee-shifts` chỉ nhận `employeeId` và `status` (`scheduling/controller/EmployeeShiftController.java:167-180`). Xem một tuần cũ hoặc nhiều ca sẽ thiếu dữ liệu.

**Màn Nhật ký (audit log) chưa có ô tìm kiếm bằng chữ.**

- `features/audit/pages/AuditPage.tsx:177-246` chỉ có ba hộp chọn (phân hệ, hành động, người thực hiện) và nút "Đặt lại". Muốn tìm một sự kiện phải biết trước phân hệ hoặc chọn đúng người trong danh sách tối đa vài chục tài khoản.
- Backend `GET /audit` chỉ lọc `actorId` và `action` khớp đúng (`audit/repository/AuditEventRepository.java:20-24`, `audit/controller/AuditController.java:41-50`). Nhãn tiếng Việt của hành động nằm ở frontend (`features/audit/constants/auditCatalog.ts`), nên máy chủ không tự tìm được theo "Đăng nhập" hay "Hủy hóa đơn".

### 1.2. Mục tiêu

- Thủ kho/quản trị viên xuất kho phụ tùng cho một lệnh sửa chữa ngay trong form "Xuất kho sửa chữa": chọn lệnh theo mã hoặc biển số, hệ thống tự liệt kê phụ tùng đã duyệt và giữ chỗ chờ xuất (số lượng, tồn kho, thiếu hàng), tạo chứng từ nháp đúng chuẩn và ghi sổ được.
- Sau khi ghi sổ, phần giữ chỗ về 0, `repair_order_parts` có dòng phụ tùng và lệnh lập được hóa đơn.
- Mọi lý do bị từ chối khi xuất kho báo bằng tiếng Việt với mã nghiệp vụ (409), không còn "Request fields or JSON are invalid".
- Duyệt báo giá đang chờ khách hoạt động đúng: bấm lưu gửi đủ các dòng còn chờ quyết định với id đúng kiểu.
- Báo giá không còn gì để duyệt (đã quyết định xong, hoặc ở trạng thái không duyệt được) không hiện nút lưu và nói rõ lý do, thay vì gọi máy chủ rồi nhận lỗi.
- Tab "Ca làm việc" có sơ đồ tuần giống màn đặt lịch: nhìn một tuần biết ai trực ca nào, khoang nào, ngày nào; bấm vào ca để xem chi tiết và hủy; bấm `+` để xếp ca cho người và ngày đó. Bảng cũ vẫn dùng được như chế độ "Danh sách".
- Gõ chữ để tìm ca (tên, mã, tài khoản, chuyên môn, loại ca, khoang, ghi chú), có hoặc không dấu.
- Màn Nhật ký có ô tìm kiếm cùng kiểu: gõ tên hành động, người thực hiện, đối tượng để tìm sự kiện.

### 1.3. Ngoài phạm vi

- Không tự động xuất kho khi hoàn thành công việc hay QC; người dùng vẫn tạo chứng từ rồi ghi sổ.
- Không đổi quy tắc giữ chỗ khi duyệt báo giá và không đổi cách tính giá vốn lúc ghi sổ (vẫn lấy giá nhập hiện tại của phụ tùng).
- Không làm màn "Trả hàng" (`RETURN`) và không đổi chứng từ nhập/điều chỉnh.
- Không thêm banner "còn phụ tùng chờ xuất" trên màn chi tiết lệnh sửa chữa (lễ tân và kỹ thuật viên không có quyền xuất kho); lý do đã hiển thị ở hộp phát hành hóa đơn.
- Không chặn lệnh sang `COMPLETED`/QC khi còn phụ tùng chưa xuất (ghi nhận từ plan trước).
- Không đổi quy tắc duyệt báo giá ở backend và không chuyển toàn bộ id sang chuỗi ở API client (rủi ro tương tự có thể có chỗ khác; ghi nhận để rà soát riêng).
- Không sửa dữ liệu mẫu của báo giá #4 (hai dòng phụ tùng vẫn còn `reserved_quantity = 1` dù lệnh đã đi tiếp).
- Sơ đồ ca không kéo thả để dời ca, không sửa giờ ca trực tiếp trên sơ đồ, không có chế độ xem theo giờ trong ngày hay theo khoang (chỉ lưới kỹ thuật viên × ngày); không thêm thư viện lịch bên ngoài.
- Không đổi quy tắc tạo, đăng ký, duyệt, hủy ca và không đổi màn "Điều phối trực tiếp", "Khoang sửa chữa", "Kỹ thuật viên".
- Tìm kiếm nhật ký không quét nội dung `afterJson` hay `resultJson` của sự kiện (dữ liệu lớn và có thể nhạy cảm).

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: `GET /api/v1/inventory/documents/issue-candidates?repairOrderId={id}` trả danh sách phụ tùng đã duyệt, còn giữ chỗ của lệnh: `estimateItemId`, `partId`, `partCode`, `partName`, `unit`, `reservedQuantity`, `stockQuantity`, `unitCost`, `shortage` (tồn nhỏ hơn số cần xuất). Lệnh không có phụ tùng chờ xuất trả danh sách rỗng; lệnh không tồn tại trả 404. _Kiểm chứng_: unit test; gọi API với lệnh #21 thấy dòng Lốp Michelin, `reservedQuantity = 1`.
- **AC-2**: Tạo chứng từ `OUT` mà có dòng không gắn `estimateItemId` bị từ chối ngay lúc tạo (không đợi ghi sổ) với 409, `code = 2024`, thông điệp tiếng Việt. _Kiểm chứng_: unit test.
- **AC-3**: Ghi sổ `OUT` bị từ chối bằng 409/2024 kèm thông điệp riêng cho từng trường hợp: lệnh đã hóa đơn/đóng/hủy, dòng trùng, dòng không khớp phụ tùng đã duyệt hoặc thuộc lệnh khác, số lượng vượt giữ chỗ (nêu tên phụ tùng và số giữ chỗ), tồn kho không đủ (nêu còn bao nhiêu, cần bao nhiêu), giữ chỗ của phụ tùng không đủ. Lệnh không tồn tại là `ResourceNotFoundException`. Không còn trả 2004. _Kiểm chứng_: unit test từng nhánh.
- **AC-4**: Form "Xuất kho sửa chữa" cho chọn lệnh bằng ô tìm theo mã lệnh hoặc biển số (không gõ mã số), tự nạp các phụ tùng chờ xuất của lệnh đó; mỗi dòng có hộp chọn, số lượng mặc định bằng số giữ chỗ và không vượt số giữ chỗ, hiển thị tồn kho và cảnh báo thiếu hàng. _Kiểm chứng_: thao tác trình duyệt với lệnh #21.
- **AC-5**: Bấm tạo chứng từ gửi `estimateItemId` đúng; chứng từ nháp ghi sổ thành công: tồn kho Lốp Michelin giảm 1, giữ chỗ về 0, `repair_order_parts` có dòng của dòng báo giá #71. _Kiểm chứng_: thao tác trình duyệt, đối chiếu màn Phụ tùng và truy vấn dữ liệu.
- **AC-6**: Sau khi xuất kho lệnh #21, `GET /billing/invoices/order/21/readiness` trả `ready = true` và phát hành được hóa đơn. _Kiểm chứng_: thao tác trình duyệt (nối với plan hóa đơn).
- **AC-7**: Phụ tùng hết hàng (thiếu) vẫn hiện trong danh sách nhưng dòng bị khóa kèm cảnh báo "Thiếu hàng, tồn còn X"; không tạo được chứng từ có dòng thiếu hàng. _Kiểm chứng_: thao tác trình duyệt hoặc test frontend.
- **AC-8**: Test hiện có vẫn qua: `InventoryServiceTest`, `TV4 Inventory and Billing integration tests`, `BusinessWorkflowIntegrationTests`, Vitest, lint, `tsc`. _Kiểm chứng_: `./mvnw test` (bật `mysql-test`) và `npm --prefix frontend test`.

- **AC-9**: Với báo giá đang chờ khách duyệt có dòng `PENDING` (kể cả khi `id` của dòng là số), bấm "Lưu quyết định phê duyệt" gửi `lines` gồm đúng các dòng còn chờ, mỗi dòng có `estimateItemId` là số, `decision` theo lựa chọn và `note`; lưu thành công và backend tạo việc sửa chữa, giữ phụ tùng cho dòng được duyệt. _Kiểm chứng_: unit test Vitest cho hàm lập `lines` với `id` dạng số và dạng chuỗi; thao tác trình duyệt với một báo giá đã gửi.
- **AC-10**: Báo giá không còn dòng `PENDING` (như #4) mở ra không có nút "Lưu quyết định", nút "Duyệt tất cả/Từ chối tất cả" bị khóa, có dòng giải thích "Báo giá đã được quyết định hết, không thể đổi. Cần thêm hạng mục, hãy lập báo giá phát sinh."; không gọi API. _Kiểm chứng_: thao tác trình duyệt với báo giá #4.
- **AC-11**: Báo giá ở trạng thái `REJECTED`, `EXPIRED`, `CANCELLED` mở ra có dòng giải thích trạng thái không duyệt được và không có nút lưu. _Kiểm chứng_: unit test điều kiện hiển thị hoặc thao tác trình duyệt.
- **AC-12**: Nút ở danh sách báo giá ghi "Xem chi tiết" khi không còn gì để duyệt và "Xem & duyệt dòng" khi còn. _Kiểm chứng_: thao tác trình duyệt.

- **AC-13**: Tab "Ca làm việc" mặc định hiện sơ đồ tuần: mỗi hàng là một kỹ thuật viên (tên, mã, huy hiệu QC), mỗi cột là một ngày T2-CN của tuần đang xem (ô hôm nay được làm nổi), mỗi ca là một chip ghi loại ca, giờ bắt đầu-kết thúc và mã khoang nếu có. Màu chip theo tình trạng: sắp tới, đang trực, đã xong, đã hủy, chờ duyệt. Có nút Tuần trước/Tuần sau/Tuần này, ô chọn ngày và thanh tóm tắt (tổng ca trong tuần, đang trực, sắp tới, đã hủy). _Kiểm chứng_: thao tác trình duyệt trên dữ liệu có ca trong tuần.
- **AC-14**: Bấm chip mở khung chi tiết (kỹ thuật viên, ngày, giờ, thời lượng, loại ca, khoang, điểm danh, ghi chú); ca còn hiệu lực có nút "Hủy ca" dùng lại luồng hủy hiện có và chỉ admin thấy. Ô trống có nút `+` mở form xếp ca đã điền sẵn kỹ thuật viên và ngày của ô. _Kiểm chứng_: thao tác trình duyệt.
- **AC-15**: Có công tắc "Sơ đồ tuần / Danh sách" trong tab; chuyển qua lại giữ nguyên từ khóa tìm kiếm và bộ lọc; chế độ Danh sách vẫn là bảng cũ. _Kiểm chứng_: thao tác trình duyệt.
- **AC-16**: `GET /api/v1/employee-shifts` nhận thêm `from` và `to` (ngày `YYYY-MM-DD`, theo múi giờ xưởng) và trả mọi ca giao với khoảng đó, kể cả ca qua nửa đêm; vẫn nhận `employeeId`, `status`, phân trang. Xem một tuần không bị cắt ở 100 ca mới nhất. _Kiểm chứng_: unit test service/repository và gọi API.
- **AC-17**: Ô "Tìm kiếm ca" lọc theo chữ gõ, không phân biệt hoa thường và dấu, trên: tên, mã nhân viên, tài khoản, chuyên môn, loại ca, mã và tên khoang, ghi chú. Áp dụng cho cả sơ đồ lẫn danh sách; có nút xóa; không có kết quả thì hiện "Không có ca nào khớp với từ khóa". _Kiểm chứng_: unit test Vitest cho hàm lọc và thao tác trình duyệt.
- **AC-18**: Màn Nhật ký có ô tìm kiếm cùng kiểu (`user-search`, `type="search"`, biểu tượng kính lúp, nút xóa), nằm đầu thanh lọc; gõ có chờ khoảng ngắn rồi mới tìm; từ khóa nằm trên địa chỉ (`q`) nên tải lại vẫn còn; nút "Đặt lại" xóa luôn từ khóa; đổi từ khóa về trang đầu. _Kiểm chứng_: thao tác trình duyệt.
- **AC-19**: `GET /api/v1/audit` nhận thêm `query` (tối đa 100 ký tự) và `actions` (tối đa 50 mã hành động). Sự kiện khớp nếu mã hành động, loại đối tượng chứa từ khóa, mã đối tượng bằng số trong từ khóa, tên hoặc tài khoản người thực hiện chứa từ khóa, hoặc mã hành động nằm trong `actions`. Kết hợp VÀ với `actorId` và `action` hiện có; ký tự `%` và `_` trong từ khóa không bị coi là ký tự đại diện; chỉ admin gọi được như cũ. Frontend đổi chữ gõ thành `actions` bằng nhãn tiếng Việt trong danh mục hành động (không dấu). _Kiểm chứng_: unit test repository/service (gồm `%`, `_`, danh sách `actions` rỗng) và thao tác trình duyệt, ví dụ gõ "đăng nhập" ra các sự kiện đăng nhập.
- **AC-20**: Test hiện có vẫn qua: `SchedulingServiceTest` (hoặc test ca hiện có), `ShiftRegistrationModal.test.tsx`, test nhật ký, Vitest, lint, `tsc`. _Kiểm chứng_: `./mvnw test` (bật `mysql-test`) và `npm --prefix frontend test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `common/enums/ResponseCode.java` | sau `INVOICE_BLOCKED` (dòng 59-60) | Thêm `INVENTORY_ISSUE_BLOCKED(2024, "Stock cannot be issued in the current state", HttpStatus.CONFLICT)` |
| `common/constant/ApiPaths.java` | cạnh `INVENTORY_DOCUMENTS_BASE` (dòng 86-87) | Thêm `INVENTORY_ISSUE_CANDIDATES = "/issue-candidates"` |
| `inventory/dto/response/IssueCandidateResponse.java` | file mới | Record theo AC-1 |
| `inventory/service/InventoryService.java` | cạnh `searchDocuments` | Thêm `List<IssueCandidateResponse> getIssueCandidates(Long repairOrderId)` |
| `inventory/service/impl/InventoryServiceImpl.java` | tạo chứng từ 117-150; ghi sổ 238-266 và 312-336 | Kiểm tra `estimateItemId` ngay lúc tạo `OUT`; thay các `BusinessException(BAD_REQUEST)` của luồng xuất kho bằng `ConflictException(INVENTORY_ISSUE_BLOCKED, "…")`; thêm `getIssueCandidates` |
| `inventory/controller/InventoryDocumentController.java` | gần dòng 63 | Thêm `GET issue-candidates` với `@PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN)`, `@Operation` |
| `src/test/.../inventory/service/InventoryServiceTest.java` | cuối file | Test AC-1 → AC-3 |
| `frontend/src/features/inventory/types/inventory.ts` | 129-134 | Thêm `estimateItemId?` vào `InventoryDocumentItemInput`; thêm `IssueCandidate` |
| `frontend/src/features/inventory/api/inventoryApi.ts` | cạnh API chứng từ | Thêm `getIssueCandidates(orderId)` |
| `frontend/src/features/inventory/hooks/useInventory.ts` | cạnh hook chứng từ | Thêm `useIssueCandidatesQuery(orderId, enabled)` |
| `frontend/src/features/inventory/pages/InventoryDocumentsPage.tsx` | 110-175 (state, tạo), 681-713 (form OUT), 720-860 (bảng dòng) | Với `OUT`: ô chọn lệnh bằng `Combobox`, nạp ứng viên, bảng dòng theo ứng viên, payload có `estimateItemId`. Giữ nguyên các loại chứng từ khác |
| `docs/REPAIR_ORDER_LIFECYCLE.md` | mục 2.6 | Ghi cách xuất kho cho lệnh và điều kiện |
| `frontend/src/features/estimate/utils/lineDecisions.ts` | file mới | Hàm `buildLineDecisions(items, decisions, note)` và `canDecideEstimate(status)`; so sánh id bằng `String(...)` |
| `frontend/src/features/estimate/utils/lineDecisions.test.ts` | file mới | Test AC-9 và AC-11 (id số và chuỗi, dòng đã quyết định bị loại, ghi chú rỗng, trạng thái không duyệt được) |
| `frontend/src/features/estimate/components/EstimateDecisionModal.tsx` | 38-48, 66-83, 148-160, 215-232 | Dùng hàm mới; tính `pendingCount`; khóa nút duyệt/từ chối tất cả và ẩn nút lưu khi không còn dòng chờ; dòng giải thích theo trạng thái |
| `frontend/src/features/estimate/pages/EstimatesPage.tsx` | 222-229 | Nhãn nút "Xem chi tiết" / "Xem & duyệt dòng" theo số dòng còn chờ |
| `scheduling/controller/EmployeeShiftController.java` | 167-180 (`getShifts`) | Thêm `@RequestParam(required = false) @DateTimeFormat(iso = DATE) LocalDate from, to`; truyền xuống service |
| `scheduling/service/SchedulingService.java` và `service/impl/SchedulingServiceImpl.java` | `getShifts` (dòng 48) | Thêm tham số `from`, `to`; đổi khoảng ngày sang `Instant` theo `ShiftPolicy.ZONE` (nửa mở `[from, to + 1 ngày)`) |
| `scheduling/repository/EmployeeShiftRepository.java` | cạnh các truy vấn dòng 18-60 | Thêm truy vấn phân trang `searchShifts(employeeId, status, from, to, pageable)`: `(:from IS NULL OR s.endsAt > :from) AND (:to IS NULL OR s.startsAt < :to)` |
| `src/test/.../scheduling/` (test của `SchedulingServiceImpl`/`EmployeeShiftRepository`) | cuối file | Test AC-16 (ca qua nửa đêm, biên khoảng, kết hợp `employeeId`/`status`) |
| `frontend/src/features/scheduling/api/schedulingApi.ts` và `hooks/useScheduling.ts` | `listShifts` (dòng 128-145), `useShiftsQuery` (dòng 123-133) | Thêm `from`, `to` vào tham số và `queryKey` |
| `frontend/src/features/scheduling/components/ShiftTimelineBoard.tsx` | file mới | Sơ đồ tuần kỹ thuật viên × ngày (AC-13, AC-14), mô phỏng `BookingTimelineBoard` |
| `frontend/src/features/scheduling/utils/shiftTimeline.ts` và `.test.ts` | file mới | Hàm gom ca theo ô `kỹ thuật viên|ngày`, phân loại tình trạng chip, hàm lọc ca theo từ khóa (AC-17); dùng lại `weekStart`, `shiftDate`, `todayKey`, `localDate` của `features/reception/constants/bookingSlots.ts` (chuyển sang `shared/utils/dates.ts` nếu cần dùng chung, giữ nguyên chữ ký) |
| `frontend/src/shared/utils/text.ts` và `.test.ts` | file mới | `normalizeSearchText(value)`: hạ chữ thường, bỏ dấu tiếng Việt, gộp khoảng trắng; dùng cho AC-17 và AC-19 |
| `frontend/src/features/scheduling/pages/SchedulingPage.tsx` | 100-103 (state), 119-121 (truy vấn), 186-198 (lọc ca), 646-698 (bộ lọc ca), 828-843 (hiển thị) | Thêm `shiftKeyword`, `shiftView`, `shiftWeek`; truy vấn theo tuần; ô tìm kiếm; công tắc Sơ đồ/Danh sách; gắn `ShiftTimelineBoard` |
| `frontend/src/features/scheduling/components/ShiftEditor.tsx` | props và giá trị mặc định | Nhận thêm ngày/giờ gợi ý để mở từ nút `+` của sơ đồ (kỹ thuật viên đã có sẵn qua `shiftPresetEmployee`) |
| `frontend/src/styles.css` | cạnh khối `.timeline-*` (dòng 3207-3330) | Chỉ thêm biến thể chip cho ca (đang trực, đã xong, chờ duyệt) và cột tiêu đề hàng kỹ thuật viên nếu lớp hiện có không đủ; không viết bộ style riêng |
| `audit/controller/AuditController.java` | 41-50 | Thêm `@RequestParam(required = false) @Size(max = AuditConstants.ACTION_MAX_LENGTH) String query` và `List<@Size(max = AuditConstants.ACTION_MAX_LENGTH) String> actions` (tối đa 50 phần tử, bằng hằng số mới trong `AuditConstants`) |
| `audit/service/AuditQueryService.java` và `service/impl/` | hàm tìm kiếm sự kiện | Truyền `query`, `actions`; chuẩn hóa từ khóa, thoát `%` `_` `\` thành mẫu `LIKE`, bỏ khoảng trắng đầu cuối, rỗng thì coi như không có |
| `audit/repository/AuditEventRepository.java` | 20-24 (`searchEvents`) | Mở rộng truy vấn theo AC-19; dùng truy vấn con trên `User` để khớp tên và tài khoản; xử lý danh sách `actions` rỗng không làm hỏng điều kiện |
| `src/test/.../audit/` (test của `AuditQueryService`/repository) | cuối file | Test AC-19 |
| `frontend/src/features/audit/api/auditApi.ts` và `hooks/useAuditEvents.ts` | tham số truy vấn | Thêm `query`, `actions` vào tham số và `queryKey` |
| `frontend/src/features/audit/pages/AuditPage.tsx` | 77-106 (tham số, truy vấn), 177-246 (thanh lọc) | Thêm ô tìm kiếm đầu thanh lọc (mẫu `BookingFilters.tsx:42-67`), tham số URL `q`, debounce, đổi chữ thành `actions` bằng danh mục nhãn; "Đặt lại" xóa luôn `q` |
| `frontend/src/features/audit/constants/auditCatalog.ts` | hàm tra cứu | Thêm hàm `auditActionCodesMatching(text)` trả mã hành động có nhãn hoặc mô tả khớp (không dấu), tối đa 50 |
| `docs/REPAIR_ORDER_LIFECYCLE.md` hoặc `docs/` mô tả màn hình (nếu có) | mục lịch ca và nhật ký | Ghi tham số mới của hai API và cách tìm kiếm |

## 4. Hợp Đồng Kỹ Thuật

**Mã lỗi mới**:

```java
INVENTORY_ISSUE_BLOCKED(2024, "Stock cannot be issued in the current state", HttpStatus.CONFLICT),
```

Dùng `new ConflictException(ResponseCode.INVENTORY_ISSUE_BLOCKED, "<thông điệp>")` để `authoredDetail` hiển thị lời nhắn, không cần đổi `GlobalExceptionHandler`.

**Endpoint**:

```
GET /api/v1/inventory/documents/issue-candidates?repairOrderId={id}
    @PreAuthorize(SecurityConstants.HAS_ROLE_WAREHOUSE_OR_ADMIN) -> 200 ApiResponse<List<IssueCandidateResponse>>
```

```java
public record IssueCandidateResponse(
        Long estimateItemId, Long partId, String partCode, String partName, String unit,
        BigDecimal reservedQuantity, BigDecimal stockQuantity, BigDecimal unitCost, boolean shortage) {}
```

Nguồn dữ liệu: `estimate_items` loại `PART`, `approval_status = 'APPROVED'`, `reserved_quantity > 0` của các báo giá thuộc lệnh, nối `parts` lấy mã, tên, đơn vị, `stock_quantity`, `purchase_price`. `shortage = stockQuantity < reservedQuantity`. Chỉ đọc, `@Transactional(readOnly = true)`.

**Thông điệp (409/2024)**:

| Trường hợp | Thông điệp |
|---|---|
| Tạo `OUT` có dòng không gắn báo giá | "Dòng xuất kho phải gắn với phụ tùng đã duyệt trong báo giá của lệnh. Hãy chọn từ danh sách phụ tùng chờ xuất." |
| Lệnh đã hóa đơn/đóng/hủy | "Lệnh sửa chữa đã lập hóa đơn, đóng hoặc hủy, không thể xuất kho thêm." |
| Trùng dòng báo giá trong chứng từ | "Mỗi phụ tùng chỉ xuất một lần trong cùng chứng từ." |
| Dòng không khớp phụ tùng đã duyệt của lệnh | "Dòng xuất kho không khớp với phụ tùng đã duyệt của lệnh #<id>." |
| Vượt số giữ chỗ | "Số lượng xuất của <tên> vượt số đã giữ chỗ (<reserved>)." |
| Tồn kho không đủ | "Tồn kho không đủ để xuất <tên>: còn <stock>, cần <qty>." |
| Giữ chỗ của phụ tùng không đủ | "Số lượng giữ chỗ của <tên> không đủ để xuất. Hãy kiểm tra đối soát tồn kho." |

**Frontend (form OUT)**:

```ts
interface IssueCandidate { estimateItemId: string; partId: string; partCode: string; partName: string; unit: string;
  reservedQuantity: number; stockQuantity: number; unitCost: number; shortage: boolean }
```

- Chọn lệnh: `Combobox` (đã dùng ở `CheckInModal.tsx`) với danh sách lệnh qua `useRepairOrdersQuery` (từ khóa tìm theo mã/biển số), loại lệnh `INVOICED`/`CLOSED`/`CANCELLED`.
- Sau khi chọn lệnh: `useIssueCandidatesQuery`, mỗi ứng viên là một dòng có hộp chọn, ô số lượng (mặc định = `reservedQuantity`, tối đa = `reservedQuantity`), cột tồn kho; dòng `shortage` bị khóa kèm "Thiếu hàng, tồn còn X".
- Payload: `items: [{ partId, estimateItemId, quantity, unitCost }]`, `repairOrderId`. Giá vốn chỉ mang tính tham khảo vì backend tính lại lúc ghi sổ.
- Dùng component và class sẵn có (`Field`, `Combobox`, `Badge`, bảng của trang), không thêm style mới; form giữ vừa khung nhìn theo `UI_DESIGN_GUIDELINES.md`.

**Sơ đồ lịch ca và tìm kiếm ca**:

```ts
// features/scheduling/utils/shiftTimeline.ts
export type ShiftChipState = 'upcoming' | 'live' | 'done' | 'cancelled' | 'pending';
export function shiftChipState(shift: Shift, now: number): ShiftChipState;
export function groupShiftsByCell(shifts: readonly Shift[], days: readonly string[]): Map<string, Shift[]>;  // khóa `${employeeId}|${yyyy-mm-dd}`, ca qua nửa đêm xuất hiện ở ngày bắt đầu
export function filterShifts(shifts: readonly Shift[], term: string, ctx: {
  employeeOf: (id: string | number) => Employee | undefined;
  nameOf: (id: string | number) => string; bayOf: (id?: string | number | null) => ServiceBay | undefined;
}): Shift[];                 // normalizeSearchText cả hai phía; từ khóa rỗng trả lại toàn bộ
```

- `ShiftTimelineBoard` props: `shifts`, `employees`, `weekDate`, `onWeekChange`, `canManage`, `onAddShift(employeeId, isoLocal)`, `onCancelShift(shift)`; tự quản lý chip đang chọn như `BookingTimelineBoard`. Dùng `Button`, `Badge`, các lớp `booking-timeline*`/`timeline-*`; hàng tiêu đề kỹ thuật viên thay cho cột giờ.
- Truy vấn ca của sơ đồ: `useShiftsQuery({ from: weekStart, to: weekEnd, size: policy.paginationMaxSize })`; chế độ Danh sách giữ truy vấn cũ.
- `normalizeSearchText`: `value.normalize('NFD').replace(/\p{M}/gu, '').replace(/đ/gi, 'd').toLowerCase().trim().replace(/\s+/g, ' ')`.

**Tìm kiếm nhật ký**:

```
GET /api/v1/audit?query=<text>&actions=A&actions=B     (ADMIN)
JPQL: (:actorId IS NULL OR a.actorUserId = :actorId) AND (:action IS NULL OR a.action = :action)
  AND (:hasQuery = false
       OR LOWER(a.action) LIKE :pattern ESCAPE '\' OR LOWER(a.entityType) LIKE :pattern ESCAPE '\'
       OR (:entityId IS NOT NULL AND a.entityId = :entityId)
       OR a.action IN :actions
       OR a.actorUserId IN (SELECT u.id FROM User u WHERE LOWER(u.fullName) LIKE :pattern ESCAPE '\' OR LOWER(u.username) LIKE :pattern ESCAPE '\'))
```

- `AuditConstants`: thêm `QUERY_MAX_LENGTH = 100` và `ACTIONS_MAX_COUNT = 50`; không dùng số cứng trong controller.
- Không thêm `ResponseCode`; vi phạm kích thước đã đi qua `GlobalExceptionHandler` (2001).
- Frontend: `AuditPage` đọc `q` từ `useTableSearchParams`, `useDebouncedValue` như `actor`/`action`, gọi `auditActionCodesMatching(q)` để tạo `actions`.

**Duyệt báo giá**:

```ts
// features/estimate/utils/lineDecisions.ts
export function buildLineDecisions(
  items: EstimateItem[], decisions: Record<string, Decision>, note?: string,
): LineDecisionInput[];   // chỉ các dòng approvalStatus === 'PENDING'; estimateItemId = Number(it.id); decision = decisions[String(it.id)] ?? 'APPROVED'
export const DECIDABLE_STATUSES: readonly EstimateStatus[] = ['DRAFT', 'SENT', 'PARTIALLY_APPROVED', 'APPROVED'];  // khớp EstimateServiceImpl.java:355-357 (DRAFT được gửi rồi mới duyệt)
```

- `handleSubmit`: nếu `lines.length === 0` thì không gọi API; hiển thị thông báo "Báo giá không còn dòng nào chờ quyết định."
- Thông điệp trong modal dùng `info-strip` có sẵn, không thêm style. Hai nút "Duyệt tất cả/Từ chối tất cả" dùng `disabled`.
- Không đổi kiểu `id` khai báo ở `estimateTypes.ts`; hàm mới chấp nhận cả số và chuỗi bằng `String(...)`.

## 5. Checklist Thực Thi

- [x] **Step 1: Mã lỗi và thông điệp luồng xuất kho (backend)** _(AC-2, AC-3)_
  - Thêm `INVENTORY_ISSUE_BLOCKED`.
  - Thêm kiểm tra `estimateItemId` ngay lúc tạo chứng từ `OUT`.
  - Thay các `BusinessException(BAD_REQUEST)` trong ghi sổ `OUT` theo bảng thông điệp (dòng 238-266, 312-320), giữ nguyên điều kiện.
- [x] **Step 2: Endpoint ứng viên xuất kho (backend)** _(AC-1)_
  - Thêm DTO, `ApiPaths.INVENTORY_ISSUE_CANDIDATES`, hàm service, endpoint.
- [x] **Step 3: Test backend** _(AC-1 → AC-3, AC-8)_
  - Thêm vào `InventoryServiceTest`: ứng viên có và rỗng, lệnh không tồn tại, tạo `OUT` thiếu `estimateItemId`, từng nhánh ghi sổ bị chặn, và nhánh ghi sổ thành công (trừ tồn, trừ giữ chỗ).
  - Chạy `./mvnw test -Dtest=InventoryServiceTest` và test tích hợp kho/hóa đơn.
- [x] **Step 4: Kiểu, API client, hook (frontend)** _(AC-4)_
  - Thêm `estimateItemId`, `IssueCandidate`, `getIssueCandidates`, `useIssueCandidatesQuery`.
- [x] **Step 5: Form "Xuất kho sửa chữa" (frontend)** _(AC-4, AC-5, AC-7)_
  - Thay ô nhập mã số bằng ô chọn lệnh, nạp ứng viên, bảng dòng theo ứng viên, gửi `estimateItemId`.
  - Chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [x] **Step 6: Cập nhật tài liệu** _(AC-5)_
  - Ghi cách xuất kho cho lệnh vào `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.6.
- [ ] **Step 7: Kiểm thử & xác minh** _(tất cả AC)_
  - Bật `mysql-test`, chạy `./mvnw test`; build lại container theo `README.md`.
  - Trên trình duyệt với lệnh #21: mở "Xuất kho sửa chữa", chọn lệnh, thấy Lốp Michelin; tạo chứng từ nháp; ghi sổ; kiểm tra màn Phụ tùng (tồn giảm 1, giữ chỗ 0); mở hộp phát hành hóa đơn thấy #21 lập được (AC-6).

- [x] **Step 8: Hàm lập quyết định dòng báo giá và test (frontend)** _(AC-9, AC-11)_
  - Thêm `lineDecisions.ts` và `lineDecisions.test.ts` (id số và chuỗi, dòng đã quyết định, ghi chú, trạng thái).
  - Chạy `npm --prefix frontend test`.
- [x] **Step 9: Sửa `EstimateDecisionModal` và danh sách báo giá** _(AC-9, AC-10, AC-11, AC-12)_
  - Dùng `buildLineDecisions`; ẩn nút lưu và khóa nút duyệt/từ chối tất cả khi không còn dòng chờ; dòng giải thích; nhãn nút ở danh sách.
  - Chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [ ] **Step 10: Xác minh duyệt báo giá trên trình duyệt** _(AC-9 → AC-12)_
  - Build lại container; mở báo giá #4 (đã duyệt hết): không có nút lưu, có giải thích.
  - Tạo báo giá phát sinh cho một lệnh còn duyệt được, gửi khách, duyệt một dòng và từ chối một dòng: lưu thành công, việc sửa chữa và giữ phụ tùng được tạo cho dòng được duyệt.

- [x] **Step 11: API ca theo khoảng ngày (backend)** _(AC-16)_
  - Thêm `from`/`to` vào controller, service, repository `searchShifts`; thêm test (ca qua nửa đêm, biên khoảng, kết hợp bộ lọc).
  - Chạy `./mvnw test -Dtest=<test ca>`.
- [x] **Step 12: Tiện ích văn bản và ca (frontend)** _(AC-13, AC-17)_
  - Thêm `shared/utils/text.ts`, `shiftTimeline.ts` và test Vitest (gom ô, tình trạng chip, lọc theo từ khóa có/không dấu).
  - Chạy `npm --prefix frontend test`.
- [x] **Step 13: Sơ đồ tuần ca làm việc (frontend)** _(AC-13, AC-14, AC-15)_
  - Thêm `ShiftTimelineBoard`, truyền ngày gợi ý vào `ShiftEditor`, công tắc Sơ đồ/Danh sách và truy vấn theo tuần trong `SchedulingPage`; thêm biến thể CSS tối thiểu.
- [x] **Step 14: Ô tìm kiếm ca (frontend)** _(AC-15, AC-17)_
  - Thêm `shiftKeyword` và ô tìm kiếm kiểu `user-search` vào bộ lọc tab Ca làm việc; áp dụng cho sơ đồ và danh sách; chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [x] **Step 15: Tìm kiếm nhật ký (backend)** _(AC-19)_
  - Thêm hằng số, tham số controller, service, truy vấn repository; thêm test (khớp từng trường, `%`/`_`, `actions` rỗng, kết hợp bộ lọc cũ).
  - Chạy `./mvnw test -Dtest=<test nhật ký>`.
- [x] **Step 16: Ô tìm kiếm trên màn Nhật ký (frontend)** _(AC-18, AC-19)_
  - Thêm `auditActionCodesMatching`, tham số `query`/`actions` trong API và hook, ô tìm kiếm và tham số URL `q` trong `AuditPage`, "Đặt lại" xóa `q`; chạy lint, `tsc`, test.
- [ ] **Step 17: Kiểm thử & xác minh sơ đồ ca và tìm kiếm** _(AC-13 → AC-20)_
  - Bật `mysql-test`, chạy `./mvnw test`; build lại container theo `README.md`.
  - Trên trình duyệt: xem sơ đồ tuần ca (dữ liệu có ca tuần này), bấm chip, hủy một ca thử, bấm `+` xếp ca; gõ "an", "sáng", "B01" để tìm; chuyển Sơ đồ/Danh sách; mở Nhật ký, gõ "đăng nhập", tên người dùng, mã hành động, thử `%`.

- [x] **Step 18 (converge): Bổ sung test hai nhánh ghi sổ xuất kho còn thiếu** _(AC-3)_
  - Trong `src/test/.../inventory/service/InventoryServiceTest.java` thêm: (a) ghi sổ `OUT` có hai dòng cùng `estimateItemId` → 409/`INVENTORY_ISSUE_BLOCKED`, thông điệp chứa "chỉ xuất một lần" (`InventoryServiceImpl.java` nhánh `approvedItems.containsKey`); (b) ghi sổ `OUT` khi `parts.reserved_quantity` nhỏ hơn số xuất (dòng báo giá vẫn đủ) → thông điệp chứa "giữ chỗ" và "không đủ để xuất"; (c) dòng báo giá không tồn tại → `ResourceNotFoundException`.
  - Chạy `./mvnw test -Dtest=InventoryServiceTest`.
- [x] **Step 19 (converge): Thông báo "không khớp" ở chế độ Danh sách của lịch ca** _(AC-17)_
  - Trong `frontend/src/features/scheduling/pages/SchedulingPage.tsx` (`DataTable<Shift>`, khoảng dòng 943-948) truyền `emptyTitle="Không có ca nào khớp với từ khóa"` và `emptyDescription` ngắn khi `shiftKeyword.trim()` khác rỗng; giữ nguyên thông báo mặc định khi không tìm.
  - Chạy `npm --prefix frontend run lint`, `tsc`, `test`.

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-06. Kết luận: **partial** (mọi mục trong phạm vi đã có code và test; các mục kiểm chứng bằng giao diện và gọi API thật chưa xác minh, nên Step 7, 10, 17 vẫn mở).

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Endpoint `issue-candidates` | Đạt | `InventoryServiceImpl.getIssueCandidates`; test ánh xạ dòng, cờ thiếu hàng, lệnh không tồn tại; chưa gọi API thật trên lệnh #21 |
| AC-2 Chặn dòng `OUT` không gắn báo giá lúc tạo | Đạt | Test `createDocument_OutWithoutEstimateItem_Blocked` |
| AC-3 Thông điệp 409/2024 cho từng trường hợp ghi sổ | Đạt | Sau Step 18: test lệnh đã hóa đơn, dòng trùng, không khớp, vượt giữ chỗ, giữ chỗ phụ tùng không đủ, tồn kho không đủ, dòng báo giá không tồn tại; `InventoryServiceTest` 20/20 |
| AC-4 Form chọn lệnh theo mã/biển số và nạp phụ tùng chờ xuất | Chưa xác minh | `InventoryDocumentsPage.tsx` (Combobox, bảng ứng viên); lint, `tsc`, build qua; chưa thử trên trình duyệt |
| AC-5 Ghi sổ trừ tồn, giữ chỗ, `repair_order_parts` | Đạt (backend) / chưa xác minh UI | Test `postDocument_OutSuccess_ReducesStockAndReservation` và test tích hợp kho/hóa đơn; chưa thử trọn luồng trên giao diện |
| AC-6 Xuất kho #21 rồi lập hóa đơn | Chưa xác minh | Cần thao tác tay |
| AC-7 Dòng thiếu hàng bị khóa | Chưa xác minh | Code `disabled={c.shortage}`; chưa thử trên giao diện |
| AC-8 Test hiện có vẫn qua (nhóm xuất kho) | Đạt | Toàn bộ 545 test backend |
| AC-9 Lưu quyết định gửi đủ dòng chờ, id số | Đạt (hàm) / chưa xác minh UI | `lineDecisions.test.ts` (id số và chuỗi); chưa thử duyệt trên giao diện |
| AC-10 Báo giá hết dòng chờ không có nút lưu | Chưa xác minh | `EstimateDecisionModal.tsx`; chưa mở báo giá #4 |
| AC-11 Trạng thái không duyệt được | Đạt (hàm) / chưa xác minh UI | `canDecideEstimate` có test cho 8 trạng thái |
| AC-12 Nhãn nút danh sách báo giá | Chưa xác minh | `EstimatesPage.tsx`; chưa thử trên giao diện |
| AC-13 Sơ đồ tuần ca làm việc | Chưa xác minh | `ShiftTimelineBoard.tsx`; test hàm gom ô và tình trạng chip; chưa thử trên giao diện |
| AC-14 Chi tiết ca, hủy ca, nút `+` điền sẵn | Chưa xác minh | `ShiftEditor` nhận `initialDate`; chưa thử trên giao diện |
| AC-15 Công tắc Sơ đồ/Danh sách giữ từ khóa | Chưa xác minh | Cùng state `shiftKeyword`; chưa thử trên giao diện |
| AC-16 `GET /employee-shifts?from&to` | Đạt | 6 test tích hợp MySQL: ca qua nửa đêm hai đầu tuần, biên nửa mở, kết hợp bộ lọc, khoảng ngược, không truyền khoảng |
| AC-17 Tìm ca bằng chữ, không dấu | Đạt | Sau Step 19: hàm `filterShifts` có test; sơ đồ và Danh sách đều hiện "Không có ca nào khớp với từ khóa"; thao tác trên giao diện chưa xác minh |
| AC-18 Ô tìm kiếm trên màn Nhật ký | Chưa xác minh | `AuditPage.tsx`; chưa thử trên giao diện |
| AC-19 `GET /admin/audit-events?query&actions` | Đạt | 8 test tích hợp MySQL: khớp từng trường, mã do client suy từ nhãn, `%` và `_` theo nghĩa đen, kết hợp VÀ với bộ lọc cũ, giới hạn kích thước; `auditCatalog.test.ts` cho hàm suy mã |
| AC-20 Test hiện có vẫn qua | Đạt | Backend 545/545, Vitest 149/149 (một lần chạy có 2 test trang đăng ký lỗi do máy bận, chạy lại qua) |

Lỗi phát hiện và đã sửa trong quá trình: converge lần đầu thiếu test ba nhánh ghi sổ xuất kho (dòng trùng, giữ chỗ phụ tùng không đủ, dòng báo giá không tồn tại) và thông báo "không khớp" ở Danh sách lịch ca còn là câu mặc định, đã xử lý ở Step 18 và 19.

Còn cần kiểm tay trên container mới:

- Xuất kho: mở "Xuất kho sửa chữa", chọn lệnh #21, thấy Lốp Michelin; tạo chứng từ nháp; ghi sổ; kiểm tra tồn giảm 1 và giữ chỗ về 0; hộp phát hành hóa đơn cho #21 lập được.
- Duyệt báo giá: mở #4 thấy giải thích, không có nút lưu; tạo báo giá phát sinh, duyệt một dòng và từ chối một dòng.
- Lịch ca: xem sơ đồ tuần, bấm chip, bấm `+`, hủy một ca thử, chuyển Sơ đồ/Danh sách, gõ "an", "sáng", mã khoang.
- Nhật ký: gõ "đăng nhập", tên người dùng, mã hành động, thử `%`.

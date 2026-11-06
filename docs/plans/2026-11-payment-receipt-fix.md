# Kế Hoạch Sửa Luồng Thu Tiền Hóa Đơn Và Mã Giao Dịch Tự Sinh, Chỉnh Sửa Được

> **Mã plan**: `payment-receipt-fix`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-invoice-blocked-reasons.md` (lý do bị nuốt thành mã 2004) và `plans/2026-11-inventory-issue-for-order.md`. Lần này là bước thu tiền sau khi lập hóa đơn.

Đường dẫn Java dưới đây tính từ `src/main/java/com/Group13_65CNTT/AutoGarage/`. Đường dẫn frontend tính từ `frontend/src/`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng

**Nhập đủ số tiền vẫn không thu được: nguyên nhân là ô nhập số tiền, không phải máy chủ.**

- `features/billing/pages/InvoicesPage.tsx:744-754` khai báo ô số tiền `type="number" min="1" max={số còn nợ} step="1000"`. Với trình duyệt, **mốc bước tính từ `min`**, nên chỉ các giá trị `1, 1.001, 2.001, …` là hợp lệ. Một số tiền tròn như 3.168.000 hay đúng số còn nợ đều **không hợp lệ**: trình duyệt chặn nút "Xác nhận thu tiền" bằng thông báo "Please enter a valid value. The two nearest valid values are …" và không gửi gì lên máy chủ. Form không có `noValidate` và không tự hiển thị lỗi, nên người dùng thấy như bấm không có tác dụng.
- Giá trị điền sẵn là đúng số còn nợ (`InvoicesPage.tsx:167`), nên **ngay cả khi không sửa gì** cũng không bấm thu được.
- Dữ liệu thật xác nhận: hóa đơn #10 (tổng 3.168.000 đ) có hai phiếu thu 1.793.001 đ và 9.001 đ (đuôi `001` là số khách phải gõ để lách lưới bước). Số còn nợ 1.365.998 đ không nằm trên lưới (gần nhất 1.365.001 và 1.366.001), nên **hóa đơn #10 không thể thu đủ qua giao diện** và kẹt ở `PARTIALLY_PAID`.
- Cùng lỗi ở phiếu chi nhà cung cấp: `features/billing/pages/PaymentsPage.tsx:561-566` (`min="1" step="1000"`).
- Khi gửi được lên máy chủ, `BillingServiceImpl.receivePayment` (`billing/service/impl/BillingServiceImpl.java:462-529`) ném `BusinessException(BAD_REQUEST)` không lời nhắn cho: hóa đơn đã hủy (dòng 479), đã thu đủ (dòng 482), số tiền vượt số còn nợ (dòng 489); trùng mã tham chiếu là `DUPLICATE_RESOURCE` không lời nhắn (dòng 496). `GlobalExceptionHandler.authoredDetail` bỏ lời nhắn của loại exception này nên người dùng chỉ thấy câu chung (xem hai plan trước).

**Mã giao dịch hiện không sinh trước và không sửa được.**

- Ô "Mã giao dịch / Mã chuẩn chi" (`InvoicesPage.tsx:772-778`) thực chất là `providerReference` (mã tham chiếu của ngân hàng hoặc cổng thanh toán), để trống thì lưu rỗng.
- Mã giao dịch thật của hệ thống là `transactionCode`, do máy chủ tự sinh dạng `TX-IN-<8 ký tự ngẫu nhiên>` (`BillingServiceImpl.java:500`; phiếu chi `TX-PAY-…` ở `paySupplier` và chi lương, dòng 552). Người dùng không thấy trước và không sửa được.
- Dữ liệu có ba kiểu mã: dữ liệu mẫu `PT-yyyyMMdd-NNNN` (thu) và `PC-yyyyMMdd-NNNN` (chi), còn mã tự sinh là `TX-IN-…`, `TX-PAY-…`. Cột `payments.transaction_code` là `VARCHAR(100)` duy nhất (`uq_payments_code`), `provider_reference` cũng duy nhất.
- Trang Thanh toán (`PaymentsPage.tsx:166,191`) hiển thị `transactionCode` làm mã chính và `Mã GD: <providerReference>` bên dưới, dễ nhầm hai loại mã.

### 1.2. Mục tiêu

- Thu được đúng số còn nợ (và mọi số nguyên đồng từ 1 đến số còn nợ) cho hóa đơn, kể cả hóa đơn #10; thu đủ thì hóa đơn chuyển `PAID`.
- Ô số tiền có nút "Thu đủ số còn lại"; số tiền sai (không dương, vượt số còn nợ) báo ngay dưới ô bằng tiếng Việt.
- **Mã giao dịch** tự sinh theo chuẩn `PT-yyyyMMdd-NNNN` (thu) và `PC-yyyyMMdd-NNNN` (chi) ngay khi mở form, **sửa được**, có nút "Sinh lại"; bỏ trống thì máy chủ tự sinh. Mã trùng báo rõ.
- Tách rõ hai mã: "Mã giao dịch" (mã phiếu của hệ thống) và "Mã tham chiếu ngân hàng / cổng thanh toán" (tùy chọn).
- Mọi lý do máy chủ từ chối thu tiền hiển thị bằng tiếng Việt, mã nghiệp vụ 409 thay cho câu chung của mã 2004.

### 1.3. Ngoài phạm vi

- Không đổi cách tính trạng thái hóa đơn (`PARTIALLY_PAID`, `PAID`), hạn mức công nợ hay điều kiện bàn giao.
- Không hoàn tiền, hủy hoặc sửa phiếu thu đã ghi; không in phiếu thu.
- Không đổi mã của các phiếu đã có (dữ liệu cũ giữ nguyên).
- Không đổi mã chi lương (`recordPayrollDisbursement`, `TX-PAY-…`) vì không có form nhập.
- Các ô tiền khác cũng đang dùng `step="1000"` (`InvoicesPage.tsx:695,706` thuế/giảm giá, `ServiceEditor.tsx:224`, `EstimateEditor.tsx:291`, `CreateEstimateModal.tsx:172`, `PartsPage.tsx:710-861`, `InventoryDocumentsPage.tsx:1050`) không nằm trong plan này: chúng dùng `min="0"` nên mốc bước là bội số của 1.000, chỉ chặn số lẻ nghìn. Ghi nhận để rà soát riêng.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Trên form thu tiền, nhập đúng số còn nợ (ví dụ 1.365.998 của hóa đơn #10, hoặc 3.168.000 của hóa đơn mới) rồi bấm "Xác nhận thu tiền" thu thành công; thu đủ thì trạng thái hóa đơn thành `PAID`. Nhập số lẻ bất kỳ từ 1 đến số còn nợ cũng thu được. _Kiểm chứng_: unit test Vitest cho hàm kiểm tra số tiền, và thao tác trình duyệt trên hóa đơn #10.
- **AC-2**: Ô số tiền không còn `step="1000"`; form dùng `noValidate`. Số tiền ≤ 0 hoặc lớn hơn số còn nợ hiện lỗi dưới ô ("Số tiền thu phải lớn hơn 0", "Số tiền vượt số còn nợ …") và không gọi API. Nút "Thu đủ số còn lại" điền đúng số còn nợ. _Kiểm chứng_: unit test Vitest và thao tác trình duyệt.
- **AC-3**: Form lập phiếu chi nhà cung cấp (`PaymentsPage.tsx`) có cùng cách xử lý số tiền như AC-1, AC-2. _Kiểm chứng_: unit test dùng chung hàm kiểm tra và thao tác trình duyệt.
- **AC-4**: `GET /api/v1/billing/payments/suggest-code?direction=IN|OUT` trả mã chưa dùng `PT-yyyyMMdd-NNNN` (IN) hoặc `PC-yyyyMMdd-NNNN` (OUT); ngày theo múi giờ xưởng; `NNNN` là số lớn nhất đang có cùng tiền tố trong ngày cộng 1, đệm 4 chữ số, bắt đầu `0001`. _Kiểm chứng_: test tích hợp MySQL với dữ liệu thật của bảng `payments`.
- **AC-5**: Form thu tiền và phiếu chi mở ra đã điền sẵn "Mã giao dịch" gợi ý; người dùng sửa được và bấm "Sinh lại" để lấy mã mới. Bỏ trống ô rồi gửi thì máy chủ tự sinh mã cùng chuẩn. _Kiểm chứng_: thao tác trình duyệt và unit test service.
- **AC-6**: `PaymentReceiveRequest` và `SupplierPaymentRequest` nhận thêm `transactionCode` (tùy chọn). Máy chủ cắt khoảng trắng, đổi sang chữ in hoa, chỉ chấp nhận chữ, số và `-`, `_`, `.`, tối đa 50 ký tự (`400/2001` kèm thông điệp khi sai định dạng). Mã đã tồn tại trả 409/`PAYMENT_BLOCKED` "Mã giao dịch đã tồn tại. Hãy nhập mã khác hoặc bấm Sinh lại." Phản hồi trả đúng mã đã lưu. _Kiểm chứng_: unit test service và test tích hợp.
- **AC-7**: Thu tiền bị từ chối trả 409/`PAYMENT_BLOCKED` (2025) với thông điệp riêng bằng tiếng Việt: hóa đơn đã hủy; hóa đơn đã thu đủ; số tiền vượt số còn nợ (nêu số còn nợ); mã tham chiếu đã dùng cho giao dịch khác. Không còn trả 2004 cho các trường hợp này. _Kiểm chứng_: unit test từng nhánh.
- **AC-8**: Form ghi hai ô tách biệt: "Mã giao dịch" (`transactionCode`) và "Mã tham chiếu ngân hàng / cổng thanh toán" (`providerReference`, tùy chọn). Trang Thanh toán ghi "Mã tham chiếu:" thay cho "Mã GD:" khi hiển thị `providerReference`. _Kiểm chứng_: thao tác trình duyệt.
- **AC-9**: Phát lại cùng `requestKey` vẫn trả lại phiếu thu cũ, không tạo phiếu thứ hai. _Kiểm chứng_: test `receivePayment` hiện có vẫn qua.
- **AC-10**: Test hiện có vẫn qua: `BillingServiceTest`, test tích hợp kho/hóa đơn, Vitest, lint, `tsc`. _Kiểm chứng_: `./mvnw test` (bật `mysql-test`) và `npm --prefix frontend test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `common/enums/ResponseCode.java` | sau `INVENTORY_ISSUE_BLOCKED` (dòng 61-62) | Thêm `PAYMENT_BLOCKED(2025, "Payment cannot be recorded in the current state", HttpStatus.CONFLICT)` |
| `billing/constant/PaymentCodePolicy.java` | file mới | Hằng số: tiền tố `PT`/`PC`, định dạng ngày, độ rộng số thứ tự (4), độ dài tối đa (50), mẫu ký tự cho phép |
| `billing/dto/request/PaymentReceiveRequest.java` | dòng 14-30 | Thêm `transactionCode` với `@Size(max = 50)` và `@Pattern` có thông điệp |
| `billing/dto/request/SupplierPaymentRequest.java` | cạnh `providerReference` | Thêm `transactionCode` như trên |
| `billing/repository/PaymentRepository.java` | cạnh `existsByTransactionCode` (dòng 19-21) | Thêm truy vấn `findMaxTransactionCodeStartingWith(prefix)` (JPQL `MAX` trên `LIKE CONCAT(:prefix, '%')`) |
| `billing/service/BillingService.java` | cạnh `receivePayment` | Thêm `String suggestTransactionCode(PaymentDirection direction)` |
| `billing/service/impl/BillingServiceImpl.java` | `receivePayment` 462-529; `paySupplier` ~545-620 | Hàm `resolveTransactionCode(requested, direction)` dùng chung (chuẩn hóa, kiểm tra trùng, tự sinh khi trống, thử lại tối đa vài lần nếu đụng độ); thay các `BusinessException` không lời nhắn bằng `ConflictException(PAYMENT_BLOCKED, "…")`; thêm `suggestTransactionCode` |
| `billing/controller/PaymentController.java` | gần dòng 50-75 | Thêm `GET /suggest-code`; `@PreAuthorize` theo hướng: `IN` dùng quyền của `receive` (`HAS_ROLE_RECEPTION_OR_ADMIN`), `OUT` dùng quyền của `pay-supplier` (`HAS_ROLE_ADMIN`) |
| `common/constant/ApiPaths.java` | cạnh `PAYMENTS_PAY_SUPPLIER` (dòng 102-103) | Thêm `PAYMENTS_SUGGEST_CODE = "/suggest-code"` |
| `src/test/.../billing/service/BillingServiceTest.java` | cuối file | Test AC-6, AC-7, AC-9 |
| `src/test/.../billing/PaymentCodeIntegrationTest.java` | file mới | Test AC-4 và AC-6 trên MySQL (`ActiveProfiles("test")`, mẫu `EmployeeShiftRangeIntegrationTest`) |
| `frontend/src/features/billing/utils/paymentAmount.ts` và `.test.ts` | file mới | `validatePaymentAmount(amount, balance)` trả thông báo hoặc `null`; test các biên (0, 1, đúng số còn nợ, vượt 1 đồng, số lẻ) |
| `frontend/src/features/billing/types/billing.ts` | 100-115 | Thêm `transactionCode?` vào `PaymentReceiveInput` và `SupplierPaymentInput` |
| `frontend/src/features/billing/api/billingApi.ts` và `hooks/useBilling.ts` | cạnh API/hook thanh toán | Thêm `suggestPaymentCode(direction)` và `usePaymentCodeSuggestionQuery` (không cache, lấy mới mỗi lần mở) |
| `frontend/src/features/billing/pages/InvoicesPage.tsx` | 81-92, 164-200 (`handleOpenPayment`, `handleReceivePayment`), 733-805 (modal) | Trạng thái mã giao dịch và lỗi số tiền; `noValidate`; bỏ `step`/`min` cứng; nút "Thu đủ số còn lại"; ô "Mã giao dịch" có "Sinh lại"; ô "Mã tham chiếu ngân hàng" |
| `frontend/src/features/billing/pages/PaymentsPage.tsx` | 95-130, 555-600 (phiếu chi), 166-195 (hiển thị mã) | Áp dụng như trên cho phiếu chi; đổi nhãn "Mã GD:" thành "Mã tham chiếu:" |
| `docs/api/` (tệp mô tả API thanh toán nếu có, ngược lại `docs/REPAIR_ORDER_LIFECYCLE.md` mục 2.6) | mục thanh toán | Ghi `transactionCode`, `suggest-code`, mã lỗi 2025 |

## 4. Hợp Đồng Kỹ Thuật

**Mã lỗi mới**:

```java
PAYMENT_BLOCKED(2025, "Payment cannot be recorded in the current state", HttpStatus.CONFLICT),
```

Dùng `new ConflictException(ResponseCode.PAYMENT_BLOCKED, "<thông điệp>")` (có sẵn hàm tạo `(ResponseCode, String)`) để `authoredDetail` hiển thị lời nhắn mà không đổi handler.

**Thông điệp (409/2025)**:

| Trường hợp | Thông điệp |
|---|---|
| Hóa đơn đã hủy | "Hóa đơn đã bị hủy, không thể thu tiền." |
| Hóa đơn đã thu đủ | "Hóa đơn đã được thu đủ." |
| Số tiền vượt số còn nợ | "Số tiền thu vượt số còn nợ (<còn nợ> đ)." |
| Mã tham chiếu trùng | "Mã tham chiếu <mã> đã được dùng cho giao dịch khác." |
| Mã giao dịch trùng | "Mã giao dịch <mã> đã tồn tại. Hãy nhập mã khác hoặc bấm Sinh lại." |

Số tiền ≤ 0 giữ lỗi kiểm tra dữ liệu (`@DecimalMin`, 400/2001).

**Mã giao dịch**:

```
GET /api/v1/billing/payments/suggest-code?direction=IN   -> 200 ApiResponse<String>   ví dụ "PT-20261106-0001"
```

- `PaymentCodePolicy`: `PT` cho `IN`, `PC` cho `OUT`; định dạng `{tiền tố}-{yyyyMMdd}-{NNNN}`; ngày theo múi giờ xưởng (`ShiftPolicy.ZONE`).
- `suggestTransactionCode`: lấy mã lớn nhất cùng tiền tố hôm nay (`MAX`), cộng 1; mã lớn nhất không đúng định dạng thì bỏ qua và dùng `0001`; vượt `9999` thì thêm chữ số, không cắt.
- `resolveTransactionCode(requested, direction)`: trống thì sinh bằng quy tắc trên và kiểm tra `existsByTransactionCode`, đụng độ thì thử lại tối đa 5 lần; có giá trị thì chuẩn hóa (trim, in hoa) rồi kiểm tra định dạng và trùng.
- Đây chỉ là gợi ý: hai người mở form cùng lúc có thể nhận cùng mã; người gửi sau gặp 409 "Mã giao dịch … đã tồn tại" và bấm "Sinh lại". Ràng buộc `uq_payments_code` vẫn là chốt chặn cuối.

**Frontend**:

```ts
// features/billing/utils/paymentAmount.ts
export function validatePaymentAmount(amount: number, balance: number): string | null;
// null khi 1 <= amount <= balance và là số hữu hạn; ngược lại thông báo tiếng Việt
```

- Ô số tiền: `type="number" min="1" step="any"` hoặc `inputMode="numeric"` cùng `noValidate` trên form, lỗi hiển thị bằng prop `error` của `Field` có sẵn.
- Nút "Thu đủ số còn lại": đặt `amount = balance`.
- Ô "Mã giao dịch": `className="input font-mono"`, giá trị gợi ý nạp khi mở modal; nút "Sinh lại" gọi lại `suggest-code`; lỗi 409 mã trùng gợi ý bấm "Sinh lại". Dùng `Field`, `Button` có sẵn, không thêm style.

## 5. Checklist Thực Thi

- [x] **Step 1: Backend mã lỗi, mã giao dịch và thông điệp thu tiền** _(AC-4, AC-5, AC-6, AC-7)_
  - Thêm `PAYMENT_BLOCKED`, `PaymentCodePolicy`, trường `transactionCode` ở hai DTO, truy vấn `findMaxTransactionCodeStartingWith`, `resolveTransactionCode`, `suggestTransactionCode`, endpoint `suggest-code`, và thay các `BusinessException` không lời nhắn trong `receivePayment`/`paySupplier`.
- [x] **Step 2: Test backend** _(AC-4, AC-6, AC-7, AC-9, AC-10)_
  - Thêm vào `BillingServiceTest`: từng nhánh thông điệp, mã tự sinh khi trống, mã nhập tay được chuẩn hóa, mã trùng, định dạng sai, phát lại `requestKey`.
  - Thêm `PaymentCodeIntegrationTest` (MySQL): đánh số tiếp theo trong ngày, tiền tố riêng cho IN và OUT, mã lạ không phá việc đánh số.
  - Chạy `./mvnw test -Dtest=BillingServiceTest,PaymentCodeIntegrationTest` (bật `mysql-test`).
- [x] **Step 3: Hàm kiểm tra số tiền (frontend)** _(AC-1, AC-2, AC-3)_
  - Thêm `paymentAmount.ts` và test Vitest; chạy `npm --prefix frontend test`.
- [x] **Step 4: API client, hook và kiểu (frontend)** _(AC-5)_
  - Thêm `transactionCode` vào kiểu, `suggestPaymentCode`, `usePaymentCodeSuggestionQuery`.
- [x] **Step 5: Form thu tiền hóa đơn** _(AC-1, AC-2, AC-5, AC-8)_
  - Sửa modal trong `InvoicesPage.tsx` theo hợp đồng: bỏ `step`, `noValidate`, lỗi dưới ô, nút "Thu đủ số còn lại", ô "Mã giao dịch" có "Sinh lại", ô "Mã tham chiếu ngân hàng".
- [x] **Step 6: Phiếu chi nhà cung cấp và nhãn trang Thanh toán** _(AC-3, AC-5, AC-8)_
  - Áp dụng như Step 5 cho `PaymentsPage.tsx`, đổi nhãn "Mã GD:" thành "Mã tham chiếu:".
  - Chạy `npm --prefix frontend run lint`, `tsc`, `test`.
- [x] **Step 7: Cập nhật tài liệu** _(AC-6)_
  - Ghi `transactionCode`, `suggest-code`, mã lỗi 2025 vào tài liệu API/lifecycle.
- [ ] **Step 8: Kiểm thử & xác minh** _(tất cả AC)_
  - Bật `mysql-test`, chạy `./mvnw test`; build lại container theo `README.md`.
  - Trên trình duyệt: mở hóa đơn #10, form thấy sẵn số còn nợ 1.365.998 và mã `PT-…`; bấm "Xác nhận thu tiền" thu được, hóa đơn thành "Đã thu đủ"; thử nhập số lẻ, số vượt còn nợ (thấy lỗi dưới ô), sửa mã giao dịch, nhập mã trùng (thấy 409), để trống mã (máy chủ tự sinh); thử lập phiếu chi nhà cung cấp.

- [x] **Step 9 (converge): Bổ sung test kiểm tra dữ liệu và luồng phiếu chi** _(AC-5, AC-6)_
  - Trong `src/test/.../billing/` thêm test dùng `jakarta.validation` cho `PaymentReceiveRequest` và `SupplierPaymentRequest`: `transactionCode` rỗng và chữ thường hợp lệ; ký tự lạ (khoảng trắng, `/`) và quá 50 ký tự bị từ chối kèm thông điệp (`PaymentReceiveRequest.java`, `SupplierPaymentRequest.java`).
  - Trong `BillingServiceTest` thêm cho `paySupplier`: bỏ trống mã thì mã lưu có tiền tố `PC-` và ngày hôm nay; mã nhập tay được chuẩn hóa; mã đã tồn tại trả 409/`PAYMENT_BLOCKED`; mã tham chiếu trùng trả thông điệp riêng (`BillingServiceImpl.paySupplier`).
  - Chạy `./mvnw test -Dtest=BillingServiceTest`.

## 6. Kết Quả Hội Tụ (lưu trữ tạm khi bắt đầu plan mới)

- **Kết luận**: partial. Step 1–7 và Step 9 xong; backend 565/565 test và frontend 155/155 đạt trước Step 9, nhóm billing 56/56 sau Step 9.
- **Còn thiếu**: Step 8 (xác minh trên trình duyệt) chưa làm vì không có phiên đăng nhập. Cần thử thu hóa đơn #10, sửa/sinh lại mã, mã trùng và phiếu chi nhà cung cấp. AC-1/2/3/5/8 mới có test mã, chưa xác minh giao diện.

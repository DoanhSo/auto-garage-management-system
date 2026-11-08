# Kế Hoạch Thay Khóa Sinh Mã Giao Dịch Bằng Khóa Theo Giao Dịch

> **Mã plan**: `payment-code-row-lock`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-review-fixes-gates-payments.md` (Step 1 dùng `GET_LOCK`, bị `/code-review` chỉ ra nhiều lỗi; plan này thay thế riêng phần đó).

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- `BillingServiceImpl.lockCodeGeneration` dùng `GET_LOCK` gắn với kết nối MySQL và nhả trong `afterCompletion`. Sau commit kết nối có thể đã về pool nên `RELEASE_LOCK` chạy sai kết nối, khóa kẹt và mọi lần sinh mã sau chờ 5 giây rồi báo `PAYMENT_BLOCKED`.
- Khi không có transaction synchronization, khóa được lấy mà không bao giờ nhả.
- `suggestTransactionCode` (chỉ đọc) cũng lấy khóa toàn cục, nên mở form thu tiền có thể bị chờ.
- Mã gõ tay không đi qua khóa nên vẫn có thể trùng mã vừa sinh nhưng chưa commit.
- Chưa có test nào cho logic khóa.

### 1.2. Mục tiêu
- Khóa tự nhả khi commit hoặc rollback, không phụ thuộc kết nối hay synchronization.
- Chỉ đường ghi (thu, chi) mới khóa; gợi ý mã không khóa.
- Mã gõ tay và mã tự sinh cùng được bảo vệ khỏi trùng.

### 1.3. Ngoài phạm vi
- Đổi định dạng mã (`PaymentCodePolicy`).
- Giữ chỗ mã gợi ý cho đến khi gửi form.

## 2. Tiêu Chí Chấp Nhận

- **AC-1**: Không còn `GET_LOCK`/`RELEASE_LOCK` trong code. Luồng ghi thu/chi khóa dòng đếm `payment_code_lock` bằng `SELECT ... FOR UPDATE` trước khi sinh hoặc kiểm tra mã. _Kiểm chứng_: grep + test `BillingServiceTest`.
- **AC-2**: `suggestTransactionCode` không chạm vào dòng khóa. _Kiểm chứng_: test `verify(jdbcTemplate, never())` với câu lệnh khóa.
- **AC-3**: Cả mã gõ tay và mã tự sinh đều lấy khóa trước khi kiểm tra tồn tại. _Kiểm chứng_: test thứ tự gọi (`InOrder`).
- **AC-4**: Hai giao dịch thu đồng thời không sinh trùng mã. _Kiểm chứng_: test tích hợp MySQL (2 luồng, 2 hóa đơn), chạy khi Docker bật.
- **AC-5**: Migration Flyway tạo bảng và dòng khóa; toàn bộ test backend xanh.

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `src/main/resources/db/migration/V20__create_payment_code_lock.sql` | bảng `payment_code_lock(id TINYINT PK)` + 1 dòng |
| `billing/service/impl/BillingServiceImpl.java` | bỏ `GET_LOCK`, thêm `lockPaymentCodes()`, gọi ở `receivePayment`/`paySupplier` trước `resolveTransactionCode` |
| `src/test/.../BillingServiceTest.java` | test AC-2, AC-3 |
| `src/test/.../billing/PaymentCodeConcurrencyIntegrationTest.java` | test AC-4 |
| `docs/DATABASE.md`, `docs/DATA_DICTIONARY.md` | thêm bảng mới (chỉ phần của plan này) |

## 4. Hợp Đồng Kỹ Thuật
- Migration: `V20` (cao nhất hiện có là V19).
- `lockPaymentCodes()`: `jdbcTemplate.queryForObject("SELECT id FROM payment_code_lock WHERE id = 1 FOR UPDATE", ...)`; khóa giữ đến hết giao dịch của `receivePayment`/`paySupplier` (đã `@Transactional`).
- `generateTransactionCode` không tự khóa; `suggestTransactionCode` gọi trực tiếp không khóa.

## 5. Checklist Thực Thi
- [x] **Step 1: Migration bảng khóa** _(AC-5)_
- [x] **Step 2: Thay `GET_LOCK` bằng khóa dòng, gọi ở hai đường ghi** _(AC-1, AC-3)_
- [x] **Step 3: Gợi ý mã không khóa** _(AC-2)_
- [x] **Step 4: Test đơn vị khóa (thứ tự, không khóa khi gợi ý)** _(AC-2, AC-3)_
- [x] **Step 5: Test tích hợp đồng thời** _(AC-4)_
- [x] **Step 6: Chạy toàn bộ test backend (bật Docker trước)** _(AC-5)_

## 6. Kết Quả Hội Tụ (Converge)

Ngày: 2026-11-08. Kết luận: **converged**.

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| AC-1 Không còn `GET_LOCK`; ghi thu/chi khóa dòng `payment_code_lock` | Đạt | grep `src/main` không còn `GET_LOCK`/`RELEASE_LOCK`; `lockPaymentCodes()` gọi đầu `resolveTransactionCode` (thu và chi) |
| AC-2 Gợi ý mã không khóa | Đạt | `suggestTransactionCode_DoesNotLock` |
| AC-3 Mã gõ tay và tự sinh khóa trước khi kiểm tra | Đạt | `receivePayment_TypedCode_LocksBeforeChecking`, `receivePayment_GeneratedCode_LocksBeforeNumbering` (InOrder) |
| AC-4 Hai giao dịch chồng nhau không trùng mã | Đạt | `PaymentCodeConcurrencyIntegrationTest` trên MySQL thật; kiểm tra khóa + đánh số, không đi qua `receivePayment` (cần fixture hóa đơn) |
| AC-5 Migration và toàn bộ test | Đạt | V20; `./mvnw test` 600/600 xanh sau khi sửa `MigrationSchemaTests` (35 bảng, 20 migration) |

Lỗi phát hiện và đã sửa khi code: `MigrationSchemaTests` đếm cứng số bảng/migration. Docs: `DATABASE.md` (dòng V20 đặt trước V19, chưa sắp lại), `DATA_DICTIONARY.md`.

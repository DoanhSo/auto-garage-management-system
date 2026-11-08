# Kế Hoạch Sửa Lỗi Sau Review Cổng Luồng Sửa Chữa

> **Mã plan**: `review-fixes-gates-payments`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`).
> **Kế thừa từ**: `plans/2026-11-workflow-gates-warranty-hr-nav.md`, `plans/2026-11-payment-receipt-fix.md` (lỗi tìm thấy khi `/code-review` hai commit `0a10539`, `8c327b0`).

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- `BillingServiceImpl.generateTransactionCode` chọn mã max+1 rồi kiểm tra tồn tại, không có khóa: hai khoản thu đồng thời cho hai hóa đơn khác nhau sinh cùng mã, bản ghi sau vi phạm `uq_payments_code` và trả 500.
- `evaluateHandover` ghi "thu tối thiểu debt − hạn mức" (không chặn trên) trong khi `minimumPayment` bị chặn ở số còn nợ hóa đơn, nên thông báo đòi nhiều hơn mức được phép thu.
- `LaborExecutionServiceImpl` tiêm `InventoryOperations` với `required=false` và bỏ qua cổng khi null, nên thiếu bean thì cổng chặn tắt âm thầm.
- `updateInvoiceItemWarranty` cho qua khi không đọc được trạng thái phiếu.
- Nhánh `if (balance.signum() > 0)` trong `evaluateHandover` luôn đúng.

### 1.2. Mục tiêu
- Mã giao dịch tự sinh không trùng khi thu đồng thời.
- Thông báo vượt hạn mức khớp `minimumPayment`.
- Cổng xuất kho trước khi bắt đầu công việc luôn có hiệu lực.
- Sửa bảo hành bị chặn khi không xác định được phiếu.

### 1.3. Ngoài phạm vi
- Đổi lại mã lỗi `DUPLICATE_RESOURCE` (frontend không phân biệt mã này).
- Lọc `approval_status` trong truy vấn phụ tùng chờ xuất (giữ chỗ chỉ tạo cho dòng đã duyệt).
- Đổi tên lớp viết đầy đủ thành import.

## 2. Tiêu Chí Chấp Nhận

- **AC-1**: Sinh mã tự động lấy khóa MySQL `GET_LOCK` giữ đến hết giao dịch; không lấy được thì báo `PAYMENT_BLOCKED`. _Kiểm chứng_: test `BillingServiceTest`.
- **AC-2**: Thông báo vượt hạn mức dùng đúng `minimumPayment`. _Kiểm chứng_: test `BillingServiceTest`.
- **AC-3**: `InventoryOperations` bắt buộc; `startTimer` luôn gọi cổng. _Kiểm chứng_: `LaborExecutionServiceTest`.
- **AC-4**: Sửa bảo hành khi không có phiếu trả `WARRANTY_BLOCKED`. _Kiểm chứng_: test `BillingServiceTest`.
- **AC-5**: Toàn bộ test backend xanh.

## 3. Danh Sách File Mục Tiêu

| File | Thay đổi |
|---|---|
| `billing/service/impl/BillingServiceImpl.java` | khóa sinh mã, thông báo hạn mức, bỏ nhánh thừa, chặn khi không có phiếu |
| `repair/service/impl/LaborExecutionServiceImpl.java` | tiêm bắt buộc, bỏ kiểm tra null |
| `src/test/.../BillingServiceTest.java` | test AC-2, AC-4 |

## 4. Hợp Đồng Kỹ Thuật
- Khóa: `SELECT GET_LOCK('autogarage.payment-code', 5)`; nhả bằng `RELEASE_LOCK` trong `TransactionSynchronization.afterCompletion` khi có giao dịch.

## 5. Checklist Thực Thi
- [x] **Step 1: Khóa sinh mã** _(AC-1)_
- [x] **Step 2: Thông báo hạn mức và nhánh thừa** _(AC-2)_
- [x] **Step 3: Tiêm bắt buộc InventoryOperations** _(AC-3)_
- [x] **Step 4: Chặn bảo hành khi không có phiếu** _(AC-4)_
- [x] **Step 5: Chạy test backend** _(AC-5)_

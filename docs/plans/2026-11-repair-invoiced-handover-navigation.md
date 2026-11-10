# Kế Hoạch Hiển Thị Nút Bàn Giao Xe Ở Trạng Thái Đã Lập Hóa Đơn Và Điều Hướng Thanh Toán Hóa Đơn

> **Mã plan**: `repair-invoiced-handover-navigation`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: _(không)_ — plan trước `invoice-search-customer-vehicle` đã lưu tại `docs/plans/2026-11-invoice-search-customer-vehicle.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- Tại backend, sau khi kiểm tra chất lượng (QC) đạt, phiếu sửa chữa chuyển sang trạng thái `COMPLETED` (`QualityServiceImpl`). Khi phát hành hóa đơn dịch vụ, `BillingServiceImpl.issueInvoice` (`src/main/java/com/Group13_65CNTT/AutoGarage/billing/service/impl/BillingServiceImpl.java:281`) cập nhật trạng thái phiếu sửa chữa sang `INVOICED` ("Đã lập hóa đơn").
- Tuy nhiên tại frontend, trong `RepairOrderDetailModal.tsx` (`frontend/src/features/repair/components/RepairOrderDetailModal.tsx`):
  1. Truy vấn hóa đơn `handoverInvoices` (dòng 138–141) chỉ bật khi `order?.status === 'COMPLETED'`, không bật khi `order?.status === 'INVOICED'` hoặc `'CLOSED'`.
  2. Nút **Bàn giao xe** trên thanh tóm tắt đầu hộp thoại (dòng 352–356) chỉ hiển thị khi `canFrontDesk && order.status === 'COMPLETED'`. Khi phiếu sang `INVOICED`, nút này biến mất.
  3. Tại **Tab 5 – Bàn giao xe** (dòng 1048–1060 và 1091–1110), cả phần hiển thị `Trạng thái hóa đơn` lẫn nút **Tiến hành bàn giao xe** đều chỉ kiểm tra `order.status === 'COMPLETED'`. Khi phiếu sang `INVOICED`, giao diện rơi vào nhánh `order.status !== 'CLOSED'` và hiển thị nhầm thông báo *"Cần hoàn thành sửa chữa và kiểm định QC đạt chuẩn trước khi bàn giao xe."* đồng thời ẩn mất nút **Tiến hành bàn giao xe**.
  4. Khi hóa đơn chưa thanh toán đủ số tiền (còn nợ `balanceAmount > 0` hoặc chưa đủ điều kiện công nợ theo `handover-readiness`), trong màn chi tiết lệnh sửa chữa (`RepairOrderDetailModal.tsx`) chưa có nút điều hướng trực tiếp sang hóa đơn để thu tiền; liên kết trong `HandoverModal.tsx` (dòng 206) chỉ trỏ về `/billing/invoices` chung mà chưa truyền mã hóa đơn, và `InvoicesPage.tsx` (dòng 77) chưa đọc tham số `?query=` từ URL để lọc sẵn hóa đơn cần thu tiền.

### 1.2. Mục tiêu
- Khi lệnh sửa chữa ở trạng thái `INVOICED` (**Đã lập hóa đơn**) hoặc `COMPLETED` (**Hoàn thành**):
  - Hiển thị đầy đủ nút **Bàn giao xe** trên thanh hành động đầu modal và nút **Tiến hành bàn giao xe** tại tab **Bàn giao xe** cho nhân sự lễ tân/quản trị (`canFrontDesk`).
  - Hiển thị rõ thông tin hóa đơn (`Mã hóa đơn`, trạng thái `đã thanh toán đủ` hoặc `còn nợ ...`) tại bước **2. Quyết toán chi phí** trong tab **Bàn giao xe** (kể cả khi phiếu đang ở `COMPLETED`, `INVOICED` hoặc `CLOSED`).
- Nếu lệnh sửa chữa đã lập hóa đơn nhưng **chưa thanh toán đủ số tiền** (`handoverInvoice.status !== 'PAID'` / `balanceAmount > 0` hoặc `handoverReadiness.allowed === false`):
  - Hiển thị nút điều hướng **Đến hóa đơn thanh toán** (trên thanh hành động và trong tab **Bàn giao xe** của `RepairOrderDetailModal.tsx`, cũng như trong `HandoverModal.tsx`) dẫn tới `/billing/invoices?query=<invoiceCode>`.
  - Nếu lệnh đang ở `COMPLETED` mà chưa lập hóa đơn (`!handoverInvoice`), hiển thị nút **Lập hóa đơn** điều hướng đến `/billing/invoices`.
  - Trang `InvoicesPage.tsx` đọc tham số `?query=` trên URL để tự động điền ô tìm kiếm và lọc đúng hóa đơn của lệnh sửa chữa đó, giúp lễ tân bấm **Thu tiền** ngay mà không phải tìm lại thủ công.
  - Khi ghi nhận thanh toán (`useReceivePaymentMutation`), làm mới cache `handover-readiness` và `invoice-by-order` để quay lại lệnh sửa chữa thấy ngay trạng thái đủ điều kiện bàn giao.

### 1.3. Ngoài phạm vi
- Không thay đổi quy tắc nghiệp vụ backend về phát hành hóa đơn (`BillingServiceImpl.issueInvoice`), tính hạn mức công nợ (`evaluateHandover`) hoặc đóng lệnh khi bàn giao (`RepairServiceImpl.handOver`).
- Không tạo migration Flyway mới.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Khi mở chi tiết lệnh sửa chữa có trạng thái `INVOICED` ("Đã lập hóa đơn") bằng tài khoản lễ tân/admin (`FRONT_DESK_ROLES`), người dùng thấy nút **Bàn giao xe** ở thanh tóm tắt phía trên và nút **Tiến hành bàn giao xe** (đang bật) ở tab **Bàn giao xe**, đồng thời mục **2. Quyết toán chi phí** hiển thị đúng mã hóa đơn và số tiền còn nợ / đã thanh toán. _Kiểm chứng_: unit/component test RTL cho `RepairOrderDetailModal` và kiểm tra trên trình duyệt.
- **AC-2**: Khi hóa đơn của lệnh sửa chữa chưa thanh toán đủ số tiền (`status !== 'PAID'` và `balanceAmount > 0`, hoặc `handover-readiness` trả về `allowed: false`), người dùng thấy nút điều hướng đến hóa đơn (**Đến hóa đơn thanh toán** / **Mở Hóa đơn dịch vụ để thu tiền**) tại `RepairOrderDetailModal` và `HandoverModal` với đích `/billing/invoices?query=<invoiceCode>`. _Kiểm chứng_: test RTL trong `HandoverModal.test.tsx` và test cho `RepairOrderDetailModal`.
- **AC-3**: Khi điều hướng sang `/billing/invoices?query=<invoiceCode>`, trang `InvoicesPage.tsx` tự động khởi tạo từ khóa tìm kiếm theo tham số `query` trên URL và lọc danh sách hóa đơn khớp mã đó. _Kiểm chứng_: test RTL hoặc kiểm tra `InvoicesPage.tsx` + `npm --prefix frontend run check`.
- **AC-4**: Khi hóa đơn đã thanh toán đủ (hoặc nằm trong hạn mức tín dụng cho phép bàn giao), bấm **Bàn giao xe** -> nhập ODO xuất xưởng -> bấm **Xác nhận bàn giao xe** thành công và đóng phiếu sang `CLOSED`. _Kiểm chứng_: `npm --prefix frontend run test` và `./mvnw test`.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `frontend/src/features/repair/components/RepairOrderDetailModal.tsx` | 138–150, 350–358, 1038–1114 | Hỗ trợ trạng thái `INVOICED` (cùng `COMPLETED`) cho truy vấn `handoverInvoices` và `useHandoverReadinessQuery`; hiển thị nút **Bàn giao xe** và nút điều hướng **Đến hóa đơn thanh toán** (`/billing/invoices?query=<invoiceCode>`) khi hóa đơn chưa thanh toán đủ ở cả banner đầu và tab **Bàn giao xe** |
| `frontend/src/features/repair/components/HandoverModal.tsx` | 120–135, 195–222 | Cập nhật link/nút điều hướng hóa đơn kèm `?query=<invoiceCode>`; hiển thị nút điều hướng đến hóa đơn cả khi `invoiceBalance > 0` (chưa thanh toán đủ 100%) hoặc khi `allowed === false` |
| `frontend/src/features/billing/pages/InvoicesPage.tsx` | 70–136 | Đọc `useSearchParams()` để khởi tạo/đồng bộ `query` từ `?query=` trên URL khi điều hướng từ lệnh sửa chữa sang |
| `frontend/src/features/billing/hooks/useBilling.ts` | 134–145 | Trong `useReceivePaymentMutation.onSuccess`, invalidate thêm `['handover-readiness']` và `['invoice-by-order']` |
| `frontend/src/features/repair/components/HandoverModal.test.tsx` | 88–159 | Bổ sung test kiểm chứng điều hướng đến `/billing/invoices?query=INV-9` khi chưa thanh toán đủ và hiển thị nút **Bàn giao xe** / **Đến hóa đơn thanh toán** cho lệnh ở trạng thái `INVOICED` |

## 4. Hợp Đồng Kỹ Thuật

- **Backend API**: Giữ nguyên không đổi:
  - `GET /api/v1/billing/invoices?orderId={orderId}`
  - `GET /api/v1/billing/invoices/order/{orderId}`
  - `GET /api/v1/billing/invoices/order/{orderId}/handover-readiness`
  - `POST /api/v1/repair-orders/{id}/handover`
- **Logic trạng thái bàn giao trên `RepairOrderDetailModal.tsx`**:
  - `const canHandoverStage = order?.status === 'COMPLETED' || order?.status === 'INVOICED';`
  - Bật `handoverInvoices` khi `open && canFrontDesk && (canHandoverStage || order?.status === 'CLOSED')`.
  - Bật `handoverReadiness = useHandoverReadinessQuery(orderId, open && canFrontDesk && canHandoverStage && !!handoverInvoice)`.
  - Tính `const invoiceBalanceAmount = Number(handoverInvoice?.balanceAmount ?? handoverInvoice?.totalAmount ?? 0);`
  - `const isInvoiceUnpaid = !!handoverInvoice && handoverInvoice.status !== 'PAID' && invoiceBalanceAmount > 0;`
  - `const isHandoverBlocked = handoverReadiness.data?.allowed === false;`
  - Đường dẫn điều hướng hóa đơn:
    `const invoiceHref = handoverInvoice ? `/billing/invoices?query=${encodeURIComponent(handoverInvoice.invoiceCode)}` : '/billing/invoices';`
  - Khi `canFrontDesk && canHandoverStage`:
    - Nếu `isInvoiceUnpaid || isHandoverBlocked`: hiển thị `<Button variant="secondary" onClick={handleGoToInvoice}><Receipt size={15} /> Đến hóa đơn thanh toán</Button>` cạnh nút `<Button variant="primary" onClick={() => setHandoverOpen(true)}><UserCheck size={15} /> Bàn giao xe</Button>`.
    - Nếu `!handoverInvoices.isLoading && !handoverInvoice`: hiển thị `<Button variant="secondary" onClick={handleGoToInvoice}><Receipt size={15} /> Lập hóa đơn</Button>` và ở tab Bàn giao xe khóa nút `Tiến hành bàn giao xe` kèm chú thích *"Cần lập hóa đơn trước khi bàn giao."*.
- **Component UI dùng chung**: Tái sử dụng `Button`, `Badge`, `Sheet` từ `frontend/src/shared/components/ui.tsx` và `Link` / `useNavigate` / `useSearchParams` từ `react-router-dom`.

## 5. Checklist Thực Thi

- [x] **Step 1: Cập nhật `RepairOrderDetailModal.tsx` hỗ trợ trạng thái `INVOICED` và nút điều hướng hóa đơn** — Bật truy vấn hóa đơn và `useHandoverReadinessQuery` cho cả `COMPLETED` và `INVOICED`; hiển thị nút **Bàn giao xe** / **Tiến hành bàn giao xe** khi `order.status === 'INVOICED'`; bổ sung nút **Đến hóa đơn thanh toán** (`/billing/invoices?query=<invoiceCode>`) khi hóa đơn chưa thanh toán đủ số tiền. _(AC-1, AC-2)_
- [x] **Step 2: Cập nhật `HandoverModal.tsx`, `InvoicesPage.tsx` và `useBilling.ts`** — Truyền `?query=<invoiceCode>` vào liên kết điều hướng thu tiền trong `HandoverModal.tsx` (hiển thị khi `allowed === false` hoặc `invoiceBalance > 0`), đọc `?query=` từ `useSearchParams()` trong `InvoicesPage.tsx`, và invalidate `handover-readiness` / `invoice-by-order` sau khi thu tiền trong `useBilling.ts`. _(AC-2, AC-3, AC-4)_
- [x] **Step 3: Viết test & xác minh** — Bổ sung test trong `HandoverModal.test.tsx` cho trạng thái `INVOICED` và điều hướng `/billing/invoices?query=...`; chạy `npm --prefix frontend run check` và `npm --prefix frontend run test`. _(AC-1, AC-2, AC-3, AC-4)_

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

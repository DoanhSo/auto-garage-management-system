# Kế Hoạch Tìm Kiếm Hóa Đơn Dịch Vụ Theo Khách Hàng, Xe Và Phiếu Sửa Chữa

> **Mã plan**: `invoice-search-customer-vehicle`
> **Chế độ**: `PLAN:` (tuân thủ `AGENTS.md`, `docs/CODING_GUIDELINES.md`, `docs/UI_DESIGN_GUIDELINES.md`).
> **Kế thừa từ**: _(không)_ — plan trước `qc-failed-add-work` đã lưu tại `docs/plans/2026-11-qc-failed-add-work.md`.

---

## 1. Mục Tiêu & Phạm Vi

### 1.1. Hiện trạng
- Trên trang **Hóa đơn dịch vụ** (`frontend/src/features/billing/pages/InvoicesPage.tsx:467`), ô tìm kiếm hiển thị placeholder `"Nhập mã hóa đơn, tên khách, biển số xe..."` và gửi tham số `query` lên `GET /api/v1/billing/invoices`.
- Tuy nhiên tại backend, `InvoiceRepository.searchInvoices` (`src/main/java/com/Group13_65CNTT/AutoGarage/billing/repository/InvoiceRepository.java:42-50`) chỉ lọc theo mã hóa đơn:
  ```java
  AND (:query IS NULL OR :query = '' OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%')))
  ```
- Tương tự, `InvoiceBalanceViewRepository.searchInvoiceBalances` (`src/main/java/com/Group13_65CNTT/AutoGarage/billing/repository/InvoiceBalanceViewRepository.java:22-29`) chỉ tìm theo `v.invoiceCode`.
- Hệ quả: khi người dùng nhập **tên khách hàng** (hoặc mã khách, SĐT, biển số xe, mã lệnh sửa chữa), truy vấn trả về rỗng. Ngoài ra khi lọc không có kết quả trên giao diện mobile (`compact`), bảng chỉ hiện khung trống không có thông báo `Empty`.

### 1.2. Mục tiêu
- Tìm kiếm trên trang **Hóa đơn dịch vụ** (`GET /api/v1/billing/invoices?query=...`) trả về đúng hóa đơn khi người dùng nhập:
  - Mã hóa đơn (`invoiceCode`)
  - Tên khách hàng, tên công ty, mã khách hàng hoặc số điện thoại (khớp cả dữ liệu khách hàng hiện tại ở bảng `customers` lẫn bản chụp `customerSnapshot` trên hóa đơn)
  - Biển số xe, hãng/dòng xe hoặc số khung VIN (khớp cả bảng `vehicles` của lệnh sửa chữa lẫn `vehicleSnapshot` trên hóa đơn)
  - Mã lệnh sửa chữa (`orderCode`)
- Đồng bộ tìm kiếm theo khách hàng/mã lệnh cho `InvoiceBalanceViewRepository.searchInvoiceBalances` (`GET /api/v1/billing/debts/invoices`).
- Trên `InvoicesPage.tsx`, hiển thị trạng thái rỗng rõ ràng khi tìm kiếm/lọc không có kết quả (cả desktop và mobile).

### 1.3. Ngoài phạm vi
- Không thay đổi cấu trúc bảng `invoices`, không tạo migration Flyway mới.
- Không thay đổi luồng phát hành hóa đơn, thu tiền hoặc hủy hóa đơn.
- Không thay đổi cách tính công nợ trên `DebtLedgerPage.tsx`.

## 2. Tiêu Chí Chấp Nhận (Acceptance Criteria)

- **AC-1**: Khi tìm kiếm hóa đơn (`GET /api/v1/billing/invoices?query=<từ-khóa>`) theo **tên khách hàng** (có dấu, không phân biệt hoa thường, một phần họ tên như `"Nguyễn Văn An"` hoặc `"Hoàng Long"`), API trả về đúng các hóa đơn của khách hàng đó (khớp theo `Customer` hoặc `customerSnapshot`). _Kiểm chứng_: integration test trong `PaymentCodeIntegrationTest` (hoặc `InvoiceSearchIntegrationTest`) và thao tác trên trình duyệt tại trang Hóa đơn dịch vụ.
- **AC-2**: Khi tìm kiếm hóa đơn theo **mã khách hàng**, **số điện thoại**, **biển số xe** (VD: `"30H-889.23"`), hoặc **mã phiếu sửa chữa** (`RO-...`), API trả về đúng hóa đơn tương ứng; tìm theo **mã hóa đơn** vẫn hoạt động như cũ. _Kiểm chứng_: integration test gọi `billingService.searchInvoices`.
- **AC-3**: Khách hàng đăng nhập (`Role.CUSTOMER`) khi tìm kiếm vẫn bị giới hạn trong phạm vi `customerId` của chính mình (`customerScope`), không lộ hóa đơn của khách khác dù trùng từ khóa. _Kiểm chứng_: `BillingServiceTest` + integration test.
- **AC-4**: Trên trang Hóa đơn dịch vụ (`InvoicesPage.tsx`), khi nhập tên khách hàng vào ô tìm kiếm thì danh sách cập nhật theo từ khóa; nếu từ khóa không khớp hóa đơn nào thì hiển thị thông báo rỗng phù hợp ở cả giao diện bảng (desktop) và thẻ (mobile). _Kiểm chứng_: `npm --prefix frontend run check` và kiểm tra trên trình duyệt.

## 3. Danh Sách File Mục Tiêu

| File | Vùng dòng | Thay đổi |
|---|---|---|
| `src/main/java/com/Group13_65CNTT/AutoGarage/billing/repository/InvoiceRepository.java` | 42–50 | Mở rộng JPQL `searchInvoices`: khớp `:query` theo `i.invoiceCode`, `CAST(i.customerSnapshot AS string)`, `CAST(i.vehicleSnapshot AS string)`, `EXISTS (SELECT 1 FROM Customer c WHERE c.id = i.customerId AND ...)`, và `EXISTS (SELECT 1 FROM RepairOrder ro LEFT JOIN Vehicle v ON v.id = ro.vehicleId WHERE ro.id = i.repairOrderId AND ...)` |
| `src/main/java/com/Group13_65CNTT/AutoGarage/billing/repository/InvoiceBalanceViewRepository.java` | 22–29 | Mở rộng JPQL `searchInvoiceBalances`: ngoài `v.invoiceCode`, khớp thêm `Customer` (`fullName`, `customerCode`, `phone`, `companyName`) và `RepairOrder`/`Vehicle` (`orderCode`, `licensePlate`) |
| `frontend/src/features/billing/pages/InvoicesPage.tsx` | 545–640 | Cập nhật thông báo rỗng khi đang lọc (`isFiltered`) cho `DataTable` và bổ sung `Empty` khi danh sách rỗng ở chế độ `compact`; sửa chỉ số hiển thị phân trang theo `page` |
| `src/test/java/com/Group13_65CNTT/AutoGarage/billing/PaymentCodeIntegrationTest.java` | cuối file | Bổ sung test tích hợp kiểm chứng `searchInvoices` theo tên khách hàng, SĐT, biển số xe, mã lệnh sửa chữa và mã hóa đơn |

## 4. Hợp Đồng Kỹ Thuật

- **Endpoint & chữ ký hàm**: Giữ nguyên:
  - `GET /api/v1/billing/invoices` (`InvoiceController.search`)
  - `BillingService.searchInvoices(InvoiceStatus status, Long customerId, Long orderId, String query, Pageable pageable)`
  - `InvoiceRepository.searchInvoices(@Param("status") InvoiceStatus status, @Param("customerId") Long customerId, @Param("orderId") Long orderId, @Param("query") String query, Pageable pageable)`
- **Truy vấn JPQL tại `InvoiceRepository.searchInvoices`**:
  ```java
  @Query("""
          SELECT i FROM Invoice i
          WHERE (:status IS NULL OR i.status = :status)
            AND (:customerId IS NULL OR i.customerId = :customerId)
            AND (:orderId IS NULL OR i.repairOrderId = :orderId)
            AND (:query IS NULL OR :query = ''
                 OR LOWER(i.invoiceCode) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(CAST(i.customerSnapshot AS string)) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(CAST(i.vehicleSnapshot AS string)) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR EXISTS (
                      SELECT 1 FROM Customer c
                      WHERE c.id = i.customerId
                        AND (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                             OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :query, '%'))
                             OR (c.phone IS NOT NULL AND c.phone LIKE CONCAT('%', :query, '%'))
                             OR (c.companyName IS NOT NULL AND LOWER(c.companyName) LIKE LOWER(CONCAT('%', :query, '%'))))
                 )
                 OR EXISTS (
                      SELECT 1 FROM RepairOrder ro
                      LEFT JOIN Vehicle v ON v.id = ro.vehicleId
                      WHERE ro.id = i.repairOrderId
                        AND (LOWER(ro.orderCode) LIKE LOWER(CONCAT('%', :query, '%'))
                             OR (v.licensePlate IS NOT NULL AND LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :query, '%')))
                             OR (v.vin IS NOT NULL AND LOWER(v.vin) LIKE LOWER(CONCAT('%', :query, '%'))))
                 ))
          """)
  ```
- **Quyền truy cập & Exception**: Không đổi (`@PreAuthorize(SecurityConstants.HAS_ANY_ROLE)`, khách hàng bị giới hạn bởi `customerScope.customerIdForQuery(customerId)`).
- **UI Components (`InvoicesPage.tsx`)**: Tái sử dụng `Empty` từ `shared/components/ui` cho chế độ `compact` khi `invoicesList.length === 0`.

## 5. Checklist Thực Thi

- [x] **Step 1: Mở rộng truy vấn tìm kiếm hóa đơn tại Backend** — Cập nhật `InvoiceRepository.searchInvoices` và `InvoiceBalanceViewRepository.searchInvoiceBalances` để tìm theo tên/mã/SĐT khách hàng, biển số/VIN xe, mã phiếu sửa chữa và snapshot. _(AC-1, AC-2, AC-3)_
- [x] **Step 2: Hoàn thiện hiển thị kết quả tìm kiếm trên `InvoicesPage.tsx`** — Hiển thị `Empty` đồng nhất khi tìm kiếm không ra kết quả (cả desktop và mobile) và hiển thị đúng dải số thứ tự theo trang. _(AC-4)_
- [x] **Step 3: Viết test & xác minh** — Thêm test tìm kiếm hóa đơn theo tên khách hàng, biển số xe, mã lệnh sửa chữa vào `PaymentCodeIntegrationTest`; chạy `./mvnw test` và `npm --prefix frontend run build`. _(AC-1, AC-2, AC-3, AC-4)_

<!-- Mục 6 "Kết Quả Hội Tụ" do /converge thêm vào, không viết sẵn. -->

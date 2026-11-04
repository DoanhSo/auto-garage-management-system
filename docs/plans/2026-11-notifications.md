# Kế Hoạch Tái Thiết Kế Màn Hình Thông Báo (Notifications), Đa Dạng Hóa Thông Tin & Chuẩn Hóa Trình Bày Trực Quan

> **Chế độ**: `PLAN:` (Tuân thủ nghiêm ngặt `AutoGarage/AGENTS.md`, `AutoGarage/docs/CODING_GUIDELINES.md`, và `AutoGarage/docs/UI_DESIGN_GUIDELINES.md`).
> **Nguyên tắc**: Không sửa code ứng dụng trong chế độ `PLAN`. Khảo sát toàn diện hệ thống Thông báo từ Backend (`notification` module, `ReminderRepository`, `NotificationAuditServiceImpl`, `EstimateServiceImpl`) đến Frontend (`NotificationsPage.tsx`, `NoticeDetails.tsx`, `BroadcastModal.tsx`, `useNotifications.ts`, `notificationsApi.ts`, `message.ts`). Xây dựng trải nghiệm hộp thư thông báo hiện đại, giàu thông tin ngữ cảnh, có icon/badge phân loại nghiệp vụ, liên kết điều hướng nhanh (Deep Link CTA), hỗ trợ đánh dấu đọc tất cả và xem trước thông báo.

---

## 1. Mục Tiêu & Phạm Vi (Goal & Scope)

### 1.1. Hiện trạng & Thiếu sót của màn hình Thông báo hiện tại
1. **Trình bày đơn điệu, thiếu thông tin ngữ cảnh nghiệp vụ**:
   - Trang `/notifications` hiện tại dùng bảng `DataTable` đơn điệu, chỉ có tiêu đề và văn bản cụt lủn, hoàn toàn thiếu tính trực quan của một trung tâm thông báo hiện đại.
   - Không có icon phân loại (Lịch hẹn 📅, Bảo dưỡng định kỳ 🔧, Hết hạn bảo hành 🛡️, Báo giá dịch vụ 📄, Bảng lương 💰, Cảnh báo kho 📦, Thông báo chung 📢).
   - Không phân biệt được mức độ quan trọng/khẩn cấp (`severity`/`tone`: Khẩn cấp `danger`, Cảnh báo `warning`, Thông tin nghiệp vụ `info`, Hoàn thành `success`).
2. **Lỗi "nuốt" nội dung biến thành thông báo vô nghĩa**:
   - Trong `message.ts`: Hàm `notificationMessage` chỉ đọc trường `message` hoặc `text`. Khi hệ thống tự động sinh thông báo nhắc lịch bảo hành (`WARRANTY_EXPIRY`), bảo dưỡng (`MAINTENANCE`) từ `ReminderRepository`, hoặc báo giá (`ESTIMATE_SENT`) từ `EstimateServiceImpl`, payload JSON chứa `{ vehicleId, warrantyId, expiresAt, serviceName, estimateId, code... }` mà không có trường `message`.
   - Kết quả: Tiêu đề hiển thị mặc định *"Thông báo từ gara"*, nội dung hiển thị câu chung chung *"Thông báo từ gara. Liên hệ nhân viên phụ trách nếu cần thêm thông tin."*! Khách hàng và nhân viên không biết là xe nào, bảo dưỡng món gì, báo giá bao nhiêu tiền!
3. **Thiếu nút hành động điều hướng nhanh (Actionable CTA)**:
   - Khi nhận thông báo "Có báo giá mới", người dùng không thể bấm 1 chạm để mở ngay trang Báo giá (`/estimates`).
   - Khi nhận thông báo "Đến kỳ bảo dưỡng", khách hàng không có nút "Đặt lịch ngay" (`/reception`).
   - Khi nhân viên nhận thông báo "Lương tháng đã duyệt", không có nút "Xem phiếu lương" (`/payroll`).
4. **Trải nghiệm thao tác bất tiện**:
   - Chưa có tính năng "Đánh dấu tất cả đã đọc" (Mark all as read), người dùng phải click từng thông báo để đánh dấu đọc.
   - Chưa có phân nhóm thông báo theo thời gian (Hôm nay, Hôm qua, Tuần này, Cũ hơn).
   - Modal phát thông báo (`BroadcastModal.tsx`) chỉ cho nhập văn bản thô, không chọn được mức độ ưu tiên và không có khung xem trước (Live Preview).

### 1.2. Mục tiêu cải tiến trọng tâm
1. **Bộ giải mã thông báo thông minh (`notificationParser.ts`)**:
   - Phân tích và định dạng thông minh mọi payload JSON từ hệ thống gara:
     * `MAINTENANCE`: Nhắc bảo dưỡng định kỳ kèm tên dịch vụ, biển số/mã xe, hạn ngày và mốc km. CTA $\rightarrow$ `/reception`.
     * `WARRANTY_EXPIRY`: Nhắc hết hạn bảo hành kèm mã sổ, hạn bảo hành, số km. CTA $\rightarrow$ `/warranties`.
     * `ESTIMATE_SENT`: Báo giá dịch vụ mới kèm mã báo giá. CTA $\rightarrow$ `/estimates`.
     * `PAYROLL_APPROVED` / `PAYROLL_DISBURSED`: Bảng lương tháng kèm kỳ công. CTA $\rightarrow$ `/payroll`.
     * `REPAIR_COMPLETED` / `QC_PASSED`: Xe sửa xong, nghiệm thu đạt, sẵn sàng nhận xe. CTA $\rightarrow$ `/repair-orders`.
     * `BROADCAST`: Thông báo quản trị kèm cấp độ ưu tiên và nút liên kết tùy chọn.
2. **Tái thiết kế giao diện Hộp thư Thông báo (`NotificationsPage.tsx`)**:
   - Chuyển đổi hiển thị sang **Thẻ Thông Báo Tương Tác Giàu Thông Tin (Rich Notification Feed Cards)**:
     * Icon loại thông báo với nền màu nhận diện riêng biệt.
     * Badge phân hệ (Bảo dưỡng, Báo giá, Bảo hành, Tiền lương, Hệ thống...).
     * Tiêu đề to rõ, phân biệt Chưa đọc (đậm + chấm xanh `unread-dot`) và Đã đọc (`muted`).
     * Nội dung xúc tích, trích xuất đúng thông tin cốt lõi (biển số, ngày giờ, số tiền).
     * Thời gian hiển thị tương đối thân thiện ("10 phút trước", "Hôm nay 08:30" kèm tooltip ngày giờ đầy đủ).
     * Nút hành động nhanh (CTA) dẫn thẳng đến phân hệ tương ứng.
3. **Cửa sổ Thông Báo Nhanh (Notification Popover / Dropdown Window) trên Thanh Tiêu Đề Header (`AppShell.tsx`)**:
   - Hiện tại, nút chuông nhỏ (`bell-button`) trên thanh header chỉ là một thẻ `<NavLink to="/notifications">` chuyển trang đơn thuần, **chưa có cửa sổ popover nhỏ**.
   - Bổ sung component `NotificationPopover.tsx` gắn vào nút chuông nhỏ trên Header:
     * Khi click vào nút chuông $\rightarrow$ bung ra một **cửa sổ popover nhỏ (Dropdown Menu)** nằm gọn gàng bên dưới nút chuông.
     * Header cửa sổ nhỏ: Tiêu đề "Thông báo" + Huy hiệu số lượng chưa đọc + Nút "✓ Đọc tất cả".
     * Thân cửa sổ: Danh sách 5 thông báo mới nhất (dạng thẻ rút gọn có icon phân loại, tiêu đề in đậm, trích đoạn nội dung, thời gian "vừa xong/5 phút trước", chấm xanh chưa đọc).
     * Bấm vào từng thông báo trong cửa sổ nhỏ: Đánh dấu đã đọc và điều hướng ngay đến trang xử lý liên quan (ví dụ: `/estimates`, `/warranties`, `/payroll`...).
     * Chân cửa sổ (Footer): Nút **"Xem tất cả thông báo trong hộp thư →"** chuyển đến trang `/notifications`.
     * Tự động đóng khi người dùng click ra ngoài (click-outside) hoặc nhấn phím `Escape`.
4. **Thanh công cụ & Bộ lọc đa năng trên trang Hộp thư `/notifications`**:
   - Bổ sung nút **"✓ Đánh dấu tất cả đã đọc"** gọi API hàng loạt nhanh chóng.
   - Tabs lọc nhanh: `Tất cả`, `Chưa đọc` (kèm badge đếm số lượng), `Dịch vụ & Xe`, `Báo giá & Hóa đơn`, `Toàn hệ thống` (dành cho Admin).
5. **Hộp thoại Chi Tiết Căn Giữa Màn Hình (`Sheet` placement="center")**:
   - Hiển thị đầy đủ thông tin, thời gian, người nhận, các thông số kỹ thuật (xe, ODO, số tiền...) và nút chính chuyển đến trang nghiệp vụ tương ứng.
6. **Nâng cấp Modal Phát Thông Báo (`BroadcastModal.tsx`)**:
   - Cho phép Admin chọn loại thông báo (Tin tức chung, Cảnh báo khẩn, Nhắc việc).
   - Tùy chọn gắn liên kết điều hướng (chọn đích đến: Lệnh sửa chữa, Báo giá, Bảng lương...).
   - Khung **Xem trước trực quan (Live Preview)** hiển thị ngay thẻ thông báo thực tế.
7. **Backend hỗ trợ Đánh dấu đọc tất cả (Atomic Bulk Mark Read)**:
   - Bổ sung API `POST /api/v1/notifications/read-all` cập nhật nguyên tử trạng thái đã đọc cho tất cả thông báo web chưa đọc của người dùng hiện tại trong 1 câu SQL duy nhất.

---

## 2. Thiết Kế Trực Quan & Hệ Thống Nhận Diện Loại Thông Báo

### 2.1. Bảng phân loại & Nhận diện thông báo gara

| Loại thông báo (`type`) | Phân hệ / Badge | Biểu tượng (Icon) | Màu sắc (`tone`) | Tiêu đề mẫu | Nút hành động (CTA) | Đích đến liên kết |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `MAINTENANCE` | Bảo dưỡng định kỳ | 🔧 `Wrench` | `info` (Xanh dương) | Nhắc lịch bảo dưỡng định kỳ | Đặt lịch ngay | `/reception?view=timeline` |
| `WARRANTY_EXPIRY` | Bảo hành xe | 🛡️ `ShieldAlert` | `warning` (Vàng cam) | Sắp hết hạn bảo hành xe | Xem sổ bảo hành | `/warranties?tab=warranties` |
| `ESTIMATE_SENT` | Báo giá dịch vụ | 📄 `FileText` | `info` (Tím chàm) | Báo giá dịch vụ mới #[Mã] | Xem & duyệt báo giá | `/estimates` |
| `REPAIR_COMPLETED` | Lệnh sửa chữa | 🚘 `Car` | `success` (Xanh lá) | Xe sửa chữa hoàn tất | Xem lệnh sửa | `/repair-orders` |
| `QC_PASSED` | Nghiệm thu KCS | ✅ `CheckCircle2` | `success` (Xanh ngọc) | Xe đã nghiệm thu xuất xưởng | Chi tiết nghiệm thu | `/repair-orders` |
| `PAYROLL_APPROVED` | Bảng lương | 💰 `Coins` | `success` (Xanh lục) | Bảng lương kỳ [Tháng] đã duyệt | Xem phiếu lương | `/payroll` |
| `STOCK_ALERT` | Kho & Vật tư | 📦 `PackageAlert`| `danger` (Đỏ cảnh báo) | Cảnh báo tồn kho vật tư | Kiểm tra kho | `/inventory/parts` |
| `BROADCAST_URGENT`| Cảnh báo khẩn | ⚠️ `AlertTriangle`| `danger` (Đỏ) | [Tiêu đề khẩn cấp] | Xem chi tiết | Modal / Link đính kèm |
| `BROADCAST_GENERAL`| Thông báo chung | 📢 `Megaphone` | `neutral` (Xám tro) | [Tiêu đề ban quản trị] | Xem chi tiết | Modal / Link đính kèm |

### 2.2. Bố cục giao diện Hộp thư Thông báo mới

```text
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ THÔNG BÁO                                                    [↻ Làm mới] [✓ Đọc tất cả] [＋ Phát thông báo] │
│ Theo dõi cập nhật tiến độ xe, nhắc bảo dưỡng định kỳ và thông báo từ ban quản trị gara. │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ [ Tất cả (24) ] [ Chưa đọc (3) ] [ 🔧 Dịch vụ & Xe ] [ 📄 Báo giá & Hóa đơn ] [ ⚙️ Toàn hệ thống ]  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 📌 HÔM NAY                                                                             │
│ ┌────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ [🔧]  BẢO DƯỠNG ĐỊNH KỲ • 15 phút trước                                     • Chưa đọc │ │
│ │ <strong>Nhắc lịch bảo dưỡng định kỳ xe Mazda 3 (30A-999.88)</strong>                 │ │
│ │ Xe đã đến hạn thực hiện dịch vụ "Thay dầu động cơ & Cân chỉnh góc đặt bánh xe".   │ │
│ │ Hạn khuyến nghị: 05/11/2026 hoặc mốc 45.000 km.                                     │ │
│ │ [ 📅 Đặt lịch bảo dưỡng ngay → ]                 [✓ Đã đọc] [🗑️ Xóa]             │ │
│ └────────────────────────────────────────────────────────────────────────────────────┘ │
│ ┌────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ [📄]  BÁO GIÁ DỊCH VỤ • 2 giờ trước                                          • Chưa đọc │ │
│ │ <strong>Báo giá dịch vụ #BG-2026-088 đã sẵn sàng</strong>                          │ │
│ │ Cố vấn dịch vụ vừa gửi bảng báo giá 4 hạng mục sửa chữa gầm cho xe Honda CR-V.     │ │
│ │ Tổng số tiền dự kiến: 4.850.000 đ.                                                 │ │
│ │ [ 👁️ Xem & Duyệt báo giá → ]                     [✓ Đã đọc] [🗑️ Xóa]             │ │
│ └────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                        │
│ 📌 TUẦN TRƯỚC                                                                          │
│ ┌────────────────────────────────────────────────────────────────────────────────────┐ │
│ │ [💰]  BẢNG LƯƠNG • 25/10/2026 17:30                                           Đã đọc │ │
│ │ <strong>Bảng lương kỳ Tháng 10/2026 đã được phê duyệt</strong>                       │ │
│ │ Quản lý đã duyệt chi bảng lương tháng 10. Thu nhập thực nhận: 14.250.000 đ.        │ │
│ │ [ 💵 Xem phiếu lương chi tiết → ]                               [🗑️ Xóa]             │ │
│ └────────────────────────────────────────────────────────────────────────────────────┘ │
│                                                                                        │
│ [ < ] Trang 1 / 3 [ > ]                                                Hiển thị: [ 10 ]│
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.3. Bố cục Cửa sổ Thông Báo Nhanh (Notification Popover Dropdown)

```text
               ┌────────────────────── [🔔] 3 ───┐  <- Nút chuông nhỏ trên Header
               │ <strong>Thông báo</strong>         [✓ Đọc tất cả]│
               ├─────────────────────────────────┤
               │ [🔧] <strong>Lịch bảo dưỡng định kỳ</strong>     │
               │ Xe 30A-999.88 đến hạn thay dầu. │
               │ <small>15 phút trước</small>              • │  <- Chấm xanh chưa đọc
               ├─────────────────────────────────┤
               │ [📄] <strong>Báo giá dịch vụ mới</strong>        │
               │ Phiếu #BG-2026-088 đã sẵn sàng. │
               │ <small>2 giờ trước</small>               • │
               ├─────────────────────────────────┤
               │ [💰] <strong>Bảng lương Tháng 10/2026</strong>  │
               │ Đã được duyệt chi: 14.250.000 đ │
               │ <small>Hôm qua 17:30</small>              │
               ├─────────────────────────────────┤
               │ [ <strong>Xem tất cả thông báo trong hộp thư →</strong> ]│
               └─────────────────────────────────┘
```

---

## 3. Danh Sách File Mục Tiêu (Target Files & Line Ranges)

### 3.1. Frontend Files
1. **`AutoGarage/frontend/src/features/notification/utils/notificationParser.ts` (Tạo mới hoàn toàn)**
   - Khai báo kiểu dữ liệu:
     * `NotificationCategory`: `'maintenance' | 'warranty' | 'estimate' | 'repair' | 'payroll' | 'inventory' | 'system' | 'broadcast'`
     * `ParsedNotification`: `{ category, categoryLabel, icon, tone, title, summary, fullText, linkUrl, linkText, metadata, isUnread, timeFormatted }`
   - Hàm `parseNotification(notice: Notice, zone: string): ParsedNotification`.
   - Hàm `timeAgo(dateString: string, zone: string): string` (format thân thiện: "vừa xong", "15 phút trước", "hôm nay lúc...", "hôm qua...").
2. **`AutoGarage/frontend/src/features/notification/utils/message.ts` (Dòng 1–12)**
   - Nâng cấp hàm `notificationMessage(raw: string): string` sử dụng parser mới để không bao giờ bị trả về văn bản mặc định rỗng. Giữ export nguyên vẹn để không làm gãy test.
3. **`AutoGarage/frontend/src/features/notification/api/notificationsApi.ts` (Dòng 8–39)**
   - Bổ sung hàm API `markAllRead: async () => request<number>('/notifications/read-all', { method: 'POST' })`.
4. **`AutoGarage/frontend/src/features/notification/hooks/useNotifications.ts` (Dòng 44–63)**
   - Bổ sung hook `useMarkAllReadMutation()` tự động invalidate query cache `notifications` và `unread-count`.
5. **`AutoGarage/frontend/src/features/notification/components/NotificationPopover.tsx` (Tạo mới hoàn toàn)**
   - Component cửa sổ popover nhỏ gắn vào biểu tượng chuông:
     * Hiển thị danh sách 5 thông báo mới nhất kèm icon, tiêu đề, thời gian tương đối.
     * Nút "Đọc tất cả" nhanh và nút "Xem tất cả thông báo trong hộp thư →" dẫn sang `/notifications`.
     * Tự động đóng khi click-outside hoặc phím `Esc`.
6. **`AutoGarage/frontend/src/app/layouts/AppShell.tsx` (Dòng 543–552)**
   - Tích hợp `NotificationPopover` vào nút `<button className="bell-button">` trên Header thay thế cho việc nhảy trang trực tiếp khi click.
7. **`AutoGarage/frontend/src/features/notification/pages/NotificationsPage.tsx` (Dòng 31–270)**
   - Tái thiết kế giao diện danh sách thẻ thông báo tương tác (`Rich Notification Cards`).
   - Tích hợp nút "Đánh dấu tất cả đã đọc" trên header.
   - Bổ sung các Filter Tabs (Tất cả, Chưa đọc, Dịch vụ & Xe, Báo giá & Tiền tệ, Toàn hệ thống).
   - Nâng cấp Dialog xem chi tiết (`Sheet` placement="center"): hiển thị bảng tham số chi tiết và nút CTA hành động trực tiếp.
8. **`AutoGarage/frontend/src/features/notification/components/BroadcastModal.tsx` (Dòng 1–118)**
   - Bổ sung trường chọn Mức độ ưu tiên (`priority`: Thông thường, Quan trọng, Khẩn cấp).
   - Bổ sung trường chọn Đường dẫn hành động đính kèm (`actionUrl`).
   - Tích hợp khung **Xem trước trực quan (Live Preview)** dạng thẻ thông báo thu nhỏ.
9. **`AutoGarage/frontend/src/styles.css` (Dòng 2430–2610)**
   - Bổ sung styles cho thẻ thông báo tương tác (`.notification-card-item`, `.notification-card-header`, `.notification-badge-chip`, `.notification-cta-button`, `.notification-group-title`, `.broadcast-preview-box`, `.notification-popover-menu`, `.notification-popover-item`).

### 3.2. Backend Files
1. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/repository/NotificationRepository.java` (Dòng 18–83)**
   - Bổ sung derived/custom query:
     ```java
     @Modifying
     @Query("UPDATE Notification n SET n.readAt = :now, n.updatedBy = :userId WHERE n.userId = :userId AND n.channel = 'WEB' AND n.readAt IS NULL")
     int markAllRead(@Param("userId") long userId, @Param("now") Instant now);
     ```
2. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/service/NotificationService.java` (Dòng 10–25)**
   - Khai báo phương thức `int markAllRead();`.
3. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/service/impl/NotificationServiceImpl.java` (Dòng 235–257)**
   - Hiện thực phương thức `markAllRead()` có `@Transactional` và `@PreAuthorize(SecurityConstants.HAS_ANY_ROLE)`.
4. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/controller/NotificationController.java` (Dòng 120–130)**
   - Bổ sung endpoint `POST /api/v1/notifications/read-all`:
     ```java
     @PostMapping("/read-all")
     @PreAuthorize(SecurityConstants.HAS_ANY_ROLE)
     @Operation(summary = "Mark all unread web notifications read for current user")
     public ApiResponse<Integer> markAllRead(HttpServletRequest http) {
         return ApiResponse.success(ResponseCode.SUCCESS, notificationService.markAllRead(), http.getRequestURI());
     }
     ```

---

## 4. Hợp Đồng Kỹ Thuật (Technical Contract)

### 4.1. TypeScript Contract (`notificationParser.ts`)
```typescript
export type NotificationCategory =
  | 'maintenance'
  | 'warranty'
  | 'estimate'
  | 'repair'
  | 'payroll'
  | 'inventory'
  | 'system'
  | 'broadcast';

export interface NotificationMetaItem {
  label: string;
  value: string;
}

export interface ParsedNotification {
  category: NotificationCategory;
  categoryLabel: string;
  tone: 'info' | 'success' | 'warning' | 'danger';
  title: string;
  summary: string;
  fullMessage: string;
  linkUrl?: string;
  linkText?: string;
  metadata: NotificationMetaItem[];
  isUnread: boolean;
  timeFormatted: string;
  timeRelative: string;
}
```

### 4.2. REST Endpoint Contract (`NotificationController.java`)
```http
POST /api/v1/notifications/read-all
Authorization: Bearer <JWT>

HTTP/1.1 200 OK
Content-Type: application/json

{
  "code": 1000,
  "message": "Thành công",
  "success": true,
  "data": 5,
  "timestamp": "2026-11-02T09:25:00Z",
  "path": "/api/v1/notifications/read-all"
}
```

---

## 5. Checklist Thực Thi (Execution Checklist)

- [x] **Step 1: Bổ sung API & Repository cho tính năng "Đánh dấu tất cả đã đọc" tại Backend**
  - Cập nhật [`NotificationRepository.java`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/repository/NotificationRepository.java): Thêm method `@Modifying markAllRead(userId, now)`.
  - Cập nhật [`NotificationService.java`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/service/NotificationService.java) & [`NotificationServiceImpl.java`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/service/impl/NotificationServiceImpl.java): Thêm method `markAllRead()`.
  - Cập nhật [`NotificationController.java`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/notification/controller/NotificationController.java): Thêm endpoint `POST /api/v1/notifications/read-all`.
  - Kiểm tra compile backend: chạy `.\AutoGarage\mvnw.cmd -f AutoGarage/pom.xml test-compile`.

- [x] **Step 2: Xây dựng module Parser & Formatter thông báo thông minh trên Frontend**
  - Tạo file `frontend/src/features/notification/utils/notificationParser.ts`:
    + Trích xuất dữ liệu từ các loại payload JSON: `WARRANTY_EXPIRY`, `MAINTENANCE`, `ESTIMATE_SENT`, `PAYROLL`, `BROADCAST`.
    + Sinh tiêu đề, nội dung, huy hiệu phân loại, icon, màu sắc và đường dẫn liên kết nhanh (CTA).
    + Tính toán thời gian tương đối thân thiện ("10 phút trước", "Hôm nay 08:30"...).
  - Cập nhật `frontend/src/features/notification/utils/message.ts`: Ủy quyền xử lý sang `notificationParser.ts`, đảm bảo fallback thông minh không bao giờ rỗng.

- [x] **Step 3: Cập nhật API Client & Hook trên Frontend**
  - Cập nhật `notificationsApi.ts`: Thêm method `markAllRead()`.
  - Cập nhật `useNotifications.ts`: Thêm hook `useMarkAllReadMutation()` và hỗ trợ lọc theo nhóm phân hệ.

- [x] **Step 4: Tái thiết kế giao diện Hộp thư Thông báo (`NotificationsPage.tsx` & `styles.css`)**
  - Bổ sung thanh công cụ với nút "✓ Đọc tất cả" và các Tabs lọc phân loại (`Tất cả`, `Chưa đọc`, `Dịch vụ & Xe`, `Báo giá & Tiền tệ`, `Toàn hệ thống`).
  - Thay thế bảng phẳng bằng danh sách **Thẻ Thông Báo Tương Tác (Rich Notification Cards)** có icon bo tròn, badge phân hệ, tiêu đề in đậm, nội dung trích dẫn và nút CTA điều hướng nhanh.
  - Nâng cấp Dialog xem chi tiết (`Sheet` placement="center"): hiển thị bảng tham số chi tiết và nút điều hướng chính (`primary`).
  - Cập nhật CSS trong `styles.css` theo đúng token màu sắc và nguyên tắc minimalism, không layout shift.

- [x] **Step 5: Xây dựng Cửa sổ Thông Báo Nhanh Popover (`NotificationPopover.tsx`) trên Header (`AppShell.tsx`)**
  - Tạo component `NotificationPopover.tsx` hiển thị cửa sổ nhỏ dropdown ngay bên dưới biểu tượng chuông.
  - Hiển thị 5 thông báo mới nhất kèm icon, tiêu đề, thời gian tương đối, chấm xanh chưa đọc, nút "Đọc tất cả" và nút "Xem tất cả thông báo trong hộp thư →".
  - Tích hợp vào nút chuông nhỏ trong [`AppShell.tsx`](file:///c:/Users/mrdoanh/IdeaProjects/AutoGarage/AutoGarage/frontend/src/app/layouts/AppShell.tsx), xử lý đóng mở mượt mà, hỗ trợ click-outside và phím `Escape`.

- [x] **Step 6: Nâng cấp Modal Phát thông báo (`BroadcastModal.tsx`)**
  - Thêm tùy chọn loại thông báo (Tin tức chung, Cảnh báo khẩn, Nhắc việc).
  - Thêm tùy chọn đính kèm liên kết điều hướng (`actionUrl`).
  - Tích hợp khung **Xem trước trực quan (Live Preview)** dạng thẻ thông báo thực tế.

- [x] **Step 7: Kiểm thử và Xác minh toàn diện (Verify Phase)**
  - Chạy toàn bộ test suite frontend: `npm --prefix AutoGarage/frontend run test` (đảm bảo 10/10 test files, 91/91 tests pass, bao gồm cả `NotificationsPage.test.tsx`).
  - Chạy build frontend: `npm --prefix AutoGarage/frontend run build` (kiểm tra TypeScript và CSS không có lỗi).
  - Chạy test suite backend: `.\AutoGarage\mvnw.cmd -f AutoGarage/pom.xml test '-Dtest=*Notification*,*ServiceTest'` đảm bảo tất cả tests pass.
  - Kiểm tra thực tế trên trình duyệt: xác nhận thông báo hiển thị đầy đủ thông tin, icon màu sắc rõ ràng và các nút CTA hoạt động chính xác.

---

## 6. Kết Quả Hội Tụ (Converge)

> Lưu trữ ngày 2026-11-03.

**Kết luận**: Hội tụ một phần (`partial`). Mọi bước đã xong và đã kiểm tra trên trình duyệt; các phần còn lệch nằm ngoài phạm vi plan.

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| Bộ giải mã `notificationParser.ts` | Đạt | 5 test; link phát thông báo chỉ nhận danh sách cho phép. |
| Hộp thư dạng thẻ, nhóm theo thời gian | Đạt | |
| Đọc tất cả (`POST /read-all`) | Đạt | |
| Popover chuông | Đạt | Đóng bằng Esc và bấm ra ngoài. |
| Phát thông báo có mức ưu tiên, link, xem trước | Đạt | |
| Tab nhóm Dịch vụ / Báo giá | Lệch | Chỉ lọc trong trang hiện tại (lọc phía client). |
| Thông báo PAYROLL, REPAIR, STOCK | Thiếu | Backend chưa phát; parser đã sẵn sàng. |
| Hiện biển số xe | Thiếu | Payload chỉ có `vehicleId`, đang hiện "Xe #id". |
| Thứ tự tab Toàn hệ thống | Lệch | Backend trả cũ trước. |

**Lỗi phát hiện khi xác minh và đã sửa**: nhãn chuông đọc "0 chưa đọc"; badge thông báo khẩn ghi "Thông báo chung".

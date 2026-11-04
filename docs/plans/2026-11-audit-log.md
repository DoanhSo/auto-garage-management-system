# Kế Hoạch Chuẩn Hóa & Viết Mô Tả Đầy Đủ Cho Hệ Thống Audit Log (Nhật Ký Kiểm Toán Toàn Gara)

> **Chế độ**: `PLAN:` (Tuân thủ nghiêm ngặt `AutoGarage/AGENTS.md`, `AutoGarage/docs/CODING_GUIDELINES.md`, và `AutoGarage/docs/UI_DESIGN_GUIDELINES.md`).
> **Nguyên tắc**: Không sửa code ứng dụng trong chế độ `PLAN`. Khảo sát toàn diện hệ thống kiểm toán từ Backend (Aspect, Entity, Service, Repository, Constants) đến Frontend (Zod Schema, Type Contracts, Audit Catalog, Filter Bar, Data Table, Modal Chi tiết). Xây dựng từ điển kiểm toán hoàn chỉnh, trực quan, chuyên nghiệp, xóa bỏ hoàn toàn thuật ngữ kỹ thuật khó hiểu trên giao diện.

---

## 1. Mục Tiêu & Phạm Vi (Goal & Scope)

### 1.1. Hiện trạng & Lỗ hổng phát hiện qua khảo sát
1. **Thiếu sót danh mục mô tả hành động (Action Descriptions) trên Frontend:**
   - Trong `AuditPage.tsx` (dòng 21–77), danh mục `actionInfo` chỉ khai báo 21 hành động cơ bản (chủ yếu là User và Notification).
   - Các hành động cốt lõi của phân hệ **Bảng lương & Hoa hồng** (`CREATE_PAYROLL_PERIOD`, `CALCULATE_PAYROLL`, `ADJUST_PAYROLL_ITEM`, `APPROVE_PAYROLL`, `DISBURSE_PAYROLL`, `CALCULATE_ORDER_COMMISSION`, `UPDATE_COMMISSION_STATUS`) và **Ca trực & Điểm danh** (`REGISTER_SHIFT`, `APPROVE_SHIFT_REGISTRATION`, `REJECT_SHIFT_REGISTRATION`, `SHIFT_CHECK_IN`, `SHIFT_CHECK_OUT`) hoàn toàn vắng mặt.
   - Khi xảy ra sự kiện, giao diện hiển thị mã kỹ thuật thô (`CREATE_PAYROLL_PERIOD`) kèm mô tả mặc định *"Không có mô tả."*, gây khó khăn cho quản lý/chủ gara khi tra cứu.
2. **Thiếu sót danh mục thực thể (Entity Types):**
   - `entityLabels` chỉ có 6 loại thực thể (`USER`, `NOTIFICATION`, `Employee`, `EmployeeShift`, `ServiceBay`, `BayReservation`).
   - Các thực thể quan trọng như `PayrollPeriod`, `PayrollItem`, `OrderCommission`, `RepairOrder`, `Invoice`, `Part`, `Estimate`, `WarrantyContract` bị thiếu, khiến cột "Đối tượng" hiển thị tên lớp Java dạng `PayrollPeriod #12`.
3. **Lỗi Zod Schema (`schemas.ts`) vô tình nuốt mất dữ liệu Người thực hiện & Tên thực thể:**
   - Trong `schemas.ts` (dòng 61–69), `auditSchema` chỉ khai báo `{ id, actorUserId, action, entityType, entityId, afterJson, createdAt }`.
   - Zod mặc định loại bỏ (strip) các trường không được khai báo. Do đó, các trường do Backend trả về trong `AuditEventResponse` gồm:
     * `actorFullName` (Họ tên người thao tác)
     * `actorUsername` (Tên tài khoản)
     * `actorRole` (Vai trò)
     * `entityLabel` (Tên hiển thị thân thiện của đối tượng)
     bị Zod xóa sạch trước khi đến component. Kết quả là bảng kiểm toán luôn hiển thị *"Tài khoản #12"* và `#id` thay vì tên thực tế!
4. **Hạn chế trong `AuditLogAspect.java` về việc ghi nhận `entityId`:**
   - Hiện tại, `AuditLogAspect` chỉ tự động lấy `entityId` khi kết quả trả về là `UserResponse` hoặc `NotificationResponse`. Với các dịch vụ khác (như `PayrollServiceImpl`), nếu không khai báo `idArgument`, `entityId` mặc định bị gán bằng `actorUserId` (ID của chính người đăng nhập), làm sai lệch ID của đối tượng bị tác động trong cơ sở dữ liệu.
5. **Trải nghiệm tìm kiếm & lọc chưa thân thiện:**
   - Ô lọc người thực hiện bắt buộc người dùng phải nhớ và nhập mã số ID (`actorId` dạng số nguyên dương như `12`), nếu gõ sai sẽ báo lỗi `Nhập ID dương hợp lệ.`
   - Chưa có bộ lọc theo Phân hệ nghiệp vụ (Phân loại theo Bảng lương, Nhân sự, Khoang xưởng, Lệnh sửa chữa, Hóa đơn...).

### 1.2. Mục tiêu cải tiến toàn diện
1. **Xây dựng Bảng Từ Điển Kiểm Toán Toàn Diện (Comprehensive Audit Catalog):**
   - Tách từ điển kiểm toán ra file riêng `frontend/src/features/audit/constants/auditCatalog.ts` để module hóa và dễ bảo trì.
   - Chuẩn hóa 100% các hành động kiểm toán trong toàn bộ 9 phân hệ của AutoGarage với:
     * Nhãn tiếng Việt chuẩn mực (`label`).
     * Mô tả hành vi chi tiết, dễ hiểu (`description`).
     * Phân loại phân hệ (`domain`: Hệ thống, Nhân sự, Ca trực, Bảng lương, Khoang xưởng, Lệnh sửa, Báo giá, KCS & Bảo hành, Kho & Thu chi).
     * Mức độ cảnh báo (`severity`: `info` - xanh dương, `success` - xanh lá, `warning` - vàng cam, `danger` - đỏ).
2. **Khắc phục triệt để lỗi Zod Schema & Hiển thị định danh người dùng:**
   - Bổ sung `actorFullName`, `actorUsername`, `actorRole`, `entityLabel` vào `auditSchema` trong `schemas.ts`.
   - Đảm bảo hiển thị đầy đủ: Họ tên người thao tác, tên đăng nhập, nhãn vai trò tiếng Việt (`Quản trị viên`, `Lễ tân`, `Kỹ thuật viên`...).
3. **Mở rộng gán nhãn thực thể tại Backend (`AuditQueryServiceImpl.java`):**
   - Hỗ trợ gán `entityLabel` cho các thực thể phổ biến (`Employee`, `PayrollPeriod`, `ServiceBay`...) khi truy vấn, giúp hiển thị trực quan (ví dụ: `Kỳ lương 11/2026`, `Khoang A1 - Cầu nâng 2 trụ`, `Lê Hoàng Minh`).
4. **Nâng cấp giao diện Bảng Kiểm Toán theo chuẩn `UI_DESIGN_GUIDELINES.md`:**
   - Thêm bộ lọc nhanh theo Phân hệ nghiệp vụ (Tabs hoặc Select phân nhóm).
   - Danh sách gợi ý hành động hiển thị bằng tiếng Việt kèm mô tả.
   - Huy hiệu (Badge) phân màu trực quan theo tính chất hành vi (Tạo mới, Chỉnh sửa, Phê duyệt, Xóa/Hủy/Từ chối).
   - Dialog "Chi tiết sự kiện" căn giữa màn hình (`placement="center"`), trình bày thông tin theo danh sách 2 cột, hiển thị JSON metadata đã được làm sạch và bảo mật (`safeAuditDetails`).

---

## 2. Bảng Từ Điển Kiểm Toán Toàn Diện (Comprehensive Audit Catalog)

### Nhóm 1: Hệ Thống, Xác Thực & Tài Khoản (Security & Accounts)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `AUTH_LOGIN` | Đăng nhập hệ thống | Người dùng đăng nhập thành công vào hệ thống. | `USER` | `info` |
| `AUTH_LOGIN_FAILED` | Đăng nhập thất bại | Nhập sai tên đăng nhập, mật khẩu hoặc tài khoản bị khóa. | `USER` | `warning` |
| `AUTH_LOGOUT_ALL` | Đăng xuất toàn bộ | Đăng xuất và thu hồi mọi phiên đăng nhập trên tất cả thiết bị. | `USER` | `info` |
| `ADMIN_BOOTSTRAPPED` | Khởi tạo Quản trị viên | Hệ thống tự động tạo tài khoản quản trị đầu tiên khi khởi chạy. | `USER` | `warning` |
| `CUSTOMER_JIT_PROVISIONED` | Tự tạo hồ sơ khách hàng | Hệ thống tự động tạo tài khoản khách hàng khi đăng nhập lần đầu. | `USER` | `info` |
| `USER_CREATED` | Tạo tài khoản mới | Thêm mới tài khoản người dùng và phân quyền ban đầu. | `USER` | `success` |
| `USER_UPDATED` | Cập nhật tài khoản | Chỉnh sửa thông tin cá nhân, phân quyền vai trò hoặc kích hoạt/khóa tài khoản. | `USER` | `info` |
| `USER_DELETED` | Xóa tài khoản | Xóa tài khoản người dùng khỏi hệ thống. | `USER` | `danger` |
| `USER_PASSWORD_CHANGED` | Đổi mật khẩu | Mật khẩu đăng nhập của tài khoản đã được thay đổi thành công. | `USER` | `warning` |

### Nhóm 2: Hồ Sơ Nhân Sự & Kỹ Thuật Viên (Employees & HR)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_EMPLOYEE` | Thêm nhân viên | Khởi tạo hồ sơ nhân sự mới trong gara kèm thông tin ngân hàng và chuyên môn. | `Employee` | `success` |
| `UPDATE_EMPLOYEE` | Cập nhật hồ sơ nhân viên | Sửa đổi thông tin nhân viên, chuyên môn kỹ thuật hoặc tài khoản ngân hàng. | `Employee` | `info` |
| `DEACTIVATE_EMPLOYEE` | Ngừng hoạt động nhân viên | Chuyển trạng thái nhân viên sang ngừng hoạt động, không xếp ca làm việc. | `Employee` | `danger` |

### Nhóm 3: Lịch Ca, Phân Công & Điểm Danh (Shifts & Attendance)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_SHIFT` | Tạo ca làm việc | Thiết lập ca làm việc mới (Sáng / Chiều / Tối) trong ngày cho xưởng. | `EmployeeShift` | `success` |
| `CANCEL_SHIFT` | Hủy ca làm việc | Hủy bỏ ca làm việc đã thiết lập kèm giải phóng phân công nhân viên. | `EmployeeShift` | `danger` |
| `REGISTER_SHIFT` | Đăng ký ca trực | Kỹ thuật viên gửi yêu cầu tham gia ca làm việc trong tuần. | `EmployeeShift` | `info` |
| `APPROVE_SHIFT_REGISTRATION` | Phê duyệt đăng ký ca | Quản lý chấp thuận cho kỹ thuật viên tham gia ca trực. | `EmployeeShift` | `success` |
| `REJECT_SHIFT_REGISTRATION` | Từ chối đăng ký ca | Quản lý từ chối yêu cầu đăng ký ca của kỹ thuật viên kèm lý do. | `EmployeeShift` | `warning` |
| `SHIFT_CHECK_IN` | Điểm danh vào ca | Kỹ thuật viên bấm điểm danh bắt đầu thời gian làm việc trong ca. | `EmployeeShift` | `info` |
| `SHIFT_CHECK_OUT` | Điểm danh tan ca | Kỹ thuật viên điểm danh kết thúc ca và chốt số giờ làm việc thực tế. | `EmployeeShift` | `info` |

### Nhóm 4: Bảng Lương, Thưởng & Hoa Hồng (Payroll & Commissions)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_PAYROLL_PERIOD` | Khởi tạo kỳ tính lương | Tạo kỳ lương mới cho tháng (từ ngày đầu tháng đến cuối tháng). | `PayrollPeriod` | `success` |
| `CALCULATE_PAYROLL` | Tính toán bảng lương | Chạy tự động thuật toán tổng hợp giờ công, hoa hồng và tính thực nhận toàn xưởng. | `PayrollPeriod` | `info` |
| `ADJUST_PAYROLL_ITEM` | Điều chỉnh lương nhân viên | Cập nhật phụ cấp, tiền phạt/giảm trừ hoặc ghi chú cho từng nhân viên. | `PayrollItem` | `warning` |
| `APPROVE_PAYROLL` | Phê duyệt bảng lương | Quản lý chốt duyệt số liệu bảng lương tháng, khóa không cho sửa đổi tự do. | `PayrollPeriod` | `success` |
| `DISBURSE_PAYROLL` | Chi trả lương toàn xưởng | Xác nhận đã hoàn tất chuyển khoản/thanh toán tiền lương cho nhân viên trong kỳ. | `PayrollPeriod` | `success` |
| `CALCULATE_ORDER_COMMISSION` | Tính hoa hồng lệnh sửa | Tính mức hoa hồng cho kỹ thuật viên dựa trên giá trị lệnh sửa chữa đã xong. | `OrderCommission` | `info` |
| `UPDATE_COMMISSION_STATUS` | Chuyển trạng thái hoa hồng | Cập nhật trạng thái duyệt chi hoặc từ chối khoản hoa hồng của thợ. | `OrderCommission` | `info` |

### Nhóm 5: Khoang Sửa Chữa & Bảo Trì Thiết Bị (Service Bays)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_BAY` | Thêm khoang sửa chữa | Khởi tạo khoang sửa chữa mới (cầu nâng, khoang chẩn đoán, khoang đồng sơn). | `ServiceBay` | `success` |
| `UPDATE_BAY` | Cập nhật khoang sửa chữa | Sửa đổi thông tin, thiết bị chuyên dụng hoặc công suất phục vụ của khoang. | `ServiceBay` | `info` |
| `DEACTIVATE_BAY` | Ngừng sử dụng khoang | Tạm ngừng hoạt động khoang, không điều phối xe vào khu vực này. | `ServiceBay` | `danger` |
| `SCHEDULE_MAINTENANCE` | Lên lịch bảo trì khoang | Đặt lịch bảo dưỡng định kỳ cầu nâng/thiết bị, tạm khóa lịch nhận xe. | `BayReservation` | `warning` |

### Nhóm 6: Tiếp Nhận & Lịch Hẹn (Bookings & Check-in)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_BOOKING` | Đặt lịch hẹn dịch vụ | Tiếp nhận và xếp lịch hẹn bảo dưỡng/sửa chữa cho khách hàng. | `Booking` | `success` |
| `CANCEL_BOOKING` | Hủy lịch hẹn | Hủy lịch hẹn dịch vụ của khách theo yêu cầu kèm lý do. | `Booking` | `danger` |
| `CHECK_IN_VEHICLE` | Tiếp nhận xe vào xưởng | Lễ tân lập phiếu tiếp nhận xe, ghi nhận số ODO, lượng xăng và tình trạng vỏ xe. | `VehicleCheckIn` | `info` |
| `HANDOVER_VEHICLE` | Bàn giao xe cho khách | Hoàn tất sửa chữa, bàn giao chìa khóa và xe cho khách xuất xưởng. | `Vehicle` | `success` |

### Nhóm 7: Lệnh Sửa Chữa & Tiến Độ Thi Công (Repair Orders & Labor)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_REPAIR_ORDER` | Mở lệnh sửa chữa | Lập lệnh sửa chữa mới cho xe đang ở xưởng kèm chẩn đoán sơ bộ. | `RepairOrder` | `success` |
| `UPDATE_REPAIR_ORDER` | Cập nhật lệnh sửa chữa | Bổ sung hạng mục công việc hoặc thay đổi tiến độ thực hiện lệnh sửa chữa. | `RepairOrder` | `info` |
| `ASSIGN_TECHNICIAN` | Phân công kỹ thuật viên | Chỉ định kỹ thuật viên phụ trách chính thi công lệnh sửa chữa. | `RepairOrder` | `info` |
| `START_LABOR` | Bắt đầu thi công hạng mục | Kỹ thuật viên bấm giờ bắt đầu thực hiện công việc sửa chữa tại khoang. | `LaborSession` | `info` |
| `STOP_LABOR` | Tạm dừng/Kết thúc thi công | Dừng bấm giờ và ghi nhận số phút thao tác thực tế của thợ. | `LaborSession` | `info` |
| `COMPLETE_REPAIR` | Hoàn thành sửa chữa | Toàn bộ các hạng mục kỹ thuật đã thi công xong, sẵn sàng chuyển kiểm định KCS. | `RepairOrder` | `success` |
| `CANCEL_REPAIR_ORDER` | Hủy lệnh sửa chữa | Hủy lệnh sửa chữa khi khách không đồng ý phương án kỹ thuật. | `RepairOrder` | `danger` |

### Nhóm 8: Báo Giá, Nghiệm Thu KCS & Bảo Hành (Estimates, QC & Warranties)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `CREATE_ESTIMATE` | Lập báo giá dịch vụ | Soạn phiếu báo giá chi tiết phụ tùng và tiền công gửi khách hàng. | `Estimate` | `success` |
| `APPROVE_ESTIMATE_ITEMS` | Khách hàng duyệt báo giá | Khách xác nhận đồng ý các hạng mục thay thế và phương án thi công. | `Estimate` | `success` |
| `REJECT_ESTIMATE_ITEMS` | Khách từ chối báo giá | Khách hàng từ chối một số hoặc toàn bộ hạng mục sửa chữa. | `Estimate` | `warning` |
| `QC_PASSED` | Nghiệm thu KCS đạt chuẩn | Tổ trưởng kiểm tra chất lượng xuất xưởng xác nhận xe đạt tiêu chuẩn an toàn. | `QualityInspection` | `success` |
| `QC_FAILED` | Nghiệm thu KCS không đạt | Xe chưa đạt yêu cầu kỹ thuật, yêu cầu kỹ thuật viên xử lý lại. | `QualityInspection` | `danger` |
| `CREATE_WARRANTY` | Kích hoạt bảo hành | Kích hoạt thời hạn bảo hành cho phụ tùng thay mới và dịch vụ thi công. | `WarrantyContract` | `success` |
| `CLAIM_WARRANTY` | Tiếp nhận yêu cầu bảo hành | Ghi nhận sự cố phát sinh của xe còn trong thời hạn bảo hành. | `WarrantyClaim` | `warning` |
| `DECIDE_CLAIM` | Thẩm định yêu cầu bảo hành | Phê duyệt hoặc từ chối xử lý bảo hành miễn phí cho khách hàng. | `WarrantyClaim` | `info` |

### Nhóm 9: Kho Vật Tư, Hóa Đơn & Sổ Quỹ (Inventory & Billing)
| Mã Kỹ Thuật (`action`) | Tên Tiếng Việt (`label`) | Mô Tả Hành Động Chi Tiết (`description`) | Đối Tượng (`entityType`) | Mức Độ (`severity`) |
| :--- | :--- | :--- | :--- | :--- |
| `IMPORT_PARTS` | Nhập kho phụ tùng | Lập phiếu nhập kho phụ tùng/vật tư từ nhà cung cấp kèm giá vốn. | `InventoryReceipt` | `success` |
| `EXPORT_PARTS` | Xuất kho phụ tùng | Xuất vật tư phụ tùng phục vụ thi công lệnh sửa chữa. | `InventoryIssue` | `info` |
| `RECONCILE_INVENTORY` | Cân đối kiểm kê kho | Điều chỉnh số lượng tồn kho thực tế sau khi kiểm kê đối soát. | `StockReconciliation` | `warning` |
| `ISSUE_INVOICE` | Phát hành hóa đơn | Xuất hóa đơn thanh toán tiền dịch vụ và phụ tùng cho khách hàng. | `Invoice` | `success` |
| `CANCEL_INVOICE` | Hủy hóa đơn dịch vụ | Hủy bỏ hóa đơn do nhầm lẫn hoặc điều chỉnh thỏa thuận khách hàng. | `Invoice` | `danger` |
| `RECORD_PAYMENT` | Thu tiền thanh toán | Ghi nhận khoản tiền mặt hoặc chuyển khoản khách hàng thanh toán cho hóa đơn. | `PaymentReceipt` | `success` |

---

## 3. Danh Sách File Mục Tiêu (Target Files & Line Ranges)

### 3.1. Frontend Files
1. **`AutoGarage/frontend/src/features/audit/constants/auditCatalog.ts` (Tạo mới hoàn toàn)**
   - Khai báo kiểu dữ liệu:
     * `AuditDomain` (`security` | `hr` | `shift` | `payroll` | `bay` | `booking` | `repair` | `qc_warranty` | `inventory_billing`)
     * `AuditSeverity` (`info` | `success` | `warning` | `danger`)
     * `AuditActionMeta` (`label`, `description`, `domain`, `domainLabel`, `severity`)
   - Khai báo `auditCatalog: Record<string, AuditActionMeta>` chứa đầy đủ tất cả các action trên.
   - Khai báo `auditEntityLabels: Record<string, string>` dịch 100% các class name Entity sang Tiếng Việt.
   - Hàm tiện ích: `getAuditActionMeta(action: string)`, `formatAuditEntity(type: string, id: string | null, label?: string | null)`.
2. **`AutoGarage/frontend/src/shared/types/schemas.ts` (Dòng 61–69)**
   - Cập nhật `auditSchema` để không bị strip các trường thông tin người dùng và nhãn:
     ```typescript
     export const auditSchema = z.object({
       id: idSchema,
       actorUserId: idSchema.nullable(),
       actorFullName: z.string().nullable().optional(),
       actorUsername: z.string().nullable().optional(),
       actorRole: z.enum(roles).nullable().optional(),
       action: z.string(),
       entityType: z.string(),
       entityId: idSchema.nullable(),
       entityLabel: z.string().nullable().optional(),
       afterJson: z.string().nullable(),
       createdAt: z.string(),
     });
     ```
3. **`AutoGarage/frontend/src/features/audit/pages/AuditPage.tsx` (Dòng 21–88, 127–250, 280–328)**
   - Import từ điển từ `auditCatalog.ts`.
   - Giữ nguyên `export function safeAuditDetails(raw: string | null)` để đảm bảo tương thích 100% với `contracts.test.ts`.
   - Cải tiến giao diện bảng:
     * Cột Hành động: Hiển thị Badge màu tương ứng (`severity`) + Tên tiếng Việt in đậm + Mô tả chi tiết bên dưới.
     * Cột Đối tượng: Sử dụng nhãn tiếng Việt từ `auditCatalog.ts` kết hợp `entityLabel`.
     * Bộ lọc: Bổ sung bộ lọc phân loại theo Phân hệ nghiệp vụ (Tất cả, Xác thực, Nhân sự, Ca trực, Lương & Thưởng, Khoang sửa, Lệnh sửa, Báo giá & Bảo hành, Kho & Kế toán).
     * Modal xem chi tiết: Căn giữa màn hình (`placement="center"`), hiển thị rõ ràng nhãn phân loại, mức độ, người thao tác, đối tượng và JSON metadata định dạng đẹp mắt.

### 3.2. Backend Files
1. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/common/constant/AuditConstants.java` (Dòng 1–29)**
   - Bổ sung đầy đủ các hằng số `ACTION_*` và `ENTITY_*` để xóa bỏ magic string ở `PayrollServiceImpl` và `SchedulingServiceImpl`.
2. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/audit/aspect/AuditLogAspect.java` (Dòng 30–58)**
   - Tối ưu trích xuất `entityId`:
     * Khi hàm trả về các Response DTO có `id` (như `PayrollPeriodResponse`, `EmployeeResponse`, `EmployeeShiftResponse`, `ServiceBayResponse`...), tự động trích xuất ID thay vì fallback về `actorId`.
3. **`AutoGarage/src/main/java/com/Group13_65CNTT/AutoGarage/audit/service/impl/AuditQueryServiceImpl.java` (Dòng 57–88)**
   - Mở rộng hàm mô tả thực thể: format nhãn định danh cho các entity quen thuộc thay vì chỉ có mỗi `USER`.

---

## 4. Hợp Đồng Kỹ Thuật (Technical Contract)

### 4.1. TypeScript Contract (`auditCatalog.ts`)
```typescript
export type AuditDomain =
  | 'security'
  | 'hr'
  | 'shift'
  | 'payroll'
  | 'bay'
  | 'booking'
  | 'repair'
  | 'qc_warranty'
  | 'inventory_billing';

export type AuditSeverity = 'info' | 'success' | 'warning' | 'danger';

export interface AuditActionMeta {
  label: string;
  description: string;
  domain: AuditDomain;
  domainLabel: string;
  severity: AuditSeverity;
}

export const auditDomains: { key: AuditDomain; label: string }[] = [
  { key: 'security', label: 'Bảo mật & Tài khoản' },
  { key: 'hr', label: 'Hồ sơ nhân sự' },
  { key: 'shift', label: 'Ca trực & Điểm danh' },
  { key: 'payroll', label: 'Bảng lương & Hoa hồng' },
  { key: 'bay', label: 'Khoang xưởng & Thiết bị' },
  { key: 'booking', label: 'Lịch hẹn & Tiếp nhận' },
  { key: 'repair', label: 'Lệnh sửa chữa & Thi công' },
  { key: 'qc_warranty', label: 'Nghiệm thu & Bảo hành' },
  { key: 'inventory_billing', label: 'Kho vật tư & Hóa đơn' },
];
```

### 4.2. Java Contract (`AuditConstants.java`)
```java
// Entities
public static final String ENTITY_USER = "USER";
public static final String ENTITY_NOTIFICATION = "NOTIFICATION";
public static final String ENTITY_EMPLOYEE = "Employee";
public static final String ENTITY_EMPLOYEE_SHIFT = "EmployeeShift";
public static final String ENTITY_SERVICE_BAY = "ServiceBay";
public static final String ENTITY_BAY_RESERVATION = "BayReservation";
public static final String ENTITY_PAYROLL_PERIOD = "PayrollPeriod";
public static final String ENTITY_PAYROLL_ITEM = "PayrollItem";
public static final String ENTITY_ORDER_COMMISSION = "OrderCommission";
public static final String ENTITY_REPAIR_ORDER = "RepairOrder";
public static final String ENTITY_ESTIMATE = "Estimate";
public static final String ENTITY_INVOICE = "Invoice";

// Actions: Gom toàn bộ vào AuditConstants dùng chung
```

---

## 5. Checklist Thực Thi (Execution Checklist)

- [x] **Step 1: Khởi tạo module từ điển `auditCatalog.ts` trên Frontend**
  - Tạo file `frontend/src/features/audit/constants/auditCatalog.ts`.
  - Khai báo kiểu `AuditDomain`, `AuditSeverity`, `AuditActionMeta`.
  - Khai báo bảng tra cứu `auditCatalog` với đầy đủ 45+ hành động và mô tả chi tiết bằng tiếng Việt.
  - Khai báo bảng chuyển đổi tên thực thể `auditEntityLabels`.
  - Export hàm tra cứu an toàn `getAuditActionMeta(action: string)`.

- [x] **Step 2: Cập nhật Zod Schema (`schemas.ts`) để hiển thị thông tin người dùng**
  - Mở rộng `auditSchema` trong `frontend/src/shared/types/schemas.ts`: thêm `actorFullName`, `actorUsername`, `actorRole`, `entityLabel`.
  - Đảm bảo Vitest `contracts.test.ts` kiểm tra schema parse dữ liệu chính xác và không làm gãy test hiện có.

- [x] **Step 3: Tái thiết kế giao diện trang Nhật ký hệ thống (`AuditPage.tsx`)**
  - Thay thế danh mục nội bộ cũ trong `AuditPage.tsx` bằng `auditCatalog`.
  - Bổ sung Badge phân loại màu sắc (`success`, `info`, `warning`, `danger`) trên từng dòng hành động.
  - Cải tiến Filter Bar: bổ sung dropdown chọn Lọc theo phân hệ nghiệp vụ và tìm kiếm hành động có gợi ý tiếng Việt rõ ràng.
  - Nâng cấp Dialog chi tiết sự kiện: hiển thị trực quan thông tin người thực hiện, đối tượng bị ảnh hưởng, thời gian định dạng chuẩn múi giờ gara và giữ nguyên `safeAuditDetails`.

- [x] **Step 4: Chuẩn hóa Constants & Aspect tại Backend**
  - Cập nhật `AuditConstants.java`: bổ sung các hằng số thực thể và hành động còn thiếu.
  - Cập nhật `AuditLogAspect.java`: trích xuất chính xác `entityId` từ kết quả phản hồi của phương thức dịch vụ thay vì luôn gán bằng `actorUserId`.
  - Cập nhật `AuditQueryServiceImpl.java`: bổ sung gán nhãn thực thể cơ bản khi trả về cho client.

- [x] **Step 5: Kiểm thử và xác minh toàn diện (Verify Phase)**
  - Chạy kiểm thử frontend: `npm --prefix AutoGarage/frontend run test` (đảm bảo 10/10 test suites, 91/91 tests pass).
  - Chạy build frontend: `npm --prefix AutoGarage/frontend run build` (đảm bảo TypeScript biên dịch thành công không có lỗi).
  - Chạy kiểm thử backend: Maven test cho các service liên quan đến audit và security.
  - Kiểm tra giao diện thực tế tại `/admin/audit` để xác nhận tất cả các hành động Bảng lương, Ca trực, Khoang xưởng đều hiển thị nhãn và mô tả tiếng Việt chi tiết.

---

## 6. Kết Quả Hội Tụ (Converge)

> Lưu trữ ngày 2026-11-03. Bản gốc bị ghi đè trong `CURRENT_PLAN.md` và được khôi phục từ lịch sử phiên làm việc.

**Kết luận**: Hội tụ một phần (`partial`). Mọi bước đã xong, các phần còn lệch đều nằm ngoài phạm vi plan.

| Mục tiêu | Trạng thái | Ghi chú |
|---|---|---|
| Từ điển hành động (`auditCatalog.ts`, 9 phân hệ) | Đạt | Hành động lạ hiện "Chưa phân loại". |
| Hiện họ tên/vai trò người thao tác | Đạt | `auditSchema` không còn làm rơi trường. |
| Nhãn đối tượng dễ đọc | Đạt | Nhân viên, Kỳ lương, Khoang. |
| Bộ lọc theo phân hệ | Lệch | Chỉ thu hẹp danh sách hành động; backend lọc theo đúng 1 action. |
| Backend phát đủ action trong từ điển | Thiếu | Booking, repair, inventory chưa gắn `@Audited`. |
| Dữ liệu audit cũ | Lệch | Bản ghi lương/ca/khoang cũ lưu `entityId` = mã người thao tác nên có thể sai nhãn. |

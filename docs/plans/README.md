# Lưu Trữ Kế Hoạch (Plans)

`docs/CURRENT_PLAN.md` chỉ chứa **một** plan đang làm. Khi plan đó hội tụ (`/converge`), bản đầy đủ kèm phần "Kết Quả Hội Tụ" được lưu vào thư mục này. Nhờ vậy, plan sau có thể ghi đè `CURRENT_PLAN.md` mà không làm mất lịch sử.

## Quy ước

- **Tên file**: `YYYY-MM-<mã-plan>.md`, ví dụ `2026-11-notifications.md`. `<mã-plan>` lấy từ dòng `> **Mã plan**:` ở đầu plan.
- **Không sửa plan đã lưu**, trừ khi cập nhật phần "Kết Quả Hội Tụ". Plan mới cần sửa tiếp phần còn thiếu thì ghi rõ "Kế thừa từ `plans/<file>`" trong phần Mục tiêu.
- **Plan mới viết theo [`_TEMPLATE.md`](_TEMPLATE.md)**, có mục "Tiêu chí chấp nhận" để bước converge đối chiếu được.

## Danh sách

| Tháng | Plan | Kết luận | Còn thiếu đáng chú ý |
|---|---|---|---|
| 2026-11 | [Audit Log](2026-11-audit-log.md) | partial | Backend chưa gắn `@Audited` cho booking/repair/inventory; lọc theo phân hệ ở backend |
| 2026-11 | [Thông báo](2026-11-notifications.md) | partial | Backend chưa phát PAYROLL/REPAIR/STOCK; thiếu biển số xe; tab nhóm lọc phía client |
| 2026-11 | [Đăng ký ca làm việc](2026-11-shift-registration-ui.md) | converged | Tab Kỹ thuật viên vẫn hiện nút thao tác quản trị trong bảng (backend chặn 403); chưa xác minh UI bằng trình duyệt |
| 2026-11 | [Chốt dữ liệu theo giai đoạn phiếu sửa chữa](2026-11-repair-lifecycle-lock.md) | done (chưa /converge) | 11/11 bước; thiếu test RTL cho `RepairOrderDetailModal` và test riêng `RepairStageRules`; đã thử hủy/lập lại hóa đơn trên dữ liệu thật |
| 2026-11 | [Phân tích hệ thống & ERD Chen](2026-11-system-analysis-erd.md) | done (chưa /converge) | 10/10 bước; lưu trước khi ghi đè bằng plan `qc-admin-on-behalf` |
| 2026-11 | [Admin ghi QC thay kỹ thuật viên](2026-11-qc-admin-on-behalf.md) | partial | AC-5 (nút QC theo vai trò) và AC-6 (sửa xe) chưa xác minh trên trình duyệt; chưa có test RTL cho nút QC |
| 2026-11 | [Phân công QC tại màn lệnh sửa chữa](2026-11-qc-assignment-on-order.md) | partial | AC-5 (403 theo vai trò) và AC-6..8 chưa xác minh trên trình duyệt; thiếu test riêng cho đường QC khi người được chọn ngoài ca |
| 2026-11 | [Danh sách người QC theo ca hiện tại và lỗi bàn giao](2026-11-qc-candidates-clarity.md) | partial | AC-5/6/7/12 chưa xác minh trên trình duyệt (hộp phân công QC, nhãn ca, tab Bàn giao lệnh #21) |
| 2026-11 | [Báo đúng lý do khi không lập được hóa đơn](2026-11-invoice-blocked-reasons.md) | partial | AC-5/6 chưa xác minh trên trình duyệt (hộp phát hành, xuất kho lệnh #21); chưa gọi API thật trên #21 |
| 2026-11 | [Xuất kho cho lệnh, duyệt báo giá, sơ đồ lịch ca và tìm kiếm](2026-11-inventory-issue-for-order.md) | partial | Step 7, 10, 17: chưa xác minh trên trình duyệt (xuất kho lệnh #21, duyệt báo giá, sơ đồ tuần ca, tìm kiếm nhật ký) |
| 2026-11 | [Sửa thu tiền hóa đơn và mã giao dịch tự sinh](2026-11-payment-receipt-fix.md) | partial | Step 8: chưa xác minh trên trình duyệt (thu hóa đơn #10, sửa/sinh lại mã, mã trùng, phiếu chi NCC) |
| 2026-11 | [Cổng chặn luồng sửa chữa, bảo hành trước bàn giao, menu nhân sự](2026-11-workflow-gates-warranty-hr-nav.md) | partial | Step 11–12: thiếu test tích hợp MySQL và test 403; AC-6/7/10 chưa xác minh trên trình duyệt; menu nhân sự được thay bởi plan hr-navigation-hubs |
| 2026-11 | [Điều hướng nhân sự: quản lý nhân viên và khoang & ca làm việc](2026-11-hr-navigation-hubs.md) | partial | Chưa xác minh trên trình duyệt (sidebar tab con, khung chi tiết, liên kết chéo, địa chỉ cũ); container frontend chưa build lại sau thay đổi cuối |

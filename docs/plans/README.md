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

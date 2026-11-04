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

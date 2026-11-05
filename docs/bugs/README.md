# Hồ Sơ Sửa Lỗi (Bugs)

Mỗi lỗi có một file `<slug>.md` theo [`_TEMPLATE.md`](_TEMPLATE.md), tạo bằng skill `/fix-bug` (chế độ `FIX:`). Quy trình gồm 3 pha: tìm nguyên nhân gốc → sửa trong phạm vi hẹp → xác nhận triệu chứng ban đầu đã hết. Chỉ kết luận `verified` mới xác nhận lỗi đã được sửa.

## Danh sách

| Ngày | Lỗi | Nguyên nhân gốc | Kết luận |
|---|---|---|---|
| 2026-11-03 | Chuông đọc "0 chưa đọc" | Số lượng dạng chuỗi `"0"` bị coi là có giá trị (`NotificationPopover.tsx`) | verified |
| 2026-11-03 | Thông báo khẩn mang badge "Thông báo chung" | Parser luôn lấy nhãn theo nhóm, bỏ qua mức ưu tiên (`notificationParser.ts`) | verified |
| 2026-11-05 | [Mất định dạng biển số xe khi load và gửi form chỉnh sửa](2026-11-vehicle-edit-license-plate-format-stripping.md) | `VehicleServiceImpl` bóc tách ký tự `-` và `.`; `UpdateVehicleRequest` yêu cầu `@NotBlank` khiến payload cập nhật bị từ chối | verified |

_Hai lỗi trên được sửa trước khi có quy trình này nên không có file hồ sơ riêng; chi tiết nằm trong [plan Thông báo](../plans/2026-11-notifications.md)._

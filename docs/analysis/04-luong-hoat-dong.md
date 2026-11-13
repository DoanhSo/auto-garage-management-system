# 4. Luồng hoạt động

> Thuộc plan `system-analysis-erd`. Mã nguồn sơ đồ nằm ở `activity/*.puml` (PlantUML, có làn theo vai trò); ảnh SVG ở `activity/svg/`.
> Muốn render lại, xem lệnh ở [`../erd/README.md`](../erd/README.md).

| Mã | Luồng | Use case | Sơ đồ |
|---|---|---|---|
| A1 | Đặt, xác nhận, dời, hủy lịch hẹn | UC07, UC08 | [SVG](activity/svg/A1-dat-lich.svg) · [nguồn](activity/A1-dat-lich.puml) |
| A2 | Từ tiếp nhận xe đến bàn giao | UC09–UC12, UC16–UC19, UC27, UC31 | [SVG](activity/svg/A2-tiep-nhan-ban-giao.svg) · [nguồn](activity/A2-tiep-nhan-ban-giao.puml) |
| A3 | Báo giá, khách duyệt, báo giá phát sinh | UC13–UC15, UC23 | [SVG](activity/svg/A3-bao-gia-duyet.svg) · [nguồn](activity/A3-bao-gia-duyet.puml) |
| A4 | Nhập, giữ, xuất, trả, điều chỉnh kho | UC22–UC26 | [SVG](activity/svg/A4-kho.svg) · [nguồn](activity/A4-kho.puml) |
| A5 | Hóa đơn, thu tiền, hủy và lập lại | UC28–UC31 | [SVG](activity/svg/A5-hoa-don-thanh-toan.svg) · [nguồn](activity/A5-hoa-don-thanh-toan.puml) |
| A6 | Bảo hành: sổ, khiếu nại, quyết định, điều chỉnh/hủy sổ | UC31–UC33 | [SVG](activity/svg/A6-bao-hanh.svg) · [nguồn](activity/A6-bao-hanh.puml) |
| A7 | Ca làm, chấm công, giờ công, lương | UC37, V12–V17 | [SVG](activity/svg/A7-ca-lam-luong.svg) · [nguồn](activity/A7-ca-lam-luong.puml) |
| A8 | Thông báo, hàng đợi gửi, nhắc bảo dưỡng | UC34, UC35 | [SVG](activity/svg/A8-thong-bao-nhac.svg) · [nguồn](activity/A8-thong-bao-nhac.puml) |

## Đối chiếu trạng thái với code

| Enum | Chuyển trạng thái có trong sơ đồ | Sơ đồ |
|---|---|---|
| `BookingStatus` | PENDING → CONFIRMED → RECEIVED; → CANCELLED; → NO_SHOW | A1 |
| `RepairOrderStatus` | NEW → DIAGNOSING → WAITING_APPROVAL → APPROVED → IN_PROGRESS ⇄ (WAITING_PARTS) → WAITING_QC → COMPLETED / QC_FAILED → INVOICED → CLOSED; mở lại chẩn đoán; INVOICED → COMPLETED khi hủy hóa đơn | A2, A3, A5 |
| `EstimateStatus` | DRAFT → SENT → PARTIALLY_APPROVED / APPROVED / REJECTED; CANCELLED (bị thay thế); EXPIRED (hiển thị) | A3 |
| `InvoiceStatus` | UNPAID → PARTIALLY_PAID → PAID; UNPAID → VOID | A5 |
| `ClaimStatus` | OPEN → APPROVED → IN_PROGRESS → CLOSED; OPEN → REJECTED | A6 |
| `WarrantyStatus` | ACTIVE → EXPIRED (theo ngày hoặc km); → VOID | A6 |

Chi tiết từng chuyển trạng thái và ma trận khóa dữ liệu theo giai đoạn nằm ở [`../REPAIR_ORDER_LIFECYCLE.md`](../REPAIR_ORDER_LIFECYCLE.md).

`WAITING_PARTS` chỉ ghi trong bảng này, không vẽ thành nhánh riêng ở A2, để sơ đồ dễ đọc.

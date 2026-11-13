# 3. Sơ đồ luồng dữ liệu (DFD)

> Thuộc plan `system-analysis-erd`. Sơ đồ viết bằng Mermaid nên GitHub và VS Code hiển thị trực tiếp.

**Ký hiệu**

| Hình | Ý nghĩa |
|---|---|
| `((P… tên))` hình tròn | Tiến trình |
| `[(D… tên)]` hình trụ | Kho dữ liệu |
| `[tên]` hình chữ nhật | Tác nhân ngoài |
| Nhãn trên mũi tên | Dữ liệu chảy qua |

## 3.1. Kho dữ liệu ↔ bảng thật (V19)

| Kho | Tên | Bảng |
|---|---|---|
| D1 | Tài khoản | `users` |
| D2 | Khách hàng và xe | `customers`, `vehicles` |
| D3 | Danh mục dịch vụ | `services` |
| D4 | Lịch hẹn và khoang | `bookings`, `service_bays`, `bay_reservations` |
| D5 | Nhân sự và ca | `employees`, `employee_shifts` |
| D6 | Phiếu sửa chữa | `repair_orders`, `vehicle_checks`, `attachments` |
| D7 | Báo giá | `estimates`, `estimate_items` |
| D8 | Công việc và giờ công | `repair_tasks`, `task_assignments`, `labor_sessions`, `repair_order_assignments` |
| D9 | Kho phụ tùng | `suppliers`, `parts`, `repair_order_parts`, `inventory_documents`, `inventory_document_items`, `inventory_transactions` |
| D10 | Hóa đơn và thu chi | `invoices`, `invoice_items`, `payments` (+ view `v_invoice_balances`, `v_supplier_balances`) |
| D11 | Bảo hành | `warranties`, `warranty_claims` |
| D12 | Thông báo và nhật ký | `notifications`, `audit_events` |
| D13 | Lương | `payroll_periods`, `payroll_items`, `order_commissions` |

Cả 34 bảng đều thuộc đúng một kho.

## 3.2. Mức ngữ cảnh

```mermaid
flowchart LR
    KH[Khách hàng]
    NV[Nhân viên gara<br/>Admin · Lễ tân · Kho · KTV]
    NCC[Nhà cung cấp]
    IDP[Keycloak]
    KENH[Kênh Email/SMS]
    HT((0. Hệ thống<br/>quản lý gara))

    KH -- "thông tin xe, yêu cầu đặt lịch,<br/>quyết định duyệt báo giá, yêu cầu bảo hành" --> HT
    HT -- "báo giá, tiến độ, hóa đơn,<br/>lịch sử xe, thông báo" --> KH
    NV -- "tiếp nhận, chẩn đoán, phân công, bấm giờ,<br/>chứng từ kho, thu chi, quyết định" --> HT
    HT -- "lịch làm việc, việc được giao,<br/>tồn kho, báo cáo, bảng lương" --> NV
    NCC -- "hóa đơn mua hàng, phụ tùng giao" --> HT
    HT -- "phiếu chi thanh toán" --> NCC
    HT -- "thông tin đăng nhập" --> IDP
    IDP -- "token, định danh" --> HT
    HT -- "nội dung thông báo" --> KENH
    KENH -- "kết quả gửi" --> HT
```

## 3.3. Mức 0

```mermaid
flowchart TB
    KH[Khách hàng]
    LT[Lễ tân]
    KTV[Kỹ thuật viên]
    KHO[Nhân viên kho]
    AD[Quản trị viên]
    NCC[Nhà cung cấp]
    KENH[Kênh Email/SMS]

    P1((P1 Tài khoản<br/>và danh mục))
    P2((P2 Lịch hẹn<br/>và điều phối))
    P3((P3 Tiếp nhận<br/>và chẩn đoán))
    P4((P4 Báo giá<br/>và duyệt))
    P5((P5 Thi công<br/>và QC))
    P6((P6 Kho<br/>phụ tùng))
    P7((P7 Hóa đơn<br/>và thu chi))
    P8((P8 Bàn giao<br/>và bảo hành))
    P9((P9 Chăm sóc<br/>và thông báo))
    P10((P10 Báo cáo<br/>và lương))

    D1[(D1 Tài khoản)]
    D2[(D2 Khách, xe)]
    D3[(D3 Dịch vụ)]
    D4[(D4 Lịch, khoang)]
    D5[(D5 Nhân sự, ca)]
    D6[(D6 Phiếu sửa chữa)]
    D7[(D7 Báo giá)]
    D8[(D8 Công việc, giờ công)]
    D9[(D9 Kho)]
    D10[(D10 Hóa đơn, thu chi)]
    D11[(D11 Bảo hành)]
    D12[(D12 Thông báo, nhật ký)]
    D13[(D13 Lương)]

    AD -- "tài khoản, dịch vụ, giá" --> P1
    LT -- "hồ sơ khách, xe" --> P1
    P1 --> D1 & D2 & D3

    KH -- "yêu cầu đặt lịch" --> P2
    AD -- "ca làm, khoang" --> P2
    D2 & D3 --> P2
    P2 -- "lịch hẹn, giữ khoang" --> D4
    P2 -- "ca đã xếp" --> D5

    LT -- "ODO, hiện trạng, ảnh" --> P3
    KTV -- "kết luận chẩn đoán" --> P3
    D4 -- "lịch hẹn" --> P3
    P3 -- "phiếu, biên bản" --> D6

    LT -- "hạng mục, giá" --> P4
    KH -- "quyết định từng dòng" --> P4
    D6 & D3 --> P4
    P4 -- "báo giá, dòng duyệt" --> D7
    P4 -- "công việc từ dòng duyệt" --> D8
    P4 -- "yêu cầu giữ hàng" --> P6

    LT -- "phân công" --> P5
    KTV -- "bắt đầu, dừng, hoàn thành, QC" --> P5
    D5 & D7 --> P5
    P5 -- "phiên giờ công" --> D8
    P5 -- "biên bản QC, trạng thái" --> D6

    KHO -- "chứng từ nhập, xuất, trả" --> P6
    NCC -- "hóa đơn mua" --> P6
    P6 -- "tồn, sổ kho" --> D9

    LT -- "lập hóa đơn, thu tiền" --> P7
    AD -- "chi nhà cung cấp, hủy hóa đơn" --> P7
    D8 & D9 & D6 --> P7
    P7 -- "hóa đơn, thanh toán" --> D10
    P7 -- "phiếu chi" --> NCC

    LT -- "ODO ra, bàn giao" --> P8
    KH -- "yêu cầu bảo hành" --> P8
    AD -- "quyết định bảo hành" --> P8
    D10 -- "dòng hóa đơn" --> P8
    P8 -- "sổ bảo hành, khiếu nại" --> D11
    P8 -- "đóng phiếu, phiếu xử lý mới" --> D6

    D2 & D11 & D8 --> P9
    P9 -- "thông báo PENDING" --> D12
    P9 -- "nội dung gửi" --> KENH
    P9 -- "thông báo, lịch sử xe" --> KH

    D8 & D10 & D9 & D5 --> P10
    P10 -- "kỳ lương, hoa hồng" --> D13
    P10 -- "báo cáo, bảng lương" --> AD
```

## 3.4. Mức 1 — P5 Thi công và QC

```mermaid
flowchart LR
    LT[Lễ tân]
    KTV[Kỹ thuật viên]
    D5[(D5 Nhân sự, ca)]
    D6[(D6 Phiếu sửa chữa)]
    D7[(D7 Báo giá)]
    D8[(D8 Công việc, giờ công)]

    P51((5.1 Phân công<br/>thợ))
    P52((5.2 Bấm giờ<br/>bắt đầu, dừng))
    P53((5.3 Hoàn thành<br/>công việc))
    P54((5.4 Kiểm tra<br/>chất lượng))

    LT -- "phân công" --> P51
    D5 -- "ca, kỹ năng, can_qc" --> P51
    D7 -- "dòng đã duyệt" --> P51
    P51 -- "phân công MAIN/ASSISTANT" --> D8
    KTV -- "bắt đầu, dừng" --> P52
    D8 -- "phân công đang hoạt động" --> P52
    P52 -- "phiên giờ công" --> D8
    KTV -- "hoàn thành" --> P53
    P53 -- "công việc COMPLETED" --> D8
    P53 -- "phiếu WAITING_QC" --> D6
    KTV -- "QC" --> P54
    D8 -- "việc xong, không còn timer" --> P54
    P54 -- "biên bản QC, COMPLETED / QC_FAILED" --> D6
    P54 -- "công việc REWORK" --> D8
```

## 3.5. Mức 1 — P6 Kho phụ tùng

```mermaid
flowchart LR
    KHO[Nhân viên kho]
    NCC[Nhà cung cấp]
    P4((P4 Báo giá))
    D7[(D7 Báo giá)]
    D9[(D9 Kho)]
    D6[(D6 Phiếu sửa chữa)]

    P61((6.1 Nhập mua,<br/>đầu kỳ))
    P62((6.2 Giữ hàng))
    P63((6.3 Xuất cho<br/>sửa chữa))
    P64((6.4 Nhập trả))
    P65((6.5 Điều chỉnh,<br/>ghi sổ))

    NCC -- "hóa đơn mua" --> P61
    KHO -- "chứng từ IN/OPENING" --> P61
    P61 -- "chứng từ DRAFT" --> D9
    P4 -- "yêu cầu giữ hàng" --> P62
    P62 -- "reserved_quantity" --> D9
    P62 -- "lượng giữ theo dòng" --> D7
    KHO -- "phiếu xuất" --> P63
    D6 -- "phiếu đang sửa" --> P63
    P63 -- "chứng từ OUT, repair_order_parts" --> D9
    KHO -- "phiếu trả" --> P64
    P64 -- "chứng từ RETURN" --> D9
    KHO -- "điều chỉnh, ghi sổ" --> P65
    D9 -- "chứng từ DRAFT" --> P65
    P65 -- "POSTED, sổ kho, tồn mới" --> D9
```

## 3.6. Mức 1 — P7 Hóa đơn và thu chi

```mermaid
flowchart LR
    LT[Lễ tân]
    AD[Quản trị viên]
    NCC[Nhà cung cấp]
    D6[(D6 Phiếu sửa chữa)]
    D8[(D8 Công việc, giờ công)]
    D9[(D9 Kho)]
    D10[(D10 Hóa đơn, thu chi)]

    P71((7.1 Phát hành<br/>hóa đơn))
    P72((7.2 Thu tiền<br/>khách))
    P73((7.3 Hủy hóa đơn))
    P74((7.4 Chi nhà<br/>cung cấp))

    LT -- "lập hóa đơn" --> P71
    D6 -- "QC PASSED" --> P71
    D8 -- "công việc COMPLETED" --> P71
    D9 -- "phụ tùng đã dùng, giá vốn" --> P71
    P71 -- "hóa đơn, dòng hóa đơn" --> D10
    P71 -- "phiếu INVOICED" --> D6
    LT -- "khoản thu" --> P72
    D10 -- "dư nợ" --> P72
    P72 -- "thanh toán IN" --> D10
    AD -- "lý do hủy" --> P73
    P73 -- "hóa đơn VOID" --> D10
    P73 -- "phiếu COMPLETED" --> D6
    AD -- "khoản chi" --> P74
    D9 -- "chứng từ IN POSTED" --> P74
    P74 -- "thanh toán OUT" --> D10
    P74 -- "phiếu chi" --> NCC
```

## 3.7. Kiểm tra cân bằng mức 0 ↔ mức 1

| Tiến trình | Luồng mức 0 | Có trong mức 1 | Kết quả |
|---|---|---|---|
| P5 | Vào: Lễ tân (phân công), KTV (bắt đầu/dừng/hoàn thành/QC), D5, D7 | 5.1 nhận Lễ tân, D5, D7; 5.2–5.4 nhận KTV | Cân bằng |
| P5 | Ra: D8 (phiên giờ công), D6 (biên bản QC, trạng thái) | 5.1–5.4 ghi D8; 5.3, 5.4 ghi D6 | Cân bằng |
| P6 | Vào: Kho (chứng từ), NCC (hóa đơn mua), P4 (yêu cầu giữ hàng) | 6.1 nhận NCC và Kho; 6.2 nhận P4; 6.3–6.5 nhận Kho | Cân bằng; 6.3 đọc thêm D6 để kiểm tra phiếu (chi tiết hóa luồng đọc) |
| P6 | Ra: D9 (tồn, sổ kho) | 6.1–6.5 ghi D9; 6.2 ghi thêm D7 (lượng giữ theo dòng) | Cân bằng; D7 là chi tiết hóa của "yêu cầu giữ hàng" |
| P7 | Vào: Lễ tân, Admin, D6, D8, D9 | 7.1, 7.2 nhận Lễ tân; 7.3, 7.4 nhận Admin; 7.1 đọc D6, D8, D9 | Cân bằng |
| P7 | Ra: D10, NCC (phiếu chi) | 7.1–7.4 ghi D10; 7.4 gửi NCC; 7.1, 7.3 ghi thêm trạng thái phiếu D6 | Cân bằng; ghi D6 là cập nhật trạng thái phiếu, mức 0 gộp trong luồng "hóa đơn" |

**Không có tiến trình "hố đen"** (chỉ nhận vào mà không xuất ra) **hay "phép màu"** (xuất ra mà không có đầu vào). Ở mức 0, mỗi tiến trình đều có ít nhất một luồng vào và một luồng ra.

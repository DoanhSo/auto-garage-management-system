# 6. Phát hiện khi đối chiếu báo cáo ↔ schema ↔ code

> Thuộc plan `system-analysis-erd`. Plan này **không sửa code hay schema**; mỗi mục ghi đề xuất để làm ở plan hoặc bug riêng.

| # | Phát hiện | Bằng chứng | Đề xuất | Mức |
|---|---|---|---|---|
| F1 | **Báo cáo và `DATABASE.md` ghi số liệu cũ.** Ghi "30 bảng, 373 cột, 71 FK"; thực tế V19 có 34 bảng, 540 cột, 98 FK | `docs/erd/schema-inventory.md` (sinh từ migration) | Đã cập nhật `DATABASE.md`; khi nộp báo cáo, ghi rõ hai mốc "thiết kế V4" và "hiện thực V19" | Tài liệu |
| F2 | **BR05 nói lịch hẹn chọn một dịch vụ chính, nhưng V11 cho `bookings.service_id` NULL** | `V11__optional_booking_service_and_unique_customer_phone.sql`; `schema-inventory.md` | Bổ sung BR05: "dịch vụ chính tùy chọn khi khách chỉ mô tả triệu chứng", hoặc bắt buộc lại ở service | Nghiệp vụ |
| F3 | **`DATABASE.md` mục 12 còn ghi "hóa đơn duy nhất mỗi phiếu"**, đã sai từ V19 | V19 thay `uq_invoices_order` bằng `uq_invoices_active_order` | Đã sửa câu này trong `DATABASE.md` | Tài liệu |
| F4 | **`warranties.voided_by` và `adjusted_by` không có khóa ngoại tới `users`**, khác các cột "người thực hiện" khác | V18 chỉ thêm cột, không thêm FK; `schema-inventory.md` | Thêm FK ở migration sau (V20) cho đồng bộ | Schema |
| F5 | **Báo cáo gộp ca làm vào UC37 nhưng không có use case cho đăng ký ca, chấm công, lương**; V12–V17 đã hiện thực các phần này | `docs/analysis/activity/A7-ca-lam-luong.puml` | Bổ sung use case UC39–UC41 (đăng ký ca, chấm công, kỳ lương) khi cập nhật báo cáo | Nghiệp vụ |
| F6 | **Báo cáo mô tả đăng nhập bằng BCrypt/JWT tự cấp; hiện thực dùng Keycloak (OIDC)** | UC01 trong `docs/source/bao-cao-goc.md`; `users.identity_id` (V9) | Cập nhật đặc tả UC01–UC02 theo Keycloak | Tài liệu |
| F7 | **`DATA_DICTIONARY.md` chỉ mô tả 30 bảng V1–V8** | Thiếu 4 bảng và các cột V9–V19 | Đã bổ sung mục "V9–V19" kèm liên kết tới `schema-inventory.md` | Tài liệu |
| F8 | **Các ràng buộc xuyên bảng như BR13, BR14, BR17, BR20 chỉ có ở service**, không có CHECK hay trigger | Bảng BR ở `02-nghiep-vu.md`, cột "Tầng" | Giữ ở service nhưng phủ test; cân nhắc trigger cho BR14 nếu có lệch tồn trong thực tế | Kiến trúc |

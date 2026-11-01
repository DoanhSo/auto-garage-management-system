-- DỮ LIỆU ĐỒNG BỘ TOÀN HỆ THỐNG AUTO GARAGE (TV1 - TV2 - TV3 - TV4)
SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET FOREIGN_KEY_CHECKS = 0;

-- 1. NGƯỜI DÙNG & KỸ THUẬT VIÊN (TV1 / TV3)
SET @admin_id = COALESCE((SELECT MIN(id) FROM users WHERE role = 'ADMIN' AND is_active = 1 LIMIT 1), 1);

INSERT INTO users (id, username, full_name, role, is_active, created_by, updated_by)
VALUES
  (2, 'ktv.nam', 'Nguyễn Thành Nam', 'TECHNICIAN', 1, @admin_id, @admin_id),
  (3, 'ktv.minh', 'Thân Đức Minh', 'TECHNICIAN', 1, @admin_id, @admin_id),
  (4, 'ktv.hung', 'Trần Văn Hùng', 'TECHNICIAN', 1, @admin_id, @admin_id)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name), role = VALUES(role), is_active = 1;

DELETE FROM employees WHERE id > 0;
INSERT INTO employees (id, user_id, employee_code, skills_json, can_qc, hourly_cost, is_active)
VALUES
  (1, 2, 'EMP-NAM', '["GẦM_MÁY", "PHANH_LỐP", "BẢO_DƯỠNG"]', 0, 120000, 1),
  (2, 3, 'EMP-MINH', '["CHẨN_ĐOÁN_ĐIỆN", "ĐẠI_TU_HỘP_SỐ", "KIỂM_ĐỊNH_QC"]', 1, 180000, 1),
  (3, 4, 'EMP-HUNG', '["THAY_DẦU_NHANH", "CÂN_MÂM", "RỬA_XE"]', 0, 80000, 1);


-- 2. KHOANG SỬA CHỮA (M06 / TV3)
DELETE FROM service_bays WHERE id > 0;
INSERT INTO service_bays (id, bay_code, bay_name, bay_type, description, is_active)
VALUES
  (1, 'BAY-01', 'Cầu nâng 1', 'TWO_POST', 'Bảo dưỡng định kỳ & Thay dầu phanh', 1),
  (2, 'BAY-02', 'Cầu nâng 2', 'TWO_POST', 'Đại tu gầm máy & Hệ thống treo', 1),
  (3, 'BAY-03', 'Cầu nâng 3', 'FOUR_POST', 'Cân chỉnh góc đặt bánh xe 3D', 1),
  (4, 'BAY-04', 'Cầu nâng 4', 'SCISSOR', 'Thay lốp, phanh & Cân mâm', 1),
  (5, 'BAY-05', 'Khoang điện', 'DIAGNOSTIC', 'Chẩn đoán điện & Quét mã lỗi OBD-II', 1),
  (6, 'BAY-06', 'Khoang QC', 'INSPECTION', 'Kiểm định xuất xưởng & Bàn giao xe', 1);


-- 3. CA LÀM VIỆC KỸ THUẬT VIÊN (M06 / TV3)
DELETE FROM employee_shifts WHERE id > 0;
INSERT INTO employee_shifts (id, employee_id, starts_at, ends_at, status, notes, created_by)
VALUES
  (1, 1, CONCAT(UTC_DATE(), ' 08:00:00'), CONCAT(UTC_DATE(), ' 12:00:00'), 'ACTIVE', 'Ca sáng: Xưởng gầm máy', @admin_id),
  (2, 1, CONCAT(UTC_DATE(), ' 13:00:00'), CONCAT(UTC_DATE(), ' 17:00:00'), 'ACTIVE', 'Ca chiều: Xưởng gầm máy & phanh', @admin_id),
  (3, 2, CONCAT(UTC_DATE(), ' 08:00:00'), CONCAT(UTC_DATE(), ' 17:00:00'), 'ACTIVE', 'Ca toàn ngày: Chẩn đoán điện & Nghiệm thu QC', @admin_id),
  (4, 3, CONCAT(UTC_DATE(), ' 08:00:00'), CONCAT(UTC_DATE(), ' 12:00:00'), 'ACTIVE', 'Ca sáng: Hỗ trợ cầu nâng 1 & 4', @admin_id),
  (5, 3, CONCAT(UTC_DATE(), ' 13:00:00'), CONCAT(UTC_DATE(), ' 17:00:00'), 'ACTIVE', 'Ca chiều: Hỗ trợ khoang lốp & vệ sinh', @admin_id);


-- 4. NHÀ CUNG CẤP & PHỤ TÙNG (M09 / TV2 / TV4)
DELETE FROM suppliers WHERE id > 0;
INSERT INTO suppliers (id, supplier_code, supplier_name, phone, email, address, is_active)
VALUES
  (1, 'NCC-BOSCH', 'Bosch Auto Parts Vietnam', '0901234567', 'bosch.contact@bosch.test', 'KCN VSIP, Bình Dương', 1),
  (2, 'NCC-CASTROL', 'Castrol BP Petco', '0912345678', 'order@castrol.test', 'Quận 1, TP. Hồ Chí Minh', 1),
  (3, 'NCC-DENSO', 'Denso International Asia', '0987654321', 'sales@denso.test', 'KCN Thăng Long, Hà Nội', 1);

DELETE FROM parts WHERE id > 0;
INSERT INTO parts (id, sku, part_name, unit, purchase_price, selling_price, stock_quantity, reserved_quantity, reorder_level, warranty_days, warranty_km, is_active)
VALUES
  (1, 'OIL-CAS-5W30', 'Dầu động cơ Castrol Magnatec 5W-30 (Can 4L)', 'Can', 320000, 480000, 45.0, 0.0, 10.0, 90, 5000, 1),
  (2, 'FIL-TOY-001', 'Lọc dầu động cơ Toyota/Lexus chính hãng', 'Cái', 85000, 150000, 30.0, 0.0, 10.0, 180, 10000, 1),
  (3, 'BRK-BRE-F01', 'Má phanh đĩa trước Brembo Ceramic cao cấp', 'Bộ', 850000, 1350000, 18.0, 0.0, 5.0, 365, 20000, 1),
  (4, 'SPK-DEN-IX01', 'Bugi đánh lửa Iridium Denso TT', 'Chiếc', 140000, 250000, 36.0, 0.0, 8.0, 365, 40000, 1),
  (5, 'ARM-CTR-002', 'Rotuyn cân bằng trước CTR Hàn Quốc', 'Chiếc', 280000, 490000, 14.0, 0.0, 4.0, 180, 15000, 1);


-- 5. KHÁCH HÀNG & XE (M02 / M03 / TV1)
DELETE FROM customers WHERE id > 0;
INSERT INTO customers (id, customer_code, full_name, phone, email, customer_type, address, credit_limit, payment_term_days, is_active)
VALUES
  (1, 'KH-001', 'Nguyễn Văn An', '0912001122', 'an.nguyen@gmail.test', 'INDIVIDUAL', 'Số 12 Kim Mã, Ba Đình, Hà Nội', 20000000, 30, 1),
  (2, 'KH-002', 'Trần Thị Mai', '0983112233', 'mai.tran@gmail.test', 'INDIVIDUAL', 'Số 45 Lê Văn Lương, Cầu Giấy, Hà Nội', 15000000, 15, 1),
  (3, 'KH-003', 'Lê Hoàng Long', '0904556677', 'long.le@outlook.test', 'INDIVIDUAL', 'Số 88 Nguyễn Trãi, Thanh Xuân, Hà Nội', 25000000, 30, 1),
  (4, 'KH-004', 'Phạm Đức Thắng', '0977889900', 'thang.pham@viettel.test', 'INDIVIDUAL', 'Số 9 Võ Chí Công, Tây Hồ, Hà Nội', 50000000, 45, 1),
  (5, 'KH-005', 'Công ty Vận tải An Bình', '0243889977', 'contact@anbinhtrans.test', 'BUSINESS', 'Lô 5 KCN Quang Minh, Mê Linh, Hà Nội', 100000000, 60, 1);

DELETE FROM vehicles WHERE id > 0;
INSERT INTO vehicles (id, customer_id, license_plate, brand, model, manufacture_year, color, fuel_type, odometer, is_active)
VALUES
  (1, 1, '30E-888.88', 'Toyota', 'Camry 2.5Q', 2021, 'Đen', 'Xăng', 42500, 1),
  (2, 2, '29A-678.90', 'Honda', 'CR-V 1.5 Turbo', 2022, 'Trắng', 'Xăng', 31200, 1),
  (3, 3, '30F-555.55', 'Mazda', 'CX-5 2.0 Premium', 2020, 'Đỏ pha lê', 'Xăng', 58000, 1),
  (4, 4, '51G-999.99', 'Mercedes-Benz', 'C200 Exclusive', 2023, 'Xanh Cavansite', 'Xăng', 18500, 1),
  (5, 5, '29C-112.23', 'Ford', 'Ranger Wildtrak 2.0 Bi-Turbo', 2021, 'Cam Cyber', 'Dầu (Diesel)', 86000, 1);

-- 6. DỊCH VỤ SỬA CHỮA / BẢO DƯỠNG (M04 / TV2)
INSERT INTO services (id, service_code, service_name, service_type, unit, unit_price, standard_minutes, is_active)
VALUES
  (1, 'SRV-OIL-CHG', 'Bảo dưỡng và thay dầu động cơ', 'SINGLE', 'lượt', 150000, 30, 1),
  (2, 'SRV-BRK-SRV', 'Bảo dưỡng và thay thế má phanh', 'SINGLE', 'gói', 250000, 60, 1),
  (3, 'SRV-WHL-BAL', 'Cân mâm bấm chì và cân chỉnh góc lái', 'SINGLE', 'gói', 300000, 45, 1),
  (4, 'SRV-OBD-DIA', 'Chẩn đoán quét mã lỗi động cơ OBD-II', 'SINGLE', 'lượt', 200000, 30, 1)
ON DUPLICATE KEY UPDATE service_name = VALUES(service_name), unit_price = VALUES(unit_price);


-- 7. LỆNH SỬA CHỮA (M08 / TV3)
DELETE FROM repair_orders WHERE id > 0;
INSERT INTO repair_orders (id, order_code, customer_id, vehicle_id, status, odometer_in, fuel_level_percent, customer_complaint, diagnosis, received_by, received_at)
VALUES
  (1, 'RO-2026-001', 1, 1, 'IN_PROGRESS', 42500, 60, 'Xe có tiếng kêu lục cục gầm trước khi qua gờ giảm tốc, phanh phát ra tiếng rít', 'Rơ rotuyn cân bằng trước trái, má phanh mòn còn dưới 2mm. Cần thay rotuyn CTR và má phanh Brembo.', @admin_id, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
  (2, 'RO-2026-002', 2, 2, 'WAITING_QC', 31200, 80, 'Bảo dưỡng định kỳ cấp 30.000 km: Thay dầu động cơ, lọc dầu', 'Đã thay dầu Castrol 5W-30 và lọc dầu Toyota. Chờ KTV trưởng kiểm tra xuất xưởng.', @admin_id, DATE_SUB(NOW(), INTERVAL 4 HOUR)),
  (3, 'RO-2026-003', 3, 3, 'DIAGNOSING', 58000, 45, 'Đèn báo Check Engine sáng liên tục, xe bị rung giật nhẹ khi tăng ga', 'Quét mã lỗi OBD-II phát hiện P0302 (bỏ lửa xi lanh 2). Đang kiểm tra đánh lửa bugi và bô-bin.', @admin_id, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
  (4, 'RO-2026-004', 4, 4, 'COMPLETED', 18500, 75, 'Bảo dưỡng định kỳ 20.000 km, thay dầu máy tổng hợp và cân mâm bấm chì', 'Đã bảo dưỡng toàn diện, cân mâm bấm chì êm ái. Kiểm định QC đạt xuất sắc.', @admin_id, DATE_SUB(NOW(), INTERVAL 1 DAY)),
  (5, 'RO-2026-005', 5, 5, 'QC_FAILED', 86000, 30, 'Kiểm tra độ chụm bánh xe và rung giật vô lăng ở tốc độ 80 km/h', 'Chạy thử cao tốc phát hiện vô lăng vẫn rung nhẹ ở dải 80-90 km/h. Yêu cầu tháo bánh cân mâm lại.', @admin_id, DATE_SUB(NOW(), INTERVAL 5 HOUR));


-- 8. BÁO GIÁ VÀ HẠNG MỤC DUYỆT (M07 / TV3)
DELETE FROM estimate_items WHERE id > 0;
DELETE FROM estimates WHERE id > 0;

INSERT INTO estimates (id, estimate_code, repair_order_id, version_no, estimate_kind, status, subtotal, discount_amount, tax_amount, total_amount, notes, created_by, sent_at)
VALUES
  (1, 'EST-2026-001', 1, 1, 'INITIAL', 'APPROVED', 2090000, 90000, 200000, 2200000, 'Báo giá phụ tùng má phanh Brembo & rotuyn CTR', @admin_id, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
  (2, 'EST-2026-002', 3, 1, 'INITIAL', 'SENT', 1200000, 0, 120000, 1320000, 'Báo giá thay 4 bugi Iridium Denso và công kiểm tra', @admin_id, DATE_SUB(NOW(), INTERVAL 30 MINUTE));

INSERT INTO estimate_items (id, estimate_id, line_no, item_type, part_id, service_id, item_name, unit, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, approval_status, approved_by, decided_at, reserved_quantity, warranty_days, warranty_km)
VALUES
  (1, 1, 1, 'PART', 3, NULL, 'Má phanh đĩa trước Brembo Ceramic cao cấp', 'Bộ', 1, 1350000, 850000, 0, 135000, 1485000, 'APPROVED', @admin_id, DATE_SUB(NOW(), INTERVAL 2 HOUR), 0, 365, 20000),
  (2, 1, 2, 'PART', 5, NULL, 'Rotuyn cân bằng trước CTR Hàn Quốc', 'Chiếc', 1, 490000, 280000, 0, 49000, 539000, 'APPROVED', @admin_id, DATE_SUB(NOW(), INTERVAL 2 HOUR), 0, 180, 15000),
  (3, 1, 3, 'SERVICE', NULL, 2, 'Bảo dưỡng và thay thế má phanh', 'gói', 1, 250000, 0, 90000, 16000, 176000, 'APPROVED', @admin_id, DATE_SUB(NOW(), INTERVAL 2 HOUR), 0, 90, 5000),
  (4, 2, 1, 'PART', 4, NULL, 'Bugi đánh lửa Iridium Denso TT', 'Chiếc', 4, 250000, 140000, 0, 100000, 1100000, 'PENDING', NULL, NULL, 0, 365, 40000),
  (5, 2, 2, 'SERVICE', NULL, 4, 'Chẩn đoán quét mã lỗi động cơ OBD-II', 'lượt', 1, 200000, 0, 0, 20000, 220000, 'PENDING', NULL, NULL, 0, 90, 5000);


-- 9. CÔNG VIỆC SỬA CHỮA (M08 / TV3)
DELETE FROM repair_tasks WHERE id > 0;
INSERT INTO repair_tasks (id, repair_order_id, estimate_item_id, service_id, task_type, task_name, status, quantity, unit_price, started_at)
VALUES
  (1, 1, 3, 2, 'REPAIR', 'Bảo dưỡng và thay thế má phanh', 'IN_PROGRESS', 1, 250000, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
  (2, 3, NULL, NULL, 'DIAGNOSIS', 'Quét mã lỗi chẩn đoán động cơ OBD-II', 'IN_PROGRESS', 1, 200000, DATE_SUB(NOW(), INTERVAL 45 MINUTE));


-- 10. ĐẶT CHỖ KHOANG SỬA CHỮA (M06 / TV3)
DELETE FROM bay_reservations WHERE id > 0;
INSERT INTO bay_reservations (id, bay_id, repair_order_id, reservation_type, status, starts_at, ends_at, notes, created_by)
VALUES
  (1, 1, 1, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'Đang thay thế rotuyn và má phanh gầm trước', @admin_id),
  (2, 6, 2, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'Khoang QC: Kiểm định xuất xưởng sau bảo dưỡng 30.000 km', @admin_id),
  (3, 5, 3, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'Khoang điện: Chẩn đoán mã lỗi động cơ P0302', @admin_id),
  (4, 3, 5, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'Cân mâm bấm chì lại do chưa đạt QC chạy thử', @admin_id);


-- 11. BIÊN BẢN KIỂM ĐỊNH QC (M10 / TV3)
DELETE FROM vehicle_checks WHERE check_type = 'QC';
INSERT INTO vehicle_checks (id, repair_order_id, check_type, attempt_no, result, checklist_json, odometer, notes, checked_by, checked_at)
VALUES
  (1, 4, 'QC', 1, 'PASSED', '{"phanh":"ĐẠT","gầm":"ĐẠT","lốp":"ĐẠT","chạy_thử":"ÊM_ÁI"}', 18520, 'Xe vận hành cực êm, các thông số kỹ thuật chuẩn xác, đủ điều kiện xuất xưởng.', @admin_id, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
  (2, 5, 'QC', 1, 'FAILED', '{"phanh":"ĐẠT","gầm":"ĐẠT","lốp":"RUNG_NHẸ","chạy_thử":"KHÔNG_ĐẠT"}', 86030, 'Vô lăng còn rung nhẹ ở dải tốc độ 80-90 km/h trên đường thử. Yêu cầu tháo bánh cân mâm lại.', @admin_id, DATE_SUB(NOW(), INTERVAL 2 HOUR));


-- 12. HÓA ĐƠN & PHỤ TÙNG LỆNH SỬA CHỮA & BẢO HÀNH (M11 / M12 / TV4 & TV3)
DELETE FROM warranty_claims WHERE id > 0;
DELETE FROM warranties WHERE id > 0;
DELETE FROM invoice_items WHERE id > 0;
DELETE FROM invoices WHERE id > 0;
DELETE FROM repair_order_parts WHERE id > 0;

INSERT INTO repair_order_parts (id, repair_order_id, estimate_item_id, part_id, quantity, unit_price, unit_cost)
VALUES
  (1, 1, 1, 3, 1, 1350000, 850000),
  (2, 1, 2, 5, 1, 490000, 280000);

-- Hóa đơn thanh toán cho khách hàng
INSERT INTO invoices (id, invoice_code, repair_order_id, customer_id, status, customer_snapshot, vehicle_snapshot, subtotal, discount_amount, tax_amount, total_amount, issued_by, issued_at)
VALUES (1, 'INV-2026-001', 4, 4, 'PAID', '{"name":"Phạm Đức Thắng","phone":"0977889900"}', '{"plate":"51G-999.99","brand":"Mercedes-Benz"}', 1840000, 0, 184000, 2024000, @admin_id, DATE_SUB(NOW(), INTERVAL 20 HOUR));

INSERT INTO invoice_items (id, invoice_id, line_no, item_type, repair_task_id, repair_order_part_id, item_name, unit, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, warranty_days, warranty_km, warranty_terms)
VALUES
  (1, 1, 1, 'SERVICE', 1, NULL, 'Bảo dưỡng và thay thế má phanh', 'gói', 1, 250000, 0, 0, 25000, 275000, 90, 5000, 'Bảo hành tiền công căn chỉnh cụm phanh 3 tháng'),
  (2, 1, 2, 'PART', NULL, 1, 'Má phanh đĩa trước Brembo Ceramic cao cấp', 'Bộ', 1, 1350000, 850000, 0, 135000, 1485000, 365, 20000, 'Bảo hành chính hãng Brembo mòn đĩa bất thường hoặc nứt vỡ');

-- Sổ bảo hành phát sinh từ invoice_item
INSERT INTO warranties (id, warranty_code, invoice_item_id, vehicle_id, customer_id, starts_at, expires_at, start_odometer, end_odometer, warranty_days, warranty_km, terms_snapshot, status, created_at)
VALUES
  (1, 'WAR-2026-001', 2, 4, 4, DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_ADD(NOW(), INTERVAL 365 DAY), 18500, 38500, 365, 20000, 'Bảo hành chính hãng Brembo 12 tháng hoặc 20.000 km đối với má phanh đĩa trước', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
  (2, 'WAR-2026-002', 1, 1, 1, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 75 DAY), 42000, 47000, 90, 5000, 'Bảo hành cụm phanh và công lắp ráp 3 tháng hoặc 5.000 km', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 15 DAY));

-- Hồ sơ khiếu nại bảo hành đang mở
INSERT INTO warranty_claims (id, claim_code, warranty_id, requested_by, requested_at, current_odometer, description, status, version)
VALUES
  (1, 'CLM-2026-001', 2, @admin_id, DATE_SUB(NOW(), INTERVAL 2 HOUR), 42650, 'Khách hàng phản ánh cụm phanh trước có tiếng kêu rít nhỏ khi rà phanh ở tốc độ chậm', 'OPEN', 0);

SET FOREIGN_KEY_CHECKS = 1;
COMMIT;

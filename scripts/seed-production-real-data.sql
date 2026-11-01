-- =====================================================================
-- AutoGarage: làm sạch dữ liệu demo cũ và nạp bộ dữ liệu thực tế cho gara.
-- Giữ nguyên: flyway_schema_history, tài khoản garage.admin, mọi tài khoản đã liên kết Keycloak (identity_id).
-- Mốc thời gian dùng UTC_TIMESTAMP() nên dữ liệu luôn tươi quanh thời điểm chạy.
-- File này được sinh tự động; chạy bằng scripts/reset-and-seed-real-data.ps1.
-- =====================================================================
SET NAMES utf8mb4;
SET @now = UTC_TIMESTAMP(6);
SET @u_admin = (SELECT id FROM users WHERE username = 'garage.admin');
-- Dừng nếu thiếu tài khoản admin gốc (subquery trả nhiều hơn 1 dòng sẽ báo lỗi).
DO IF(@u_admin IS NULL, (SELECT 1 UNION ALL SELECT 2), 0);

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE attachments;
TRUNCATE TABLE warranty_claims;
TRUNCATE TABLE warranties;
TRUNCATE TABLE payments;
TRUNCATE TABLE invoice_items;
TRUNCATE TABLE invoices;
TRUNCATE TABLE inventory_transactions;
TRUNCATE TABLE inventory_document_items;
TRUNCATE TABLE inventory_documents;
TRUNCATE TABLE vehicle_checks;
TRUNCATE TABLE bay_reservations;
TRUNCATE TABLE repair_order_parts;
TRUNCATE TABLE labor_sessions;
TRUNCATE TABLE task_assignments;
TRUNCATE TABLE repair_tasks;
TRUNCATE TABLE estimate_items;
TRUNCATE TABLE estimates;
TRUNCATE TABLE repair_orders;
TRUNCATE TABLE bookings;
TRUNCATE TABLE employee_shifts;
TRUNCATE TABLE employees;
TRUNCATE TABLE vehicles;
TRUNCATE TABLE customers;
TRUNCATE TABLE parts;
TRUNCATE TABLE suppliers;
TRUNCATE TABLE services;
TRUNCATE TABLE service_bays;
TRUNCATE TABLE notifications;
TRUNCATE TABLE audit_events;
DELETE FROM users WHERE username <> 'garage.admin' AND identity_id IS NULL AND (email IS NULL OR email <> 'doanhfilwl@gmail.com');
SET FOREIGN_KEY_CHECKS = 1;

-- Đội ngũ nhân sự (đăng nhập qua Keycloak nên không lưu mật khẩu cục bộ)
INSERT INTO users (username, email, password_hash, full_name, role, is_active) VALUES ('letan.huong', 'letan.huong@autogarage.vn', NULL, 'Nguyễn Mai Hương', 'RECEPTIONIST', 1) AS new ON DUPLICATE KEY UPDATE full_name = new.full_name, role = new.role, is_active = 1, deleted_at = NULL;
SET @u_huong = (SELECT id FROM users WHERE username = 'letan.huong' OR email = 'letan.huong@autogarage.vn' ORDER BY (username = 'letan.huong') DESC LIMIT 1);
INSERT INTO users (username, email, password_hash, full_name, role, is_active) VALUES ('thukho.tuan', 'thukho.tuan@autogarage.vn', NULL, 'Trần Anh Tuấn', 'WAREHOUSE_STAFF', 1) AS new ON DUPLICATE KEY UPDATE full_name = new.full_name, role = new.role, is_active = 1, deleted_at = NULL;
SET @u_tuan = (SELECT id FROM users WHERE username = 'thukho.tuan' OR email = 'thukho.tuan@autogarage.vn' ORDER BY (username = 'thukho.tuan') DESC LIMIT 1);
INSERT INTO users (username, email, password_hash, full_name, role, is_active) VALUES ('kythuat.minh', 'kythuat.minh@autogarage.vn', NULL, 'Lê Hoàng Minh', 'TECHNICIAN', 1) AS new ON DUPLICATE KEY UPDATE full_name = new.full_name, role = new.role, is_active = 1, deleted_at = NULL;
SET @u_minh = (SELECT id FROM users WHERE username = 'kythuat.minh' OR email = 'kythuat.minh@autogarage.vn' ORDER BY (username = 'kythuat.minh') DESC LIMIT 1);
INSERT INTO users (username, email, password_hash, full_name, role, is_active) VALUES ('kythuat.duc', 'kythuat.duc@autogarage.vn', NULL, 'Vũ Trọng Đức', 'TECHNICIAN', 1) AS new ON DUPLICATE KEY UPDATE full_name = new.full_name, role = new.role, is_active = 1, deleted_at = NULL;
SET @u_duc = (SELECT id FROM users WHERE username = 'kythuat.duc' OR email = 'kythuat.duc@autogarage.vn' ORDER BY (username = 'kythuat.duc') DESC LIMIT 1);
INSERT INTO users (username, email, password_hash, full_name, role, is_active) VALUES ('doanhfilwl.gmail.com', 'doanhfilwl@gmail.com', NULL, 'Nguyễn Quốc Doanh', 'CUSTOMER', 1) AS new ON DUPLICATE KEY UPDATE full_name = new.full_name, role = new.role, is_active = 1, deleted_at = NULL;
SET @u_cust = (SELECT id FROM users WHERE username = 'doanhfilwl.gmail.com' OR email = 'doanhfilwl@gmail.com' ORDER BY (username = 'doanhfilwl.gmail.com') DESC LIMIT 1);

INSERT INTO employees (id, user_id, employee_code, can_qc, hourly_cost, is_active) VALUES
  (1, @u_minh, 'NV-TECH-01', 1, 120000, 1),
  (2, @u_duc, 'NV-TECH-02', 0, 100000, 1);

INSERT INTO employee_shifts (id, employee_id, starts_at, ends_at, status, notes, created_by) VALUES
  (1, 1, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -2 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -2 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (2, 1, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -1 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -1 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (3, 1, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 0 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 0 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (4, 1, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (5, 1, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 2 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 2 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (6, 2, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -2 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -2 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (7, 2, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -1 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -1 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (8, 2, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 0 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 0 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (9, 2, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin),
  (10, 2, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 2 DAY),'01:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 2 DAY),'10:00:00'), 'ACTIVE', 'Ca hành chính 08:00-17:00', @u_admin);

INSERT INTO customers (id, user_id, customer_code, full_name, customer_type, phone, email, address, company_name, tax_code, credit_limit, payment_term_days, is_active) VALUES
  (1, NULL, 'KH-0001', 'Nguyễn Văn An', 'INDIVIDUAL', '0912345601', 'an.nguyen@gmail.com', '25 Phố Huế, Hai Bà Trưng, Hà Nội', NULL, NULL, 0, 0, 1),
  (2, NULL, 'KH-0002', 'Trần Thị Mai', 'INDIVIDUAL', '0987654302', 'mai.tran@gmail.com', '112 Nguyễn Trãi, Quận 1, TP. Hồ Chí Minh', NULL, NULL, 5000000, 7, 1),
  (3, NULL, 'KH-0003', 'Công ty TNHH Vận tải & Dịch vụ Hoàng Long', 'BUSINESS', '0243888903', 'ketoan@hoanglongtransport.vn', 'Số 8 Phạm Hùng, Nam Từ Liêm, Hà Nội', 'Công ty TNHH Vận tải & Dịch vụ Hoàng Long', '0108123456', 100000000, 30, 1),
  (4, NULL, 'KH-0004', 'Lê Hoàng Long', 'INDIVIDUAL', '0912345604', 'long.le@gmail.com', '57 Trần Thái Tông, Cầu Giấy, Hà Nội', NULL, NULL, 10000000, 15, 1),
  (5, NULL, 'KH-0005', 'Phạm Quốc Doanh', 'INDIVIDUAL', '0983456705', 'doanh.pham@gmail.com', '14 Lê Văn Lương, Thanh Xuân, Hà Nội', NULL, NULL, 5000000, 7, 1),
  (6, @u_cust, 'KH-0006', 'Nguyễn Quốc Doanh', 'INDIVIDUAL', '0912345606', 'doanhfilwl@gmail.com', '30 Tôn Thất Thuyết, Cầu Giấy, Hà Nội', NULL, NULL, 0, 0, 1),
  (7, NULL, 'KH-0007', 'Công ty Cổ phần Thương mại Minh Phát', 'BUSINESS', '0283999807', 'mua-hang@minhphat.vn', '88 Điện Biên Phủ, Bình Thạnh, TP. Hồ Chí Minh', 'Công ty Cổ phần Thương mại Minh Phát', '0312456789', 50000000, 30, 1),
  (8, NULL, 'KH-0008', 'Hoàng Thu Hà', 'INDIVIDUAL', '0983456708', 'ha.hoang@gmail.com', '9 Nguyễn Chí Thanh, Đống Đa, Hà Nội', NULL, NULL, 10000000, 15, 1);

INSERT INTO vehicles (id, customer_id, license_plate, vin, engine_number, brand, model, manufacture_year, color, fuel_type, odometer, inspection_due_date, is_active) VALUES
  (1, 1, '30H-889.23', 'MHFBA3EF9N0100001', '2NR-FE-100001', 'Toyota', 'Vios 1.5G', 2022, 'Trắng ngọc trai', 'Xăng', 38500, DATE_ADD(UTC_DATE(),INTERVAL 157 DAY), 1),
  (2, 2, '51K-678.90', 'JM7KFB4E9M0100002', 'PE-VPS-100002', 'Mazda', 'CX-5 2.0 Premium', 2021, 'Đỏ Soul Crystal', 'Xăng', 52300, DATE_ADD(UTC_DATE(),INTERVAL 194 DAY), 1),
  (3, 4, '29A-123.45', 'RLHRW2830L0100003', 'L15B7-100003', 'Honda', 'CR-V 1.5L Turbo', 2020, 'Xám Modern Steel', 'Xăng', 61200, DATE_ADD(UTC_DATE(),INTERVAL 231 DAY), 1),
  (4, 3, '30G-774.19', 'MNCLSFF40P0100004', 'YN2-BT-100004', 'Ford', 'Ranger Wildtrak 2.0L Bi-Turbo', 2023, 'Cam Sunset', 'Dầu Diesel', 33806, DATE_ADD(UTC_DATE(),INTERVAL 268 DAY), 1),
  (5, 5, '30K-155.68', 'KMHS381ABNU100005', 'D4HB-100005', 'Hyundai', 'SantaFe 2.2 Dầu', 2022, 'Đen Phantom', 'Dầu Diesel', 45900, DATE_ADD(UTC_DATE(),INTERVAL 305 DAY), 1),
  (6, 6, '30L-512.77', 'RLVVF8PLS0P100006', 'EV-MOTOR-100006', 'VinFast', 'VF8 Plus', 2023, 'Xanh Dương Jet', 'Điện', 15320, DATE_ADD(UTC_DATE(),INTERVAL 342 DAY), 1),
  (7, 8, '30F-366.28', 'W1KWF8EB9MR100007', 'M274-100007', 'Mercedes-Benz', 'C200 Avantgarde', 2021, 'Bạc Iridium', 'Xăng', 48750, DATE_ADD(UTC_DATE(),INTERVAL 379 DAY), 1),
  (8, 7, '51H-245.36', 'KNAMB81ANN0100008', 'D4HB-100008', 'Kia', 'Carnival 2.2D Signature', 2022, 'Trắng Snow', 'Dầu Diesel', 29850, DATE_ADD(UTC_DATE(),INTERVAL 416 DAY), 1),
  (9, 3, '29H-908.15', 'MMCSRGA1JP0100009', '4A91-100009', 'Mitsubishi', 'Xpander AT', 2023, 'Bạc Sterling', 'Xăng', 40120, DATE_ADD(UTC_DATE(),INTERVAL 453 DAY), 1),
  (10, 5, '30E-433.52', 'MHFAB3EF9P0100010', '2ZR-FE-100010', 'Toyota', 'Corolla Cross 1.8V', 2023, 'Xám Metal', 'Xăng', 21480, DATE_ADD(UTC_DATE(),INTERVAL 490 DAY), 1);

INSERT INTO services (id, service_code, service_name, service_type, description, unit, unit_price, standard_minutes, required_bay_type, interval_km, interval_months, warranty_days, warranty_km, warranty_terms, is_active) VALUES
  (1, 'DV-BD01', 'Bảo dưỡng định kỳ cấp 1 (5.000 km)', 'PACKAGE', 'Thay dầu động cơ, thay lọc dầu, kiểm tra tổng quát 20 hạng mục.', 'gói', 450000, 60, 'Cầu nâng 2 trụ', 5000, 6, 0, 0, NULL, 1),
  (2, 'DV-BD02', 'Bảo dưỡng định kỳ cấp 2 (10.000 km)', 'PACKAGE', 'Cấp 1 cộng kiểm tra hệ thống phanh, treo, lái và thay lọc gió cabin.', 'gói', 850000, 90, 'Cầu nâng 2 trụ', 10000, 12, 0, 0, NULL, 1),
  (3, 'DV-BD03', 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', 'PACKAGE', 'Cấp 2 cộng thay dầu phanh, vệ sinh buồng đốt và kiểm tra dây curoa.', 'gói', 1450000, 150, 'Cầu nâng 2 trụ', 20000, 12, 0, 0, NULL, 1),
  (4, 'DV-BD04', 'Bảo dưỡng định kỳ cấp 4 (40.000 km - đại tu định kỳ)', 'PACKAGE', 'Đại tu định kỳ: thay nước làm mát, dầu hộp số, kiểm tra toàn bộ hệ thống gầm.', 'gói', 3200000, 300, 'Cầu nâng 2 trụ', 40000, 24, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (5, 'DV-PHANH01', 'Láng đĩa phanh chuyên dụng', 'SINGLE', 'Láng đĩa phanh trên máy chuyên dụng, đảm bảo độ phẳng theo chuẩn hãng.', 'lượt', 600000, 90, 'Cầu nâng cắt kéo', NULL, NULL, 90, 5000, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (6, 'DV-LAI01', 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', 'SINGLE', 'Cân chỉnh góc camber, caster, toe bằng máy 3D Hunter.', 'lượt', 700000, 60, 'Cầu nâng 4 trụ', NULL, NULL, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (7, 'DV-MAM01', 'Cân mâm bấm chì triệt tiêu rung lắc', 'SINGLE', 'Cân bằng động 4 bánh, bấm chì chuẩn hãng.', 'lượt', 350000, 45, 'Cầu nâng cắt kéo', NULL, NULL, 15, 500, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (8, 'DV-KIM01', 'Vệ sinh kim phun buồng đốt siêu âm', 'SINGLE', 'Súc kim phun siêu âm, vệ sinh buồng đốt và họng ga.', 'lượt', 1100000, 120, 'Cầu nâng 2 trụ', NULL, NULL, 0, 0, NULL, 1),
  (9, 'DV-LANH01', 'Nạp gas & khử khuẩn dàn lạnh nội soi', 'SINGLE', 'Nạp gas lạnh, khử khuẩn dàn lạnh bằng camera nội soi.', 'lượt', 950000, 90, 'Khoang chẩn đoán điện', NULL, NULL, 30, 0, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (10, 'DV-SON01', 'Sơn gò hấp cản trước/sau', 'SINGLE', 'Gò nắn, sơn và sấy cản trước hoặc sau theo mã màu hãng.', 'lượt', 3500000, 480, 'Phòng sơn sấy', NULL, NULL, 180, 0, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (11, 'DV-DB01', 'Đánh bóng toàn thân xe', 'SINGLE', 'Đánh bóng hai bước, phủ bảo vệ sơn.', 'lượt', 2400000, 240, 'Khoang chăm sóc xe (Detailing)', NULL, NULL, 0, 0, NULL, 1),
  (12, 'DV-LAI02', 'Đại tu thước lái trợ lực điện', 'SINGLE', 'Tháo, thay phớt, hiệu chuẩn lại thước lái trợ lực điện.', 'lượt', 4800000, 360, 'Cầu nâng 2 trụ', NULL, NULL, 180, 20000, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1),
  (13, 'DV-CD01', 'Chẩn đoán lỗi OBD-II chuyên sâu', 'SINGLE', 'Quét lỗi toàn bộ ECU, đọc dữ liệu thời gian thực và lập biên bản.', 'lượt', 300000, 45, 'Khoang chẩn đoán điện', NULL, NULL, 0, 0, NULL, 1),
  (14, 'DV-PHANH02', 'Công thay má phanh trước/sau', 'SINGLE', 'Tháo lắp má phanh, vệ sinh và bôi trơn cụm phanh.', 'lượt', 450000, 75, 'Cầu nâng cắt kéo', NULL, NULL, 90, 10000, 'Bảo hành tay nghề theo chính sách xưởng, không áp dụng khi tự ý sửa chữa nơi khác.', 1);

INSERT INTO service_bays (id, bay_code, bay_name, bay_type, description, is_active) VALUES
  (1, 'BAY-01', 'Cầu nâng 2 trụ 1', 'TWO_POST', 'Bảo dưỡng nhanh và thay dầu.', 1),
  (2, 'BAY-02', 'Cầu nâng 2 trụ 2', 'TWO_POST', 'Đại tu máy gầm và hộp số.', 1),
  (3, 'BAY-03', 'Cầu nâng 4 trụ 3', 'FOUR_POST', 'Cân chỉnh thước lái 3D Hunter.', 1),
  (4, 'BAY-04', 'Cầu nâng cắt kéo 4', 'SCISSOR', 'Thay lốp và hệ thống phanh.', 1),
  (5, 'BAY-05', 'Khoang chẩn đoán điện lạnh & OBD-II', 'DIAGNOSTIC', 'Chẩn đoán lỗi điện tử, điều hòa.', 1),
  (6, 'BAY-06', 'Khoang nghiệm thu QC & hoàn thiện xe', 'INSPECTION', 'Kiểm định chất lượng trước khi giao xe.', 1);

INSERT INTO suppliers (id, supplier_code, supplier_name, phone, email, address, tax_code, payment_term_days, is_active) VALUES
  (1, 'NCC-DENSO', 'Công ty TNHH Phụ tùng Denso Việt Nam', '0243555101', 'sales@denso.vn', 'Khu công nghiệp Thăng Long, Đông Anh, Hà Nội', '0100112233', 30, 1),
  (2, 'NCC-MOTUL', 'Motul Châu Á Thái Bình Dương', '0283555102', 'order@motul-apac.vn', '45 Nguyễn Hữu Cảnh, Bình Thạnh, TP. Hồ Chí Minh', '0300998877', 15, 1),
  (3, 'NCC-MICHELIN', 'Nhà phân phối Lốp Michelin Phương Đông', '0243555103', 'dealer@michelin-phuongdong.vn', '120 Nguyễn Văn Linh, Long Biên, Hà Nội', '0101223344', 30, 1),
  (4, 'NCC-BOSCH', 'Công ty Phụ tùng Bosch Rexroth Việt Nam', '0283555104', 'parts@bosch-rexroth.vn', 'Khu chế xuất Tân Thuận, Quận 7, TP. Hồ Chí Minh', '0301445566', 30, 1),
  (5, 'NCC-ANTHAI', 'Phụ tùng Má phanh Brembo & Akebono An Thái', '0243555105', 'kinhdoanh@anthai-parts.vn', '36 Giải Phóng, Hoàng Mai, Hà Nội', '0102667788', 20, 1);

INSERT INTO parts (id, sku, part_name, unit, preferred_supplier_id, purchase_price, selling_price, stock_quantity, reserved_quantity, reorder_level, warranty_days, warranty_km, warranty_terms, is_active) VALUES
  (1, 'MOT-8100-5W40-4L', 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 2, 780000, 980000, 0, 0, 10, 0, 0, NULL, 1),
  (2, 'DEN-OF-90915', 'Lọc dầu Denso chính hãng', 'Cái', 1, 65000, 95000, 0, 0, 15, 0, 0, NULL, 1),
  (3, 'BOS-AF-1457', 'Lọc gió động cơ Bosch', 'Cái', 4, 120000, 185000, 0, 0, 10, 0, 0, NULL, 1),
  (4, 'DEN-CF-CARBON', 'Lọc gió cabin than hoạt tính', 'Cái', 1, 110000, 170000, 0, 0, 10, 0, 0, NULL, 1),
  (5, 'BRE-CER-FRONT', 'Má phanh gốm Brembo Ceramic (trước)', 'Bộ', 5, 1450000, 2050000, 0, 0, 6, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 1),
  (6, 'MIC-PRI4-2155517', 'Lốp xe Michelin Primacy 4 ST 215/55R17', 'Chiếc', 3, 2350000, 2980000, 0, 0, 8, 365, 40000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 1),
  (7, 'DEN-IR-TOUGH', 'Bugi Denso Iridium Tough', 'Chiếc', 1, 185000, 260000, 0, 0, 12, 0, 0, NULL, 1),
  (8, 'PRE-COOL-5050', 'Nước làm mát Prestone 50/50 (Can 4L)', 'Can 4L', 2, 190000, 270000, 0, 0, 8, 0, 0, NULL, 1),
  (9, 'CAS-DOT4-500', 'Dầu phanh Castrol DOT 4 (Chai 500ml)', 'Chai', 2, 85000, 130000, 0, 0, 10, 0, 0, NULL, 1),
  (10, 'CTR-RCB-FRONT', 'Rotuyn cân bằng trước CTR', 'Quả', 5, 230000, 340000, 0, 0, 6, 90, 5000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 1),
  (11, 'GAT-BELT-6PK', 'Dây curoa tổng Gates', 'Cái', 4, 320000, 470000, 0, 0, 5, 180, 20000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 1),
  (12, 'MOT-ATF-MULTI-1L', 'Dầu hộp số Motul Multi ATF (Chai 1L)', 'Chai', 2, 245000, 340000, 0, 0, 10, 0, 0, NULL, 1),
  (13, 'BOS-WIPER-AERO', 'Gạt mưa Bosch Aerotwin', 'Bộ', 4, 210000, 310000, 0, 0, 6, 0, 0, NULL, 1),
  (14, 'DEN-O2-SENSOR', 'Cảm biến oxy Denso', 'Cái', 1, 1250000, 1750000, 0, 0, 3, 365, 20000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 1),
  (15, 'AKE-PAD-REAR', 'Má phanh sau Akebono', 'Bộ', 5, 780000, 1150000, 0, 0, 5, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 1);

INSERT INTO bookings (id, booking_code, customer_id, vehicle_id, service_id, appointment_at, expected_end_at, request_description, status, cancellation_reason, created_by, created_at) VALUES
  (1, 'LH-20261101-001', 3, 9, 4, TIMESTAMPADD(MINUTE,-360,@now), TIMESTAMPADD(MINUTE,-60,@now), 'Bảo dưỡng cấp 4 vạn km & kiểm tra tiếng kêu gầm trước.', 'RECEIVED', NULL, @u_huong, TIMESTAMPADD(MINUTE,-8640,@now)),
  (2, 'LH-20261101-002', 3, 4, 6, TIMESTAMPADD(MINUTE,180,@now), TIMESTAMPADD(MINUTE,240,@now), 'Cân chỉnh góc lái & thước lái 3D Hunter.', 'CONFIRMED', NULL, @u_huong, TIMESTAMPADD(MINUTE,-7200,@now)),
  (3, 'LH-20261101-003', 4, 3, 9, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'02:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'03:30:00'), 'Bảo dưỡng điều hòa, khử mùi nội soi.', 'CONFIRMED', NULL, @u_huong, TIMESTAMPADD(MINUTE,-5760,@now)),
  (4, 'LH-20261101-004', 5, 5, 10, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'07:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'15:00:00'), 'Sơn sấy gò nắn cản trước sau va quệt nhẹ.', 'PENDING', NULL, @u_huong, TIMESTAMPADD(MINUTE,-600,@now)),
  (5, 'LH-20261101-005', 1, 1, 3, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 6 DAY),'01:30:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 6 DAY),'04:00:00'), 'Bảo dưỡng định kỳ cấp 3 theo sổ bảo hành.', 'PENDING', NULL, @u_huong, TIMESTAMPADD(MINUTE,-600,@now)),
  (6, 'LH-20261101-006', 2, 2, 2, TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -1 DAY),'03:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL -1 DAY),'04:30:00'), 'Bảo dưỡng định kỳ cấp 2.', 'CANCELLED', 'Khách bận dời lịch.', @u_huong, TIMESTAMPADD(MINUTE,-600,@now));

INSERT INTO repair_orders (id, order_code, booking_id, customer_id, vehicle_id, order_type, status, odometer_in, odometer_out, fuel_level_percent, customer_complaint, diagnosis, received_by, received_at, latest_qc_check_id, handed_over_by, handed_over_at, closed_at) VALUES
  (1, 'RO-20261101-001', 1, 3, 9, 'NORMAL', 'NEW', 40120, NULL, 60, 'Bảo dưỡng cấp 4 vạn km & kiểm tra tiếng kêu gầm trước khi qua gờ giảm tốc.', NULL, @u_huong, TIMESTAMPADD(MINUTE,-300,@now), NULL, NULL, NULL, NULL),
  (2, 'RO-20261031-001', NULL, 8, 7, 'NORMAL', 'DIAGNOSING', 48750, NULL, 45, 'Đèn báo lỗi hệ thống treo, xe rung nhẹ khi chạy ở 60-80 km/h.', 'Đang quét lỗi OBD-II toàn bộ ECU, kiểm tra cụm giảm xóc trước.', @u_huong, TIMESTAMPADD(MINUTE,-1500,@now), NULL, NULL, NULL, NULL),
  (3, 'RO-20261030-001', NULL, 5, 10, 'NORMAL', 'WAITING_APPROVAL', 21480, NULL, 25, 'Hao xăng hơn bình thường, máy rung nhẹ khi dừng đèn đỏ.', 'Kim phun bám cặn, bugi mòn điện cực, lọc gió bẩn.', @u_huong, TIMESTAMPADD(MINUTE,-2000,@now), NULL, NULL, NULL, NULL),
  (4, 'RO-20261031-002', NULL, 4, 3, 'NORMAL', 'APPROVED', 61200, NULL, 70, 'Vô lăng lệch về bên phải, phanh kêu khi đạp nhẹ.', 'Lệch góc đặt bánh trước, má phanh trước còn 2 mm.', @u_huong, TIMESTAMPADD(MINUTE,-1000,@now), NULL, NULL, NULL, NULL),
  (5, 'RO-20261101-002', NULL, 1, 1, 'NORMAL', 'IN_PROGRESS', 38500, NULL, 50, 'Bảo dưỡng định kỳ cấp 2 theo sổ bảo hành.', 'Xe hoạt động bình thường, đến kỳ bảo dưỡng 10.000 km.', @u_huong, TIMESTAMPADD(MINUTE,-240,@now), NULL, NULL, NULL, NULL),
  (6, 'RO-20261030-002', NULL, 6, 6, 'NORMAL', 'WAITING_QC', 15320, NULL, 40, 'Xe bị lệch hướng khi chạy tốc độ cao, muốn đánh bóng lại sơn.', 'Lệch toe bánh trước; sơn xỉn do rửa xe tự động nhiều lần.', @u_huong, TIMESTAMPADD(MINUTE,-2800,@now), NULL, NULL, NULL, NULL),
  (7, 'RO-20261030-003', NULL, 2, 2, 'NORMAL', 'QC_FAILED', 52300, NULL, 55, 'Thay lốp mới và cân chỉnh vì xe rung vô lăng.', 'Lốp mòn lệch; sau thay lốp cần cân mâm bấm chì.', @u_huong, TIMESTAMPADD(MINUTE,-3000,@now), NULL, NULL, NULL, NULL),
  (8, 'RO-20261030-004', NULL, 3, 4, 'NORMAL', 'COMPLETED', 33800, 33806, 35, 'Bảo dưỡng cấp 3 và thay má phanh trước.', 'Má phanh trước mòn tới vạch báo, dầu động cơ đã quá hạn.', @u_huong, TIMESTAMPADD(MINUTE,-2200,@now), NULL, NULL, NULL, NULL),
  (9, 'RO-20261028-001', NULL, 3, 4, 'NORMAL', 'INVOICED', 32900, 32908, 60, 'Đại tu định kỳ cấp 4 theo lịch đội xe.', 'Xe đạt 32.900 km, đến kỳ thay nước làm mát và dầu hộp số.', @u_huong, TIMESTAMPADD(MINUTE,-5760,@now), NULL, NULL, NULL, NULL),
  (10, 'RO-20261031-003', NULL, 5, 5, 'NORMAL', 'WAITING_PARTS', 45900, NULL, 45, 'Đại tu máy gầm và hộp số do chảy dầu hộp số.', 'Chảy dầu phớt hộp số, dây curoa tổng nứt chân.', @u_huong, TIMESTAMPADD(MINUTE,-400,@now), NULL, NULL, NULL, NULL),
  (11, 'RO-20261031-004', NULL, 7, 8, 'NORMAL', 'IN_PROGRESS', 29850, NULL, 25, 'Phanh sau kêu, thay má phanh và láng đĩa.', 'Má phanh sau mòn hết, đĩa phanh có vết xước nhẹ.', @u_huong, TIMESTAMPADD(MINUTE,-500,@now), NULL, NULL, NULL, NULL),
  (12, 'RO-20261007-001', NULL, 6, 6, 'NORMAL', 'CLOSED', 14100, 14106, 70, 'Bảo dưỡng 15.000 km đầu tiên cho xe điện, cân chỉnh lại góc đặt bánh.', 'Xe bình thường, lệch toe nhẹ, vệ sinh và kiểm tra tổng quát.', @u_huong, TIMESTAMPADD(MINUTE,-36000,@now), NULL, @u_huong, TIMESTAMPADD(MINUTE,-35520,@now), TIMESTAMPADD(MINUTE,-35520,@now)),
  (13, 'RO-20261012-001', NULL, 7, 8, 'NORMAL', 'CLOSED', 28300, 28309, 50, 'Bảo dưỡng cấp 3 xe công ty.', 'Đến kỳ 20.000 km.', @u_huong, TIMESTAMPADD(MINUTE,-28800,@now), NULL, @u_huong, TIMESTAMPADD(MINUTE,-28470,@now), TIMESTAMPADD(MINUTE,-28470,@now)),
  (14, 'RO-20260914-001', NULL, 1, 1, 'NORMAL', 'CLOSED', 33100, 33104, 40, 'Bảo dưỡng cấp 1.', 'Xe tốt.', @u_huong, TIMESTAMPADD(MINUTE,-69120,@now), NULL, @u_huong, TIMESTAMPADD(MINUTE,-68880,@now), TIMESTAMPADD(MINUTE,-68880,@now)),
  (15, 'RO-20261018-001', NULL, 5, 10, 'NORMAL', 'CLOSED', 19800, 19806, 55, 'Phanh trước bị rít, kêu khi dừng xe.', 'Má phanh trước mòn, đĩa phanh cần láng lại.', @u_huong, TIMESTAMPADD(MINUTE,-20160,@now), NULL, @u_huong, TIMESTAMPADD(MINUTE,-19815,@now), TIMESTAMPADD(MINUTE,-19815,@now)),
  (16, 'RO-20261025-001', NULL, 8, 7, 'NORMAL', 'INVOICED', 47200, 47207, 35, 'Bảo dưỡng cấp 2 và chẩn đoán đèn báo lỗi.', 'Lỗi cảm biến áp suất lốp, đã xóa lỗi.', @u_huong, TIMESTAMPADD(MINUTE,-10080,@now), NULL, NULL, NULL, NULL),
  (17, 'RO-20261027-001', NULL, 4, 3, 'NORMAL', 'INVOICED', 60100, 60107, 60, 'Xe hao xăng, điều hòa không mát sâu.', 'Kim phun bẩn, dàn lạnh có mùi.', @u_huong, TIMESTAMPADD(MINUTE,-7200,@now), NULL, NULL, NULL, NULL),
  (18, 'RO-20261020-001', NULL, 3, 9, 'NORMAL', 'INVOICED', 38200, 38207, 45, 'Bảo dưỡng cấp 3 theo lịch đội xe.', 'Đến kỳ 20.000 km.', @u_huong, TIMESTAMPADD(MINUTE,-17280,@now), NULL, NULL, NULL, NULL);

INSERT INTO estimates (id, estimate_code, repair_order_id, version_no, estimate_kind, parent_estimate_id, status, subtotal, discount_amount, tax_amount, total_amount, notes, created_by, sent_at, valid_until, created_at) VALUES
  (3, 'BG-20261030-001', 3, 1, 'INITIAL', NULL, 'SENT', 2325000.00, 0, 232500.00, 2557500.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-1960,@now), TIMESTAMPADD(MINUTE,8120,@now), TIMESTAMPADD(MINUTE,-1965,@now)),
  (4, 'BG-20261031-002', 4, 1, 'INITIAL', NULL, 'APPROVED', 3480000.00, 0, 348000.00, 3828000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-960,@now), TIMESTAMPADD(MINUTE,9120,@now), TIMESTAMPADD(MINUTE,-965,@now)),
  (5, 'BG-20261101-002', 5, 1, 'INITIAL', NULL, 'APPROVED', 2095000.00, 0, 209500.00, 2304500.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-200,@now), TIMESTAMPADD(MINUTE,9880,@now), TIMESTAMPADD(MINUTE,-205,@now)),
  (6, 'BG-20261030-002', 6, 1, 'INITIAL', NULL, 'APPROVED', 3580000.00, 0, 358000.00, 3938000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-2760,@now), TIMESTAMPADD(MINUTE,7320,@now), TIMESTAMPADD(MINUTE,-2765,@now)),
  (7, 'BG-20261030-003', 7, 1, 'INITIAL', NULL, 'APPROVED', 12970000.00, 0, 1297000.00, 14267000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-2960,@now), TIMESTAMPADD(MINUTE,7120,@now), TIMESTAMPADD(MINUTE,-2965,@now)),
  (8, 'BG-20261030-004', 8, 1, 'INITIAL', NULL, 'APPROVED', 6190000.00, 0, 619000.00, 6809000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-2160,@now), TIMESTAMPADD(MINUTE,7920,@now), TIMESTAMPADD(MINUTE,-2165,@now)),
  (9, 'BG-20261028-001', 9, 1, 'INITIAL', NULL, 'APPROVED', 6080000.00, 0, 608000.00, 6688000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-5720,@now), TIMESTAMPADD(MINUTE,4360,@now), TIMESTAMPADD(MINUTE,-5725,@now)),
  (10, 'BG-20261031-003', 10, 1, 'INITIAL', NULL, 'APPROVED', 4620000.00, 0, 462000.00, 5082000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-360,@now), TIMESTAMPADD(MINUTE,9720,@now), TIMESTAMPADD(MINUTE,-365,@now)),
  (11, 'BG-20261031-004', 11, 1, 'INITIAL', NULL, 'APPROVED', 2330000.00, 0, 233000.00, 2563000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-460,@now), TIMESTAMPADD(MINUTE,9620,@now), TIMESTAMPADD(MINUTE,-465,@now)),
  (12, 'BG-20261007-001', 12, 1, 'INITIAL', NULL, 'APPROVED', 3580000.00, 0, 358000.00, 3938000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-35960,@now), TIMESTAMPADD(MINUTE,-25880,@now), TIMESTAMPADD(MINUTE,-35965,@now)),
  (13, 'BG-20261012-001', 13, 1, 'INITIAL', NULL, 'APPROVED', 3860000.00, 0, 386000.00, 4246000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-28760,@now), TIMESTAMPADD(MINUTE,-18680,@now), TIMESTAMPADD(MINUTE,-28765,@now)),
  (14, 'BG-20260914-001', 14, 1, 'INITIAL', NULL, 'APPROVED', 1525000.00, 0, 152500.00, 1677500.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-69080,@now), TIMESTAMPADD(MINUTE,-59000,@now), TIMESTAMPADD(MINUTE,-69085,@now)),
  (15, 'BG-20261018-001', 15, 1, 'INITIAL', NULL, 'APPROVED', 3100000.00, 0, 310000.00, 3410000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-20120,@now), TIMESTAMPADD(MINUTE,-10040,@now), TIMESTAMPADD(MINUTE,-20125,@now)),
  (16, 'BG-20261025-001', 16, 1, 'INITIAL', NULL, 'APPROVED', 2355000.00, 0, 235500.00, 2590500.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-10040,@now), TIMESTAMPADD(MINUTE,40,@now), TIMESTAMPADD(MINUTE,-10045,@now)),
  (17, 'BG-20261027-001', 17, 1, 'INITIAL', NULL, 'APPROVED', 3260000.00, 0, 326000.00, 3586000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-7160,@now), TIMESTAMPADD(MINUTE,2920,@now), TIMESTAMPADD(MINUTE,-7165,@now)),
  (18, 'BG-20261020-001', 18, 1, 'INITIAL', NULL, 'APPROVED', 2880000.00, 0, 288000.00, 3168000.00, 'Báo giá gồm tiền công và phụ tùng chính hãng, đã bao gồm VAT 10%.', @u_huong, TIMESTAMPADD(MINUTE,-17240,@now), TIMESTAMPADD(MINUTE,-7160,@now), TIMESTAMPADD(MINUTE,-17245,@now));

INSERT INTO estimate_items (id, estimate_id, line_no, item_type, service_id, part_id, item_name, unit, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, approval_status, approved_by, decided_at, decision_note, reserved_quantity, warranty_days, warranty_km, warranty_terms, created_at) VALUES
  (1, 3, 1, 'SERVICE', 8, NULL, 'Vệ sinh kim phun buồng đốt siêu âm', 'lượt', 1, 1100000, 385000, 0, 110000.00, 1210000.00, 'PENDING', NULL, NULL, NULL, 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-1965,@now)),
  (2, 3, 2, 'PART', NULL, 7, 'Bugi Denso Iridium Tough', 'Chiếc', 4, 260000, 185000, 0, 104000.00, 1144000.00, 'PENDING', NULL, NULL, NULL, 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-1965,@now)),
  (3, 3, 3, 'PART', NULL, 3, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 'PENDING', NULL, NULL, NULL, 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-1965,@now)),
  (4, 4, 1, 'SERVICE', 6, NULL, 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', 'lượt', 1, 700000, 245000, 0, 70000.00, 770000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-950,@now), 'Khách đồng ý hạng mục.', 0, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-965,@now)),
  (5, 4, 2, 'SERVICE', 5, NULL, 'Láng đĩa phanh chuyên dụng', 'lượt', 1, 600000, 210000, 0, 60000.00, 660000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-950,@now), 'Khách đồng ý hạng mục.', 0, 90, 5000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-965,@now)),
  (6, 4, 3, 'PART', NULL, 5, 'Má phanh gốm Brembo Ceramic (trước)', 'Bộ', 1, 2050000, 1450000, 0, 205000.00, 2255000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-950,@now), 'Khách đồng ý hạng mục.', 1, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-965,@now)),
  (7, 4, 4, 'PART', NULL, 9, 'Dầu phanh Castrol DOT 4 (Chai 500ml)', 'Chai', 1, 130000, 85000, 0, 13000.00, 143000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-950,@now), 'Khách đồng ý hạng mục.', 1, 0, 0, NULL, TIMESTAMPADD(MINUTE,-965,@now)),
  (8, 5, 1, 'SERVICE', 2, NULL, 'Bảo dưỡng định kỳ cấp 2 (10.000 km)', 'gói', 1, 850000, 297000, 0, 85000.00, 935000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-190,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-205,@now)),
  (9, 5, 2, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-190,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-205,@now)),
  (10, 5, 3, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-190,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-205,@now)),
  (11, 5, 4, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-190,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-205,@now)),
  (12, 6, 1, 'SERVICE', 6, NULL, 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', 'lượt', 1, 700000, 245000, 0, 70000.00, 770000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2750,@now), 'Khách đồng ý hạng mục.', 0, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-2765,@now)),
  (13, 6, 2, 'SERVICE', 11, NULL, 'Đánh bóng toàn thân xe', 'lượt', 1, 2400000, 840000, 0, 240000.00, 2640000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2765,@now)),
  (14, 6, 3, 'PART', NULL, 13, 'Gạt mưa Bosch Aerotwin', 'Bộ', 1, 310000, 210000, 0, 31000.00, 341000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2765,@now)),
  (15, 6, 4, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2765,@now)),
  (16, 7, 1, 'SERVICE', 6, NULL, 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', 'lượt', 1, 700000, 245000, 0, 70000.00, 770000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2950,@now), 'Khách đồng ý hạng mục.', 0, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-2965,@now)),
  (17, 7, 2, 'SERVICE', 7, NULL, 'Cân mâm bấm chì triệt tiêu rung lắc', 'lượt', 1, 350000, 122000, 0, 35000.00, 385000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2950,@now), 'Khách đồng ý hạng mục.', 0, 15, 500, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-2965,@now)),
  (18, 7, 3, 'PART', NULL, 6, 'Lốp xe Michelin Primacy 4 ST 215/55R17', 'Chiếc', 4, 2980000, 2350000, 0, 1192000.00, 13112000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2950,@now), 'Khách đồng ý hạng mục.', 0, 365, 40000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-2965,@now)),
  (19, 8, 1, 'SERVICE', 3, NULL, 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', 'gói', 1, 1450000, 507000, 0, 145000.00, 1595000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2165,@now)),
  (20, 8, 2, 'SERVICE', 14, NULL, 'Công thay má phanh trước/sau', 'lượt', 1, 450000, 157000, 0, 45000.00, 495000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2150,@now), 'Khách đồng ý hạng mục.', 0, 90, 10000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-2165,@now)),
  (21, 8, 3, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 2, 980000, 780000, 0, 196000.00, 2156000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2165,@now)),
  (22, 8, 4, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2165,@now)),
  (23, 8, 5, 'PART', NULL, 3, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-2165,@now)),
  (24, 8, 6, 'PART', NULL, 5, 'Má phanh gốm Brembo Ceramic (trước)', 'Bộ', 1, 2050000, 1450000, 0, 205000.00, 2255000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-2150,@now), 'Khách đồng ý hạng mục.', 0, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-2165,@now)),
  (25, 9, 1, 'SERVICE', 4, NULL, 'Bảo dưỡng định kỳ cấp 4 (40.000 km - đại tu định kỳ)', 'gói', 1, 3200000, 1120000, 0, 320000.00, 3520000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-5710,@now), 'Khách đồng ý hạng mục.', 0, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-5725,@now)),
  (26, 9, 2, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 2, 980000, 780000, 0, 196000.00, 2156000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-5710,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-5725,@now)),
  (27, 9, 3, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-5710,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-5725,@now)),
  (28, 9, 4, 'PART', NULL, 3, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-5710,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-5725,@now)),
  (29, 9, 5, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-5710,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-5725,@now)),
  (30, 9, 6, 'PART', NULL, 11, 'Dây curoa tổng Gates', 'Cái', 1, 470000, 320000, 0, 47000.00, 517000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-5710,@now), 'Khách đồng ý hạng mục.', 0, 180, 20000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-5725,@now)),
  (31, 10, 1, 'SERVICE', 4, NULL, 'Bảo dưỡng định kỳ cấp 4 (40.000 km - đại tu định kỳ)', 'gói', 1, 3200000, 1120000, 0, 320000.00, 3520000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-350,@now), 'Khách đồng ý hạng mục.', 0, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-365,@now)),
  (32, 10, 2, 'PART', NULL, 12, 'Dầu hộp số Motul Multi ATF (Chai 1L)', 'Chai', 2, 340000, 245000, 0, 68000.00, 748000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-350,@now), 'Khách đồng ý hạng mục.', 2, 0, 0, NULL, TIMESTAMPADD(MINUTE,-365,@now)),
  (33, 10, 3, 'PART', NULL, 11, 'Dây curoa tổng Gates', 'Cái', 1, 470000, 320000, 0, 47000.00, 517000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-350,@now), 'Khách đồng ý hạng mục.', 1, 180, 20000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-365,@now)),
  (34, 10, 4, 'PART', NULL, 8, 'Nước làm mát Prestone 50/50 (Can 4L)', 'Can 4L', 1, 270000, 190000, 0, 27000.00, 297000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-350,@now), 'Khách đồng ý hạng mục.', 1, 0, 0, NULL, TIMESTAMPADD(MINUTE,-365,@now)),
  (35, 11, 1, 'SERVICE', 14, NULL, 'Công thay má phanh trước/sau', 'lượt', 1, 450000, 157000, 0, 45000.00, 495000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-450,@now), 'Khách đồng ý hạng mục.', 0, 90, 10000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-465,@now)),
  (36, 11, 2, 'SERVICE', 5, NULL, 'Láng đĩa phanh chuyên dụng', 'lượt', 1, 600000, 210000, 0, 60000.00, 660000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-450,@now), 'Khách đồng ý hạng mục.', 0, 90, 5000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-465,@now)),
  (37, 11, 3, 'PART', NULL, 15, 'Má phanh sau Akebono', 'Bộ', 1, 1150000, 780000, 0, 115000.00, 1265000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-450,@now), 'Khách đồng ý hạng mục.', 0, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-465,@now)),
  (38, 11, 4, 'PART', NULL, 9, 'Dầu phanh Castrol DOT 4 (Chai 500ml)', 'Chai', 1, 130000, 85000, 0, 13000.00, 143000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-450,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-465,@now)),
  (39, 12, 1, 'SERVICE', 6, NULL, 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', 'lượt', 1, 700000, 245000, 0, 70000.00, 770000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-35950,@now), 'Khách đồng ý hạng mục.', 0, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-35965,@now)),
  (40, 12, 2, 'SERVICE', 11, NULL, 'Đánh bóng toàn thân xe', 'lượt', 1, 2400000, 840000, 0, 240000.00, 2640000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-35950,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-35965,@now)),
  (41, 12, 3, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-35950,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-35965,@now)),
  (42, 12, 4, 'PART', NULL, 13, 'Gạt mưa Bosch Aerotwin', 'Bộ', 1, 310000, 210000, 0, 31000.00, 341000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-35950,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-35965,@now)),
  (43, 13, 1, 'SERVICE', 3, NULL, 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', 'gói', 1, 1450000, 507000, 0, 145000.00, 1595000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-28750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-28765,@now)),
  (44, 13, 2, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 2, 980000, 780000, 0, 196000.00, 2156000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-28750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-28765,@now)),
  (45, 13, 3, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-28750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-28765,@now)),
  (46, 13, 4, 'PART', NULL, 3, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-28750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-28765,@now)),
  (47, 13, 5, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-28750,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-28765,@now)),
  (48, 14, 1, 'SERVICE', 1, NULL, 'Bảo dưỡng định kỳ cấp 1 (5.000 km)', 'gói', 1, 450000, 157000, 0, 45000.00, 495000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-69070,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-69085,@now)),
  (49, 14, 2, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-69070,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-69085,@now)),
  (50, 14, 3, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-69070,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-69085,@now)),
  (51, 15, 1, 'SERVICE', 14, NULL, 'Công thay má phanh trước/sau', 'lượt', 1, 450000, 157000, 0, 45000.00, 495000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-20110,@now), 'Khách đồng ý hạng mục.', 0, 90, 10000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-20125,@now)),
  (52, 15, 2, 'SERVICE', 5, NULL, 'Láng đĩa phanh chuyên dụng', 'lượt', 1, 600000, 210000, 0, 60000.00, 660000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-20110,@now), 'Khách đồng ý hạng mục.', 0, 90, 5000, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-20125,@now)),
  (53, 15, 3, 'PART', NULL, 5, 'Má phanh gốm Brembo Ceramic (trước)', 'Bộ', 1, 2050000, 1450000, 0, 205000.00, 2255000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-20110,@now), 'Khách đồng ý hạng mục.', 0, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', TIMESTAMPADD(MINUTE,-20125,@now)),
  (54, 16, 1, 'SERVICE', 2, NULL, 'Bảo dưỡng định kỳ cấp 2 (10.000 km)', 'gói', 1, 850000, 297000, 0, 85000.00, 935000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-10030,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-10045,@now)),
  (55, 16, 2, 'SERVICE', 13, NULL, 'Chẩn đoán lỗi OBD-II chuyên sâu', 'lượt', 1, 300000, 105000, 0, 30000.00, 330000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-10030,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-10045,@now)),
  (56, 16, 3, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-10030,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-10045,@now)),
  (57, 16, 4, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-10030,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-10045,@now)),
  (58, 16, 5, 'PART', NULL, 9, 'Dầu phanh Castrol DOT 4 (Chai 500ml)', 'Chai', 1, 130000, 85000, 0, 13000.00, 143000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-10030,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-10045,@now)),
  (59, 17, 1, 'SERVICE', 8, NULL, 'Vệ sinh kim phun buồng đốt siêu âm', 'lượt', 1, 1100000, 385000, 0, 110000.00, 1210000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-7150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-7165,@now)),
  (60, 17, 2, 'SERVICE', 9, NULL, 'Nạp gas & khử khuẩn dàn lạnh nội soi', 'lượt', 1, 950000, 332000, 0, 95000.00, 1045000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-7150,@now), 'Khách đồng ý hạng mục.', 0, 30, 0, 'Bảo hành tay nghề theo chính sách xưởng.', TIMESTAMPADD(MINUTE,-7165,@now)),
  (61, 17, 3, 'PART', NULL, 7, 'Bugi Denso Iridium Tough', 'Chiếc', 4, 260000, 185000, 0, 104000.00, 1144000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-7150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-7165,@now)),
  (62, 17, 4, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-7150,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-7165,@now)),
  (63, 18, 1, 'SERVICE', 3, NULL, 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', 'gói', 1, 1450000, 507000, 0, 145000.00, 1595000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-17230,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-17245,@now)),
  (64, 18, 2, 'PART', NULL, 1, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-17230,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-17245,@now)),
  (65, 18, 3, 'PART', NULL, 2, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-17230,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-17245,@now)),
  (66, 18, 4, 'PART', NULL, 3, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-17230,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-17245,@now)),
  (67, 18, 5, 'PART', NULL, 4, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 'APPROVED', @u_huong, TIMESTAMPADD(MINUTE,-17230,@now), 'Khách đồng ý hạng mục.', 0, 0, 0, NULL, TIMESTAMPADD(MINUTE,-17245,@now));

INSERT INTO repair_tasks (id, repair_order_id, estimate_item_id, service_id, task_type, task_name, description, status, quantity, unit_price, started_at, completed_at, completion_note, created_at) VALUES
  (1, 2, NULL, 13, 'DIAGNOSIS', 'Chẩn đoán lỗi OBD-II chuyên sâu', 'Quét lỗi toàn bộ ECU và kiểm tra hệ thống treo.', 'IN_PROGRESS', 1, 300000, TIMESTAMPADD(MINUTE,-45,@now), NULL, NULL, TIMESTAMPADD(MINUTE,-1445,@now)),
  (2, 4, 4, 6, 'REPAIR', 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', NULL, 'PENDING', 1, 700000, NULL, NULL, NULL, TIMESTAMPADD(MINUTE,-945,@now)),
  (3, 4, 5, 5, 'REPAIR', 'Láng đĩa phanh chuyên dụng', NULL, 'PENDING', 1, 600000, NULL, NULL, NULL, TIMESTAMPADD(MINUTE,-945,@now)),
  (4, 5, 8, 2, 'REPAIR', 'Bảo dưỡng định kỳ cấp 2 (10.000 km)', NULL, 'IN_PROGRESS', 1, 850000, TIMESTAMPADD(MINUTE,-40,@now), NULL, NULL, TIMESTAMPADD(MINUTE,-185,@now)),
  (5, 6, 12, 6, 'REPAIR', 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', NULL, 'COMPLETED', 1, 700000, TIMESTAMPADD(MINUTE,-2740,@now), TIMESTAMPADD(MINUTE,-2680,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-2745,@now)),
  (6, 6, 13, 11, 'REPAIR', 'Đánh bóng toàn thân xe', NULL, 'COMPLETED', 1, 2400000, TIMESTAMPADD(MINUTE,-2665,@now), TIMESTAMPADD(MINUTE,-2425,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-2745,@now)),
  (7, 7, 16, 6, 'REPAIR', 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', NULL, 'COMPLETED', 1, 700000, TIMESTAMPADD(MINUTE,-2940,@now), TIMESTAMPADD(MINUTE,-2880,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-2945,@now)),
  (8, 7, 17, 7, 'REPAIR', 'Cân mâm bấm chì triệt tiêu rung lắc', NULL, 'REWORK', 1, 350000, TIMESTAMPADD(MINUTE,-2865,@now), TIMESTAMPADD(MINUTE,-2820,@now), NULL, TIMESTAMPADD(MINUTE,-2945,@now)),
  (9, 8, 19, 3, 'REPAIR', 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', NULL, 'COMPLETED', 1, 1450000, TIMESTAMPADD(MINUTE,-2140,@now), TIMESTAMPADD(MINUTE,-1990,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-2145,@now)),
  (10, 8, 20, 14, 'REPAIR', 'Công thay má phanh trước/sau', NULL, 'COMPLETED', 1, 450000, TIMESTAMPADD(MINUTE,-1975,@now), TIMESTAMPADD(MINUTE,-1900,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-2145,@now)),
  (11, 9, 25, 4, 'REPAIR', 'Bảo dưỡng định kỳ cấp 4 (40.000 km - đại tu định kỳ)', NULL, 'COMPLETED', 1, 3200000, TIMESTAMPADD(MINUTE,-5700,@now), TIMESTAMPADD(MINUTE,-5400,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-5705,@now)),
  (12, 10, 31, 4, 'REPAIR', 'Bảo dưỡng định kỳ cấp 4 (40.000 km - đại tu định kỳ)', NULL, 'PAUSED', 1, 3200000, TIMESTAMPADD(MINUTE,-300,@now), NULL, NULL, TIMESTAMPADD(MINUTE,-345,@now)),
  (13, 11, 35, 14, 'REPAIR', 'Công thay má phanh trước/sau', NULL, 'COMPLETED', 1, 450000, TIMESTAMPADD(MINUTE,-440,@now), TIMESTAMPADD(MINUTE,-365,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-445,@now)),
  (14, 11, 36, 5, 'REPAIR', 'Láng đĩa phanh chuyên dụng', NULL, 'ASSIGNED', 1, 600000, NULL, NULL, NULL, TIMESTAMPADD(MINUTE,-445,@now)),
  (15, 12, 39, 6, 'REPAIR', 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', NULL, 'COMPLETED', 1, 700000, TIMESTAMPADD(MINUTE,-35940,@now), TIMESTAMPADD(MINUTE,-35880,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-35945,@now)),
  (16, 12, 40, 11, 'REPAIR', 'Đánh bóng toàn thân xe', NULL, 'COMPLETED', 1, 2400000, TIMESTAMPADD(MINUTE,-35865,@now), TIMESTAMPADD(MINUTE,-35625,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-35945,@now)),
  (17, 13, 43, 3, 'REPAIR', 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', NULL, 'COMPLETED', 1, 1450000, TIMESTAMPADD(MINUTE,-28740,@now), TIMESTAMPADD(MINUTE,-28590,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-28745,@now)),
  (18, 14, 48, 1, 'REPAIR', 'Bảo dưỡng định kỳ cấp 1 (5.000 km)', NULL, 'COMPLETED', 1, 450000, TIMESTAMPADD(MINUTE,-69060,@now), TIMESTAMPADD(MINUTE,-69000,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-69065,@now)),
  (19, 15, 51, 14, 'REPAIR', 'Công thay má phanh trước/sau', NULL, 'COMPLETED', 1, 450000, TIMESTAMPADD(MINUTE,-20100,@now), TIMESTAMPADD(MINUTE,-20025,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-20105,@now)),
  (20, 15, 52, 5, 'REPAIR', 'Láng đĩa phanh chuyên dụng', NULL, 'COMPLETED', 1, 600000, TIMESTAMPADD(MINUTE,-20010,@now), TIMESTAMPADD(MINUTE,-19920,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-20105,@now)),
  (21, 16, 54, 2, 'REPAIR', 'Bảo dưỡng định kỳ cấp 2 (10.000 km)', NULL, 'COMPLETED', 1, 850000, TIMESTAMPADD(MINUTE,-10020,@now), TIMESTAMPADD(MINUTE,-9930,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-10025,@now)),
  (22, 16, 55, 13, 'REPAIR', 'Chẩn đoán lỗi OBD-II chuyên sâu', NULL, 'COMPLETED', 1, 300000, TIMESTAMPADD(MINUTE,-9915,@now), TIMESTAMPADD(MINUTE,-9870,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-10025,@now)),
  (23, 17, 59, 8, 'REPAIR', 'Vệ sinh kim phun buồng đốt siêu âm', NULL, 'COMPLETED', 1, 1100000, TIMESTAMPADD(MINUTE,-7140,@now), TIMESTAMPADD(MINUTE,-7020,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-7145,@now)),
  (24, 17, 60, 9, 'REPAIR', 'Nạp gas & khử khuẩn dàn lạnh nội soi', NULL, 'COMPLETED', 1, 950000, TIMESTAMPADD(MINUTE,-7005,@now), TIMESTAMPADD(MINUTE,-6915,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-7145,@now)),
  (25, 18, 63, 3, 'REPAIR', 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', NULL, 'COMPLETED', 1, 1450000, TIMESTAMPADD(MINUTE,-17220,@now), TIMESTAMPADD(MINUTE,-17070,@now), 'Hoàn thành đúng quy trình.', TIMESTAMPADD(MINUTE,-17225,@now));

INSERT INTO task_assignments (id, task_id, employee_id, assignment_role, is_active, starts_at, ends_at, assigned_by, assigned_at) VALUES
  (1, 1, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-45,@now), TIMESTAMPADD(MINUTE,60,@now), @u_admin, TIMESTAMPADD(MINUTE,-1442,@now)),
  (2, 4, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-40,@now), TIMESTAMPADD(MINUTE,80,@now), @u_admin, TIMESTAMPADD(MINUTE,-182,@now)),
  (3, 5, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-2740,@now), TIMESTAMPADD(MINUTE,-2650,@now), @u_admin, TIMESTAMPADD(MINUTE,-2742,@now)),
  (4, 6, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-2665,@now), TIMESTAMPADD(MINUTE,-2395,@now), @u_admin, TIMESTAMPADD(MINUTE,-2742,@now)),
  (5, 7, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-2940,@now), TIMESTAMPADD(MINUTE,-2850,@now), @u_admin, TIMESTAMPADD(MINUTE,-2942,@now)),
  (6, 8, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-2865,@now), TIMESTAMPADD(MINUTE,-2790,@now), @u_admin, TIMESTAMPADD(MINUTE,-2942,@now)),
  (7, 9, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-2140,@now), TIMESTAMPADD(MINUTE,-1960,@now), @u_admin, TIMESTAMPADD(MINUTE,-2142,@now)),
  (8, 10, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-1975,@now), TIMESTAMPADD(MINUTE,-1870,@now), @u_admin, TIMESTAMPADD(MINUTE,-2142,@now)),
  (9, 11, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-5700,@now), TIMESTAMPADD(MINUTE,-5370,@now), @u_admin, TIMESTAMPADD(MINUTE,-5702,@now)),
  (10, 12, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-300,@now), TIMESTAMPADD(MINUTE,60,@now), @u_admin, TIMESTAMPADD(MINUTE,-342,@now)),
  (11, 13, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-440,@now), TIMESTAMPADD(MINUTE,-335,@now), @u_admin, TIMESTAMPADD(MINUTE,-442,@now)),
  (12, 14, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,30,@now), TIMESTAMPADD(MINUTE,150,@now), @u_admin, TIMESTAMPADD(MINUTE,-442,@now)),
  (13, 15, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-35940,@now), TIMESTAMPADD(MINUTE,-35850,@now), @u_admin, TIMESTAMPADD(MINUTE,-35942,@now)),
  (14, 16, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-35865,@now), TIMESTAMPADD(MINUTE,-35595,@now), @u_admin, TIMESTAMPADD(MINUTE,-35942,@now)),
  (15, 17, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-28740,@now), TIMESTAMPADD(MINUTE,-28560,@now), @u_admin, TIMESTAMPADD(MINUTE,-28742,@now)),
  (16, 18, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-69060,@now), TIMESTAMPADD(MINUTE,-68970,@now), @u_admin, TIMESTAMPADD(MINUTE,-69062,@now)),
  (17, 19, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-20100,@now), TIMESTAMPADD(MINUTE,-19995,@now), @u_admin, TIMESTAMPADD(MINUTE,-20102,@now)),
  (18, 20, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-20010,@now), TIMESTAMPADD(MINUTE,-19890,@now), @u_admin, TIMESTAMPADD(MINUTE,-20102,@now)),
  (19, 21, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-10020,@now), TIMESTAMPADD(MINUTE,-9900,@now), @u_admin, TIMESTAMPADD(MINUTE,-10022,@now)),
  (20, 22, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-9915,@now), TIMESTAMPADD(MINUTE,-9840,@now), @u_admin, TIMESTAMPADD(MINUTE,-10022,@now)),
  (21, 23, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-7140,@now), TIMESTAMPADD(MINUTE,-6990,@now), @u_admin, TIMESTAMPADD(MINUTE,-7142,@now)),
  (22, 24, 2, 'MAIN', 1, TIMESTAMPADD(MINUTE,-7005,@now), TIMESTAMPADD(MINUTE,-6885,@now), @u_admin, TIMESTAMPADD(MINUTE,-7142,@now)),
  (23, 25, 1, 'MAIN', 1, TIMESTAMPADD(MINUTE,-17220,@now), TIMESTAMPADD(MINUTE,-17040,@now), @u_admin, TIMESTAMPADD(MINUTE,-17222,@now));

INSERT INTO labor_sessions (id, assignment_id, employee_id, starts_at, ends_at, stop_reason, hourly_cost_snapshot, created_at) VALUES
  (1, 1, 2, TIMESTAMPADD(MINUTE,-45,@now), NULL, NULL, 100000, TIMESTAMPADD(MINUTE,-45,@now)),
  (2, 2, 1, TIMESTAMPADD(MINUTE,-40,@now), NULL, NULL, 120000, TIMESTAMPADD(MINUTE,-40,@now)),
  (3, 3, 1, TIMESTAMPADD(MINUTE,-2740,@now), TIMESTAMPADD(MINUTE,-2680,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-2740,@now)),
  (4, 4, 1, TIMESTAMPADD(MINUTE,-2665,@now), TIMESTAMPADD(MINUTE,-2425,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-2665,@now)),
  (5, 5, 1, TIMESTAMPADD(MINUTE,-2940,@now), TIMESTAMPADD(MINUTE,-2880,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-2940,@now)),
  (6, 6, 1, TIMESTAMPADD(MINUTE,-2865,@now), TIMESTAMPADD(MINUTE,-2820,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-2865,@now)),
  (7, 7, 1, TIMESTAMPADD(MINUTE,-2140,@now), TIMESTAMPADD(MINUTE,-1990,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-2140,@now)),
  (8, 8, 1, TIMESTAMPADD(MINUTE,-1975,@now), TIMESTAMPADD(MINUTE,-1900,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-1975,@now)),
  (9, 9, 1, TIMESTAMPADD(MINUTE,-5700,@now), TIMESTAMPADD(MINUTE,-5400,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-5700,@now)),
  (10, 10, 1, TIMESTAMPADD(MINUTE,-300,@now), TIMESTAMPADD(MINUTE,-180,@now), 'PAUSE', 120000, TIMESTAMPADD(MINUTE,-300,@now)),
  (11, 11, 2, TIMESTAMPADD(MINUTE,-440,@now), TIMESTAMPADD(MINUTE,-365,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-440,@now)),
  (12, 13, 1, TIMESTAMPADD(MINUTE,-35940,@now), TIMESTAMPADD(MINUTE,-35880,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-35940,@now)),
  (13, 14, 1, TIMESTAMPADD(MINUTE,-35865,@now), TIMESTAMPADD(MINUTE,-35625,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-35865,@now)),
  (14, 15, 1, TIMESTAMPADD(MINUTE,-28740,@now), TIMESTAMPADD(MINUTE,-28590,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-28740,@now)),
  (15, 16, 2, TIMESTAMPADD(MINUTE,-69060,@now), TIMESTAMPADD(MINUTE,-69000,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-69060,@now)),
  (16, 17, 2, TIMESTAMPADD(MINUTE,-20100,@now), TIMESTAMPADD(MINUTE,-20025,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-20100,@now)),
  (17, 18, 2, TIMESTAMPADD(MINUTE,-20010,@now), TIMESTAMPADD(MINUTE,-19920,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-20010,@now)),
  (18, 19, 2, TIMESTAMPADD(MINUTE,-10020,@now), TIMESTAMPADD(MINUTE,-9930,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-10020,@now)),
  (19, 20, 2, TIMESTAMPADD(MINUTE,-9915,@now), TIMESTAMPADD(MINUTE,-9870,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-9915,@now)),
  (20, 21, 2, TIMESTAMPADD(MINUTE,-7140,@now), TIMESTAMPADD(MINUTE,-7020,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-7140,@now)),
  (21, 22, 2, TIMESTAMPADD(MINUTE,-7005,@now), TIMESTAMPADD(MINUTE,-6915,@now), 'COMPLETE', 100000, TIMESTAMPADD(MINUTE,-7005,@now)),
  (22, 23, 1, TIMESTAMPADD(MINUTE,-17220,@now), TIMESTAMPADD(MINUTE,-17070,@now), 'COMPLETE', 120000, TIMESTAMPADD(MINUTE,-17220,@now));

INSERT INTO repair_order_parts (id, repair_order_id, estimate_item_id, part_id, quantity, unit_price, unit_cost, created_at) VALUES
  (1, 5, 9, 1, 1, 980000, 780000, TIMESTAMPADD(MINUTE,-140,@now)),
  (2, 5, 10, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-140,@now)),
  (3, 5, 11, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-140,@now)),
  (4, 6, 14, 13, 1, 310000, 210000, TIMESTAMPADD(MINUTE,-2700,@now)),
  (5, 6, 15, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-2700,@now)),
  (6, 7, 18, 6, 4, 2980000, 2350000, TIMESTAMPADD(MINUTE,-2900,@now)),
  (7, 8, 21, 1, 2, 980000, 780000, TIMESTAMPADD(MINUTE,-2100,@now)),
  (8, 8, 22, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-2100,@now)),
  (9, 8, 23, 3, 1, 185000, 120000, TIMESTAMPADD(MINUTE,-2100,@now)),
  (10, 8, 24, 5, 1, 2050000, 1450000, TIMESTAMPADD(MINUTE,-2100,@now)),
  (11, 9, 26, 1, 2, 980000, 780000, TIMESTAMPADD(MINUTE,-5660,@now)),
  (12, 9, 27, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-5660,@now)),
  (13, 9, 28, 3, 1, 185000, 120000, TIMESTAMPADD(MINUTE,-5660,@now)),
  (14, 9, 29, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-5660,@now)),
  (15, 9, 30, 11, 1, 470000, 320000, TIMESTAMPADD(MINUTE,-5660,@now)),
  (16, 11, 37, 15, 1, 1150000, 780000, TIMESTAMPADD(MINUTE,-400,@now)),
  (17, 11, 38, 9, 1, 130000, 85000, TIMESTAMPADD(MINUTE,-400,@now)),
  (18, 12, 41, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-35900,@now)),
  (19, 12, 42, 13, 1, 310000, 210000, TIMESTAMPADD(MINUTE,-35900,@now)),
  (20, 13, 44, 1, 2, 980000, 780000, TIMESTAMPADD(MINUTE,-28700,@now)),
  (21, 13, 45, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-28700,@now)),
  (22, 13, 46, 3, 1, 185000, 120000, TIMESTAMPADD(MINUTE,-28700,@now)),
  (23, 13, 47, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-28700,@now)),
  (24, 14, 49, 1, 1, 980000, 780000, TIMESTAMPADD(MINUTE,-69020,@now)),
  (25, 14, 50, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-69020,@now)),
  (26, 15, 53, 5, 1, 2050000, 1450000, TIMESTAMPADD(MINUTE,-20060,@now)),
  (27, 16, 56, 1, 1, 980000, 780000, TIMESTAMPADD(MINUTE,-9980,@now)),
  (28, 16, 57, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-9980,@now)),
  (29, 16, 58, 9, 1, 130000, 85000, TIMESTAMPADD(MINUTE,-9980,@now)),
  (30, 17, 61, 7, 4, 260000, 185000, TIMESTAMPADD(MINUTE,-7100,@now)),
  (31, 17, 62, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-7100,@now)),
  (32, 18, 64, 1, 1, 980000, 780000, TIMESTAMPADD(MINUTE,-17180,@now)),
  (33, 18, 65, 2, 1, 95000, 65000, TIMESTAMPADD(MINUTE,-17180,@now)),
  (34, 18, 66, 3, 1, 185000, 120000, TIMESTAMPADD(MINUTE,-17180,@now)),
  (35, 18, 67, 4, 1, 170000, 110000, TIMESTAMPADD(MINUTE,-17180,@now));

INSERT INTO vehicle_checks (id, repair_order_id, check_type, attempt_no, result, checklist_json, schema_version, odometer, notes, checked_by, checked_at) VALUES
  (1, 1, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 40120, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-295,@now)),
  (2, 2, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Có", "personalItems": "Đã bàn giao lại cho khách"}', 1, 48750, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-1495,@now)),
  (3, 3, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 21480, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-1995,@now)),
  (4, 4, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 61200, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-995,@now)),
  (5, 5, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 38500, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-235,@now)),
  (6, 6, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 15320, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-2795,@now)),
  (7, 7, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Có", "personalItems": "Đã bàn giao lại cho khách"}', 1, 52300, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-2995,@now)),
  (8, 7, 'QC', 1, 'FAILED', '{"roadTest": true, "steeringVibration": true, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 52306, 'Chạy thử vẫn còn rung vô lăng ở 80 km/h, yêu cầu cân mâm lại.', @u_minh, TIMESTAMPADD(MINUTE,-2805,@now)),
  (9, 8, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 33800, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-2195,@now)),
  (10, 8, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 33806, 'Chạy thử 15 km, phanh êm, không rung lắc.', @u_minh, TIMESTAMPADD(MINUTE,-1885,@now)),
  (11, 9, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 32900, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-5755,@now)),
  (12, 9, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 32906, 'Đạt yêu cầu.', @u_minh, TIMESTAMPADD(MINUTE,-5370,@now)),
  (13, 10, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 45900, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-395,@now)),
  (14, 11, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 29850, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-495,@now)),
  (15, 12, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 14100, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-35995,@now)),
  (16, 12, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 14106, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-35610,@now)),
  (17, 12, 'HANDOVER', 1, 'RECORDED', '{"documentsReturned": true, "keysReturned": true, "customerSignature": true}', 1, 14106, 'Bàn giao xe và hướng dẫn khách các hạng mục đã thực hiện.', @u_huong, TIMESTAMPADD(MINUTE,-35520,@now)),
  (18, 13, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 28300, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-28795,@now)),
  (19, 13, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 28306, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-28560,@now)),
  (20, 13, 'HANDOVER', 1, 'RECORDED', '{"documentsReturned": true, "keysReturned": true, "customerSignature": true}', 1, 28309, 'Bàn giao xe và hướng dẫn khách các hạng mục đã thực hiện.', @u_huong, TIMESTAMPADD(MINUTE,-28470,@now)),
  (21, 14, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 33100, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-69115,@now)),
  (22, 14, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 33106, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-68970,@now)),
  (23, 14, 'HANDOVER', 1, 'RECORDED', '{"documentsReturned": true, "keysReturned": true, "customerSignature": true}', 1, 33104, 'Bàn giao xe và hướng dẫn khách các hạng mục đã thực hiện.', @u_huong, TIMESTAMPADD(MINUTE,-68880,@now)),
  (24, 15, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 19800, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-20155,@now)),
  (25, 15, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 19806, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-19905,@now)),
  (26, 15, 'HANDOVER', 1, 'RECORDED', '{"documentsReturned": true, "keysReturned": true, "customerSignature": true}', 1, 19806, 'Bàn giao xe và hướng dẫn khách các hạng mục đã thực hiện.', @u_huong, TIMESTAMPADD(MINUTE,-19815,@now)),
  (27, 16, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 47200, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-10075,@now)),
  (28, 16, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 47206, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-9855,@now)),
  (29, 17, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 60100, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-7195,@now)),
  (30, 17, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 60106, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-6900,@now)),
  (31, 18, 'CHECK_IN', 1, 'RECORDED', '{"exterior": "Không có trầy xước mới", "interior": "Sạch sẽ", "warningLights": "Không", "personalItems": "Đã bàn giao lại cho khách"}', 1, 38200, 'Tiếp nhận xe, chụp ảnh hiện trạng cùng khách hàng.', @u_huong, TIMESTAMPADD(MINUTE,-17275,@now)),
  (32, 18, 'QC', 1, 'PASSED', '{"roadTest": true, "steeringVibration": false, "brakes": "Tốt", "leaks": "Không", "cleanliness": "Đã vệ sinh"}', 1, 38206, 'Đạt.', @u_minh, TIMESTAMPADD(MINUTE,-17040,@now));

-- Liên kết biên bản QC mới nhất (khóa vòng giữa repair_orders và vehicle_checks)
UPDATE repair_orders SET latest_qc_check_id = 8 WHERE id = 7;
UPDATE repair_orders SET latest_qc_check_id = 10 WHERE id = 8;
UPDATE repair_orders SET latest_qc_check_id = 12 WHERE id = 9;
UPDATE repair_orders SET latest_qc_check_id = 16 WHERE id = 12;
UPDATE repair_orders SET latest_qc_check_id = 19 WHERE id = 13;
UPDATE repair_orders SET latest_qc_check_id = 22 WHERE id = 14;
UPDATE repair_orders SET latest_qc_check_id = 25 WHERE id = 15;
UPDATE repair_orders SET latest_qc_check_id = 28 WHERE id = 16;
UPDATE repair_orders SET latest_qc_check_id = 30 WHERE id = 17;
UPDATE repair_orders SET latest_qc_check_id = 32 WHERE id = 18;

INSERT INTO bay_reservations (id, bay_id, booking_id, repair_order_id, reservation_type, status, starts_at, ends_at, expires_at, released_at, notes, created_by, created_at) VALUES
  (1, 5, NULL, 2, 'REPAIR', 'IN_USE', TIMESTAMPADD(MINUTE,-60,@now), TIMESTAMPADD(MINUTE,60,@now), NULL, NULL, 'Xe đang trong khoang.', @u_huong, TIMESTAMPADD(MINUTE,-1440,@now)),
  (2, 6, NULL, 4, 'REPAIR', 'HELD', TIMESTAMPADD(MINUTE,90,@now), TIMESTAMPADD(MINUTE,240,@now), TIMESTAMPADD(MINUTE,120,@now), NULL, 'Giữ khoang cho xe đã duyệt báo giá.', @u_huong, TIMESTAMPADD(MINUTE,-30,@now)),
  (3, 1, NULL, 5, 'REPAIR', 'IN_USE', TIMESTAMPADD(MINUTE,-60,@now), TIMESTAMPADD(MINUTE,150,@now), NULL, NULL, 'Xe đang trong khoang.', @u_huong, TIMESTAMPADD(MINUTE,-180,@now)),
  (4, 6, NULL, 6, 'REPAIR', 'IN_USE', TIMESTAMPADD(MINUTE,-2740,@now), TIMESTAMPADD(MINUTE,80,@now), NULL, NULL, 'Xe đang trong khoang.', @u_huong, TIMESTAMPADD(MINUTE,-2740,@now)),
  (5, 3, NULL, 7, 'REPAIR', 'IN_USE', TIMESTAMPADD(MINUTE,-2940,@now), TIMESTAMPADD(MINUTE,80,@now), NULL, NULL, 'Xe đang trong khoang.', @u_huong, TIMESTAMPADD(MINUTE,-2940,@now)),
  (6, 2, NULL, 8, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-2140,@now), TIMESTAMPADD(MINUTE,-1885,@now), NULL, TIMESTAMPADD(MINUTE,-1885,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-2140,@now)),
  (7, 1, NULL, 9, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-5700,@now), TIMESTAMPADD(MINUTE,-5370,@now), NULL, TIMESTAMPADD(MINUTE,-5370,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-5700,@now)),
  (8, 2, NULL, 10, 'REPAIR', 'IN_USE', TIMESTAMPADD(MINUTE,-60,@now), TIMESTAMPADD(MINUTE,360,@now), NULL, NULL, 'Xe đang trong khoang.', @u_huong, TIMESTAMPADD(MINUTE,-340,@now)),
  (9, 4, NULL, 11, 'REPAIR', 'IN_USE', TIMESTAMPADD(MINUTE,-60,@now), TIMESTAMPADD(MINUTE,225,@now), NULL, NULL, 'Xe đang trong khoang.', @u_huong, TIMESTAMPADD(MINUTE,-440,@now)),
  (10, 6, NULL, 12, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-35940,@now), TIMESTAMPADD(MINUTE,-35610,@now), NULL, TIMESTAMPADD(MINUTE,-35610,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-35940,@now)),
  (11, 1, NULL, 13, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-28740,@now), TIMESTAMPADD(MINUTE,-28560,@now), NULL, TIMESTAMPADD(MINUTE,-28560,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-28740,@now)),
  (12, 1, NULL, 14, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-69060,@now), TIMESTAMPADD(MINUTE,-68970,@now), NULL, TIMESTAMPADD(MINUTE,-68970,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-69060,@now)),
  (13, 4, NULL, 15, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-20100,@now), TIMESTAMPADD(MINUTE,-19905,@now), NULL, TIMESTAMPADD(MINUTE,-19905,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-20100,@now)),
  (14, 5, NULL, 16, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-10020,@now), TIMESTAMPADD(MINUTE,-9855,@now), NULL, TIMESTAMPADD(MINUTE,-9855,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-10020,@now)),
  (15, 5, NULL, 17, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-7140,@now), TIMESTAMPADD(MINUTE,-6900,@now), NULL, TIMESTAMPADD(MINUTE,-6900,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-7140,@now)),
  (16, 1, NULL, 18, 'REPAIR', 'RELEASED', TIMESTAMPADD(MINUTE,-17220,@now), TIMESTAMPADD(MINUTE,-17040,@now), NULL, TIMESTAMPADD(MINUTE,-17040,@now), 'Đã trả khoang sau nghiệm thu.', @u_huong, TIMESTAMPADD(MINUTE,-17220,@now)),
  (17, 3, 2, NULL, 'BOOKING', 'HELD', TIMESTAMPADD(MINUTE,180,@now), TIMESTAMPADD(MINUTE,240,@now), TIMESTAMPADD(MINUTE,210,@now), NULL, 'Giữ khoang cho lịch hẹn đã xác nhận.', @u_huong, TIMESTAMPADD(MINUTE,-120,@now)),
  (18, 5, 3, NULL, 'BOOKING', 'HELD', TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'02:00:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'03:30:00'), TIMESTAMP(DATE_ADD(UTC_DATE(),INTERVAL 1 DAY),'02:30:00'), NULL, 'Giữ khoang cho lịch hẹn đã xác nhận.', @u_huong, TIMESTAMPADD(MINUTE,-90,@now));

INSERT INTO inventory_documents (id, document_code, document_type, status, supplier_id, repair_order_id, source_document_id, supplier_invoice_number, supplier_invoice_date, due_date, subtotal, tax_amount, total_amount, reason, created_by, posted_by, posted_at, created_at) VALUES
  (1, 'NK-20260829-001', 'IN', 'POSTED', 1, NULL, NULL, 'VAT-001289', DATE(TIMESTAMPADD(MINUTE,-92160,@now)), DATE(TIMESTAMPADD(MINUTE,-48960,@now)), 15775000.00, 1577500.00, 17352500.00, 'Nhập hàng theo hóa đơn nhà cung cấp.', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-92160,@now), TIMESTAMPADD(MINUTE,-92190,@now)),
  (2, 'NK-20260830-002', 'IN', 'POSTED', 2, NULL, NULL, 'HD-893021', DATE(TIMESTAMPADD(MINUTE,-90720,@now)), DATE(TIMESTAMPADD(MINUTE,-69120,@now)), 24950000.00, 2495000.00, 27445000.00, 'Nhập hàng theo hóa đơn nhà cung cấp.', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-90720,@now), TIMESTAMPADD(MINUTE,-90750,@now)),
  (3, 'NK-20260831-003', 'IN', 'POSTED', 3, NULL, NULL, 'HD-110552', DATE(TIMESTAMPADD(MINUTE,-89280,@now)), DATE(TIMESTAMPADD(MINUTE,-46080,@now)), 16450000.00, 1645000.00, 18095000.00, 'Nhập hàng theo hóa đơn nhà cung cấp.', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-89280,@now), TIMESTAMPADD(MINUTE,-89310,@now)),
  (4, 'NK-20260901-004', 'IN', 'POSTED', 4, NULL, NULL, 'VAT-007731', DATE(TIMESTAMPADD(MINUTE,-87840,@now)), DATE(TIMESTAMPADD(MINUTE,-44640,@now)), 6680000.00, 668000.00, 7348000.00, 'Nhập hàng theo hóa đơn nhà cung cấp.', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-87840,@now), TIMESTAMPADD(MINUTE,-87870,@now)),
  (5, 'NK-20260902-005', 'IN', 'POSTED', 5, NULL, NULL, 'AT-2026-0341', DATE(TIMESTAMPADD(MINUTE,-86400,@now)), DATE(TIMESTAMPADD(MINUTE,-57600,@now)), 21730000.00, 2173000.00, 23903000.00, 'Nhập hàng theo hóa đơn nhà cung cấp.', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-86400,@now), TIMESTAMPADD(MINUTE,-86430,@now)),
  (6, 'NK-20261101-006', 'IN', 'DRAFT', 5, NULL, NULL, 'AT-2026-0412', DATE(TIMESTAMPADD(MINUTE,-120,@now)), DATE(TIMESTAMPADD(MINUTE,28680,@now)), 15680000.00, 1568000.00, 17248000.00, 'Bản nháp nhập bổ sung má phanh, đang kiểm đếm.', @u_tuan, NULL, NULL, TIMESTAMPADD(MINUTE,-120,@now)),
  (7, 'XK-20261101-002', 'OUT', 'POSTED', NULL, 5, NULL, NULL, NULL, NULL, 955000.00, 0, 955000.00, 'Xuất kho cho lệnh sửa chữa RO-20261101-002', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-140,@now), TIMESTAMPADD(MINUTE,-145,@now)),
  (8, 'XK-20261030-002', 'OUT', 'POSTED', NULL, 6, NULL, NULL, NULL, NULL, 320000.00, 0, 320000.00, 'Xuất kho cho lệnh sửa chữa RO-20261030-002', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-2700,@now), TIMESTAMPADD(MINUTE,-2705,@now)),
  (9, 'XK-20261030-003', 'OUT', 'POSTED', NULL, 7, NULL, NULL, NULL, NULL, 9400000.00, 0, 9400000.00, 'Xuất kho cho lệnh sửa chữa RO-20261030-003', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-2900,@now), TIMESTAMPADD(MINUTE,-2905,@now)),
  (10, 'XK-20261030-004', 'OUT', 'POSTED', NULL, 8, NULL, NULL, NULL, NULL, 3195000.00, 0, 3195000.00, 'Xuất kho cho lệnh sửa chữa RO-20261030-004', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-2100,@now), TIMESTAMPADD(MINUTE,-2105,@now)),
  (11, 'XK-20261028-001', 'OUT', 'POSTED', NULL, 9, NULL, NULL, NULL, NULL, 2175000.00, 0, 2175000.00, 'Xuất kho cho lệnh sửa chữa RO-20261028-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-5660,@now), TIMESTAMPADD(MINUTE,-5665,@now)),
  (12, 'XK-20261031-004', 'OUT', 'POSTED', NULL, 11, NULL, NULL, NULL, NULL, 865000.00, 0, 865000.00, 'Xuất kho cho lệnh sửa chữa RO-20261031-004', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-400,@now), TIMESTAMPADD(MINUTE,-405,@now)),
  (13, 'XK-20261007-001', 'OUT', 'POSTED', NULL, 12, NULL, NULL, NULL, NULL, 320000.00, 0, 320000.00, 'Xuất kho cho lệnh sửa chữa RO-20261007-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-35900,@now), TIMESTAMPADD(MINUTE,-35905,@now)),
  (14, 'XK-20261012-001', 'OUT', 'POSTED', NULL, 13, NULL, NULL, NULL, NULL, 1855000.00, 0, 1855000.00, 'Xuất kho cho lệnh sửa chữa RO-20261012-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-28700,@now), TIMESTAMPADD(MINUTE,-28705,@now)),
  (15, 'XK-20260914-001', 'OUT', 'POSTED', NULL, 14, NULL, NULL, NULL, NULL, 845000.00, 0, 845000.00, 'Xuất kho cho lệnh sửa chữa RO-20260914-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-69020,@now), TIMESTAMPADD(MINUTE,-69025,@now)),
  (16, 'XK-20261018-001', 'OUT', 'POSTED', NULL, 15, NULL, NULL, NULL, NULL, 1450000.00, 0, 1450000.00, 'Xuất kho cho lệnh sửa chữa RO-20261018-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-20060,@now), TIMESTAMPADD(MINUTE,-20065,@now)),
  (17, 'XK-20261025-001', 'OUT', 'POSTED', NULL, 16, NULL, NULL, NULL, NULL, 930000.00, 0, 930000.00, 'Xuất kho cho lệnh sửa chữa RO-20261025-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-9980,@now), TIMESTAMPADD(MINUTE,-9985,@now)),
  (18, 'XK-20261027-001', 'OUT', 'POSTED', NULL, 17, NULL, NULL, NULL, NULL, 850000.00, 0, 850000.00, 'Xuất kho cho lệnh sửa chữa RO-20261027-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-7100,@now), TIMESTAMPADD(MINUTE,-7105,@now)),
  (19, 'XK-20261020-001', 'OUT', 'POSTED', NULL, 18, NULL, NULL, NULL, NULL, 1075000.00, 0, 1075000.00, 'Xuất kho cho lệnh sửa chữa RO-20261020-001', @u_tuan, @u_tuan, TIMESTAMPADD(MINUTE,-17180,@now), TIMESTAMPADD(MINUTE,-17185,@now));

INSERT INTO inventory_document_items (id, document_id, line_no, part_id, estimate_item_id, repair_order_part_id, source_item_id, quantity, unit_cost, line_total, notes) VALUES
  (1, 1, 1, 2, NULL, NULL, NULL, 37, 65000, 2405000.00, NULL),
  (2, 1, 2, 4, NULL, NULL, NULL, 29, 110000, 3190000.00, NULL),
  (3, 1, 3, 7, NULL, NULL, NULL, 28, 185000, 5180000.00, NULL),
  (4, 1, 4, 14, NULL, NULL, NULL, 4, 1250000, 5000000.00, NULL),
  (5, 2, 1, 1, NULL, NULL, NULL, 22, 780000, 17160000.00, NULL),
  (6, 2, 2, 8, NULL, NULL, NULL, 14, 190000, 2660000.00, NULL),
  (7, 2, 3, 9, NULL, NULL, NULL, 20, 85000, 1700000.00, NULL),
  (8, 2, 4, 12, NULL, NULL, NULL, 14, 245000, 3430000.00, NULL),
  (9, 3, 1, 6, NULL, NULL, NULL, 7, 2350000, 16450000.00, NULL),
  (10, 4, 1, 3, NULL, NULL, NULL, 24, 120000, 2880000.00, NULL),
  (11, 4, 2, 11, NULL, NULL, NULL, 4, 320000, 1280000.00, NULL),
  (12, 4, 3, 13, NULL, NULL, NULL, 12, 210000, 2520000.00, NULL),
  (13, 5, 1, 5, NULL, NULL, NULL, 9, 1450000, 13050000.00, NULL),
  (14, 5, 2, 10, NULL, NULL, NULL, 14, 230000, 3220000.00, NULL),
  (15, 5, 3, 15, NULL, NULL, NULL, 7, 780000, 5460000.00, NULL),
  (16, 6, 1, 5, NULL, NULL, NULL, 6, 1450000, 8700000.00, 'Đang kiểm đếm thực tế.'),
  (17, 6, 2, 15, NULL, NULL, NULL, 6, 780000, 4680000.00, 'Đang kiểm đếm thực tế.'),
  (18, 6, 3, 10, NULL, NULL, NULL, 10, 230000, 2300000.00, 'Đang kiểm đếm thực tế.'),
  (19, 7, 1, 1, 9, 1, NULL, 1, 780000, 780000.00, NULL),
  (20, 7, 2, 2, 10, 2, NULL, 1, 65000, 65000.00, NULL),
  (21, 7, 3, 4, 11, 3, NULL, 1, 110000, 110000.00, NULL),
  (22, 8, 1, 13, 14, 4, NULL, 1, 210000, 210000.00, NULL),
  (23, 8, 2, 4, 15, 5, NULL, 1, 110000, 110000.00, NULL),
  (24, 9, 1, 6, 18, 6, NULL, 4, 2350000, 9400000.00, NULL),
  (25, 10, 1, 1, 21, 7, NULL, 2, 780000, 1560000.00, NULL),
  (26, 10, 2, 2, 22, 8, NULL, 1, 65000, 65000.00, NULL),
  (27, 10, 3, 3, 23, 9, NULL, 1, 120000, 120000.00, NULL),
  (28, 10, 4, 5, 24, 10, NULL, 1, 1450000, 1450000.00, NULL),
  (29, 11, 1, 1, 26, 11, NULL, 2, 780000, 1560000.00, NULL),
  (30, 11, 2, 2, 27, 12, NULL, 1, 65000, 65000.00, NULL),
  (31, 11, 3, 3, 28, 13, NULL, 1, 120000, 120000.00, NULL),
  (32, 11, 4, 4, 29, 14, NULL, 1, 110000, 110000.00, NULL),
  (33, 11, 5, 11, 30, 15, NULL, 1, 320000, 320000.00, NULL),
  (34, 12, 1, 15, 37, 16, NULL, 1, 780000, 780000.00, NULL),
  (35, 12, 2, 9, 38, 17, NULL, 1, 85000, 85000.00, NULL),
  (36, 13, 1, 4, 41, 18, NULL, 1, 110000, 110000.00, NULL),
  (37, 13, 2, 13, 42, 19, NULL, 1, 210000, 210000.00, NULL),
  (38, 14, 1, 1, 44, 20, NULL, 2, 780000, 1560000.00, NULL),
  (39, 14, 2, 2, 45, 21, NULL, 1, 65000, 65000.00, NULL),
  (40, 14, 3, 3, 46, 22, NULL, 1, 120000, 120000.00, NULL),
  (41, 14, 4, 4, 47, 23, NULL, 1, 110000, 110000.00, NULL),
  (42, 15, 1, 1, 49, 24, NULL, 1, 780000, 780000.00, NULL),
  (43, 15, 2, 2, 50, 25, NULL, 1, 65000, 65000.00, NULL),
  (44, 16, 1, 5, 53, 26, NULL, 1, 1450000, 1450000.00, NULL),
  (45, 17, 1, 1, 56, 27, NULL, 1, 780000, 780000.00, NULL),
  (46, 17, 2, 2, 57, 28, NULL, 1, 65000, 65000.00, NULL),
  (47, 17, 3, 9, 58, 29, NULL, 1, 85000, 85000.00, NULL),
  (48, 18, 1, 7, 61, 30, NULL, 4, 185000, 740000.00, NULL),
  (49, 18, 2, 4, 62, 31, NULL, 1, 110000, 110000.00, NULL),
  (50, 19, 1, 1, 64, 32, NULL, 1, 780000, 780000.00, NULL),
  (51, 19, 2, 2, 65, 33, NULL, 1, 65000, 65000.00, NULL),
  (52, 19, 3, 3, 66, 34, NULL, 1, 120000, 120000.00, NULL),
  (53, 19, 4, 4, 67, 35, NULL, 1, 110000, 110000.00, NULL);

INSERT INTO inventory_transactions (id, document_item_id, part_id, transaction_type, quantity_change, stock_after, unit_cost, created_by, created_at) VALUES
  (1, 1, 2, 'IN', 37, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-92160,@now)),
  (2, 2, 4, 'IN', 29, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-92160,@now)),
  (3, 3, 7, 'IN', 28, 0, 185000, @u_tuan, TIMESTAMPADD(MINUTE,-92160,@now)),
  (4, 4, 14, 'IN', 4, 0, 1250000, @u_tuan, TIMESTAMPADD(MINUTE,-92160,@now)),
  (5, 5, 1, 'IN', 22, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-90720,@now)),
  (6, 6, 8, 'IN', 14, 0, 190000, @u_tuan, TIMESTAMPADD(MINUTE,-90720,@now)),
  (7, 7, 9, 'IN', 20, 0, 85000, @u_tuan, TIMESTAMPADD(MINUTE,-90720,@now)),
  (8, 8, 12, 'IN', 14, 0, 245000, @u_tuan, TIMESTAMPADD(MINUTE,-90720,@now)),
  (9, 9, 6, 'IN', 7, 0, 2350000, @u_tuan, TIMESTAMPADD(MINUTE,-89280,@now)),
  (10, 10, 3, 'IN', 24, 0, 120000, @u_tuan, TIMESTAMPADD(MINUTE,-87840,@now)),
  (11, 11, 11, 'IN', 4, 0, 320000, @u_tuan, TIMESTAMPADD(MINUTE,-87840,@now)),
  (12, 12, 13, 'IN', 12, 0, 210000, @u_tuan, TIMESTAMPADD(MINUTE,-87840,@now)),
  (13, 13, 5, 'IN', 9, 0, 1450000, @u_tuan, TIMESTAMPADD(MINUTE,-86400,@now)),
  (14, 14, 10, 'IN', 14, 0, 230000, @u_tuan, TIMESTAMPADD(MINUTE,-86400,@now)),
  (15, 15, 15, 'IN', 7, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-86400,@now)),
  (16, 19, 1, 'OUT', -1, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-140,@now)),
  (17, 20, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-140,@now)),
  (18, 21, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-140,@now)),
  (19, 22, 13, 'OUT', -1, 0, 210000, @u_tuan, TIMESTAMPADD(MINUTE,-2700,@now)),
  (20, 23, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-2700,@now)),
  (21, 24, 6, 'OUT', -4, 0, 2350000, @u_tuan, TIMESTAMPADD(MINUTE,-2900,@now)),
  (22, 25, 1, 'OUT', -2, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-2100,@now)),
  (23, 26, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-2100,@now)),
  (24, 27, 3, 'OUT', -1, 0, 120000, @u_tuan, TIMESTAMPADD(MINUTE,-2100,@now)),
  (25, 28, 5, 'OUT', -1, 0, 1450000, @u_tuan, TIMESTAMPADD(MINUTE,-2100,@now)),
  (26, 29, 1, 'OUT', -2, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-5660,@now)),
  (27, 30, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-5660,@now)),
  (28, 31, 3, 'OUT', -1, 0, 120000, @u_tuan, TIMESTAMPADD(MINUTE,-5660,@now)),
  (29, 32, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-5660,@now)),
  (30, 33, 11, 'OUT', -1, 0, 320000, @u_tuan, TIMESTAMPADD(MINUTE,-5660,@now)),
  (31, 34, 15, 'OUT', -1, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-400,@now)),
  (32, 35, 9, 'OUT', -1, 0, 85000, @u_tuan, TIMESTAMPADD(MINUTE,-400,@now)),
  (33, 36, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-35900,@now)),
  (34, 37, 13, 'OUT', -1, 0, 210000, @u_tuan, TIMESTAMPADD(MINUTE,-35900,@now)),
  (35, 38, 1, 'OUT', -2, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-28700,@now)),
  (36, 39, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-28700,@now)),
  (37, 40, 3, 'OUT', -1, 0, 120000, @u_tuan, TIMESTAMPADD(MINUTE,-28700,@now)),
  (38, 41, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-28700,@now)),
  (39, 42, 1, 'OUT', -1, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-69020,@now)),
  (40, 43, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-69020,@now)),
  (41, 44, 5, 'OUT', -1, 0, 1450000, @u_tuan, TIMESTAMPADD(MINUTE,-20060,@now)),
  (42, 45, 1, 'OUT', -1, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-9980,@now)),
  (43, 46, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-9980,@now)),
  (44, 47, 9, 'OUT', -1, 0, 85000, @u_tuan, TIMESTAMPADD(MINUTE,-9980,@now)),
  (45, 48, 7, 'OUT', -4, 0, 185000, @u_tuan, TIMESTAMPADD(MINUTE,-7100,@now)),
  (46, 49, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-7100,@now)),
  (47, 50, 1, 'OUT', -1, 0, 780000, @u_tuan, TIMESTAMPADD(MINUTE,-17180,@now)),
  (48, 51, 2, 'OUT', -1, 0, 65000, @u_tuan, TIMESTAMPADD(MINUTE,-17180,@now)),
  (49, 52, 3, 'OUT', -1, 0, 120000, @u_tuan, TIMESTAMPADD(MINUTE,-17180,@now)),
  (50, 53, 4, 'OUT', -1, 0, 110000, @u_tuan, TIMESTAMPADD(MINUTE,-17180,@now));

-- Sổ kho: tính tồn sau từng giao dịch và đồng bộ tồn kho từ chính sổ kho
UPDATE inventory_transactions t JOIN (SELECT id, SUM(quantity_change) OVER (PARTITION BY part_id ORDER BY created_at, id) AS running FROM inventory_transactions) r ON r.id = t.id SET t.stock_after = r.running;
UPDATE parts p SET p.stock_quantity = COALESCE((SELECT SUM(t.quantity_change) FROM inventory_transactions t WHERE t.part_id = p.id), 0);
UPDATE parts SET reserved_quantity = 1 WHERE id = 5;
UPDATE parts SET reserved_quantity = 1 WHERE id = 8;
UPDATE parts SET reserved_quantity = 1 WHERE id = 9;
UPDATE parts SET reserved_quantity = 1 WHERE id = 11;
UPDATE parts SET reserved_quantity = 2 WHERE id = 12;

INSERT INTO invoices (id, invoice_code, repair_order_id, customer_id, status, customer_snapshot, vehicle_snapshot, subtotal, discount_amount, tax_amount, total_amount, issued_by, issued_at, due_date) VALUES
  (1, 'HD-20261028-001', 9, 3, 'UNPAID', '{"customerCode": "KH-0003", "fullName": "Công ty TNHH Vận tải & Dịch vụ Hoàng Long", "phone": "0243888903", "email": "ketoan@hoanglongtransport.vn", "address": "Số 8 Phạm Hùng, Nam Từ Liêm, Hà Nội", "taxCode": "0108123456"}', '{"plateNumber": "30G-774.19", "vin": "MNCLSFF40P0100004", "make": "Ford", "model": "Ranger Wildtrak 2.0L Bi-Turbo", "year": 2023, "color": "Cam Sunset", "odometer": 32908}', 6080000.00, 0, 608000.00, 6688000.00, @u_huong, TIMESTAMPADD(MINUTE,-5310,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-5310,@now)),INTERVAL 30 DAY)),
  (2, 'HD-20261007-001', 12, 6, 'PAID', '{"customerCode": "KH-0006", "fullName": "Nguyễn Quốc Doanh", "phone": "0912345606", "email": "doanhfilwl@gmail.com", "address": "30 Tôn Thất Thuyết, Cầu Giấy, Hà Nội", "taxCode": ""}', '{"plateNumber": "30L-512.77", "vin": "RLVVF8PLS0P100006", "make": "VinFast", "model": "VF8 Plus", "year": 2023, "color": "Xanh Dương Jet", "odometer": 14106}', 3580000.00, 0, 358000.00, 3938000.00, @u_huong, TIMESTAMPADD(MINUTE,-35550,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-35550,@now)),INTERVAL 0 DAY)),
  (3, 'HD-20261012-001', 13, 7, 'PAID', '{"customerCode": "KH-0007", "fullName": "Công ty Cổ phần Thương mại Minh Phát", "phone": "0283999807", "email": "mua-hang@minhphat.vn", "address": "88 Điện Biên Phủ, Bình Thạnh, TP. Hồ Chí Minh", "taxCode": "0312456789"}', '{"plateNumber": "51H-245.36", "vin": "KNAMB81ANN0100008", "make": "Kia", "model": "Carnival 2.2D Signature", "year": 2022, "color": "Trắng Snow", "odometer": 28309}', 3860000.00, 0, 386000.00, 4246000.00, @u_huong, TIMESTAMPADD(MINUTE,-28500,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-28500,@now)),INTERVAL 30 DAY)),
  (4, 'HD-20260914-001', 14, 1, 'PAID', '{"customerCode": "KH-0001", "fullName": "Nguyễn Văn An", "phone": "0912345601", "email": "an.nguyen@gmail.com", "address": "25 Phố Huế, Hai Bà Trưng, Hà Nội", "taxCode": ""}', '{"plateNumber": "30H-889.23", "vin": "MHFBA3EF9N0100001", "make": "Toyota", "model": "Vios 1.5G", "year": 2022, "color": "Trắng ngọc trai", "odometer": 33104}', 1525000.00, 0, 152500.00, 1677500.00, @u_huong, TIMESTAMPADD(MINUTE,-68910,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-68910,@now)),INTERVAL 0 DAY)),
  (5, 'HD-20261018-001', 15, 5, 'PAID', '{"customerCode": "KH-0005", "fullName": "Phạm Quốc Doanh", "phone": "0983456705", "email": "doanh.pham@gmail.com", "address": "14 Lê Văn Lương, Thanh Xuân, Hà Nội", "taxCode": ""}', '{"plateNumber": "30E-433.52", "vin": "MHFAB3EF9P0100010", "make": "Toyota", "model": "Corolla Cross 1.8V", "year": 2023, "color": "Xám Metal", "odometer": 19806}', 3100000.00, 0, 310000.00, 3410000.00, @u_huong, TIMESTAMPADD(MINUTE,-19845,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-19845,@now)),INTERVAL 7 DAY)),
  (6, 'HD-20261025-001', 16, 8, 'PARTIALLY_PAID', '{"customerCode": "KH-0008", "fullName": "Hoàng Thu Hà", "phone": "0983456708", "email": "ha.hoang@gmail.com", "address": "9 Nguyễn Chí Thanh, Đống Đa, Hà Nội", "taxCode": ""}', '{"plateNumber": "30F-366.28", "vin": "W1KWF8EB9MR100007", "make": "Mercedes-Benz", "model": "C200 Avantgarde", "year": 2021, "color": "Bạc Iridium", "odometer": 47207}', 2355000.00, 0, 235500.00, 2590500.00, @u_huong, TIMESTAMPADD(MINUTE,-9795,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-9795,@now)),INTERVAL 15 DAY)),
  (7, 'HD-20261027-001', 17, 4, 'PARTIALLY_PAID', '{"customerCode": "KH-0004", "fullName": "Lê Hoàng Long", "phone": "0912345604", "email": "long.le@gmail.com", "address": "57 Trần Thái Tông, Cầu Giấy, Hà Nội", "taxCode": ""}', '{"plateNumber": "29A-123.45", "vin": "RLHRW2830L0100003", "make": "Honda", "model": "CR-V 1.5L Turbo", "year": 2020, "color": "Xám Modern Steel", "odometer": 60107}', 3260000.00, 0, 326000.00, 3586000.00, @u_huong, TIMESTAMPADD(MINUTE,-6840,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-6840,@now)),INTERVAL 15 DAY)),
  (8, 'HD-20261020-001', 18, 3, 'UNPAID', '{"customerCode": "KH-0003", "fullName": "Công ty TNHH Vận tải & Dịch vụ Hoàng Long", "phone": "0243888903", "email": "ketoan@hoanglongtransport.vn", "address": "Số 8 Phạm Hùng, Nam Từ Liêm, Hà Nội", "taxCode": "0108123456"}', '{"plateNumber": "29H-908.15", "vin": "MMCSRGA1JP0100009", "make": "Mitsubishi", "model": "Xpander AT", "year": 2023, "color": "Bạc Sterling", "odometer": 38207}', 2880000.00, 0, 288000.00, 3168000.00, @u_huong, TIMESTAMPADD(MINUTE,-16980,@now), DATE_ADD(DATE(TIMESTAMPADD(MINUTE,-16980,@now)),INTERVAL 30 DAY));

INSERT INTO invoice_items (id, invoice_id, line_no, item_type, repair_task_id, repair_order_part_id, item_name, unit, quantity, unit_price, unit_cost, discount_amount, tax_amount, line_total, warranty_days, warranty_km, warranty_terms) VALUES
  (1, 1, 1, 'SERVICE', 11, NULL, 'Bảo dưỡng định kỳ cấp 4 (40.000 km - đại tu định kỳ)', 'gói', 1, 3200000, 1120000, 0, 320000.00, 3520000.00, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.'),
  (2, 1, 2, 'PART', NULL, 11, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 2, 980000, 780000, 0, 196000.00, 2156000.00, 0, 0, NULL),
  (3, 1, 3, 'PART', NULL, 12, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 0, 0, NULL),
  (4, 1, 4, 'PART', NULL, 13, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 0, 0, NULL),
  (5, 1, 5, 'PART', NULL, 14, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 0, 0, NULL),
  (6, 1, 6, 'PART', NULL, 15, 'Dây curoa tổng Gates', 'Cái', 1, 470000, 320000, 0, 47000.00, 517000.00, 180, 20000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.'),
  (7, 2, 1, 'SERVICE', 15, NULL, 'Cân chỉnh thước lái, góc đặt bánh xe 3D Hunter', 'lượt', 1, 700000, 245000, 0, 70000.00, 770000.00, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.'),
  (8, 2, 2, 'SERVICE', 16, NULL, 'Đánh bóng toàn thân xe', 'lượt', 1, 2400000, 840000, 0, 240000.00, 2640000.00, 0, 0, NULL),
  (9, 2, 3, 'PART', NULL, 18, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 0, 0, NULL),
  (10, 2, 4, 'PART', NULL, 19, 'Gạt mưa Bosch Aerotwin', 'Bộ', 1, 310000, 210000, 0, 31000.00, 341000.00, 0, 0, NULL),
  (11, 3, 1, 'SERVICE', 17, NULL, 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', 'gói', 1, 1450000, 507000, 0, 145000.00, 1595000.00, 0, 0, NULL),
  (12, 3, 2, 'PART', NULL, 20, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 2, 980000, 780000, 0, 196000.00, 2156000.00, 0, 0, NULL),
  (13, 3, 3, 'PART', NULL, 21, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 0, 0, NULL),
  (14, 3, 4, 'PART', NULL, 22, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 0, 0, NULL),
  (15, 3, 5, 'PART', NULL, 23, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 0, 0, NULL),
  (16, 4, 1, 'SERVICE', 18, NULL, 'Bảo dưỡng định kỳ cấp 1 (5.000 km)', 'gói', 1, 450000, 157000, 0, 45000.00, 495000.00, 0, 0, NULL),
  (17, 4, 2, 'PART', NULL, 24, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 0, 0, NULL),
  (18, 4, 3, 'PART', NULL, 25, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 0, 0, NULL),
  (19, 5, 1, 'SERVICE', 19, NULL, 'Công thay má phanh trước/sau', 'lượt', 1, 450000, 157000, 0, 45000.00, 495000.00, 90, 10000, 'Bảo hành tay nghề theo chính sách xưởng.'),
  (20, 5, 2, 'SERVICE', 20, NULL, 'Láng đĩa phanh chuyên dụng', 'lượt', 1, 600000, 210000, 0, 60000.00, 660000.00, 90, 5000, 'Bảo hành tay nghề theo chính sách xưởng.'),
  (21, 5, 3, 'PART', NULL, 26, 'Má phanh gốm Brembo Ceramic (trước)', 'Bộ', 1, 2050000, 1450000, 0, 205000.00, 2255000.00, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.'),
  (22, 6, 1, 'SERVICE', 21, NULL, 'Bảo dưỡng định kỳ cấp 2 (10.000 km)', 'gói', 1, 850000, 297000, 0, 85000.00, 935000.00, 0, 0, NULL),
  (23, 6, 2, 'SERVICE', 22, NULL, 'Chẩn đoán lỗi OBD-II chuyên sâu', 'lượt', 1, 300000, 105000, 0, 30000.00, 330000.00, 0, 0, NULL),
  (24, 6, 3, 'PART', NULL, 27, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 0, 0, NULL),
  (25, 6, 4, 'PART', NULL, 28, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 0, 0, NULL),
  (26, 6, 5, 'PART', NULL, 29, 'Dầu phanh Castrol DOT 4 (Chai 500ml)', 'Chai', 1, 130000, 85000, 0, 13000.00, 143000.00, 0, 0, NULL),
  (27, 7, 1, 'SERVICE', 23, NULL, 'Vệ sinh kim phun buồng đốt siêu âm', 'lượt', 1, 1100000, 385000, 0, 110000.00, 1210000.00, 0, 0, NULL),
  (28, 7, 2, 'SERVICE', 24, NULL, 'Nạp gas & khử khuẩn dàn lạnh nội soi', 'lượt', 1, 950000, 332000, 0, 95000.00, 1045000.00, 30, 0, 'Bảo hành tay nghề theo chính sách xưởng.'),
  (29, 7, 3, 'PART', NULL, 30, 'Bugi Denso Iridium Tough', 'Chiếc', 4, 260000, 185000, 0, 104000.00, 1144000.00, 0, 0, NULL),
  (30, 7, 4, 'PART', NULL, 31, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 0, 0, NULL),
  (31, 8, 1, 'SERVICE', 25, NULL, 'Bảo dưỡng định kỳ cấp 3 (20.000 km)', 'gói', 1, 1450000, 507000, 0, 145000.00, 1595000.00, 0, 0, NULL),
  (32, 8, 2, 'PART', NULL, 32, 'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)', 'Can 4L', 1, 980000, 780000, 0, 98000.00, 1078000.00, 0, 0, NULL),
  (33, 8, 3, 'PART', NULL, 33, 'Lọc dầu Denso chính hãng', 'Cái', 1, 95000, 65000, 0, 9500.00, 104500.00, 0, 0, NULL),
  (34, 8, 4, 'PART', NULL, 34, 'Lọc gió động cơ Bosch', 'Cái', 1, 185000, 120000, 0, 18500.00, 203500.00, 0, 0, NULL),
  (35, 8, 5, 'PART', NULL, 35, 'Lọc gió cabin than hoạt tính', 'Cái', 1, 170000, 110000, 0, 17000.00, 187000.00, 0, 0, NULL);

INSERT INTO payments (id, transaction_code, direction, invoice_id, inventory_document_id, amount, payment_method, provider_reference, paid_at, recorded_by, notes) VALUES
  (1, 'PT-20261007-0001', 'IN', 2, NULL, 3938000.00, 'VIETQR', 'VQR262800000001', TIMESTAMPADD(MINUTE,-35520,@now), @u_admin, 'Quét mã VietQR - nội dung: RO-20261007-001 30L-512.77'),
  (2, 'PT-20261012-0002', 'IN', 3, NULL, 4246000.00, 'BANK_TRANSFER', 'FT26285000002VCB', TIMESTAMPADD(MINUTE,-28380,@now), @u_admin, 'Chuyển khoản Vietcombank - nội dung: THANH TOAN RO-20261012-001'),
  (3, 'PT-20260914-0003', 'IN', 4, NULL, 1677500.00, 'VIETQR', 'VQR262570000003', TIMESTAMPADD(MINUTE,-68880,@now), @u_admin, 'Quét mã VietQR - nội dung: RO-20260914-001 30H-889.23'),
  (4, 'PT-20261018-0004', 'IN', 5, NULL, 1705000.00, 'CASH', NULL, TIMESTAMPADD(MINUTE,-19905,@now), @u_admin, 'Khách đặt cọc tiền mặt khi duyệt báo giá.'),
  (5, 'PT-20261018-0005', 'IN', 5, NULL, 1705000.00, 'CARD', 'POS262910000005', TIMESTAMPADD(MINUTE,-19805,@now), @u_admin, 'Thanh toán phần còn lại bằng thẻ khi nhận xe.'),
  (6, 'PT-20261025-0006', 'IN', 6, NULL, 1295250.00, 'BANK_TRANSFER', 'FT26298000006VCB', TIMESTAMPADD(MINUTE,-9780,@now), @u_admin, 'Đặt cọc 50% chuyển khoản Vietcombank - RO-20261025-001'),
  (7, 'PT-20261027-0007', 'IN', 7, NULL, 1793000.00, 'BANK_TRANSFER', 'FT26300000007VCB', TIMESTAMPADD(MINUTE,-6825,@now), @u_admin, 'Đặt cọc 50% chuyển khoản Vietcombank - RO-20261027-001'),
  (8, 'PC-20260902-0008', 'OUT', NULL, 2, 27445000.00, 'BANK_TRANSFER', 'UNC-MOTUL-0001', TIMESTAMPADD(MINUTE,-86280,@now), @u_admin, 'Thanh toán đủ tiền dầu nhớt Motul, ủy nhiệm chi Vietcombank.'),
  (9, 'PC-20260907-0009', 'OUT', NULL, 1, 8676250.00, 'BANK_TRANSFER', 'UNC-DENSO-0001', TIMESTAMPADD(MINUTE,-79200,@now), @u_admin, 'Thanh toán đợt 1 (50%) phụ tùng Denso.'),
  (10, 'PC-20260912-0010', 'OUT', NULL, 4, 2204400.00, 'CASH', NULL, TIMESTAMPADD(MINUTE,-72000,@now), @u_admin, 'Thanh toán trước 30% phụ tùng Bosch.');

INSERT INTO warranties (id, warranty_code, invoice_item_id, vehicle_id, customer_id, starts_at, expires_at, start_odometer, end_odometer, warranty_days, warranty_km, terms_snapshot, status, created_at) VALUES
  (1, 'BH-20261007-001-1', 7, 6, 6, TIMESTAMPADD(MINUTE,-35550,@now), TIMESTAMPADD(DAY,30,TIMESTAMPADD(MINUTE,-35550,@now)), 14106, 15106, 30, 1000, 'Bảo hành tay nghề theo chính sách xưởng.', 'ACTIVE', TIMESTAMPADD(MINUTE,-35550,@now)),
  (2, 'BH-20261018-001-1', 19, 10, 5, TIMESTAMPADD(MINUTE,-19845,@now), TIMESTAMPADD(DAY,90,TIMESTAMPADD(MINUTE,-19845,@now)), 19806, 29806, 90, 10000, 'Bảo hành tay nghề theo chính sách xưởng.', 'ACTIVE', TIMESTAMPADD(MINUTE,-19845,@now)),
  (3, 'BH-20261018-001-2', 20, 10, 5, TIMESTAMPADD(MINUTE,-19845,@now), TIMESTAMPADD(DAY,90,TIMESTAMPADD(MINUTE,-19845,@now)), 19806, 24806, 90, 5000, 'Bảo hành tay nghề theo chính sách xưởng.', 'ACTIVE', TIMESTAMPADD(MINUTE,-19845,@now)),
  (4, 'BH-20261018-001-3', 21, 10, 5, TIMESTAMPADD(MINUTE,-19845,@now), TIMESTAMPADD(DAY,180,TIMESTAMPADD(MINUTE,-19845,@now)), 19806, 29806, 180, 10000, 'Bảo hành chính hãng, đổi mới khi lỗi nhà sản xuất.', 'ACTIVE', TIMESTAMPADD(MINUTE,-19845,@now));

INSERT INTO warranty_claims (id, claim_code, warranty_id, requested_by, requested_at, current_odometer, description, status) VALUES
  (1, 'YC-20261031-001', 1, @u_cust, TIMESTAMPADD(MINUTE,-1440,@now), 14390, 'Phát ra tiếng kêu nhẹ ở bánh trước bên trái khi qua gờ giảm tốc, nhờ xưởng kiểm tra lại.', 'OPEN');

INSERT INTO notifications (id, user_id, recipient_key, destination, event_key, channel, subject, payload_json, status, sent_at, created_at) VALUES
  (1, @u_admin, CONCAT('USER:',@u_admin), (SELECT username FROM users WHERE id = @u_admin), 'LOW_STOCK:summary', 'WEB', 'Cảnh báo tồn kho thấp', '{"message": "Một số phụ tùng đã xuống dưới mức tồn an toàn, cần lập phiếu nhập bổ sung."}', 'SENT', TIMESTAMPADD(MINUTE,-30,@now), TIMESTAMPADD(MINUTE,-30,@now)),
  (2, @u_huong, CONCAT('USER:',@u_huong), (SELECT username FROM users WHERE id = @u_huong), 'BOOKING_PENDING:summary', 'WEB', 'Lịch hẹn chờ xác nhận', '{"message": "Có 2 lịch hẹn mới đang chờ tiếp nhận xác nhận."}', 'SENT', TIMESTAMPADD(MINUTE,-60,@now), TIMESTAMPADD(MINUTE,-60,@now)),
  (3, @u_tuan, CONCAT('USER:',@u_tuan), (SELECT username FROM users WHERE id = @u_tuan), 'INVENTORY_DRAFT:IN-6', 'WEB', 'Phiếu nhập kho nháp', '{"message": "Phiếu nhập bổ sung má phanh từ An Thái đang chờ kiểm đếm."}', 'SENT', TIMESTAMPADD(MINUTE,-90,@now), TIMESTAMPADD(MINUTE,-90,@now)),
  (4, @u_minh, CONCAT('USER:',@u_minh), (SELECT username FROM users WHERE id = @u_minh), 'QC_FAILED:20261030-003', 'WEB', 'Xe chờ làm lại', '{"message": "Lệnh RO-20261030-003 chưa đạt QC, cần cân mâm lại."}', 'SENT', TIMESTAMPADD(MINUTE,-120,@now), TIMESTAMPADD(MINUTE,-120,@now)),
  (5, @u_cust, CONCAT('USER:',@u_cust), (SELECT username FROM users WHERE id = @u_cust), 'ORDER_WAITING_QC:20261030-002', 'WEB', 'Xe của bạn đã hoàn tất kiểm định', '{"message": "Xe VinFast VF8 của bạn đang chờ nghiệm thu cuối cùng trước khi bàn giao (RO-20261030-002)."}', 'SENT', TIMESTAMPADD(MINUTE,-150,@now), TIMESTAMPADD(MINUTE,-150,@now));

-- Hoàn tất. Kiểm tra: SELECT COUNT(*) FROM customers; ...

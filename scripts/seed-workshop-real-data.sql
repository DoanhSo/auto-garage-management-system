-- Seed Service Bays
INSERT INTO service_bays (id, bay_code, bay_name, bay_type, description, is_active)
VALUES
  (1, 'BAY-01', 'Cầu nâng 1', 'TWO_POST', 'Bảo dưỡng định kỳ & Thay dầu nhanh', 1),
  (2, 'BAY-02', 'Cầu nâng 2', 'TWO_POST', 'Đại tu gầm máy & Hộp số', 1),
  (3, 'BAY-03', 'Cầu nâng 3', 'FOUR_POST', 'Cân chỉnh góc đặt bánh xe 3D & Thước lái', 1),
  (4, 'BAY-04', 'Cầu nâng 4', 'SCISSOR', 'Thay lốp, phanh & Hệ thống treo', 1),
  (5, 'BAY-05', 'Khoang chẩn đoán', 'DIAGNOSTIC', 'Chẩn đoán điện & Quét mã lỗi động cơ', 1),
  (6, 'BAY-06', 'Khoang kiểm tra', 'INSPECTION', 'Kiểm định chất lượng (QC) & Bàn giao xe', 1)
ON DUPLICATE KEY UPDATE
  bay_name = VALUES(bay_name),
  bay_type = VALUES(bay_type),
  description = VALUES(description),
  is_active = VALUES(is_active);

-- Ensure technicians are active
UPDATE employees SET is_active = 1 WHERE id = 1;

-- Update Repair Orders with realistic diverse workflow statuses
UPDATE repair_orders SET
  status = 'IN_PROGRESS',
  customer_complaint = 'Xe có tiếng kêu lục cục ở gầm trước khi qua gờ giảm tốc, cần kiểm tra hệ thống phanh',
  diagnosis = 'Rơ rotuyn cân bằng trước, mòn má phanh đĩa trước còn 2mm. Cần thay rotuyn cân bằng và láng đĩa thay má phanh mới.'
WHERE id = 13;

UPDATE repair_orders SET
  status = 'DIAGNOSING',
  customer_complaint = 'Đèn báo Check Engine sáng liên tục, xe bị giật nhẹ khi tăng tốc ở dải tua 2.000 RPM',
  diagnosis = 'Quét mã lỗi phát hiện P0302 (Misfire xi lanh 2). Đang kiểm tra cuộn bô-bin đánh lửa và bugi.'
WHERE id = 14;

UPDATE repair_orders SET
  status = 'WAITING_QC',
  customer_complaint = 'Bảo dưỡng định kỳ cấp 40.000 km, thay dầu động cơ, lọc dầu, lọc gió và vệ sinh kim phun buồng đốt',
  diagnosis = 'Đã hoàn tất bảo dưỡng cấp 40.000 km. Chuyển khoang kiểm tra xuất xưởng để kiểm định QC.'
WHERE id = 15;

UPDATE repair_orders SET
  status = 'QC_FAILED',
  customer_complaint = 'Thay 2 lốp trước và cân mâm bấm chì, kiểm tra độ chụm bánh xe',
  diagnosis = 'Chạy thử kiểm tra phát hiện vô lăng còn rung nhẹ ở tốc độ 80 km/h. Cần cân mâm lại bánh trước phụ.'
WHERE id = 16;

UPDATE repair_orders SET
  status = 'APPROVED',
  customer_complaint = 'Rò rỉ dầu đáy các-te động cơ và hao nước làm mát',
  diagnosis = 'Lão hóa phớt đuôi trục cơ và nứt cổ cút nước phụ. Khách hàng đã duyệt báo giá thay thế linh kiện.'
WHERE id = 17;

UPDATE repair_orders SET
  status = 'NEW',
  customer_complaint = 'Bảo dưỡng định kỳ cấp 10.000 km, kiểm tra gầm và bổ sung nước làm mát',
  diagnosis = NULL
WHERE id = 18;

UPDATE repair_orders SET
  status = 'COMPLETED',
  customer_complaint = 'Thay dầu phanh, dầu trợ lực lái và bảo dưỡng 4 cụm phanh',
  diagnosis = 'Hệ thống phanh và trợ lực lái hoạt động chuẩn sau kiểm định.'
WHERE id = 19;

UPDATE repair_orders SET
  status = 'CLOSED',
  customer_complaint = 'Sơn dặm cản trước và đánh bóng toàn thân xe',
  diagnosis = 'Đã hoàn tất sơn dặm và bàn giao xe cho khách hàng.'
WHERE id = 20;

-- Update Task Names to realistic content
UPDATE repair_tasks SET
  task_name = 'Bảo dưỡng và thay dầu động cơ',
  status = 'COMPLETED'
WHERE id = 13;

INSERT INTO repair_tasks (repair_order_id, task_type, task_name, status, quantity, unit_price)
VALUES (13, 'DIAGNOSIS', 'Kiểm tra rotuyn cân bằng và hệ thống phanh trước', 'IN_PROGRESS', 1, 250000)
ON DUPLICATE KEY UPDATE task_name = VALUES(task_name);

UPDATE repair_tasks SET
  task_name = 'Quét mã lỗi chẩn đoán động cơ OBD-II',
  status = 'IN_PROGRESS'
WHERE id = 14;

UPDATE repair_tasks SET
  task_name = 'Bảo dưỡng cấp 40.000 km toàn diện',
  status = 'COMPLETED'
WHERE id = 15;

UPDATE repair_tasks SET
  task_name = 'Cân mâm bấm chì và căn chỉnh góc lái',
  status = 'IN_PROGRESS'
WHERE id = 16;

UPDATE repair_tasks SET
  task_name = 'Thay phớt đuôi trục cơ và cút nước',
  status = 'PENDING'
WHERE id = 17;

-- Seed Bay Reservations
DELETE FROM bay_reservations WHERE id > 0;

INSERT INTO bay_reservations (id, bay_id, repair_order_id, reservation_type, status, starts_at, ends_at, notes, created_by)
VALUES
  (1, 1, 13, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 'Đang sửa chữa gầm và phanh trước', 1),
  (2, 5, 14, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 45 MINUTE), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'Chẩn đoán hiện tượng bỏ máy xi lanh 2', 1),
  (3, 6, 15, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_ADD(NOW(), INTERVAL 40 MINUTE), 'Nghiệm thu kiểm định QC xuất xưởng', 1),
  (4, 4, 16, 'REPAIR', 'IN_USE', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'Cân mâm khắc phục rung vô lăng', 1),
  (5, 2, 17, 'REPAIR', 'HELD', DATE_SUB(NOW(), INTERVAL 10 MINUTE), DATE_ADD(NOW(), INTERVAL 3 HOUR), 'Đã giữ cầu chuẩn bị tháo hạ gầm', 1);

-- Seed quality checks in vehicle_checks
DELETE FROM vehicle_checks WHERE check_type = 'QC';
INSERT INTO vehicle_checks (repair_order_id, check_type, attempt_no, result, checklist_json, odometer, notes, checked_by)
VALUES
  (15, 'QC', 1, 'PASSED', '{"phanh":"OK","den":"OK","dong_co":"OK","chay_thu":"OK"}', 40050, 'Xe vận hành êm ái, các thông số đạt tiêu chuẩn xuất xưởng.', 1),
  (2, 'QC', 1, 'FAILED', '{"phanh":"OK","den":"OK","can_mam":"FAIL","do_chum":"OK"}', 32100, 'Chạy thử thấy vô lăng rung nhẹ ở tốc độ 80km/h. Cần cân mâm lại.', 1);

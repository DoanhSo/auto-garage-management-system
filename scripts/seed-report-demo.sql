-- Local reporting fixtures only. Not a Flyway migration or a workflow acceptance test.
-- Append-only, stable DEMO-RPT codes; replay preserves existing records and dates.
SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @actor = (SELECT MIN(id) FROM users WHERE role = 'ADMIN' AND is_active = TRUE AND deleted_at IS NULL);
SET @fixture_day = UTC_DATE();
CREATE TEMPORARY TABLE demo_report_rows (n INT PRIMARY KEY);
INSERT INTO demo_report_rows VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),
  (13),(14),(15),(16),(17),(18),(19),(20),(21),(22),(23),(24);
CREATE TEMPORARY TABLE demo_report_stock (n INT PRIMARY KEY, quantity INT, unit_cost DECIMAL(18,2), paid DECIMAL(18,2));
INSERT INTO demo_report_stock VALUES (1,20,150000,1000000),(2,30,100000,2000000),(3,10,250000,1000000);

START TRANSACTION;
-- A missing active administrator causes a NOT NULL failure and rolls back the fixture.
INSERT INTO users (username,full_name,role,is_active,created_by,updated_by)
SELECT 'demo.report.technician','DEMO - Kỹ thuật viên báo cáo','TECHNICIAN',FALSE,@actor,@actor
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='demo.report.technician');
SET @tech_user = (SELECT id FROM users WHERE username='demo.report.technician');
INSERT INTO employees (user_id,employee_code,hourly_cost,is_active)
SELECT @tech_user,'DEMO-RPT-TECH',100000,FALSE
WHERE NOT EXISTS (SELECT 1 FROM employees WHERE employee_code='DEMO-RPT-TECH');
SET @employee = (SELECT id FROM employees WHERE employee_code='DEMO-RPT-TECH');

INSERT INTO customers (customer_code,full_name,customer_type,email,address,credit_limit,payment_term_days)
SELECT CONCAT('DEMO-RPT-KH-',n),CONCAT('DEMO - Khách hàng báo cáo ',n),'INDIVIDUAL',
  CONCAT('report.customer',n,'@example.test'),'Dữ liệu mẫu báo cáo',50000000,30
FROM demo_report_rows r WHERE n<=3 AND NOT EXISTS
  (SELECT 1 FROM customers c WHERE c.customer_code=CONCAT('DEMO-RPT-KH-',r.n));
INSERT INTO vehicles (customer_id,license_plate,brand,model,manufacture_year,color,odometer)
SELECT c.id,CONCAT('DEMO-RPT-',r.n),ELT(r.n,'Toyota','Honda','Mazda'),
  ELT(r.n,'Vios','City','CX-5'),2023,'Trắng',r.n*15000
FROM demo_report_rows r JOIN customers c ON c.customer_code=CONCAT('DEMO-RPT-KH-',r.n)
WHERE r.n<=3 AND NOT EXISTS (SELECT 1 FROM vehicles v WHERE v.license_plate=CONCAT('DEMO-RPT-',r.n));
INSERT INTO services (service_code,service_name,service_type,unit,unit_price,standard_minutes)
SELECT 'DEMO-RPT-SERVICE','DEMO - Bảo dưỡng và kiểm tra','SINGLE','lượt',500000,90
WHERE NOT EXISTS (SELECT 1 FROM services WHERE service_code='DEMO-RPT-SERVICE');
SET @service = (SELECT id FROM services WHERE service_code='DEMO-RPT-SERVICE');

INSERT INTO repair_orders (order_code,customer_id,vehicle_id,status,odometer_in,customer_complaint,
  received_by,received_at)
SELECT CONCAT('DEMO-RPT-RO-',LPAD(r.n,2,'0')),c.id,v.id,'INVOICED',v.odometer,
  'DEMO - Hồ sơ lịch sử dùng minh họa báo cáo',@actor,
  TIMESTAMP(DATE_SUB(@fixture_day,INTERVAL MOD(r.n,6) DAY),'01:00:00')
FROM demo_report_rows r JOIN customers c ON c.customer_code=CONCAT('DEMO-RPT-KH-',MOD(r.n-1,3)+1)
JOIN vehicles v ON v.customer_id=c.id AND v.license_plate=CONCAT('DEMO-RPT-',MOD(r.n-1,3)+1)
WHERE NOT EXISTS (SELECT 1 FROM repair_orders o WHERE o.order_code=CONCAT('DEMO-RPT-RO-',LPAD(r.n,2,'0')));
INSERT INTO estimates (estimate_code,repair_order_id,version_no,status,subtotal,total_amount,notes,created_by,sent_at)
SELECT CONCAT('DEMO-RPT-EST-',LPAD(r.n,2,'0')),o.id,1,'APPROVED',500000+r.n*125000,
  500000+r.n*125000,'DEMO - Báo giá lịch sử',@actor,o.received_at
FROM demo_report_rows r JOIN repair_orders o ON o.order_code=CONCAT('DEMO-RPT-RO-',LPAD(r.n,2,'0'))
WHERE NOT EXISTS (SELECT 1 FROM estimates e WHERE e.estimate_code=CONCAT('DEMO-RPT-EST-',LPAD(r.n,2,'0')));
INSERT INTO estimate_items (estimate_id,line_no,item_type,service_id,item_name,unit,quantity,unit_price,
  line_total,approval_status,approved_by,decided_at)
SELECT e.id,1,'SERVICE',@service,'DEMO - Bảo dưỡng và kiểm tra','lượt',1,e.total_amount,
  e.total_amount,'APPROVED',@actor,e.sent_at
FROM estimates e WHERE e.estimate_code LIKE 'DEMO-RPT-EST-%' AND NOT EXISTS
  (SELECT 1 FROM estimate_items i WHERE i.estimate_id=e.id AND i.line_no=1);
INSERT INTO repair_tasks (repair_order_id,estimate_item_id,service_id,task_name,status,quantity,unit_price,
  started_at,completed_at,completion_note)
SELECT e.repair_order_id,i.id,@service,'DEMO - Bảo dưỡng và kiểm tra','COMPLETED',1,i.unit_price,
  DATE_ADD(e.sent_at,INTERVAL 30 MINUTE),DATE_ADD(e.sent_at,INTERVAL 2 HOUR),'DEMO - Công việc lịch sử'
FROM estimates e JOIN estimate_items i ON i.estimate_id=e.id AND i.line_no=1
WHERE e.estimate_code LIKE 'DEMO-RPT-EST-%' AND NOT EXISTS
  (SELECT 1 FROM repair_tasks t WHERE t.estimate_item_id=i.id);
INSERT INTO task_assignments (task_id,employee_id,assignment_role,is_active,starts_at,ends_at,assigned_by,released_at)
SELECT t.id,@employee,'MAIN',FALSE,t.started_at,t.completed_at,@actor,t.completed_at
FROM repair_tasks t JOIN repair_orders o ON o.id=t.repair_order_id
WHERE o.order_code LIKE 'DEMO-RPT-RO-%' AND NOT EXISTS
  (SELECT 1 FROM task_assignments a WHERE a.task_id=t.id AND a.employee_id=@employee);
INSERT INTO labor_sessions (assignment_id,employee_id,starts_at,ends_at,stop_reason,hourly_cost_snapshot)
SELECT a.id,@employee,a.starts_at,a.ends_at,'COMPLETE',100000
FROM task_assignments a JOIN repair_tasks t ON t.id=a.task_id JOIN repair_orders o ON o.id=t.repair_order_id
WHERE o.order_code LIKE 'DEMO-RPT-RO-%' AND a.employee_id=@employee AND NOT EXISTS
  (SELECT 1 FROM labor_sessions l WHERE l.assignment_id=a.id);

INSERT INTO invoices (invoice_code,repair_order_id,customer_id,status,customer_snapshot,vehicle_snapshot,
  subtotal,total_amount,issued_by,issued_at,due_date)
SELECT CONCAT('DEMO-RPT-HD-',LPAD(r.n,2,'0')),o.id,c.id,
  CASE WHEN MOD(r.n,8)=0 THEN 'PAID' WHEN MOD(r.n,2)=0 THEN 'PARTIALLY_PAID' ELSE 'UNPAID' END,
  JSON_OBJECT('fullName',c.full_name,'customerCode',c.customer_code,'email',c.email),
  JSON_OBJECT('licensePlate',v.license_plate,'brand',v.brand,'model',v.model),
  e.total_amount,e.total_amount,@actor,DATE_ADD(o.received_at,INTERVAL 3 HOUR),
  DATE_ADD(DATE(o.received_at),INTERVAL 30 DAY)
FROM demo_report_rows r JOIN repair_orders o ON o.order_code=CONCAT('DEMO-RPT-RO-',LPAD(r.n,2,'0'))
JOIN customers c ON c.id=o.customer_id JOIN vehicles v ON v.id=o.vehicle_id
JOIN estimates e ON e.repair_order_id=o.id AND e.version_no=1
WHERE NOT EXISTS (SELECT 1 FROM invoices i WHERE i.invoice_code=CONCAT('DEMO-RPT-HD-',LPAD(r.n,2,'0')));
INSERT INTO invoice_items (invoice_id,line_no,item_type,repair_task_id,item_name,unit,quantity,unit_price,unit_cost,line_total)
SELECT i.id,1,'SERVICE',t.id,'DEMO - Bảo dưỡng và kiểm tra','lượt',1,i.total_amount,150000,i.total_amount
FROM invoices i JOIN repair_tasks t ON t.repair_order_id=i.repair_order_id
WHERE i.invoice_code LIKE 'DEMO-RPT-HD-%' AND NOT EXISTS (SELECT 1 FROM invoice_items x WHERE x.invoice_id=i.id);
INSERT INTO payments (transaction_code,direction,invoice_id,amount,payment_method,paid_at,recorded_by,notes)
SELECT CONCAT('DEMO-RPT-THU-',LPAD(r.n,2,'0')),'IN',i.id,
  CASE WHEN MOD(r.n,8)=0 THEN i.total_amount ELSE i.total_amount/2 END,
  IF(MOD(r.n,4)=0,'BANK_TRANSFER','CASH'),i.issued_at,@actor,'DEMO - Phiếu thu mẫu'
FROM demo_report_rows r JOIN invoices i ON i.invoice_code=CONCAT('DEMO-RPT-HD-',LPAD(r.n,2,'0'))
WHERE MOD(r.n,2)=0 AND NOT EXISTS
  (SELECT 1 FROM payments p WHERE p.transaction_code=CONCAT('DEMO-RPT-THU-',LPAD(r.n,2,'0')));

INSERT INTO suppliers (supplier_code,supplier_name,email,payment_term_days)
SELECT CONCAT('DEMO-RPT-NCC-',n),CONCAT('DEMO - Nhà cung cấp ',n),CONCAT('report.supplier',n,'@example.test'),30
FROM demo_report_rows r WHERE n<=2 AND NOT EXISTS
  (SELECT 1 FROM suppliers s WHERE s.supplier_code=CONCAT('DEMO-RPT-NCC-',r.n));
INSERT INTO parts (sku,part_name,unit,preferred_supplier_id,purchase_price,selling_price,stock_quantity,reorder_level)
SELECT CONCAT('DEMO-RPT-PT-',d.n),ELT(d.n,'DEMO - Lọc dầu','DEMO - Dầu động cơ','DEMO - Má phanh'),
  'bộ',s.id,d.unit_cost,d.unit_cost*1.4,d.quantity,5
FROM demo_report_stock d JOIN suppliers s ON s.supplier_code=CONCAT('DEMO-RPT-NCC-',MOD(d.n-1,2)+1)
WHERE NOT EXISTS (SELECT 1 FROM parts p WHERE p.sku=CONCAT('DEMO-RPT-PT-',d.n));
INSERT INTO inventory_documents (document_code,document_type,status,supplier_id,supplier_invoice_number,
  supplier_invoice_date,due_date,subtotal,total_amount,reason,created_by,posted_by,posted_at)
SELECT CONCAT('DEMO-RPT-NHAP-',d.n),'IN','POSTED',s.id,CONCAT('DEMO-RPT-NCC-HD-',d.n),
  @fixture_day,DATE_ADD(@fixture_day,INTERVAL 30 DAY),d.quantity*d.unit_cost,d.quantity*d.unit_cost,
  'DEMO - Nhập phụ tùng mẫu',@actor,@actor,TIMESTAMP(@fixture_day,'02:00:00')
FROM demo_report_stock d JOIN suppliers s ON s.supplier_code=CONCAT('DEMO-RPT-NCC-',MOD(d.n-1,2)+1)
WHERE NOT EXISTS (SELECT 1 FROM inventory_documents x WHERE x.document_code=CONCAT('DEMO-RPT-NHAP-',d.n));
INSERT INTO inventory_document_items (document_id,line_no,part_id,quantity,unit_cost,line_total,notes)
SELECT x.id,1,p.id,d.quantity,d.unit_cost,d.quantity*d.unit_cost,'DEMO - Dòng nhập mẫu'
FROM demo_report_stock d JOIN inventory_documents x ON x.document_code=CONCAT('DEMO-RPT-NHAP-',d.n)
JOIN parts p ON p.sku=CONCAT('DEMO-RPT-PT-',d.n)
WHERE NOT EXISTS (SELECT 1 FROM inventory_document_items i WHERE i.document_id=x.id AND i.line_no=1);
INSERT INTO inventory_transactions (document_item_id,part_id,transaction_type,quantity_change,stock_after,unit_cost,created_by,created_at)
SELECT i.id,i.part_id,'IN',i.quantity,i.quantity,i.unit_cost,@actor,x.posted_at
FROM inventory_document_items i JOIN inventory_documents x ON x.id=i.document_id
WHERE x.document_code LIKE 'DEMO-RPT-NHAP-%' AND NOT EXISTS
  (SELECT 1 FROM inventory_transactions t WHERE t.document_item_id=i.id);
INSERT INTO payments (transaction_code,direction,inventory_document_id,amount,payment_method,paid_at,recorded_by,notes)
SELECT CONCAT('DEMO-RPT-CHI-',d.n),'OUT',x.id,d.paid,'BANK_TRANSFER',x.posted_at,@actor,'DEMO - Thanh toán nhà cung cấp'
FROM demo_report_stock d JOIN inventory_documents x ON x.document_code=CONCAT('DEMO-RPT-NHAP-',d.n)
WHERE NOT EXISTS (SELECT 1 FROM payments p WHERE p.transaction_code=CONCAT('DEMO-RPT-CHI-',d.n));
COMMIT;

SELECT COUNT(*) AS demo_invoices,SUM(total_amount) AS demo_revenue FROM invoices WHERE invoice_code LIKE 'DEMO-RPT-HD-%';
SELECT COUNT(*) AS demo_outstanding,SUM(balance_amount) AS demo_customer_debt FROM v_invoice_balances WHERE invoice_code LIKE 'DEMO-RPT-HD-%' AND balance_amount>0;

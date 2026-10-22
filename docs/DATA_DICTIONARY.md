# Từ điển cột của schema khởi tạo

Schema V1–V8, cập nhật ngày 15/10/2026: 30 bảng, 431 cột, kể cả hai cột generated. V6/V7 thêm audit metadata cho users/notifications; V8 thêm users.deleted_at; SQL và metadata MySQL là nguồn thực thi.

Các FK/CHECK/index xem trực tiếp tám file SQL trong `src/main/resources/db/migration`. `DEFAULT <none>` nghĩa là không có default ghi nhận trong metadata hoặc default NULL; đọc NULL/NOT NULL đi kèm. Generated column chỉ đọc từ JPA.


## attachments

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `repair_order_id`: `bigint`; NULL; DEFAULT `<none>`.
- `vehicle_check_id`: `bigint`; NULL; DEFAULT `<none>`.
- `warranty_claim_id`: `bigint`; NULL; DEFAULT `<none>`.
- `storage_key`: `varchar(500)`; NOT NULL; DEFAULT `<none>`.
- `original_filename`: `varchar(255)`; NOT NULL; DEFAULT `<none>`.
- `content_type`: `varchar(100)`; NOT NULL; DEFAULT `<none>`.
- `size_bytes`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `sha256`: `char(64)`; NULL; DEFAULT `<none>`.
- `category`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `uploaded_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `uploaded_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.

## audit_events

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `actor_user_id`: `bigint`; NULL; DEFAULT `<none>`.
- `action`: `varchar(100)`; NOT NULL; DEFAULT `<none>`.
- `entity_type`: `varchar(80)`; NOT NULL; DEFAULT `<none>`.
- `entity_id`: `bigint`; NULL; DEFAULT `<none>`.
- `request_key`: `varchar(100)`; NULL; DEFAULT `<none>`.
- `request_hash`: `char(64)`; NULL; DEFAULT `<none>`.
- `result_json`: `json`; NULL; DEFAULT `<none>`.
- `before_json`: `json`; NULL; DEFAULT `<none>`.
- `after_json`: `json`; NULL; DEFAULT `<none>`.
- `ip_address`: `varchar(45)`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.

## bay_reservations

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `bay_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `booking_id`: `bigint`; NULL; DEFAULT `<none>`.
- `repair_order_id`: `bigint`; NULL; DEFAULT `<none>`.
- `reservation_type`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `HELD`.
- `starts_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `ends_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `expires_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `released_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `notes`: `text`; NULL; DEFAULT `<none>`.
- `created_by`: `bigint`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## bookings

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `booking_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `customer_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `vehicle_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `service_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `appointment_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `expected_end_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `request_description`: `text`; NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `PENDING`.
- `cancellation_reason`: `text`; NULL; DEFAULT `<none>`.
- `created_by`: `bigint`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## customers

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `user_id`: `bigint`; NULL; DEFAULT `<none>`.
- `customer_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `full_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `customer_type`: `varchar(20)`; NOT NULL; DEFAULT `INDIVIDUAL`.
- `phone`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `email`: `varchar(254)`; NULL; DEFAULT `<none>`.
- `address`: `varchar(500)`; NULL; DEFAULT `<none>`.
- `company_name`: `varchar(255)`; NULL; DEFAULT `<none>`.
- `tax_code`: `varchar(30)`; NULL; DEFAULT `<none>`.
- `credit_limit`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `payment_term_days`: `int`; NOT NULL; DEFAULT `0`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## employee_shifts

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `employee_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `starts_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `ends_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `ACTIVE`.
- `notes`: `text`; NULL; DEFAULT `<none>`.
- `created_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## employees

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `user_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `employee_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `skills_json`: `json`; NULL; DEFAULT `<none>`.
- `can_qc`: `tinyint(1)`; NOT NULL; DEFAULT `0`.
- `hourly_cost`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## estimate_items

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `estimate_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `line_no`: `int`; NOT NULL; DEFAULT `<none>`.
- `item_type`: `varchar(10)`; NOT NULL; DEFAULT `<none>`.
- `service_id`: `bigint`; NULL; DEFAULT `<none>`.
- `part_id`: `bigint`; NULL; DEFAULT `<none>`.
- `item_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `unit`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `<none>`.
- `unit_price`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `unit_cost`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `discount_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `tax_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `line_total`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `approval_status`: `varchar(20)`; NOT NULL; DEFAULT `PENDING`.
- `approved_by`: `bigint`; NULL; DEFAULT `<none>`.
- `decided_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `decision_note`: `text`; NULL; DEFAULT `<none>`.
- `reserved_quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `0.000`.
- `warranty_days`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_km`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_terms`: `text`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## estimates

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `estimate_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `repair_order_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `version_no`: `int`; NOT NULL; DEFAULT `<none>`.
- `estimate_kind`: `varchar(20)`; NOT NULL; DEFAULT `INITIAL`.
- `parent_estimate_id`: `bigint`; NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `DRAFT`.
- `subtotal`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `discount_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `tax_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `total_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `notes`: `text`; NULL; DEFAULT `<none>`.
- `created_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `sent_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `valid_until`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## inventory_document_items

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `document_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `line_no`: `int`; NOT NULL; DEFAULT `<none>`.
- `part_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `estimate_item_id`: `bigint`; NULL; DEFAULT `<none>`.
- `repair_order_part_id`: `bigint`; NULL; DEFAULT `<none>`.
- `source_item_id`: `bigint`; NULL; DEFAULT `<none>`.
- `quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `<none>`.
- `unit_cost`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `line_total`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `notes`: `text`; NULL; DEFAULT `<none>`.

## inventory_documents

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `document_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `document_type`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `DRAFT`.
- `supplier_id`: `bigint`; NULL; DEFAULT `<none>`.
- `repair_order_id`: `bigint`; NULL; DEFAULT `<none>`.
- `source_document_id`: `bigint`; NULL; DEFAULT `<none>`.
- `supplier_invoice_number`: `varchar(100)`; NULL; DEFAULT `<none>`.
- `supplier_invoice_date`: `date`; NULL; DEFAULT `<none>`.
- `due_date`: `date`; NULL; DEFAULT `<none>`.
- `subtotal`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `tax_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `total_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `reason`: `text`; NULL; DEFAULT `<none>`.
- `created_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `posted_by`: `bigint`; NULL; DEFAULT `<none>`.
- `posted_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## inventory_transactions

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `document_item_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `part_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `transaction_type`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `quantity_change`: `decimal(14,3)`; NOT NULL; DEFAULT `<none>`.
- `stock_after`: `decimal(14,3)`; NOT NULL; DEFAULT `<none>`.
- `unit_cost`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `created_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.

## invoice_items

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `invoice_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `line_no`: `int`; NOT NULL; DEFAULT `<none>`.
- `item_type`: `varchar(10)`; NOT NULL; DEFAULT `<none>`.
- `repair_task_id`: `bigint`; NULL; DEFAULT `<none>`.
- `repair_order_part_id`: `bigint`; NULL; DEFAULT `<none>`.
- `item_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `unit`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `<none>`.
- `unit_price`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `unit_cost`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `discount_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `tax_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `line_total`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `warranty_days`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_km`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_terms`: `text`; NULL; DEFAULT `<none>`.

## invoices

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `invoice_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `repair_order_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `customer_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `UNPAID`.
- `customer_snapshot`: `json`; NOT NULL; DEFAULT `<none>`.
- `vehicle_snapshot`: `json`; NOT NULL; DEFAULT `<none>`.
- `subtotal`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `discount_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `tax_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `total_amount`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `issued_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `issued_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `due_date`: `date`; NULL; DEFAULT `<none>`.
- `void_reason`: `text`; NULL; DEFAULT `<none>`.
- `voided_by`: `bigint`; NULL; DEFAULT `<none>`.
- `voided_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## labor_sessions

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `assignment_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `employee_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `starts_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `ends_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `stop_reason`: `varchar(20)`; NULL; DEFAULT `<none>`.
- `hourly_cost_snapshot`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `active_employee_id`: `bigint`; NULL; DEFAULT `<none>`; STORED GENERATED; AS (case when (`ends_at` is null) then `employee_id` else NULL end).

## notifications

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `user_id`: `bigint`; NULL; DEFAULT `<none>`.
- `customer_id`: `bigint`; NULL; DEFAULT `<none>`.
- `recipient_key`: `varchar(100)`; NOT NULL; DEFAULT `<none>`.
- `destination`: `varchar(254)`; NOT NULL; DEFAULT `<none>`.
- `event_key`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `channel`: `varchar(10)`; NOT NULL; DEFAULT `<none>`.
- `subject`: `varchar(255)`; NULL; DEFAULT `<none>`.
- `payload_json`: `json`; NOT NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `PENDING`.
- `attempt_count`: `int`; NOT NULL; DEFAULT `0`.
- `next_attempt_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `locked_until`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `locked_by`: `varchar(100)`; NULL; DEFAULT `<none>`.
- `last_error`: `text`; NULL; DEFAULT `<none>`.
- `provider_reference`: `varchar(150)`; NULL; DEFAULT `<none>`.
- `sent_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `read_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT CURRENT_TIMESTAMP(6), ON UPDATE; V7.
- `created_by`: `bigint`; NULL; V7.
- `updated_by`: `bigint`; NULL; V7.
- `version`: `bigint`; NOT NULL; DEFAULT 0; V7 optimistic locking.

## parts

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `sku`: `varchar(50)`; NOT NULL; DEFAULT `<none>`.
- `part_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `unit`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `description`: `text`; NULL; DEFAULT `<none>`.
- `preferred_supplier_id`: `bigint`; NULL; DEFAULT `<none>`.
- `purchase_price`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `selling_price`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `stock_quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `0.000`.
- `reserved_quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `0.000`.
- `reorder_level`: `decimal(14,3)`; NOT NULL; DEFAULT `0.000`.
- `warranty_days`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_km`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_terms`: `text`; NULL; DEFAULT `<none>`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## payments

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `transaction_code`: `varchar(100)`; NOT NULL; DEFAULT `<none>`.
- `direction`: `varchar(3)`; NOT NULL; DEFAULT `<none>`.
- `invoice_id`: `bigint`; NULL; DEFAULT `<none>`.
- `inventory_document_id`: `bigint`; NULL; DEFAULT `<none>`.
- `amount`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `payment_method`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `provider_reference`: `varchar(150)`; NULL; DEFAULT `<none>`.
- `paid_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `recorded_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `notes`: `text`; NULL; DEFAULT `<none>`.

## repair_order_parts

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `repair_order_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `estimate_item_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `part_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `0.000`.
- `unit_price`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `unit_cost`: `decimal(18,2)`; NOT NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## repair_orders

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `order_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `booking_id`: `bigint`; NULL; DEFAULT `<none>`.
- `customer_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `vehicle_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `order_type`: `varchar(20)`; NOT NULL; DEFAULT `NORMAL`.
- `status`: `varchar(30)`; NOT NULL; DEFAULT `NEW`.
- `odometer_in`: `int`; NOT NULL; DEFAULT `<none>`.
- `odometer_out`: `int`; NULL; DEFAULT `<none>`.
- `fuel_level_percent`: `decimal(5,2)`; NULL; DEFAULT `<none>`.
- `customer_complaint`: `text`; NULL; DEFAULT `<none>`.
- `diagnosis`: `text`; NULL; DEFAULT `<none>`.
- `received_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `received_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `latest_qc_check_id`: `bigint`; NULL; DEFAULT `<none>`.
- `handed_over_by`: `bigint`; NULL; DEFAULT `<none>`.
- `handed_over_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `closed_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## repair_tasks

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `repair_order_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `estimate_item_id`: `bigint`; NULL; DEFAULT `<none>`.
- `service_id`: `bigint`; NULL; DEFAULT `<none>`.
- `task_type`: `varchar(20)`; NOT NULL; DEFAULT `REPAIR`.
- `task_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `description`: `text`; NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `PENDING`.
- `quantity`: `decimal(14,3)`; NOT NULL; DEFAULT `1.000`.
- `unit_price`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `started_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `completed_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `completion_note`: `text`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## service_bays

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `bay_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `bay_name`: `varchar(100)`; NOT NULL; DEFAULT `<none>`.
- `bay_type`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `description`: `text`; NULL; DEFAULT `<none>`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## services

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `service_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `service_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `service_type`: `varchar(20)`; NOT NULL; DEFAULT `SINGLE`.
- `description`: `text`; NULL; DEFAULT `<none>`.
- `unit`: `varchar(30)`; NOT NULL; DEFAULT `job`.
- `unit_price`: `decimal(18,2)`; NOT NULL; DEFAULT `0.00`.
- `standard_minutes`: `int`; NOT NULL; DEFAULT `<none>`.
- `required_bay_type`: `varchar(30)`; NULL; DEFAULT `<none>`.
- `required_skills_json`: `json`; NULL; DEFAULT `<none>`.
- `interval_km`: `int`; NULL; DEFAULT `<none>`.
- `interval_months`: `int`; NULL; DEFAULT `<none>`.
- `warranty_days`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_km`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_terms`: `text`; NULL; DEFAULT `<none>`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## suppliers

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `supplier_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `supplier_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `phone`: `varchar(30)`; NULL; DEFAULT `<none>`.
- `email`: `varchar(254)`; NULL; DEFAULT `<none>`.
- `address`: `varchar(500)`; NULL; DEFAULT `<none>`.
- `tax_code`: `varchar(30)`; NULL; DEFAULT `<none>`.
- `payment_term_days`: `int`; NOT NULL; DEFAULT `0`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## task_assignments

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `task_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `employee_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `assignment_role`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `starts_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `ends_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `assigned_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `assigned_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `released_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.
- `active_main_task_id`: `bigint`; NULL; DEFAULT `<none>`; STORED GENERATED; AS (case when ((`assignment_role` = _utf8mb4\\'MAIN\\') and (`is_active` = true)) then `task_id` else NULL end).

## users

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `username`: `varchar(100)`; NOT NULL; DEFAULT `<none>`.
- `email`: `varchar(254)`; NULL; DEFAULT `<none>`.
- `password_hash`: `varchar(255)`; NOT NULL; DEFAULT `<none>`.
- `full_name`: `varchar(150)`; NOT NULL; DEFAULT `<none>`.
- `role`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `deleted_at`: `datetime(6)`; NULL; DEFAULT NULL; V8, tài khoản đã xóa phải có is_active=FALSE.
- `last_login_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.
- `auth_version`: `bigint`; NOT NULL; DEFAULT `0`; V5, CHECK >= 0; thu hồi JWT khi thay quyền/active/password/logout.
- `created_by`: `bigint`; NULL; V6, actor tạo tài khoản.
- `updated_by`: `bigint`; NULL; V6, actor cập nhật gần nhất.

## vehicle_checks

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `repair_order_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `check_type`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `attempt_no`: `int`; NOT NULL; DEFAULT `1`.
- `result`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `checklist_json`: `json`; NOT NULL; DEFAULT `<none>`.
- `schema_version`: `int`; NOT NULL; DEFAULT `1`.
- `odometer`: `int`; NULL; DEFAULT `<none>`.
- `notes`: `text`; NULL; DEFAULT `<none>`.
- `checked_by`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `checked_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.

## vehicles

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `customer_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `license_plate`: `varchar(20)`; NOT NULL; DEFAULT `<none>`.
- `vin`: `varchar(17)`; NULL; DEFAULT `<none>`.
- `engine_number`: `varchar(50)`; NULL; DEFAULT `<none>`.
- `brand`: `varchar(80)`; NOT NULL; DEFAULT `<none>`.
- `model`: `varchar(80)`; NOT NULL; DEFAULT `<none>`.
- `manufacture_year`: `smallint`; NULL; DEFAULT `<none>`.
- `color`: `varchar(50)`; NULL; DEFAULT `<none>`.
- `fuel_type`: `varchar(30)`; NULL; DEFAULT `<none>`.
- `odometer`: `int`; NOT NULL; DEFAULT `0`.
- `inspection_due_date`: `date`; NULL; DEFAULT `<none>`.
- `is_active`: `tinyint(1)`; NOT NULL; DEFAULT `1`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `updated_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED on update CURRENT_TIMESTAMP(6).
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

## warranties

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `warranty_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `invoice_item_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `vehicle_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `customer_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `starts_at`: `datetime(6)`; NOT NULL; DEFAULT `<none>`.
- `expires_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `start_odometer`: `int`; NOT NULL; DEFAULT `<none>`.
- `end_odometer`: `int`; NULL; DEFAULT `<none>`.
- `warranty_days`: `int`; NOT NULL; DEFAULT `0`.
- `warranty_km`: `int`; NOT NULL; DEFAULT `0`.
- `terms_snapshot`: `text`; NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `ACTIVE`.
- `created_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.

## warranty_claims

- `id`: `bigint`; NOT NULL; DEFAULT `<none>`; auto_increment.
- `claim_code`: `varchar(30)`; NOT NULL; DEFAULT `<none>`.
- `warranty_id`: `bigint`; NOT NULL; DEFAULT `<none>`.
- `requested_by`: `bigint`; NULL; DEFAULT `<none>`.
- `requested_at`: `datetime(6)`; NOT NULL; DEFAULT `CURRENT_TIMESTAMP(6)`; DEFAULT_GENERATED.
- `current_odometer`: `int`; NOT NULL; DEFAULT `<none>`.
- `description`: `text`; NOT NULL; DEFAULT `<none>`.
- `status`: `varchar(20)`; NOT NULL; DEFAULT `OPEN`.
- `decided_by`: `bigint`; NULL; DEFAULT `<none>`.
- `decided_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `decision_reason`: `text`; NULL; DEFAULT `<none>`.
- `handling_order_id`: `bigint`; NULL; DEFAULT `<none>`.
- `closed_at`: `datetime(6)`; NULL; DEFAULT `<none>`.
- `version`: `bigint`; NOT NULL; DEFAULT `0`.

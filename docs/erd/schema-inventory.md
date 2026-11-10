# Kiểm kê schema (sinh tự động)

> Sinh bởi `scripts/analysis/schema_inventory.py` từ migration Flyway. Không sửa tay; chạy lại script khi thêm migration.

- **Bảng**: 34 · **Cột**: 540 · **Khóa ngoại**: 98 · **View**: 2 (v_invoice_balances, v_supplier_balances)

| Bảng | Tạo ở | Cột | PK | Khóa ngoại → bảng | UNIQUE | CHECK |
|---|---|---|---|---|---|---|
| `attachments` | V2 | 12 | id | repair_order_id → repair_orders<br>vehicle_check_id, repair_order_id → vehicle_checks<br>uploaded_by → users<br>warranty_claim_id → warranty_claims | storage_key | 4 |
| `audit_events` | V1 | 12 | id | actor_user_id → users | request_key | 1 |
| `bay_reservations` | V2 | 14 | id | bay_id → service_bays<br>booking_id → bookings<br>repair_order_id → repair_orders<br>created_by → users | — | 3 |
| `bookings` | V1 | 14 | id | customer_id → customers<br>vehicle_id → vehicles<br>service_id → services<br>created_by → users | booking_code | 2 |
| `customers` | V1 | 16 | id | user_id → users | customer_code<br>user_id<br>phone | 2 |
| `employee_shifts` | V1 | 17 | id | employee_id → employees<br>created_by → users<br>service_bay_id → service_bays<br>approved_by → users | — | 6 |
| `employees` | V1 | 18 | id | user_id → users | employee_code<br>user_id | 3 |
| `estimate_items` | V2 | 24 | id | estimate_id → estimates<br>service_id → services<br>part_id → parts<br>approved_by → users | estimate_id, line_no | 5 |
| `estimates` | V2 | 17 | id | repair_order_id → repair_orders<br>parent_estimate_id, repair_order_id → estimates<br>created_by → users | estimate_code<br>repair_order_id, version_no<br>id, repair_order_id | 4 |
| `inventory_document_items` | V3 | 11 | id | document_id → inventory_documents<br>part_id → parts<br>estimate_item_id → estimate_items<br>repair_order_part_id → repair_order_parts<br>source_item_id → inventory_document_items | document_id, line_no | 1 |
| `inventory_documents` | V3 | 19 | id | supplier_id → suppliers<br>repair_order_id → repair_orders<br>source_document_id → inventory_documents<br>created_by → users<br>posted_by → users | document_code<br>supplier_id, supplier_invoice_number | 4 |
| `inventory_transactions` | V3 | 9 | id | document_item_id → inventory_document_items<br>part_id → parts<br>created_by → users | document_item_id | 2 |
| `invoice_items` | V3 | 17 | id | invoice_id → invoices<br>repair_task_id → repair_tasks<br>repair_order_part_id → repair_order_parts | invoice_id, line_no | 3 |
| `invoices` | V3 | 19 | id | repair_order_id → repair_orders<br>customer_id → customers<br>issued_by → users<br>voided_by → users | invoice_code<br>active_repair_order_id | 3 |
| `labor_sessions` | V2 | 9 | id | assignment_id, employee_id → task_assignments | active_employee_id | 3 |
| `notifications` | V1 | 23 | id | user_id → users<br>customer_id → customers | event_key, channel, recipient_key | 4 |
| `order_commissions` | V12 | 20 | id | repair_order_id → repair_orders<br>task_id → repair_tasks<br>employee_id → employees<br>payroll_item_id → payroll_items<br>shift_id → employee_shifts<br>service_bay_id → service_bays | — | 4 |
| `parts` | V1 | 18 | id | preferred_supplier_id → suppliers | sku | 3 |
| `payments` | V3 | 12 | id | invoice_id → invoices<br>inventory_document_id → inventory_documents<br>recorded_by → users<br>payroll_period_id → payroll_periods | transaction_code<br>provider_reference | 4 |
| `payroll_items` | V12 | 22 | id | payroll_period_id → payroll_periods<br>employee_id → employees | payroll_period_id, employee_id | 2 |
| `payroll_periods` | V12 | 21 | id | approved_by → users<br>payment_id → payments | period_code<br>period_month | 2 |
| `repair_order_assignments` | V13 | 17 | id | repair_order_id → repair_orders<br>vehicle_id → vehicles<br>employee_id → employees<br>shift_id → employee_shifts<br>task_id → repair_tasks | — | 4 |
| `repair_order_parts` | V3 | 10 | id | repair_order_id → repair_orders<br>estimate_item_id → estimate_items<br>part_id → parts | estimate_item_id | 1 |
| `repair_orders` | V2 | 25 | id | booking_id → bookings<br>customer_id → customers<br>vehicle_id → vehicles<br>received_by → users<br>handed_over_by → users<br>latest_qc_check_id, id → vehicle_checks<br>lead_technician_id → employees<br>diagnostic_technician_id → employees<br>service_bay_id → service_bays<br>qc_technician_id → employees | order_code<br>booking_id | 4 |
| `repair_tasks` | V2 | 15 | id | repair_order_id → repair_orders<br>estimate_item_id → estimate_items<br>service_id → services | estimate_item_id | 3 |
| `service_bays` | V1 | 9 | id | — | bay_code | 0 |
| `services` | V1 | 19 | id | — | service_code | 3 |
| `suppliers` | V1 | 12 | id | — | supplier_code | 1 |
| `task_assignments` | V2 | 12 | id | task_id → repair_tasks<br>employee_id → employees<br>assigned_by → users | active_main_task_id<br>id, employee_id | 2 |
| `users` | V1 | 16 | id | — | username<br>email<br>identity_id | 3 |
| `vehicle_checks` | V2 | 11 | id | repair_order_id → repair_orders<br>checked_by → users | repair_order_id, check_type, attempt_no<br>id, repair_order_id | 3 |
| `vehicles` | V1 | 16 | id | customer_id → customers | license_plate<br>vin | 1 |
| `warranties` | V3 | 20 | id | invoice_item_id → invoice_items<br>vehicle_id → vehicles<br>customer_id → customers | warranty_code<br>invoice_item_id | 3 |
| `warranty_claims` | V3 | 14 | id | warranty_id → warranties<br>requested_by → users<br>decided_by → users<br>handling_order_id → repair_orders | claim_code<br>handling_order_id | 3 |

## Cột theo bảng

### `attachments`

`id`, `repair_order_id`, `vehicle_check_id`, `warranty_claim_id`, `storage_key`, `original_filename`, `content_type`, `size_bytes`, `sha256`, `category`, `uploaded_by`, `uploaded_at`

### `audit_events`

`id`, `actor_user_id`, `action`, `entity_type`, `entity_id`, `request_key`, `request_hash`, `result_json`, `before_json`, `after_json`, `ip_address`, `created_at`

### `bay_reservations`

`id`, `bay_id`, `booking_id`, `repair_order_id`, `reservation_type`, `status`, `starts_at`, `ends_at`, `expires_at`, `released_at`, `notes`, `created_by`, `created_at`, `version`

### `bookings`

`id`, `booking_code`, `customer_id`, `vehicle_id`, `service_id`, `appointment_at`, `expected_end_at`, `request_description`, `status`, `cancellation_reason`, `created_by`, `created_at`, `updated_at`, `version`

### `customers`

`id`, `user_id`, `customer_code`, `full_name`, `customer_type`, `phone`, `email`, `address`, `company_name`, `tax_code`, `credit_limit`, `payment_term_days`, `is_active`, `created_at`, `updated_at`, `version`

### `employee_shifts`

`id`, `employee_id`, `starts_at`, `ends_at`, `status`, `notes`, `created_by`, `created_at`, `version`, `shift_type`, `service_bay_id`, `actual_starts_at`, `actual_ends_at`, `attendance_status`, `registered_at`, `approved_by`, `rejection_reason`

### `employees`

`id`, `user_id`, `employee_code`, `skills_json`, `can_qc`, `hourly_cost`, `is_active`, `created_at`, `updated_at`, `version`, `base_salary`, `overtime_multiplier`, `default_commission_rate`, `salary_coefficient`, `joined_at`, `bank_name`, `bank_account_number`, `bank_account_holder`

### `estimate_items`

`id`, `estimate_id`, `line_no`, `item_type`, `service_id`, `part_id`, `item_name`, `unit`, `quantity`, `unit_price`, `unit_cost`, `discount_amount`, `tax_amount`, `line_total`, `approval_status`, `approved_by`, `decided_at`, `decision_note`, `reserved_quantity`, `warranty_days`, `warranty_km`, `warranty_terms`, `created_at`, `version`

### `estimates`

`id`, `estimate_code`, `repair_order_id`, `version_no`, `estimate_kind`, `parent_estimate_id`, `status`, `subtotal`, `discount_amount`, `tax_amount`, `total_amount`, `notes`, `created_by`, `sent_at`, `valid_until`, `created_at`, `version`

### `inventory_document_items`

`id`, `document_id`, `line_no`, `part_id`, `estimate_item_id`, `repair_order_part_id`, `source_item_id`, `quantity`, `unit_cost`, `line_total`, `notes`

### `inventory_documents`

`id`, `document_code`, `document_type`, `status`, `supplier_id`, `repair_order_id`, `source_document_id`, `supplier_invoice_number`, `supplier_invoice_date`, `due_date`, `subtotal`, `tax_amount`, `total_amount`, `reason`, `created_by`, `posted_by`, `posted_at`, `created_at`, `version`

### `inventory_transactions`

`id`, `document_item_id`, `part_id`, `transaction_type`, `quantity_change`, `stock_after`, `unit_cost`, `created_by`, `created_at`

### `invoice_items`

`id`, `invoice_id`, `line_no`, `item_type`, `repair_task_id`, `repair_order_part_id`, `item_name`, `unit`, `quantity`, `unit_price`, `unit_cost`, `discount_amount`, `tax_amount`, `line_total`, `warranty_days`, `warranty_km`, `warranty_terms`

### `invoices`

`id`, `invoice_code`, `repair_order_id`, `customer_id`, `status`, `customer_snapshot`, `vehicle_snapshot`, `subtotal`, `discount_amount`, `tax_amount`, `total_amount`, `issued_by`, `issued_at`, `due_date`, `void_reason`, `voided_by`, `voided_at`, `version`, `active_repair_order_id`

### `labor_sessions`

`id`, `assignment_id`, `employee_id`, `starts_at`, `ends_at`, `stop_reason`, `hourly_cost_snapshot`, `created_at`, `active_employee_id`

### `notifications`

`id`, `user_id`, `customer_id`, `recipient_key`, `destination`, `event_key`, `channel`, `subject`, `payload_json`, `status`, `attempt_count`, `next_attempt_at`, `locked_until`, `locked_by`, `last_error`, `provider_reference`, `sent_at`, `read_at`, `created_at`, `updated_at`, `created_by`, `updated_by`, `version`

### `order_commissions`

`id`, `repair_order_id`, `task_id`, `employee_id`, `commission_type`, `base_amount`, `rate_percent`, `commission_amount`, `status`, `earned_at`, `payroll_item_id`, `notes`, `created_at`, `updated_at`, `version`, `shift_id`, `shift_type`, `service_bay_id`, `bay_code`, `license_plate`

### `parts`

`id`, `sku`, `part_name`, `unit`, `description`, `preferred_supplier_id`, `purchase_price`, `selling_price`, `stock_quantity`, `reserved_quantity`, `reorder_level`, `warranty_days`, `warranty_km`, `warranty_terms`, `is_active`, `created_at`, `updated_at`, `version`

### `payments`

`id`, `transaction_code`, `direction`, `invoice_id`, `inventory_document_id`, `amount`, `payment_method`, `provider_reference`, `paid_at`, `recorded_by`, `notes`, `payroll_period_id`

### `payroll_items`

`id`, `payroll_period_id`, `employee_id`, `base_salary_snapshot`, `hourly_rate_snapshot`, `regular_hours`, `overtime_hours`, `shift_count`, `completed_order_count`, `hourly_pay_amount`, `commission_amount`, `allowance_amount`, `deduction_amount`, `gross_pay`, `net_pay`, `status`, `paid_at`, `notes`, `created_at`, `updated_at`, `version`, `order_bonus_amount`

### `payroll_periods`

`id`, `period_code`, `period_month`, `from_date`, `to_date`, `status`, `total_employees`, `total_regular_hours`, `total_overtime_hours`, `total_base_pay`, `total_commissions`, `total_allowances`, `total_deductions`, `total_net_pay`, `approved_by`, `approved_at`, `payment_id`, `notes`, `created_at`, `updated_at`, `version`

### `repair_order_assignments`

`id`, `repair_order_id`, `vehicle_id`, `employee_id`, `shift_id`, `assignment_stage`, `task_id`, `started_at`, `completed_at`, `actual_seconds`, `hourly_rate_snapshot`, `earned_commission`, `result_status`, `notes`, `created_at`, `updated_at`, `version`

### `repair_order_parts`

`id`, `repair_order_id`, `estimate_item_id`, `part_id`, `quantity`, `unit_price`, `unit_cost`, `created_at`, `updated_at`, `version`

### `repair_orders`

`id`, `order_code`, `booking_id`, `customer_id`, `vehicle_id`, `order_type`, `status`, `odometer_in`, `odometer_out`, `fuel_level_percent`, `customer_complaint`, `diagnosis`, `received_by`, `received_at`, `latest_qc_check_id`, `handed_over_by`, `handed_over_at`, `closed_at`, `created_at`, `updated_at`, `version`, `lead_technician_id`, `diagnostic_technician_id`, `service_bay_id`, `qc_technician_id`

### `repair_tasks`

`id`, `repair_order_id`, `estimate_item_id`, `service_id`, `task_type`, `task_name`, `description`, `status`, `quantity`, `unit_price`, `started_at`, `completed_at`, `completion_note`, `created_at`, `version`

### `service_bays`

`id`, `bay_code`, `bay_name`, `bay_type`, `description`, `is_active`, `created_at`, `updated_at`, `version`

### `services`

`id`, `service_code`, `service_name`, `service_type`, `description`, `unit`, `unit_price`, `standard_minutes`, `required_bay_type`, `required_skills_json`, `interval_km`, `interval_months`, `warranty_days`, `warranty_km`, `warranty_terms`, `is_active`, `created_at`, `updated_at`, `version`

### `suppliers`

`id`, `supplier_code`, `supplier_name`, `phone`, `email`, `address`, `tax_code`, `payment_term_days`, `is_active`, `created_at`, `updated_at`, `version`

### `task_assignments`

`id`, `task_id`, `employee_id`, `assignment_role`, `is_active`, `starts_at`, `ends_at`, `assigned_by`, `assigned_at`, `released_at`, `version`, `active_main_task_id`

### `users`

`id`, `username`, `email`, `password_hash`, `full_name`, `role`, `is_active`, `last_login_at`, `created_at`, `updated_at`, `version`, `auth_version`, `created_by`, `updated_by`, `deleted_at`, `identity_id`

### `vehicle_checks`

`id`, `repair_order_id`, `check_type`, `attempt_no`, `result`, `checklist_json`, `schema_version`, `odometer`, `notes`, `checked_by`, `checked_at`

### `vehicles`

`id`, `customer_id`, `license_plate`, `vin`, `engine_number`, `brand`, `model`, `manufacture_year`, `color`, `fuel_type`, `odometer`, `inspection_due_date`, `is_active`, `created_at`, `updated_at`, `version`

### `warranties`

`id`, `warranty_code`, `invoice_item_id`, `vehicle_id`, `customer_id`, `starts_at`, `expires_at`, `start_odometer`, `end_odometer`, `warranty_days`, `warranty_km`, `terms_snapshot`, `status`, `created_at`, `void_reason`, `voided_by`, `voided_at`, `adjustment_note`, `adjusted_by`, `adjusted_at`

### `warranty_claims`

`id`, `claim_code`, `warranty_id`, `requested_by`, `requested_at`, `current_odometer`, `description`, `status`, `decided_by`, `decided_at`, `decision_reason`, `handling_order_id`, `closed_at`, `version`


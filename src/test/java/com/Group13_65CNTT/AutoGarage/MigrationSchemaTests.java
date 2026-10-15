package com.Group13_65CNTT.AutoGarage;

import static org.junit.jupiter.api.Assertions.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Set;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MigrationSchemaTests extends IntegrationTestSupport {
    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;

    @BeforeEach
    void fixtures() {
        jdbc.update(
                "INSERT INTO users(id,username,password_hash,full_name,role)"
                    + " VALUES(910001,'schema_admin','test','Admin','ADMIN'),(910002,'schema_tech','test','Tech','TECHNICIAN')");
        jdbc.update(
                "INSERT INTO customers(id,customer_code,full_name,phone)"
                        + " VALUES(910001,'SCHEMA_C','Customer','0900000000')");
        jdbc.update(
                "INSERT INTO vehicles(id,customer_id,license_plate,brand,model)"
                        + " VALUES(910001,910001,'SCHEMA01','Test','Test')");
        jdbc.update(
                "INSERT INTO services(id,service_code,service_name,unit_price,standard_minutes)"
                        + " VALUES(910001,'SCHEMA_S','Service',100,30)");
        jdbc.update(
                "INSERT INTO employees(id,user_id,employee_code) VALUES(910001,910002,'SCHEMA_E')");
        jdbc.update(
                "INSERT INTO service_bays(id,bay_code,bay_name,bay_type)"
                        + " VALUES(910001,'SCHEMA_B','Bay','GENERAL')");
        jdbc.update(
                "INSERT INTO"
                    + " bookings(id,booking_code,customer_id,vehicle_id,service_id,appointment_at,expected_end_at)"
                    + " VALUES(910001,'SCHEMA_BK',910001,910001,910001,'2026-10-14"
                    + " 01:00:00','2026-10-14 02:00:00')");
        jdbc.update(
                "INSERT INTO"
                    + " repair_orders(id,order_code,booking_id,customer_id,vehicle_id,odometer_in,received_by)"
                    + " VALUES(910001,'SCHEMA_RO',910001,910001,910001,100,910001),(910002,'SCHEMA_RO2',NULL,910001,910001,100,910001)");
        jdbc.update(
                "INSERT INTO estimates(id,estimate_code,repair_order_id,version_no,created_by)"
                        + " VALUES(910001,'SCHEMA_EST',910001,1,910001)");
        jdbc.update(
                "INSERT INTO"
                    + " estimate_items(id,estimate_id,line_no,item_type,service_id,item_name,unit,quantity,unit_price,line_total,approval_status)"
                    + " VALUES(910001,910001,1,'SERVICE',910001,'Service','job',1,100,100,'APPROVED')");
        jdbc.update(
                "INSERT INTO repair_tasks(id,repair_order_id,estimate_item_id,service_id,task_name)"
                        + " VALUES(910001,910001,910001,910001,'Task')");
        jdbc.update(
                "INSERT INTO suppliers(id,supplier_code,supplier_name)"
                        + " VALUES(910001,'SCHEMA_SP','Supplier')");
        jdbc.update(
                "INSERT INTO parts(id,sku,part_name,unit,stock_quantity)"
                        + " VALUES(910001,'SCHEMA_P','Part','piece',10)");
        jdbc.update(
                "INSERT INTO"
                    + " inventory_documents(id,document_code,document_type,supplier_id,status,subtotal,total_amount,created_by,posted_by,posted_at)"
                    + " VALUES(910001,'SCHEMA_IN','IN',910001,'POSTED',200,200,910001,910001,CURRENT_TIMESTAMP(6)),(910002,'SCHEMA_IN2','IN',910001,'POSTED',300,300,910001,910001,CURRENT_TIMESTAMP(6))");
        jdbc.update(
                "INSERT INTO"
                    + " inventory_document_items(id,document_id,line_no,part_id,quantity,unit_cost,line_total)"
                    + " VALUES(910001,910001,1,910001,2,100,200)");
        jdbc.update(
                "INSERT INTO"
                    + " invoices(id,invoice_code,repair_order_id,customer_id,customer_snapshot,vehicle_snapshot,subtotal,total_amount,issued_by)"
                    + " VALUES(910001,'SCHEMA_INV',910001,910001,'{}','{}',100,100,910001)");
        jdbc.update(
                "INSERT INTO"
                    + " invoice_items(id,invoice_id,line_no,item_type,repair_task_id,item_name,unit,quantity,unit_price,unit_cost,line_total,warranty_days,warranty_km)"
                    + " VALUES(910001,910001,1,'SERVICE',910001,'Service','job',1,100,20,100,30,1000)");
    }

    @Test
    void allMigrationsAndTablesExist() {
        assertEquals(
                30,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.tables WHERE"
                            + " table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name"
                            + " <> 'flyway_schema_history'",
                        Integer.class));
        assertEquals(
                2,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.views WHERE"
                                + " table_schema=DATABASE()",
                        Integer.class));
        assertEquals(7, flyway.info().applied().length);
        assertEquals(0, flyway.info().pending().length);
        flyway.validate();
    }

    @Test
    void customersMayHaveNoAccountButRolesAreRestricted() {
        assertNull(
                jdbc.queryForObject("SELECT user_id FROM customers WHERE id=910001", Long.class));
        rejected("UPDATE users SET role='MANAGER' WHERE id=910001");
    }

    @Test
    void aBookingCannotGenerateTwoOrders() {
        rejected(
                "INSERT INTO"
                    + " repair_orders(order_code,booking_id,customer_id,vehicle_id,odometer_in,received_by)"
                    + " VALUES('DUP',910001,910001,910001,100,910001)");
    }

    @Test
    void estimateLinesRequireExactlyOneSourceAndValidReservations() {
        rejected("UPDATE estimate_items SET part_id=910001 WHERE id=910001");
        rejected("UPDATE estimate_items SET reserved_quantity=1 WHERE id=910001");
        rejected("UPDATE estimate_items SET line_total=99 WHERE id=910001");
    }

    @Test
    void stockCannotBeNegativeOrReservedBeyondAvailable() {
        rejected("UPDATE parts SET reserved_quantity=11 WHERE id=910001");
        rejected("UPDATE parts SET stock_quantity=-1 WHERE id=910001");
    }

    @Test
    void aTaskHasOnlyOneActiveMainAssignment() {
        assignment(910001, "MAIN");
        rejected(
                "INSERT INTO"
                    + " task_assignments(task_id,employee_id,assignment_role,starts_at,ends_at,assigned_by)"
                    + " VALUES(910001,910001,'MAIN','2026-10-14 01:00:00','2026-10-14"
                    + " 02:00:00',910001)");
        jdbc.update("UPDATE task_assignments SET is_active=FALSE WHERE id=910001");
        assignment(910002, "MAIN");
    }

    @Test
    void anEmployeeHasOnlyOneOpenTimerAndCanResumeAfterPause() {
        assignment(910001, "MAIN");
        jdbc.update(
                "INSERT INTO labor_sessions(assignment_id,employee_id,starts_at)"
                        + " VALUES(910001,910001,'2026-10-14 01:00:00')");
        rejected(
                "INSERT INTO labor_sessions(assignment_id,employee_id,starts_at)"
                        + " VALUES(910001,910001,'2026-10-14 01:10:00')");
        jdbc.update(
                "UPDATE labor_sessions SET ends_at='2026-10-14 01:30:00',stop_reason='PAUSE' WHERE"
                        + " employee_id=910001");
        jdbc.update(
                "INSERT INTO labor_sessions(assignment_id,employee_id,starts_at)"
                        + " VALUES(910001,910001,'2026-10-14 01:40:00')");
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM labor_sessions WHERE active_employee_id=910001",
                        Integer.class));
    }

    @Test
    void timersCannotBeAttributedToAnotherEmployee() {
        assignment(910001, "MAIN");
        rejected(
                "INSERT INTO labor_sessions(assignment_id,employee_id,starts_at)"
                        + " VALUES(910001,999999,'2026-10-14 01:00:00')");
    }

    @Test
    void reservationsRequireValidIntervalsAndSources() {
        rejected(
                "INSERT INTO bay_reservations(bay_id,reservation_type,starts_at,ends_at)"
                        + " VALUES(910001,'REPAIR','2026-10-14 01:00:00','2026-10-14 02:00:00')");
        rejected(
                "INSERT INTO bay_reservations(bay_id,reservation_type,starts_at,ends_at)"
                    + " VALUES(910001,'MAINTENANCE','2026-10-14 02:00:00','2026-10-14 01:00:00')");
    }

    @Test
    void latestQcMustBelongToTheSameOrder() {
        jdbc.update(
                "INSERT INTO"
                    + " vehicle_checks(id,repair_order_id,check_type,result,checklist_json,checked_by)"
                    + " VALUES(910001,910002,'QC','PASSED','{}',910002)");
        rejected("UPDATE repair_orders SET latest_qc_check_id=910001 WHERE id=910001");
        jdbc.update("UPDATE repair_orders SET latest_qc_check_id=910001 WHERE id=910002");
    }

    @Test
    void paymentsRequireTheCorrectTargetAndPositiveAmount() {
        rejected(
                "INSERT INTO"
                    + " payments(transaction_code,direction,invoice_id,amount,payment_method,recorded_by)"
                    + " VALUES('BAD_OUT','OUT',910001,10,'CASH',910001)");
        rejected(
                "INSERT INTO"
                    + " payments(transaction_code,direction,invoice_id,amount,payment_method,recorded_by)"
                    + " VALUES('ZERO','IN',910001,0,'CASH',910001)");
    }

    @Test
    void invoiceBalanceAggregatesMultiplePaymentsAndExcludesVoids() {
        payment("IN_A", "IN", 910001, 20);
        payment("IN_B", "IN", 910001, 30);
        assertAmount(
                "SELECT balance_amount FROM v_invoice_balances WHERE invoice_id=910001", "50.00");
        jdbc.update(
                "UPDATE invoices SET"
                    + " status='VOID',void_reason='test',voided_by=910001,voided_at=CURRENT_TIMESTAMP(6)"
                    + " WHERE id=910001");
        assertEquals(
                0,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM v_invoice_balances WHERE invoice_id=910001",
                        Integer.class));
    }

    @Test
    void supplierBalanceDoesNotMultiplyPurchasesAndIgnoresDraftsAndOpeningStock() {
        payment("OUT_A", "OUT", 910001, 30);
        payment("OUT_B", "OUT", 910001, 40);
        payment("OUT_C", "OUT", 910002, 50);
        jdbc.update(
                "INSERT INTO"
                    + " inventory_documents(document_code,document_type,supplier_id,subtotal,total_amount,created_by)"
                    + " VALUES('DRAFT','IN',910001,999,999,910001)");
        jdbc.update(
                "INSERT INTO"
                    + " inventory_documents(document_code,document_type,subtotal,total_amount,created_by)"
                    + " VALUES('OPEN','OPENING',999,999,910001)");
        assertAmount(
                "SELECT purchase_amount FROM v_supplier_balances WHERE supplier_id=910001",
                "500.00");
        assertAmount(
                "SELECT paid_amount FROM v_supplier_balances WHERE supplier_id=910001", "120.00");
        assertAmount(
                "SELECT balance_amount FROM v_supplier_balances WHERE supplier_id=910001",
                "380.00");
    }

    @Test
    void eachInventoryDocumentLineCanBePostedOnlyOnce() {
        jdbc.update(
                "INSERT INTO"
                    + " inventory_transactions(document_item_id,part_id,transaction_type,quantity_change,stock_after,unit_cost,created_by)"
                    + " VALUES(910001,910001,'IN',2,12,100,910001)");
        rejected(
                "INSERT INTO"
                    + " inventory_transactions(document_item_id,part_id,transaction_type,quantity_change,stock_after,unit_cost,created_by)"
                    + " VALUES(910001,910001,'IN',2,14,100,910001)");
    }

    @Test
    void noticeDeduplicationIncludesTheRecipient() {
        String insert =
                "INSERT INTO"
                    + " notifications(user_id,recipient_key,destination,event_key,channel,payload_json)"
                    + " VALUES(?,?,'web','SAME_EVENT','WEB','{}')";
        jdbc.update(insert, 910001, "user:910001");
        jdbc.update(insert, 910002, "user:910002");
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbc.update(insert, 910001, "user:910001"));
    }

    @Test
    void warrantyClaimsNeedADecisionAndANewHandlingOrder() {
        jdbc.update(
                "INSERT INTO"
                    + " warranties(id,warranty_code,invoice_item_id,vehicle_id,customer_id,starts_at,expires_at,start_odometer,end_odometer,warranty_days,warranty_km)"
                    + " VALUES(910001,'W_TEST',910001,910001,910001,'2026-10-14"
                    + " 01:00:00','2026-11-13 01:00:00',100,1100,30,1000)");
        jdbc.update(
                "INSERT INTO"
                        + " warranty_claims(id,claim_code,warranty_id,current_odometer,description)"
                        + " VALUES(910001,'CL_TEST',910001,200,'Test claim')");
        rejected("UPDATE warranty_claims SET status='APPROVED' WHERE id=910001");
        jdbc.update(
                "INSERT INTO"
                    + " attachments(warranty_claim_id,storage_key,original_filename,content_type,size_bytes,category,uploaded_by)"
                    + " VALUES(910001,'claim/photo.jpg','photo.jpg','image/jpeg',100,'WARRANTY',910001)");
        rejected(
                "INSERT INTO"
                    + " attachments(storage_key,original_filename,content_type,size_bytes,category,uploaded_by)"
                    + " VALUES('orphan/photo.jpg','photo.jpg','image/jpeg',100,'WARRANTY',910001)");
    }

    private void rejected(String sql) {
        // Connector/J reports CHECK failures as HY000/3819, which Spring may not
        // translate into DataIntegrityViolationException. Verify MySQL's actual code.
        DataAccessException failure =
                assertThrows(DataAccessException.class, () -> jdbc.update(sql));
        SQLException cause = assertInstanceOf(SQLException.class, failure.getMostSpecificCause());
        assertTrue(
                Set.of(1062, 1452, 3819).contains(cause.getErrorCode()),
                () ->
                        "Expected UNIQUE/FK/CHECK violation, got MySQL error "
                                + cause.getErrorCode());
    }

    private void assignment(long id, String role) {
        jdbc.update(
                "INSERT INTO"
                    + " task_assignments(id,task_id,employee_id,assignment_role,starts_at,ends_at,assigned_by)"
                    + " VALUES(?,910001,910001,?,'2026-10-14 01:00:00','2026-10-14"
                    + " 02:00:00',910001)",
                id,
                role);
    }

    private void payment(String code, String direction, long target, int amount) {
        jdbc.update(
                "INSERT INTO"
                    + " payments(transaction_code,direction,invoice_id,inventory_document_id,amount,payment_method,recorded_by)"
                    + " VALUES(?,?,?,?,?,'CASH',910001)",
                code,
                direction,
                direction.equals("IN") ? target : null,
                direction.equals("OUT") ? target : null,
                amount);
    }

    private void assertAmount(String sql, String expected) {
        assertEquals(
                0, new BigDecimal(expected).compareTo(jdbc.queryForObject(sql, BigDecimal.class)));
    }
}

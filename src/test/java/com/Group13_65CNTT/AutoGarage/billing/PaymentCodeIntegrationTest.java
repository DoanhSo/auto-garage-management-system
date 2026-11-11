package com.Group13_65CNTT.AutoGarage.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.billing.repository.PaymentRepository;
import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Daily numbering of system transaction codes against the real payments table. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class PaymentCodeIntegrationTest extends IntegrationTestSupport {
    private final JdbcTemplate jdbc;
    private final BillingService service;
    private final PaymentRepository payments;
    private final com.Group13_65CNTT.AutoGarage.billing.repository.InvoiceRepository invoices;
    private final org.springframework.transaction.PlatformTransactionManager transactionManager;

    private final LocalDate today = PaymentCodePolicy.today();
    private String receiptPrefix;
    private String payoutPrefix;
    private long periodId;

    @BeforeEach
    void fixtures() {
        receiptPrefix = PaymentCodePolicy.dayPrefix(PaymentDirection.IN, today);
        payoutPrefix = PaymentCodePolicy.dayPrefix(PaymentDirection.OUT, today);
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(930101,'pay_admin','test','Admin','ADMIN')");
        jdbc.update("INSERT INTO payroll_periods(id,period_code,period_month,from_date,to_date)"
                + " VALUES(930101,'PAYCODE_P','2026-11','2026-11-01','2026-11-30')");
        periodId = 930101L;
    }

    /** Payout rows are the only ones insertable without invoice fixtures; the code is what matters here. */
    private void payment(String code) {
        jdbc.update("INSERT INTO payments(transaction_code,direction,payroll_period_id,amount,payment_method,recorded_by)"
                + " VALUES(?,?,?,?,?,?)", code, "OUT", periodId, 1000, "CASH", 930101L);
    }

    @Test
    @DisplayName("Chưa có mã nào hôm nay: bắt đầu từ 0001 với tiền tố riêng cho thu và chi")
    void firstCodeOfTheDay() {
        assertThat(service.suggestTransactionCode(PaymentDirection.IN)).isEqualTo(receiptPrefix + "0001");
        assertThat(service.suggestTransactionCode(PaymentDirection.OUT)).isEqualTo(payoutPrefix + "0001");
    }

    @Test
    @DisplayName("Đánh số tiếp theo mã lớn nhất cùng tiền tố trong ngày, bỏ qua mã ngày khác và mã lạ")
    void continuesAfterHighestCodeOfTheDay() {
        payment(payoutPrefix + "0003");
        payment(payoutPrefix + "0010");
        payment(payoutPrefix + "LEGACY");
        payment("PC-19990101-0500");
        payment("TX-OUT-ABCDEF12");
        payment(receiptPrefix + "0042");

        assertThat(service.suggestTransactionCode(PaymentDirection.OUT)).isEqualTo(payoutPrefix + "0011");
        assertThat(service.suggestTransactionCode(PaymentDirection.IN)).isEqualTo(receiptPrefix + "0043");
    }

    @Test
    @DisplayName("Mã lớn nhất cuối cùng không đánh số được thì rơi về 0001 và bỏ qua mã đã bị chiếm")
    void malformedHighestFallsBackToFirstFreeNumber() {
        payment(payoutPrefix + "ZZZZ");
        payment(payoutPrefix + "0001");

        assertThat(service.suggestTransactionCode(PaymentDirection.OUT)).isEqualTo(payoutPrefix + "0002");
    }

    @Test
    @DisplayName("Truy vấn trả rỗng khi chưa có mã cùng tiền tố và sắp xếp giảm dần khi có")
    void repositoryReturnsCodesHighestFirst() {
        assertThat(payments.findCodesStartingWith("ZZ-NOPE-", PageRequest.of(0, 5))).isEmpty();

        payment(payoutPrefix + "0002");
        payment(payoutPrefix + "0009");

        assertThat(payments.findCodesStartingWith(payoutPrefix, PageRequest.of(0, 5)))
                .containsExactly(payoutPrefix + "0009", payoutPrefix + "0002");
    }

    @Test
    @DisplayName("Mã khác nhau chỉ ở chữ hoa/thường là hai mã riêng (cột phân biệt hoa thường)")
    void existsIsCaseSensitive() {
        payment("pc-case-1");

        assertThat(payments.existsByTransactionCode("pc-case-1")).isTrue();
        assertThat(payments.existsByTransactionCode("PC-CASE-1")).isFalse();
    }

    @Test
    @DisplayName("Tìm kiếm hóa đơn theo tên khách hàng, biển số xe, mã lệnh sửa chữa và mã hóa đơn")
    void searchInvoices_MatchesCustomerVehicleAndOrder() {
        jdbc.update("INSERT INTO customers(id,customer_code,full_name,phone,credit_limit,is_active) VALUES(930201,'KH-930201','Nguyễn Văn An','0912345678',0,true)");
        jdbc.update("INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer,is_active) VALUES(930201,930201,'30H-889.23','Toyota','Vios 1.5G',30000,true)");
        jdbc.update("INSERT INTO repair_orders(id,order_code,customer_id,vehicle_id,status,odometer_in,received_by) VALUES(930201,'RO-20261110-001',930201,930201,'COMPLETED',30000,930101)");
        jdbc.update("INSERT INTO invoices(id,invoice_code,repair_order_id,customer_id,status,customer_snapshot,vehicle_snapshot,subtotal,total_amount,issued_by,issued_at)"
                + " VALUES(930201,'INV-20261110-001',930201,930201,'UNPAID',CAST('{\"fullName\":\"Nguyễn Văn An\",\"customerCode\":\"KH-930201\"}' AS JSON),CAST('{\"plateNumber\":\"30H-889.23\"}' AS JSON),1000000,1000000,930101,NOW())");

        org.springframework.data.domain.Pageable p = PageRequest.of(0, 10);

        // Khớp theo tên khách hàng (có dấu / không hoa thường)
        assertThat(invoices.searchInvoices(null, null, null, "Nguyễn Văn An", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);
        assertThat(invoices.searchInvoices(null, null, null, "văn an", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);

        // Khớp theo mã khách hàng & số điện thoại
        assertThat(invoices.searchInvoices(null, null, null, "KH-930201", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);
        assertThat(invoices.searchInvoices(null, null, null, "0912345678", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);

        // Khớp theo biển số xe
        assertThat(invoices.searchInvoices(null, null, null, "30H-889.23", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);

        // Khớp theo mã phiếu sửa chữa
        assertThat(invoices.searchInvoices(null, null, null, "RO-20261110", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);

        // Khớp theo mã hóa đơn
        assertThat(invoices.searchInvoices(null, null, null, "INV-20261110", p).getContent())
                .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                .contains(930201L);

        // Không khớp
        assertThat(invoices.searchInvoices(null, null, null, "KHONG_CO_AI_TEN_NAY", p).getContent())
                .isEmpty();

        // Tên trường trong snapshot JSON không phải dữ liệu để tìm
        for (String jsonKey : java.util.List.of("fullName", "plateNumber", "customerCode")) {
            assertThat(invoices.searchInvoices(null, null, null, jsonKey, p).getContent())
                    .extracting(com.Group13_65CNTT.AutoGarage.billing.entity.Invoice::getId)
                    .doesNotContain(930201L);
        }
    }

    @Test
    @DisplayName("getInvoiceReadiness trả ready=false kèm lý do và không đánh dấu rollbackOnly khi còn phụ tùng giữ chỗ chưa xuất kho")
    void invoiceReadiness_WithReservedParts_ReturnsFalseWithoutMarkingTransactionRollbackOnly() {
        var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("930101")
                .claim("roles", java.util.List.of("ADMIN"))
                .build();
        org.springframework.security.core.context.SecurityContextHolder.getContext()
                .setAuthentication(new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(
                        jwt, java.util.List.of()));
        try {
            jdbc.update("INSERT INTO customers(id,customer_code,full_name,phone,credit_limit,is_active) VALUES(930301,'KH-930301','Lê Minh Tâm','0988777666',0,true)");
            jdbc.update("INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer,is_active) VALUES(930301,930301,'51G-112.23','Mazda','CX-5',40000,true)");
            jdbc.update("INSERT INTO repair_orders(id,order_code,customer_id,vehicle_id,status,odometer_in,received_by) VALUES(930301,'RO-20261110-301',930301,930301,'COMPLETED',40000,930101)");
            jdbc.update("INSERT INTO vehicle_checks(id,repair_order_id,check_type,attempt_no,result,checklist_json,schema_version,checked_by,checked_at)"
                    + " VALUES(930301,930301,'QC',1,'PASSED',CAST('{}' AS JSON),1,930101,NOW())");
            jdbc.update("UPDATE repair_orders SET latest_qc_check_id = 930301 WHERE id = 930301");
            jdbc.update("INSERT INTO estimates(id,estimate_code,repair_order_id,version_no,estimate_kind,status,subtotal,discount_amount,tax_amount,total_amount,created_by,created_at,version)"
                    + " VALUES(930301,'EST-930301',930301,1,'INITIAL','APPROVED',500000,0,0,500000,930101,NOW(),0)");
            jdbc.update("INSERT INTO parts(id,sku,part_name,unit,purchase_price,selling_price,stock_quantity,reserved_quantity,reorder_level,warranty_days,warranty_km,is_active,created_at,updated_at,version)"
                    + " VALUES(930301,'SKU-930301','Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)','Can',400000,500000,10,1,2,0,0,true,NOW(),NOW(),0)");
            jdbc.update("INSERT INTO estimate_items(id,estimate_id,line_no,item_type,part_id,item_name,unit,quantity,unit_price,unit_cost,discount_amount,tax_amount,line_total,approval_status,reserved_quantity,warranty_days,warranty_km,created_at,version)"
                    + " VALUES(930301,930301,1,'PART',930301,'Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)','Can',1,500000,400000,0,0,500000,'APPROVED',1,0,0,NOW(),0)");

            var txTemplate = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            var readiness = txTemplate.execute(status -> {
                var response = service.getInvoiceReadiness(930301L);
                assertThat(status.isRollbackOnly()).isFalse();
                return response;
            });

            assertThat(readiness).isNotNull();
            assertThat(readiness.ready()).isFalse();
            assertThat(readiness.reason())
                    .contains("Còn phụ tùng đã giữ chỗ nhưng chưa xuất kho")
                    .contains("Dầu nhớt tổng hợp Motul 8100 X-cess 5W40 (Can 4L)");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}

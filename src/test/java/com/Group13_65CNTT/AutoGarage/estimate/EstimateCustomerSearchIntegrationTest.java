package com.Group13_65CNTT.AutoGarage.estimate;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.estimate.entity.Estimate;
import com.Group13_65CNTT.AutoGarage.estimate.repository.EstimateRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Estimate list scoped to one customer's orders, without drafts, against MySQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@RequiredArgsConstructor
class EstimateCustomerSearchIntegrationTest extends IntegrationTestSupport {
    private final JdbcTemplate jdbc;
    private final EstimateRepository estimates;

    @BeforeEach
    void fixtures() {
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(930701,'est_admin','x','Admin','ADMIN')");
        for (long id : new long[] {930701, 930702}) {
            jdbc.update("INSERT INTO customers(id,customer_code,full_name,phone,credit_limit,is_active) VALUES(?,?,?,?,0,true)",
                    id, "KH-EST-" + id, "Khach " + id, "09" + id);
            jdbc.update("INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer,is_active) VALUES(?,?,?,'Toyota','Vios',1000,true)",
                    id, id, "30E-" + id);
            jdbc.update("INSERT INTO repair_orders(id,order_code,customer_id,vehicle_id,status,odometer_in,received_by) VALUES(?,?,?,?,'WAITING_APPROVAL',1000,930701)",
                    id, "RO-EST-" + id, id, id);
        }
        estimate(930701, 930701, "SENT");
        estimate(930702, 930701, "DRAFT");
        estimate(930703, 930702, "SENT");
    }

    private void estimate(long id, long orderId, String status) {
        jdbc.update("INSERT INTO estimates(id,estimate_code,repair_order_id,version_no,estimate_kind,status,subtotal,discount_amount,"
                + "tax_amount,total_amount,created_by,created_at,version) VALUES(?,?,?,?,'INITIAL',?,0,0,0,0,930701,NOW(),0)",
                id, "EST-" + id, orderId, id - 930700, status);
    }

    @Test
    @DisplayName("Khách chỉ thấy báo giá đã gửi trên phiếu của mình; nhân viên thấy cả bản nháp")
    void customerScopeHidesOtherCustomersAndDrafts() {
        var page = PageRequest.of(0, 50);
        assertThat(estimates.searchEstimates(null, null, 930701L, true, page).getContent())
                .extracting(Estimate::getId).containsExactly(930701L);
        assertThat(estimates.searchEstimates(null, null, null, false, page).getContent())
                .extracting(Estimate::getId).contains(930701L, 930702L, 930703L);
    }
}

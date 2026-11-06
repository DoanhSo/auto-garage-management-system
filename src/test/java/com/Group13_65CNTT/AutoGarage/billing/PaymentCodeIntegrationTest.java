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
}

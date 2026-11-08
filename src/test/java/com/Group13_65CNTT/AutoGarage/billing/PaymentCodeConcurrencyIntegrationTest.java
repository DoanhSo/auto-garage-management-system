package com.Group13_65CNTT.AutoGarage.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Two overlapping transactions that take the payment-code lock, pick the next code and commit must end with
 * different codes: the second one waits for the first to commit instead of picking the same number.
 * Commits real rows (no test-level transaction), so it cleans up after itself.
 */
@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor
class PaymentCodeConcurrencyIntegrationTest extends IntegrationTestSupport {
    private static final String LOCK_SQL = "SELECT id FROM payment_code_lock WHERE id = 1 FOR UPDATE";
    private static final long USER_ID = 930201L;
    private static final long PERIOD_ID = 930201L;

    private final JdbcTemplate jdbc;
    private final BillingService service;
    private final PlatformTransactionManager transactionManager;

    @BeforeEach
    void fixtures() {
        cleanup();
        jdbc.update("INSERT INTO users(id,username,password_hash,full_name,role) VALUES(?,?,?,?,?)",
                USER_ID, "pay_race_admin", "test", "Admin", "ADMIN");
        jdbc.update("INSERT INTO payroll_periods(id,period_code,period_month,from_date,to_date)"
                + " VALUES(?,?,?,?,?)", PERIOD_ID, "PAYRACE_P", "2026-11", "2026-11-01", "2026-11-30");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM payments WHERE payroll_period_id = ?", PERIOD_ID);
        jdbc.update("DELETE FROM payroll_periods WHERE id = ?", PERIOD_ID);
        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    /** One payout transaction: lock, pick the suggested code, insert, optionally linger before committing. */
    private String payout(long holdMillis, CountDownLatch entered) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            if (entered != null) {
                entered.countDown();
            }
            jdbc.queryForObject(LOCK_SQL, Integer.class);
            String code = service.suggestTransactionCode(PaymentDirection.OUT);
            jdbc.update("INSERT INTO payments(transaction_code,direction,payroll_period_id,amount,payment_method,"
                    + "recorded_by) VALUES(?,?,?,?,?,?)", code, "OUT", PERIOD_ID, 1000, "CASH", USER_ID);
            sleep(holdMillis);
            return code;
        });
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    @DisplayName("Hai giao dịch chồng nhau: giao dịch sau chờ khóa, thấy mã đã commit và lấy số kế tiếp")
    void overlappingTransactionsGetDistinctCodes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<String> first = pool.submit(() -> payout(1500, null));
            sleep(300);
            Future<String> second = pool.submit(() -> payout(0, secondStarted));

            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            List<String> codes = List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));

            assertThat(codes).doesNotHaveDuplicates();
            assertThat(codes.get(0)).endsWith("0001");
            assertThat(codes.get(1)).endsWith("0002");
        } finally {
            pool.shutdownNow();
        }
    }
}

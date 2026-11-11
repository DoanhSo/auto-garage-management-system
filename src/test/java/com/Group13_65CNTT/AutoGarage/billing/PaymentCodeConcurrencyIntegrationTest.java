package com.Group13_65CNTT.AutoGarage.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.Group13_65CNTT.AutoGarage.IntegrationTestSupport;
import com.Group13_65CNTT.AutoGarage.billing.dto.request.PaymentReceiveRequest;
import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.billing.service.BillingService;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations;
import java.math.BigDecimal;
import java.util.ArrayList;
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
 * Overlapping payment transactions must end with different codes: each one waits for the previous holder of the
 * payment-code lock to commit. Commits real rows (no test-level transaction), so it cleans up after itself.
 */
@SpringBootTest
@ActiveProfiles("test")
@RequiredArgsConstructor
class PaymentCodeConcurrencyIntegrationTest extends IntegrationTestSupport {
    private static final String LOCK_SQL = "SELECT id FROM payment_code_lock WHERE id = 1 FOR UPDATE";
    private static final long USER_ID = 930201L;
    private static final long PERIOD_ID = 930201L;
    private static final long FIRST_FIXTURE_ID = 930211L;
    private static final int CONCURRENT_RECEIPTS = 6;
    /** Gives the second transaction time to reach the lock; only the strength of the test depends on it. */
    private static final long LOCK_WAIT_GRACE_MILLIS = 200;

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
        long last = FIRST_FIXTURE_ID + CONCURRENT_RECEIPTS - 1;
        jdbc.update("DELETE FROM payments WHERE payroll_period_id = ? OR invoice_id BETWEEN ? AND ?",
                PERIOD_ID, FIRST_FIXTURE_ID, last);
        jdbc.update("DELETE FROM invoices WHERE id BETWEEN ? AND ?", FIRST_FIXTURE_ID, last);
        jdbc.update("DELETE FROM repair_orders WHERE id BETWEEN ? AND ?", FIRST_FIXTURE_ID, last);
        jdbc.update("DELETE FROM vehicles WHERE id BETWEEN ? AND ?", FIRST_FIXTURE_ID, last);
        jdbc.update("DELETE FROM customers WHERE id BETWEEN ? AND ?", FIRST_FIXTURE_ID, last);
        jdbc.update("DELETE FROM payroll_periods WHERE id = ?", PERIOD_ID);
        jdbc.update("DELETE FROM users WHERE id = ?", USER_ID);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other transaction");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void graceForLockWait() {
        try {
            Thread.sleep(LOCK_WAIT_GRACE_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** One payout transaction: lock, pick the suggested code, insert, then run {@code beforeCommit}. */
    private String payout(Runnable beforeLock, Runnable beforeCommit) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            beforeLock.run();
            jdbc.queryForObject(LOCK_SQL, Integer.class);
            String code = service.suggestTransactionCode(PaymentDirection.OUT);
            jdbc.update("INSERT INTO payments(transaction_code,direction,payroll_period_id,amount,payment_method,"
                    + "recorded_by) VALUES(?,?,?,?,?,?)", code, "OUT", PERIOD_ID, 1000, "CASH", USER_ID);
            beforeCommit.run();
            return code;
        });
    }

    private static long sequenceOf(String code) {
        return Long.parseLong(code.substring(code.lastIndexOf('-') + 1));
    }

    @Test
    @DisplayName("Hai giao dịch chồng nhau: giao dịch sau chờ khóa, thấy mã đã commit và lấy số kế tiếp")
    void overlappingTransactionsGetConsecutiveCodes() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch firstHoldsLock = new CountDownLatch(1);
            CountDownLatch secondReachedLock = new CountDownLatch(1);
            Future<String> first = pool.submit(() -> payout(() -> { }, () -> {
                firstHoldsLock.countDown();
                await(secondReachedLock);
                graceForLockWait();
            }));
            await(firstHoldsLock);
            Future<String> second = pool.submit(() -> payout(secondReachedLock::countDown, () -> { }));

            String firstCode = first.get(30, TimeUnit.SECONDS);
            String secondCode = second.get(30, TimeUnit.SECONDS);

            assertThat(sequenceOf(secondCode)).isEqualTo(sequenceOf(firstCode) + 1);
        } finally {
            pool.shutdownNow();
        }
    }

    private void invoiceFixture(long id) {
        jdbc.update("INSERT INTO customers(id,customer_code,full_name,phone,credit_limit,is_active)"
                + " VALUES(?,?,?,?,0,true)", id, "KH-" + id, "Khach " + id, "09" + id);
        jdbc.update("INSERT INTO vehicles(id,customer_id,license_plate,brand,model,odometer,is_active)"
                + " VALUES(?,?,?,'Toyota','Vios',1000,true)", id, id, "30R-" + id);
        jdbc.update("INSERT INTO repair_orders(id,order_code,customer_id,vehicle_id,status,odometer_in,received_by)"
                + " VALUES(?,?,?,?,'INVOICED',1000,?)", id, "RO-RACE-" + id, id, id, USER_ID);
        jdbc.update("INSERT INTO invoices(id,invoice_code,repair_order_id,customer_id,status,customer_snapshot,"
                + "vehicle_snapshot,subtotal,total_amount,issued_by,issued_at)"
                + " VALUES(?,?,?,?,'UNPAID',CAST('{}' AS JSON),CAST('{}' AS JSON),500000,500000,?,NOW())",
                id, "INV-RACE-" + id, id, id, USER_ID);
    }

    @Test
    @DisplayName("Nhiều khoản thu đồng thời qua receivePayment trên các hóa đơn khác nhau đều thành công với mã khác nhau")
    void concurrentReceiptsAllSucceedWithDistinctCodes() throws Exception {
        for (int i = 0; i < CONCURRENT_RECEIPTS; i++) {
            invoiceFixture(FIRST_FIXTURE_ID + i);
        }
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_RECEIPTS);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> receipts = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_RECEIPTS; i++) {
                long invoiceId = FIRST_FIXTURE_ID + i;
                receipts.add(pool.submit(() -> {
                    await(start);
                    service.receivePayment(USER_ID, PaymentReceiveRequest.builder()
                            .invoiceId(invoiceId)
                            .amount(new BigDecimal("100000"))
                            .paymentMethod(BillingOperations.PaymentMethod.CASH)
                            .build());
                }));
            }
            start.countDown();
            for (Future<?> receipt : receipts) {
                receipt.get(60, TimeUnit.SECONDS);
            }

            List<String> codes = jdbc.queryForList(
                    "SELECT transaction_code FROM payments WHERE invoice_id BETWEEN ? AND ?", String.class,
                    FIRST_FIXTURE_ID, FIRST_FIXTURE_ID + CONCURRENT_RECEIPTS - 1);
            assertThat(codes).hasSize(CONCURRENT_RECEIPTS).doesNotHaveDuplicates();
        } finally {
            pool.shutdownNow();
        }
    }
}

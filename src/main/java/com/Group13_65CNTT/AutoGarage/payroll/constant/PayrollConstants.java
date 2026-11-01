package com.Group13_65CNTT.AutoGarage.payroll.constant;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

public final class PayrollConstants {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final LocalTime REGULAR_START = LocalTime.of(7, 30);
    public static final LocalTime REGULAR_END = LocalTime.of(17, 0);

    public static final String PERIOD_CODE_PREFIX = "PR-";
    public static final String PAYMENT_NOTE_PREFIX = "Chi trả lương kỳ ";

    public static final BigDecimal SECONDS_PER_HOUR = BigDecimal.valueOf(3600);
    public static final BigDecimal MAIN_SHARE_WITH_ASSISTANTS = new BigDecimal("0.70");
    public static final BigDecimal ASSISTANT_SHARE = new BigDecimal("0.30");
    public static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public static final BigDecimal QC_PASS_BONUS = BigDecimal.valueOf(30_000);
    public static final BigDecimal SMALL_ORDER_LABOR_LIMIT = BigDecimal.valueOf(2_000_000);
    public static final BigDecimal MEDIUM_ORDER_LABOR_LIMIT = BigDecimal.valueOf(5_000_000);
    public static final BigDecimal SMALL_ORDER_BONUS = BigDecimal.valueOf(100_000);
    public static final BigDecimal MEDIUM_ORDER_BONUS = BigDecimal.valueOf(200_000);
    public static final BigDecimal LARGE_ORDER_BONUS = BigDecimal.valueOf(300_000);

    public static final Set<String> PERIOD_SORT_FIELDS =
            Set.of("id", "periodMonth", "status", "totalNetPay", "createdAt");
    public static final Set<String> COMMISSION_SORT_FIELDS =
            Set.of("id", "earnedAt", "commissionAmount", "status", "createdAt");
    public static final Set<String> PAYSLIP_SORT_FIELDS = Set.of("id", "createdAt", "netPay");

    private PayrollConstants() {}
}

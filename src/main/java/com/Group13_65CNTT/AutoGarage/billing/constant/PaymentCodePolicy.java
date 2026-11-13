package com.Group13_65CNTT.AutoGarage.billing.constant;

import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.scheduling.constant.ShiftPolicy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Format of the system transaction code: {@code PT-yyyyMMdd-NNNN} for receipts, {@code PC-...} for payouts. */
public final class PaymentCodePolicy {
    public static final String RECEIPT_PREFIX = "PT";
    public static final String PAYOUT_PREFIX = "PC";
    public static final int SEQUENCE_WIDTH = 4;
    /** Longest transaction code a user may type (the column itself is wider). */
    public static final int MAX_LENGTH = 50;
    /** Letters, digits and - _ . only; empty is allowed and means "generate one". */
    public static final String ALLOWED_PATTERN = "[A-Za-z0-9._-]*";
    /** How many consecutive numbers are tried when a generated code is already taken. */
    public static final int GENERATION_ATTEMPTS = 5;
    /** How many of the highest codes of the day are read when looking for the last sequence number. */
    public static final int NUMBERING_LOOKBACK = 50;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private PaymentCodePolicy() {
    }

    public static String prefixFor(PaymentDirection direction) {
        return direction == PaymentDirection.IN ? RECEIPT_PREFIX : PAYOUT_PREFIX;
    }

    /** {@code PT-20261106-}: everything before the daily sequence number. */
    public static String dayPrefix(PaymentDirection direction, LocalDate day) {
        return prefixFor(direction) + "-" + DATE.format(day) + "-";
    }

    public static LocalDate today() {
        return LocalDate.now(ShiftPolicy.ZONE);
    }

    public static String format(String dayPrefix, long sequence) {
        return dayPrefix + String.format("%0" + SEQUENCE_WIDTH + "d", sequence);
    }
}

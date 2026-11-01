package com.Group13_65CNTT.AutoGarage.scheduling.constant;

import java.time.Duration;
import java.time.ZoneId;

/** Working-time limits and attendance tolerances applied when rostering technicians. */
public final class ShiftPolicy {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final long MAX_DAILY_HOURS = 12;
    public static final long MAX_WEEKLY_HOURS = 60;
    public static final long MIN_REST_HOURS = 8;
    public static final long MAX_TECHNICIANS_PER_BAY = 2;
    public static final Duration LATE_GRACE = Duration.ofMinutes(10);
    public static final Duration EARLY_CHECK_IN_WINDOW = Duration.ofMinutes(30);

    private ShiftPolicy() {}
}

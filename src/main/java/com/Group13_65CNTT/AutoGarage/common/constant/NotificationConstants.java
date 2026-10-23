package com.Group13_65CNTT.AutoGarage.common.constant;

import java.util.Set;

public final class NotificationConstants {
    public static final String CHANNEL_WEB = "WEB";
    public static final String CHANNEL_EMAIL = "EMAIL";
    public static final String CHANNEL_SMS = "SMS";
    public static final String CHANNEL_PATTERN = "WEB|EMAIL|SMS";
    public static final Set<String> SUPPORTED_CHANNELS =
            Set.of(CHANNEL_WEB, CHANNEL_EMAIL, CHANNEL_SMS);

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String DELIVERY_FAILED_MESSAGE =
            "Delivery failed; consult provider diagnostics";

    public static final String USER_RECIPIENT_PREFIX = "user:";
    public static final String CUSTOMER_RECIPIENT_PREFIX = "customer:";
    public static final String WEB_PROVIDER_PREFIX = "web:";
    public static final String SMTP_PROVIDER_PREFIX = "smtp:";
    public static final String SMS_IDEMPOTENCY_PREFIX = "notification:";

    public static final String EMAIL_DESTINATION_PATTERN = "[^\\s@]+@[^\\s@]+\\.[^\\s@]+";
    public static final String SMS_DESTINATION_PATTERN = "\\+[1-9][0-9]{7,14}";

    public static final int EVENT_KEY_MAX_LENGTH = 150;
    public static final int DESTINATION_MAX_LENGTH = 254;
    public static final int SUBJECT_MAX_LENGTH = 255;
    public static final int PAYLOAD_MAX_LENGTH = 16384;

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String APPLICATION_JSON = "application/json";

    public static final String SMS_FIELD_DESTINATION = "destination";
    public static final String SMS_FIELD_PAYLOAD = "payload";
    public static final String SMS_FIELD_IDEMPOTENCY_KEY = "idempotencyKey";

    private NotificationConstants() {}
}

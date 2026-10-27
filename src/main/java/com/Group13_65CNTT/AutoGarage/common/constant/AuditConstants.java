package com.Group13_65CNTT.AutoGarage.common.constant;

public final class AuditConstants {
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_NOTIFICATION = "NOTIFICATION";

    public static final String ACTION_ADMIN_BOOTSTRAPPED = "ADMIN_BOOTSTRAPPED";
    public static final String ACTION_AUTH_LOGOUT_ALL = "AUTH_LOGOUT_ALL";
    public static final String ACTION_CUSTOMER_JIT_PROVISIONED = "CUSTOMER_JIT_PROVISIONED";
    public static final String ACTION_USER_CREATED = "USER_CREATED";
    public static final String ACTION_USER_UPDATED = "USER_UPDATED";
    public static final String ACTION_USER_DELETED = "USER_DELETED";
    public static final String ACTION_NOTIFICATION_ENQUEUED = "NOTIFICATION_ENQUEUED";
    public static final String ACTION_NOTIFICATION_BROADCAST = "NOTIFICATION_BROADCAST";
    public static final String ACTION_NOTIFICATION_READ = "NOTIFICATION_READ";
    public static final String ACTION_NOTIFICATION_DELETED = "NOTIFICATION_DELETED";

    public static final String DETAIL_ROLE = "role";
    public static final String DETAIL_ACTIVE = "active";

    public static final int ACTION_MAX_LENGTH = 100;
    public static final int ENTITY_TYPE_MAX_LENGTH = 80;
    public static final int REQUEST_KEY_MAX_LENGTH = 100;
    public static final int REQUEST_HASH_LENGTH = 64;
    public static final String REQUEST_HASH_PATTERN = "[a-f0-9]{64}";
    public static final int RESULT_JSON_MAX_LENGTH = 65536;

    private AuditConstants() {}
}

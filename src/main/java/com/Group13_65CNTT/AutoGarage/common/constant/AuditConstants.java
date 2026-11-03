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

    public static final String ACTION_ATTACHMENT_UPLOADED = "ATTACHMENT_UPLOADED";
    public static final String ACTION_BOOKING_CANCELLED = "BOOKING_CANCELLED";
    public static final String ACTION_BOOKING_CONFIRMED = "BOOKING_CONFIRMED";
    public static final String ACTION_BOOKING_CREATED = "BOOKING_CREATED";
    public static final String ACTION_BOOKING_NO_SHOW = "BOOKING_NO_SHOW";
    public static final String ACTION_BOOKING_RESCHEDULED = "BOOKING_RESCHEDULED";
    public static final String ACTION_CUSTOMER_CREATED = "CUSTOMER_CREATED";
    public static final String ACTION_CUSTOMER_DEACTIVATED = "CUSTOMER_DEACTIVATED";
    public static final String ACTION_CUSTOMER_UPDATED = "CUSTOMER_UPDATED";
    public static final String ACTION_SERVICE_CREATED = "SERVICE_CREATED";
    public static final String ACTION_SERVICE_DEACTIVATED = "SERVICE_DEACTIVATED";
    public static final String ACTION_SERVICE_UPDATED = "SERVICE_UPDATED";
    public static final String ACTION_VEHICLE_CHECK_RECORDED = "VEHICLE_CHECK_RECORDED";
    public static final String ACTION_VEHICLE_CREATED = "VEHICLE_CREATED";
    public static final String ACTION_VEHICLE_DEACTIVATED = "VEHICLE_DEACTIVATED";
    public static final String ACTION_VEHICLE_RECEIVED = "VEHICLE_RECEIVED";
    public static final String ACTION_VEHICLE_TRANSFERRED = "VEHICLE_TRANSFERRED";
    public static final String ACTION_VEHICLE_UPDATED = "VEHICLE_UPDATED";
    public static final String ENTITY_ATTACHMENT = "ATTACHMENT";
    public static final String ENTITY_BOOKING = "BOOKING";
    public static final String ENTITY_CUSTOMER = "CUSTOMER";
    public static final String ENTITY_REPAIR_ORDER = "REPAIR_ORDER";
    public static final String ENTITY_SERVICE = "SERVICE";
    public static final String ENTITY_VEHICLE = "VEHICLE";
    public static final String ENTITY_VEHICLE_CHECK = "VEHICLE_CHECK";

    private AuditConstants() {}
}

package com.Group13_65CNTT.AutoGarage.common.constant;

public final class AuditConstants {
    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_NOTIFICATION = "NOTIFICATION";
    public static final String ENTITY_EMPLOYEE = "Employee";
    public static final String ENTITY_EMPLOYEE_SHIFT = "EmployeeShift";
    public static final String ENTITY_SERVICE_BAY = "ServiceBay";
    public static final String ENTITY_BAY_RESERVATION = "BayReservation";
    public static final String ENTITY_PAYROLL_PERIOD = "PayrollPeriod";
    public static final String ENTITY_PAYROLL_ITEM = "PayrollItem";
    public static final String ENTITY_ORDER_COMMISSION = "OrderCommission";
    public static final String ENTITY_REPAIR_ORDER = "RepairOrder";
    public static final String ENTITY_ESTIMATE = "Estimate";
    public static final String ENTITY_INVOICE = "Invoice";

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

    public static final String ACTION_CREATE_EMPLOYEE = "CREATE_EMPLOYEE";
    public static final String ACTION_UPDATE_EMPLOYEE = "UPDATE_EMPLOYEE";
    public static final String ACTION_DEACTIVATE_EMPLOYEE = "DEACTIVATE_EMPLOYEE";

    public static final String ACTION_CREATE_SHIFT = "CREATE_SHIFT";
    public static final String ACTION_CANCEL_SHIFT = "CANCEL_SHIFT";
    public static final String ACTION_REGISTER_SHIFT = "REGISTER_SHIFT";
    public static final String ACTION_APPROVE_SHIFT_REGISTRATION = "APPROVE_SHIFT_REGISTRATION";
    public static final String ACTION_REJECT_SHIFT_REGISTRATION = "REJECT_SHIFT_REGISTRATION";
    public static final String ACTION_SHIFT_CHECK_IN = "SHIFT_CHECK_IN";
    public static final String ACTION_SHIFT_CHECK_OUT = "SHIFT_CHECK_OUT";

    public static final String ACTION_CREATE_BAY = "CREATE_BAY";
    public static final String ACTION_UPDATE_BAY = "UPDATE_BAY";
    public static final String ACTION_DEACTIVATE_BAY = "DEACTIVATE_BAY";
    public static final String ACTION_SCHEDULE_MAINTENANCE = "SCHEDULE_MAINTENANCE";

    public static final String ACTION_CREATE_PAYROLL_PERIOD = "CREATE_PAYROLL_PERIOD";
    public static final String ACTION_CALCULATE_PAYROLL = "CALCULATE_PAYROLL";
    public static final String ACTION_ADJUST_PAYROLL_ITEM = "ADJUST_PAYROLL_ITEM";
    public static final String ACTION_APPROVE_PAYROLL = "APPROVE_PAYROLL";
    public static final String ACTION_DISBURSE_PAYROLL = "DISBURSE_PAYROLL";
    public static final String ACTION_CALCULATE_ORDER_COMMISSION = "CALCULATE_ORDER_COMMISSION";
    public static final String ACTION_UPDATE_COMMISSION_STATUS = "UPDATE_COMMISSION_STATUS";

    public static final String DETAIL_ROLE = "role";
    public static final String DETAIL_ACTIVE = "active";

    /** Separator between code and name in a human-readable entity label (e.g. "A1 - Cầu nâng"). */
    public static final String LABEL_CODE_SEPARATOR = " - ";

    public static final int ACTION_MAX_LENGTH = 100;
    public static final int ENTITY_TYPE_MAX_LENGTH = 80;
    public static final int REQUEST_KEY_MAX_LENGTH = 100;
    public static final int REQUEST_HASH_LENGTH = 64;
    public static final String REQUEST_HASH_PATTERN = "[a-f0-9]{64}";
    public static final int RESULT_JSON_MAX_LENGTH = 65536;

    private AuditConstants() {}
}

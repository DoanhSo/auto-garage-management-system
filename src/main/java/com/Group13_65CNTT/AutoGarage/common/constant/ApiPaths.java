package com.Group13_65CNTT.AutoGarage.common.constant;

public final class ApiPaths {
    public static final String UI_POLICY = "/api/v1/public/ui-policy";
    public static final String AUTH_BASE = "/api/v1/auth";
    public static final String AUTH_WILDCARD = AUTH_BASE + "/**";
    public static final String ME = "/me";
    public static final String LOGOUT = "/logout";

    public static final String USERS_BASE = "/api/v1/admin/users";
    public static final String USERS_WILDCARD = USERS_BASE + "/**";
    public static final String BY_ID = "/{id}";

    public static final String NOTIFICATIONS_BASE = "/api/v1/notifications";
    public static final String NOTIFICATIONS_WILDCARD = NOTIFICATIONS_BASE + "/**";
    public static final String NOTIFICATIONS_ADMIN = "/admin";
    public static final String NOTIFICATIONS_BROADCAST = "/broadcast";
    public static final String NOTIFICATION_READ = "/{id}/read";

    public static final String AUDIT_BASE = "/api/v1/admin/audit-events";
    public static final String AUDIT_WILDCARD = AUDIT_BASE + "/**";

    public static final String REPORTS_BASE = "/api/v1/reports";
    public static final String REPORTS_WILDCARD = REPORTS_BASE + "/**";
    public static final String REPORTS_DASHBOARD = "/dashboard";
    public static final String REPORTS_OUTSTANDING_INVOICES = "/outstanding-invoices";
    public static final String REPORTS_DASHBOARD_EXPORT = "/dashboard/export";

    public static final String CUSTOMERS_BASE = "/api/v1/customers";
    public static final String CUSTOMERS_WILDCARD = CUSTOMERS_BASE + "/**";
    public static final String VEHICLES_BASE = "/api/v1/vehicles";
    public static final String VEHICLES_WILDCARD = VEHICLES_BASE + "/**";
    public static final String SERVICES_BASE = "/api/v1/services";
    public static final String SERVICES_WILDCARD = SERVICES_BASE + "/**";
    public static final String BOOKINGS_BASE = "/api/v1/bookings";
    public static final String BOOKINGS_WILDCARD = BOOKINGS_BASE + "/**";
    public static final String RECEPTION_BASE = "/api/v1/reception";
    public static final String RECEPTION_WILDCARD = RECEPTION_BASE + "/**";
    public static final String VEHICLE_CHECKS_BASE = "/api/v1/vehicle-checks";
    public static final String VEHICLE_CHECKS_WILDCARD = VEHICLE_CHECKS_BASE + "/**";
    public static final String ATTACHMENTS_BASE = "/api/v1/attachments";
    public static final String ATTACHMENTS_WILDCARD = ATTACHMENTS_BASE + "/**";

    public static final String API_CORS_PATTERN = "/api/**";
    public static final String OPENAPI_DOCS_PATTERN = "/v3/api-docs/**";
    public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";

    private ApiPaths() {}
}

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
    public static final String NOTIFICATION_READ = "/{id}/read";

    public static final String AUDIT_BASE = "/api/v1/admin/audit-events";
    public static final String AUDIT_WILDCARD = AUDIT_BASE + "/**";

    public static final String REPORTS_BASE = "/api/v1/reports";
    public static final String REPORTS_WILDCARD = REPORTS_BASE + "/**";
    public static final String REPORTS_DASHBOARD = "/dashboard";
    public static final String REPORTS_OUTSTANDING_INVOICES = "/outstanding-invoices";
    public static final String REPORTS_DASHBOARD_EXPORT = "/dashboard/export";

    public static final String SUPPLIERS_BASE = "/api/v1/inventory/suppliers";
    public static final String SUPPLIERS_WILDCARD = SUPPLIERS_BASE + "/**";

    public static final String PARTS_BASE = "/api/v1/inventory/parts";
    public static final String PARTS_WILDCARD = PARTS_BASE + "/**";
    public static final String PARTS_LOW_STOCK = "/low-stock";

    public static final String INVENTORY_DOCUMENTS_BASE = "/api/v1/inventory/documents";
    public static final String INVENTORY_DOCUMENTS_WILDCARD = INVENTORY_DOCUMENTS_BASE + "/**";
    public static final String INVENTORY_DOCUMENT_POST = "/{id}/post";
    public static final String INVENTORY_DOCUMENT_CANCEL = "/{id}/cancel";

    public static final String INVENTORY_TRANSACTIONS_BASE = "/api/v1/inventory/transactions";
    public static final String INVENTORY_RECONCILIATION_BASE = "/api/v1/inventory/reconciliation";

    public static final String INVOICES_BASE = "/api/v1/billing/invoices";
    public static final String INVOICES_WILDCARD = INVOICES_BASE + "/**";
    public static final String INVOICES_ISSUE = "/issue";
    public static final String INVOICES_VOID = "/{id}/void";

    public static final String PAYMENTS_BASE = "/api/v1/billing/payments";
    public static final String PAYMENTS_WILDCARD = PAYMENTS_BASE + "/**";
    public static final String PAYMENTS_RECEIVE = "/receive";
    public static final String PAYMENTS_PAY_SUPPLIER = "/pay-supplier";

    public static final String DEBTS_BASE = "/api/v1/billing/debts";
    public static final String DEBTS_INVOICE_BALANCES = "/invoices";
    public static final String DEBTS_SUPPLIER_BALANCES = "/suppliers";

    public static final String API_CORS_PATTERN = "/api/**";
    public static final String OPENAPI_DOCS_PATTERN = "/v3/api-docs/**";
    public static final String SWAGGER_UI_PATTERN = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";

    public static final String EMPLOYEES_BASE = "/api/v1/employees";
    public static final String SHIFTS_BASE = "/api/v1/employee-shifts";
    public static final String SERVICE_BAYS_BASE = "/api/v1/service-bays";
    public static final String BAY_RESERVATIONS_BASE = "/api/v1/bay-reservations";

    public static final String REPAIR_ORDERS_BASE = "/api/v1/repair-orders";
    public static final String REPAIR_TASKS_BASE = "/api/v1/repair-tasks";
    public static final String ESTIMATES_BASE = "/api/v1/estimates";
    public static final String QUALITY_CHECKS_BASE = "/api/v1/quality-checks";
    public static final String WARRANTIES_BASE = "/api/v1/warranties";

    public static final String CUSTOMERS_BASE = "/api/v1/customers";
    public static final String VEHICLES_BASE = "/api/v1/vehicles";
    public static final String SERVICES_BASE = "/api/v1/services";
    public static final String BOOKINGS_BASE = "/api/v1/bookings";
    public static final String RECEPTION_BASE = "/api/v1/reception";
    public static final String VEHICLE_CHECKS_BASE = "/api/v1/vehicle-checks";
    public static final String ATTACHMENTS_BASE = "/api/v1/attachments";

    private ApiPaths() {}
}

package com.Group13_65CNTT.AutoGarage.common.constant;

public final class ReportConstants {
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String CSV_CONTENT_TYPE = "text/csv;charset=UTF-8";
    public static final String DASHBOARD_CSV_DISPOSITION =
            "attachment; filename=garage-dashboard.csv";

    public static final String CSV_HEADER = "metric,value\r\n";
    public static final String CSV_LINE_ENDING = "\r\n";
    public static final String METRIC_REVENUE = "revenue,";
    public static final String METRIC_CASH_IN = "cash_in,";
    public static final String METRIC_CASH_OUT = "cash_out,";
    public static final String METRIC_CUSTOMER_DEBT = "customer_debt,";
    public static final String METRIC_SUPPLIER_DEBT = "supplier_debt,";
    public static final String METRIC_STOCK_VALUE = "stock_value,";
    public static final String METRIC_LABOR_HOURS = "labor_hours,";

    private ReportConstants() {}
}

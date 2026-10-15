package com.Group13_65CNTT.AutoGarage.reporting.repository.projection;

import java.math.BigDecimal;

public interface DashboardTotals {
    BigDecimal getRevenue();

    BigDecimal getCashIn();

    BigDecimal getCashOut();

    BigDecimal getCustomerDebt();

    BigDecimal getSupplierDebt();

    BigDecimal getStockValue();

    BigDecimal getLaborHours();
}

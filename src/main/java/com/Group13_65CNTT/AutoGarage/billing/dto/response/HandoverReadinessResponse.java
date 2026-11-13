package com.Group13_65CNTT.AutoGarage.billing.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Whether the vehicle may be handed over given the invoice and the customer's credit limit.
 * {@code minimumPayment} is what must still be collected so the customer's debt no longer exceeds the limit
 * (the whole debt when the customer has no credit limit).
 */
public record HandoverReadinessResponse(
        boolean allowed,
        List<String> reasons,
        Long invoiceId,
        BigDecimal invoiceBalance,
        BigDecimal customerDebt,
        BigDecimal creditLimit,
        BigDecimal minimumPayment,
        List<String> overdueInvoiceCodes) {
}

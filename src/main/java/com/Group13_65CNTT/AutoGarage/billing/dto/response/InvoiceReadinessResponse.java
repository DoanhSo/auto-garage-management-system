package com.Group13_65CNTT.AutoGarage.billing.dto.response;

/** Whether an invoice can be issued for a repair order now, and the first blocking reason when not. */
public record InvoiceReadinessResponse(boolean ready, String reason) {
}

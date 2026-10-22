package com.Group13_65CNTT.AutoGarage.reporting.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceBalanceResponse {
    private Long id;
    private String invoiceCode;
    private Long customerId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
}

package com.Group13_65CNTT.AutoGarage.billing.dto.response;

import com.Group13_65CNTT.AutoGarage.billing.entity.InvoiceStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceBalanceResponse {
    private Long invoiceId;
    private String invoiceCode;
    private Long customerId;
    private Long repairOrderId;
    private Instant issuedAt;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
}

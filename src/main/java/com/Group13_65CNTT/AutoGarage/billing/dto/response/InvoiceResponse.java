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
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private Long id;
    private String invoiceCode;
    private Long repairOrderId;
    private Long customerId;
    private InvoiceStatus status;
    private String customerSnapshot;
    private String vehicleSnapshot;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private Long issuedBy;
    private Instant issuedAt;
    private LocalDate dueDate;
    private String voidReason;
    private Long voidedBy;
    private Instant voidedAt;
    private Long version;
    private List<InvoiceItemResponse> items;
}

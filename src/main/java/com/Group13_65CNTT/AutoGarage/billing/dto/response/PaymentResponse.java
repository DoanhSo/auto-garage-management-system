package com.Group13_65CNTT.AutoGarage.billing.dto.response;

import com.Group13_65CNTT.AutoGarage.billing.entity.PaymentDirection;
import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations.PaymentMethod;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String transactionCode;
    private PaymentDirection direction;
    private Long invoiceId;
    private Long inventoryDocumentId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String providerReference;
    private Instant paidAt;
    private Long recordedBy;
    private String notes;
}

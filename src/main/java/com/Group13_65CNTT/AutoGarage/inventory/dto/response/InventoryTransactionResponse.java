package com.Group13_65CNTT.AutoGarage.inventory.dto.response;

import com.Group13_65CNTT.AutoGarage.inventory.entity.TransactionType;

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
public class InventoryTransactionResponse {
    private Long id;
    private Long documentItemId;
    private Long documentId;
    private String documentCode;
    private Long partId;
    private String partSku;
    private String partName;
    private TransactionType transactionType;
    private BigDecimal quantityChange;
    private BigDecimal stockAfter;
    private BigDecimal unitCost;
    private Long createdBy;
    private Instant createdAt;
}

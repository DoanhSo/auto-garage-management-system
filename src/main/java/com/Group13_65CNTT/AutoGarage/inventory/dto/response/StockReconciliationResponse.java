package com.Group13_65CNTT.AutoGarage.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReconciliationResponse {
    private Long partId;
    private String sku;
    private String partName;
    private BigDecimal stockQuantity;
    private BigDecimal ledgerQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal discrepancy;
    private boolean matched;
}

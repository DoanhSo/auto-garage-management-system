package com.Group13_65CNTT.AutoGarage.inventory.dto.response;

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
public class RepairOrderPartResponse {
    private Long id;
    private Long repairOrderId;
    private Long estimateItemId;
    private Long partId;
    private String partSku;
    private String partName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal unitCost;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}

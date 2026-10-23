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
public class InventoryDocumentItemResponse {
    private Long id;
    private Long documentId;
    private Integer lineNo;
    private Long partId;
    private String partSku;
    private String partName;
    private String unit;
    private Long estimateItemId;
    private Long repairOrderPartId;
    private Long sourceItemId;
    private BigDecimal quantity;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;
    private String notes;
}

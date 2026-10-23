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
public class PartResponse {
    private Long id;
    private String sku;
    private String partName;
    private String unit;
    private String description;
    private Long preferredSupplierId;
    private String preferredSupplierName;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private BigDecimal stockQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;
    private BigDecimal reorderLevel;
    private Integer warrantyDays;
    private Integer warrantyKm;
    private String warrantyTerms;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}

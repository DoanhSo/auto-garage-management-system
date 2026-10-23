package com.Group13_65CNTT.AutoGarage.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

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
public class InventoryDocumentItemRequest {
    @NotNull(message = "ID phụ tùng không được để trống")
    private Long partId;

    private Long estimateItemId;

    private Long repairOrderPartId;

    private Long sourceItemId;

    @NotNull(message = "Số lượng không được null")
    private BigDecimal quantity;

    @NotNull(message = "Đơn giá vốn không được null")
    @DecimalMin(value = "0.0", inclusive = true, message = "Đơn giá vốn không được âm")
    private BigDecimal unitCost;

    private String notes;
}

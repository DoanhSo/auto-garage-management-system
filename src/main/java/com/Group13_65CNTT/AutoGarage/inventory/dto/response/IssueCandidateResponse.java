package com.Group13_65CNTT.AutoGarage.inventory.dto.response;

import java.math.BigDecimal;

/** An approved, reserved part of a repair order that is still waiting to be issued from stock. */
public record IssueCandidateResponse(
        Long estimateItemId,
        Long partId,
        String partCode,
        String partName,
        String unit,
        BigDecimal reservedQuantity,
        BigDecimal stockQuantity,
        BigDecimal unitCost,
        boolean shortage) {
}

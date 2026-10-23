package com.Group13_65CNTT.AutoGarage.billing.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceIssueRequest {
    @NotNull(message = "ID phiếu sửa chữa không được để trống")
    private Long repairOrderId;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền giảm giá không được âm")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", inclusive = true, message = "Số tiền thuế không được âm")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    private LocalDate dueDate;

    private String requestKey;
}

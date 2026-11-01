package com.Group13_65CNTT.AutoGarage.payroll.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
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
public class PayrollItemAdjustRequest {

    @DecimalMin(value = "0.0", message = "Phụ cấp không được âm")
    private BigDecimal allowanceAmount;

    @DecimalMin(value = "0.0", message = "Khoản giảm trừ không được âm")
    private BigDecimal deductionAmount;

    @Size(max = 2000, message = "Ghi chú không vượt quá 2000 ký tự")
    private String notes;
}

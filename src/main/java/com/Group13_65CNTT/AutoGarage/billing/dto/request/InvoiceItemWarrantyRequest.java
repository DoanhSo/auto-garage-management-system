package com.Group13_65CNTT.AutoGarage.billing.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Warranty policy added to an issued invoice line before the vehicle is handed over. Zero days and km clear it. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceItemWarrantyRequest {
    @NotNull(message = "Số ngày bảo hành không được để trống")
    @Min(value = 0, message = "Số ngày bảo hành phải >= 0")
    @Max(value = 3650, message = "Số ngày bảo hành tối đa 3650 ngày")
    private Integer warrantyDays;

    @NotNull(message = "Số km bảo hành không được để trống")
    @Min(value = 0, message = "Số km bảo hành phải >= 0")
    @Max(value = 1000000, message = "Số km bảo hành tối đa 1.000.000 km")
    private Integer warrantyKm;

    @Size(max = 1000, message = "Điều khoản bảo hành tối đa 1000 ký tự")
    private String warrantyTerms;
}

package com.Group13_65CNTT.AutoGarage.warranty.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyAdjustRequest {

    @NotNull(message = "Số ngày bảo hành không được để trống")
    @Min(value = 0, message = "Số ngày bảo hành phải >= 0")
    private Integer warrantyDays;

    @NotNull(message = "Số km bảo hành không được để trống")
    @Min(value = 0, message = "Số km bảo hành phải >= 0")
    private Integer warrantyKm;

    @Size(max = 2000, message = "Điều khoản bảo hành tối đa 2000 ký tự")
    private String termsSnapshot;

    @NotBlank(message = "Lý do điều chỉnh không được để trống")
    @Size(max = 500, message = "Lý do điều chỉnh tối đa 500 ký tự")
    private String reason;
}

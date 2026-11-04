package com.Group13_65CNTT.AutoGarage.warranty.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyVoidRequest {

    @NotBlank(message = "Lý do hủy sổ bảo hành không được để trống")
    @Size(max = 500, message = "Lý do hủy sổ tối đa 500 ký tự")
    private String reason;
}

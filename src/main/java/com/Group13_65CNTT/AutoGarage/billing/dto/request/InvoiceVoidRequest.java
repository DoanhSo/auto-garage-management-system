package com.Group13_65CNTT.AutoGarage.billing.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceVoidRequest {
    @NotBlank(message = "Lý do hủy hóa đơn không được để trống")
    @Size(max = 500, message = "Lý do hủy tối đa 500 ký tự")
    private String voidReason;
}

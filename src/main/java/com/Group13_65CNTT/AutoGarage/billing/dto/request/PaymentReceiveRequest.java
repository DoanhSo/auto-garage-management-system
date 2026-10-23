package com.Group13_65CNTT.AutoGarage.billing.dto.request;

import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class PaymentReceiveRequest {
    @NotNull(message = "ID hóa đơn không được để trống")
    private Long invoiceId;

    @NotNull(message = "Số tiền thanh toán không được null")
    @DecimalMin(value = "0.01", inclusive = true, message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Size(max = 150, message = "Mã tham chiếu giao dịch tối đa 150 ký tự")
    private String providerReference;

    private String notes;

    private String requestKey;
}

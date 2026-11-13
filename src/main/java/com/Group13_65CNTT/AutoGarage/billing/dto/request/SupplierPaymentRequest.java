package com.Group13_65CNTT.AutoGarage.billing.dto.request;

import com.Group13_65CNTT.AutoGarage.billing.constant.PaymentCodePolicy;

import com.Group13_65CNTT.AutoGarage.shared.contracts.BillingOperations.PaymentMethod;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class SupplierPaymentRequest {
    @NotNull(message = "ID chứng từ nhập kho không được để trống")
    private Long inventoryDocumentId;

    @NotNull(message = "Số tiền thanh toán không được null")
    @DecimalMin(value = "0.01", inclusive = true, message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal amount;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Size(max = 150, message = "Mã tham chiếu giao dịch tối đa 150 ký tự")
    private String providerReference;
    /** Optional, editable system transaction code; generated when empty. */
    @Size(max = PaymentCodePolicy.MAX_LENGTH, message = "Mã giao dịch tối đa 50 ký tự")
    @Pattern(regexp = PaymentCodePolicy.ALLOWED_PATTERN,
            message = "Mã giao dịch chỉ gồm chữ, số và các ký tự - _ .")
    private String transactionCode;

    private String notes;

    private String requestKey;
}

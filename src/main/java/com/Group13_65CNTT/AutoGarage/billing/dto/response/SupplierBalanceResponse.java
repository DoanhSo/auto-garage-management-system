package com.Group13_65CNTT.AutoGarage.billing.dto.response;

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
public class SupplierBalanceResponse {
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private BigDecimal purchaseAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
}

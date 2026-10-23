package com.Group13_65CNTT.AutoGarage.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

@Entity
@Table(name = "v_supplier_balances")
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierBalanceView {
    @Id
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_code")
    private String supplierCode;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "purchase_amount")
    private BigDecimal purchaseAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "balance_amount")
    private BigDecimal balanceAmount;
}

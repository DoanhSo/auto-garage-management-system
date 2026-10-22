package com.Group13_65CNTT.AutoGarage.reporting.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/** Read-only view, not a mutable audited business table. */
@Entity
@Immutable
@Table(name = "v_invoice_balances")
@Getter
@NoArgsConstructor
public class InvoiceBalance {
    @Id
    @Column(name = "invoice_id")
    private Long id;

    @Column(name = "invoice_code", length = 30)
    private String invoiceCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "repair_order_id")
    private Long repairOrderId;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "total_amount", precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 18, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "balance_amount", precision = 18, scale = 2)
    private BigDecimal balanceAmount;
}

package com.Group13_65CNTT.AutoGarage.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "v_invoice_balances")
@Immutable
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceBalanceView {
    @Id
    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_code")
    private String invoiceCode;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "repair_order_id")
    private Long repairOrderId;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status")
    private InvoiceStatus status;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "balance_amount")
    private BigDecimal balanceAmount;
}

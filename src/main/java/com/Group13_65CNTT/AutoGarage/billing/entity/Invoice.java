package com.Group13_65CNTT.AutoGarage.billing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 30)
    private String invoiceCode;

    @Column(name = "repair_order_id", nullable = false, unique = true)
    private Long repairOrderId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "UNPAID";

    @Column(name = "customer_snapshot", nullable = false, columnDefinition = "JSON")
    private String customerSnapshot;

    @Column(name = "vehicle_snapshot", nullable = false, columnDefinition = "JSON")
    private String vehicleSnapshot;

    @Column(name = "subtotal", nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "issued_by", nullable = false)
    private Long issuedBy;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (this.issuedAt == null) {
            this.issuedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = "UNPAID";
        }
    }
}

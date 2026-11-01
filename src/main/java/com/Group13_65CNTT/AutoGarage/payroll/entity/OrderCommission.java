package com.Group13_65CNTT.AutoGarage.payroll.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_commissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repair_order_id", nullable = false)
    private Long repairOrderId;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_type", nullable = false, length = 30)
    private CommissionType commissionType;

    @Builder.Default
    @Column(name = "base_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal ratePercent = BigDecimal.ZERO;

    @Column(name = "commission_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal commissionAmount;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommissionStatus status = CommissionStatus.PENDING;

    @Column(name = "earned_at", nullable = false)
    private Instant earnedAt;

    @Column(name = "payroll_item_id")
    private Long payrollItemId;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = CommissionStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

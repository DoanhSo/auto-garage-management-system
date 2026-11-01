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
@Table(name = "payroll_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_period_id", nullable = false)
    private Long payrollPeriodId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Builder.Default
    @Column(name = "base_salary_snapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseSalarySnapshot = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "hourly_rate_snapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal hourlyRateSnapshot = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "regular_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal regularHours = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "overtime_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "shift_count", nullable = false)
    private Integer shiftCount = 0;

    @Builder.Default
    @Column(name = "completed_order_count", nullable = false)
    private Integer completedOrderCount = 0;

    @Builder.Default
    @Column(name = "hourly_pay_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal hourlyPayAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "commission_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "order_bonus_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal orderBonusAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "allowance_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal allowanceAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deduction_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal deductionAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "gross_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "net_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollItemStatus status = PayrollItemStatus.CALCULATED;

    @Column(name = "paid_at")
    private Instant paidAt;

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
            status = PayrollItemStatus.CALCULATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

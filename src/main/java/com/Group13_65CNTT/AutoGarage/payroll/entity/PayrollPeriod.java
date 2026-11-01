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
import java.time.LocalDate;

@Entity
@Table(name = "payroll_periods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_code", nullable = false, unique = true, length = 50)
    private String periodCode;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollPeriodStatus status = PayrollPeriodStatus.DRAFT;

    @Builder.Default
    @Column(name = "total_employees", nullable = false)
    private Integer totalEmployees = 0;

    @Builder.Default
    @Column(name = "total_regular_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalRegularHours = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_overtime_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalOvertimeHours = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_base_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBasePay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_commissions", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCommissions = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_allowances", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAllowances = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_deductions", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "total_net_pay", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalNetPay = BigDecimal.ZERO;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "payment_id")
    private Long paymentId;

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
            status = PayrollPeriodStatus.DRAFT;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

package com.Group13_65CNTT.AutoGarage.repair.entity;

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

/** Audit trail of who worked on which vehicle, in which shift and at which stage. */
@Entity
@Table(name = "repair_order_assignments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairOrderAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repair_order_id", nullable = false)
    private Long repairOrderId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "shift_id")
    private Long shiftId;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_stage", nullable = false, length = 30)
    private AssignmentStage assignmentStage;

    @Column(name = "task_id")
    private Long taskId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(name = "actual_seconds", nullable = false)
    private Long actualSeconds = 0L;

    @Builder.Default
    @Column(name = "hourly_rate_snapshot", nullable = false, precision = 18, scale = 2)
    private BigDecimal hourlyRateSnapshot = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "earned_commission", nullable = false, precision = 18, scale = 2)
    private BigDecimal earnedCommission = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 20)
    private WorkResultStatus resultStatus = WorkResultStatus.IN_PROGRESS;

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
        if (startedAt == null) {
            startedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

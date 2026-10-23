package com.Group13_65CNTT.AutoGarage.repair.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "labor_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "stop_reason", length = 20)
    private StopReason stopReason;

    @Column(name = "hourly_cost_snapshot", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal hourlyCostSnapshot = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.hourlyCostSnapshot == null) {
            this.hourlyCostSnapshot = BigDecimal.ZERO;
        }
    }
}

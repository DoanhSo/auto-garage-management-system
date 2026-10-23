package com.Group13_65CNTT.AutoGarage.repair.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "repair_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @Column(name = "booking_id", unique = true)
    private Long bookingId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    @Builder.Default
    private OrderType orderType = OrderType.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RepairOrderStatus status = RepairOrderStatus.NEW;

    @Column(name = "odometer_in", nullable = false)
    private Integer odometerIn;

    @Column(name = "odometer_out")
    private Integer odometerOut;

    @Column(name = "fuel_level_percent", precision = 5, scale = 2)
    private BigDecimal fuelLevelPercent;

    @Column(name = "customer_complaint", columnDefinition = "TEXT")
    private String customerComplaint;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "received_by", nullable = false)
    private Long receivedBy;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "latest_qc_check_id")
    private Long latestQcCheckId;

    @Column(name = "handed_over_by")
    private Long handedOverBy;

    @Column(name = "handed_over_at")
    private Instant handedOverAt;

    @Column(name = "closed_at")
    private Instant closedAt;

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
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.receivedAt == null) {
            this.receivedAt = now;
        }
        if (this.orderType == null) {
            this.orderType = OrderType.NORMAL;
        }
        if (this.status == null) {
            this.status = RepairOrderStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

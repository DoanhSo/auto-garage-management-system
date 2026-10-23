package com.Group13_65CNTT.AutoGarage.warranty.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warranty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warranty_code", nullable = false, unique = true, length = 30)
    private String warrantyCode;

    @Column(name = "invoice_item_id", nullable = false, unique = true)
    private Long invoiceItemId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "start_odometer", nullable = false)
    private Integer startOdometer;

    @Column(name = "end_odometer")
    private Integer endOdometer;

    @Column(name = "warranty_days", nullable = false)
    @Builder.Default
    private Integer warrantyDays = 0;

    @Column(name = "warranty_km", nullable = false)
    @Builder.Default
    private Integer warrantyKm = 0;

    @Column(name = "terms_snapshot", columnDefinition = "TEXT")
    private String termsSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WarrantyStatus status = WarrantyStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.status == null) {
            this.status = WarrantyStatus.ACTIVE;
        }
        if (this.warrantyDays == null) {
            this.warrantyDays = 0;
        }
        if (this.warrantyKm == null) {
            this.warrantyKm = 0;
        }
    }
}

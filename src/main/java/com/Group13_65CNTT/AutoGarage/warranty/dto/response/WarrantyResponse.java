package com.Group13_65CNTT.AutoGarage.warranty.dto.response;

import com.Group13_65CNTT.AutoGarage.warranty.entity.WarrantyStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyResponse {

    private Long id;
    private String warrantyCode;
    private Long invoiceItemId;
    private Long vehicleId;
    private Long customerId;
    private Instant startsAt;
    private Instant expiresAt;
    private Integer startOdometer;
    private Integer endOdometer;
    private Integer warrantyDays;
    private Integer warrantyKm;
    private String termsSnapshot;
    private WarrantyStatus status;
    private Instant createdAt;
}

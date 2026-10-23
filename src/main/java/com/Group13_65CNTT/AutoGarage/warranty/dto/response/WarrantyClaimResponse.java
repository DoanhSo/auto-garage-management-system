package com.Group13_65CNTT.AutoGarage.warranty.dto.response;

import com.Group13_65CNTT.AutoGarage.warranty.entity.ClaimStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarrantyClaimResponse {

    private Long id;
    private String claimCode;
    private Long warrantyId;
    private Long requestedBy;
    private Instant requestedAt;
    private Integer currentOdometer;
    private String description;
    private ClaimStatus status;
    private Long decidedBy;
    private Instant decidedAt;
    private String decisionReason;
    private Long handlingOrderId;
    private Instant closedAt;
    private Long version;
}

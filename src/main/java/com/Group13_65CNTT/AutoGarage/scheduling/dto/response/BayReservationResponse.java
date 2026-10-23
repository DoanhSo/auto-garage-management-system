package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ReservationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BayReservationResponse {
    private Long id;
    private Long bayId;
    private Long bookingId;
    private Long repairOrderId;
    private ReservationType reservationType;
    private ReservationStatus status;
    private Instant startsAt;
    private Instant endsAt;
    private Instant expiresAt;
    private Instant releasedAt;
    private String notes;
    private Long createdBy;
    private Instant createdAt;
    private Long version;
}

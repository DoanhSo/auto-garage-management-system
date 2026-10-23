package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class MaintenanceReservationRequest {

    @NotNull(message = "Bay ID is required")
    private Long bayId;

    @NotNull(message = "Starts at time is required")
    private Instant startsAt;

    @NotNull(message = "Ends at time is required")
    private Instant endsAt;

    private String notes;
}

package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A technician's own request for a working shift, pending manager approval. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRegisterRequest {

    @NotNull(message = "Starts at time is required")
    private Instant startsAt;

    @NotNull(message = "Ends at time is required")
    private Instant endsAt;

    private ShiftType shiftType;

    /** Optional preferred bay; empty for a floating technician. */
    private Long serviceBayId;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

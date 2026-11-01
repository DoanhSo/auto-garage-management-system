package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;
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
public class ShiftCreateRequest {

    @NotNull(message = "Employee ID is required")
    private Long employeeId;

    @NotNull(message = "Starts at time is required")
    private Instant startsAt;

    @NotNull(message = "Ends at time is required")
    private Instant endsAt;

    private ShiftType shiftType;

    /** Optional: null means a floating technician who can be dispatched to any bay. */
    private Long serviceBayId;

    private String notes;
}

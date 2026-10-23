package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
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
public class ShiftResponse {
    private Long id;
    private Long employeeId;
    private Instant startsAt;
    private Instant endsAt;
    private ShiftStatus status;
    private String notes;
    private Long createdBy;
    private Instant createdAt;
    private Long version;
}

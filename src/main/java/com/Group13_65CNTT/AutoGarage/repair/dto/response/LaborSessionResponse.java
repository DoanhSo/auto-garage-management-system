package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import com.Group13_65CNTT.AutoGarage.repair.entity.StopReason;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborSessionResponse {

    private Long id;
    private Long assignmentId;
    private Long employeeId;
    private Instant startsAt;
    private Instant endsAt;
    private StopReason stopReason;
    private BigDecimal hourlyCostSnapshot;
    private Long durationSeconds;
    private Instant createdAt;
}

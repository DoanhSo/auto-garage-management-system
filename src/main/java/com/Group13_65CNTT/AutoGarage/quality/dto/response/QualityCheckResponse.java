package com.Group13_65CNTT.AutoGarage.quality.dto.response;

import com.Group13_65CNTT.AutoGarage.reception.entity.CheckResult;
import com.Group13_65CNTT.AutoGarage.reception.entity.CheckType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QualityCheckResponse {

    private Long id;
    private Long repairOrderId;
    private CheckType checkType;
    private Integer attemptNo;
    private CheckResult result;
    private String checklistJson;
    private Integer schemaVersion;
    private Integer odometer;
    private String notes;
    private Long checkedBy;
    private Instant checkedAt;
}

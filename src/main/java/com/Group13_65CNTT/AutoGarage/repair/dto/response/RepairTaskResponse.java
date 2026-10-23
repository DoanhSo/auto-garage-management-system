package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RepairTaskResponse {

    private Long id;
    private Long repairOrderId;
    private Long estimateItemId;
    private Long serviceId;
    private TaskType taskType;
    private String taskName;
    private String description;
    private TaskStatus status;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private Instant startedAt;
    private Instant completedAt;
    private String completionNote;
    private Instant createdAt;
    private Long version;
}

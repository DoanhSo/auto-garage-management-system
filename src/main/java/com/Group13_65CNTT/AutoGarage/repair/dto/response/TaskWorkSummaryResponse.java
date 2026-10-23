package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import com.Group13_65CNTT.AutoGarage.repair.entity.TaskStatus;
import com.Group13_65CNTT.AutoGarage.repair.entity.TaskType;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskWorkSummaryResponse {

    private Long taskId;
    private Long repairOrderId;
    private TaskType taskType;
    private String taskName;
    private TaskStatus status;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private Instant startedAt;
    private Instant completedAt;
    private String completionNote;
    private Long totalActualSeconds;
    private BigDecimal totalLaborCost;
    private List<TaskAssignmentResponse> assignments;
    private List<LaborSessionResponse> sessions;
}

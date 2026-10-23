package com.Group13_65CNTT.AutoGarage.repair.dto.response;

import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentResponse {

    private Long id;
    private Long taskId;
    private Long employeeId;
    private String employeeName;
    private AssignmentRole assignmentRole;
    private Boolean isActive;
    private Instant startsAt;
    private Instant endsAt;
    private Long assignedBy;
    private Instant assignedAt;
    private Instant releasedAt;
    private Long version;
}

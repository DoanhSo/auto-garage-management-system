package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import com.Group13_65CNTT.AutoGarage.repair.entity.AssignmentRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskAssignmentRequest {

    @NotNull(message = "ID nhân viên không được để trống")
    private Long employeeId;

    @NotNull(message = "Vai trò phân công (MAIN, ASSISTANT, QC) không được để trống")
    private AssignmentRole assignmentRole;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private Instant startsAt;

    @NotNull(message = "Thời gian dự kiến kết thúc không được để trống")
    private Instant endsAt;
}

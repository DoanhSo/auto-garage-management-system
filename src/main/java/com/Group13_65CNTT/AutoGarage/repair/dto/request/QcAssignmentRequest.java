package com.Group13_65CNTT.AutoGarage.repair.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QcAssignmentRequest {

    @NotNull(message = "ID nhân viên không được để trống")
    private Long employeeId;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    private Instant startsAt;

    @NotNull(message = "Thời gian dự kiến kết thúc không được để trống")
    private Instant endsAt;
}

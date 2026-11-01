package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import com.Group13_65CNTT.AutoGarage.scheduling.entity.AttendanceStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftStatus;
import com.Group13_65CNTT.AutoGarage.scheduling.entity.ShiftType;
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
    private ShiftType shiftType;
    private Long serviceBayId;
    private Instant actualStartsAt;
    private Instant actualEndsAt;
    private AttendanceStatus attendanceStatus;
    private ShiftStatus status;
    private String notes;
    private Instant registeredAt;
    private Long approvedBy;
    private String rejectionReason;
    private Long createdBy;
    private Instant createdAt;
    private Long version;
}

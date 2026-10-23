package com.Group13_65CNTT.AutoGarage.scheduling.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private Long id;
    private Long userId;
    private String employeeCode;
    private String skillsJson;
    private Boolean canQc;
    private BigDecimal hourlyCost;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}

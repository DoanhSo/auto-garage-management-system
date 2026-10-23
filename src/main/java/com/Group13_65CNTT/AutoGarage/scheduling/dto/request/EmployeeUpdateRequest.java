package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeUpdateRequest {

    private String skillsJson;

    private Boolean canQc;

    @DecimalMin(value = "0.0", message = "Hourly cost must not be negative")
    private BigDecimal hourlyCost;

    private Boolean isActive;
}

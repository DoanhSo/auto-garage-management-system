package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EmployeeCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Employee code is required")
    @Size(max = 30, message = "Employee code must not exceed 30 characters")
    private String employeeCode;

    private String skillsJson;

    @Builder.Default
    private Boolean canQc = false;

    @NotNull(message = "Hourly cost is required")
    @DecimalMin(value = "0.0", message = "Hourly cost must not be negative")
    private BigDecimal hourlyCost;

    @DecimalMin(value = "0.0", message = "Base salary must not be negative")
    private BigDecimal baseSalary;

    @DecimalMin(value = "1.0", message = "Overtime multiplier must be at least 1")
    private BigDecimal overtimeMultiplier;

    @DecimalMin(value = "0.0", message = "Commission rate must not be negative")
    @DecimalMax(value = "100.0", message = "Commission rate must not exceed 100")
    private BigDecimal defaultCommissionRate;
}

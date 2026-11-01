package com.Group13_65CNTT.AutoGarage.scheduling.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
public class EmployeeUpdateRequest {

    private String skillsJson;

    private java.time.LocalDate joinedAt;

    @Size(max = 50, message = "Bank name must not exceed 50 characters")
    private String bankName;

    @Size(max = 30, message = "Bank account number must not exceed 30 characters")
    private String bankAccountNumber;

    @Size(max = 100, message = "Bank account holder must not exceed 100 characters")
    private String bankAccountHolder;


    private Boolean canQc;

    @DecimalMin(value = "0.0", message = "Hourly cost must not be negative")
    private BigDecimal hourlyCost;

    @DecimalMin(value = "0.0", message = "Base salary must not be negative")
    private BigDecimal baseSalary;

    @DecimalMin(value = "0.5", message = "Salary coefficient must be at least 0.5")
    @DecimalMax(value = "3.0", message = "Salary coefficient must not exceed 3.0")
    private BigDecimal salaryCoefficient;

    @DecimalMin(value = "1.0", message = "Overtime multiplier must be at least 1")
    private BigDecimal overtimeMultiplier;

    @DecimalMin(value = "0.0", message = "Commission rate must not be negative")
    @DecimalMax(value = "100.0", message = "Commission rate must not exceed 100")
    private BigDecimal defaultCommissionRate;

    private Boolean isActive;
}

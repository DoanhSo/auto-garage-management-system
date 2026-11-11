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
    /** Display identity copied from the linked account so staff screens need no admin-only user lookup. */
    private String fullName;
    private String username;
    private String email;
    private java.time.LocalDate joinedAt;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountHolder;
    private String skillsJson;
    private Boolean canQc;
    private BigDecimal hourlyCost;
    private BigDecimal baseSalary;
    private BigDecimal salaryCoefficient;
    private BigDecimal overtimeMultiplier;
    private BigDecimal defaultCommissionRate;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    private com.Group13_65CNTT.AutoGarage.identity.dto.response.AccountSummary account;
}

package com.Group13_65CNTT.AutoGarage.reporting.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal revenue;
    private BigDecimal cashIn;
    private BigDecimal cashOut;
    private BigDecimal customerDebt;
    private BigDecimal supplierDebt;
    private BigDecimal stockValue;
    private BigDecimal laborHours;
    private Instant from;
    private Instant until;
    private Instant snapshotAt;
}

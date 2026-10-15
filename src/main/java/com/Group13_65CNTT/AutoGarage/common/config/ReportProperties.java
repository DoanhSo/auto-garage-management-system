package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("garage.report")
public class ReportProperties {
    @NotNull private Duration maxPeriod = Duration.ofDays(366);

    @AssertTrue(message = "Maximum report period must be positive")
    public boolean isMaxPeriodValid() {
        return maxPeriod != null && !maxPeriod.isZero() && !maxPeriod.isNegative();
    }
}

package com.Group13_65CNTT.AutoGarage.common.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("garage.pagination")
public class PaginationProperties {
    @Min(1)
    @Max(1000)
    private int maxSize = 100;

    @Min(1)
    private int defaultSize = 20;

    @Min(0)
    private int maxPage = 100000;

    @AssertTrue(message = "Default page size must not exceed maximum page size")
    public boolean isDefaultSizeValid() {
        return defaultSize <= maxSize;
    }
}
